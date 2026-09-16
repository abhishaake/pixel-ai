# Uploaded Image Moderation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reject nude and sexually explicit reference-image uploads before they reach Ideogram, GoEnhance, S3, or the user's credit balance, and record every rejection as evidence for the Google Play appeal.

**Architecture:** A self-contained `ImageModerationService` takes image bytes and returns a verdict using AWS Rekognition `DetectModerationLabels`. It never throws — infrastructure failures come back as a `failure` flag so the caller has one code path. `GenerationsServiceImpl` calls a guard helper at the very top of both multipart entry points, before the distributed lock is taken, so a rejection costs the user nothing and never reaches a third party.

**Tech Stack:** Java 21, Spring Boot 3.4.4, AWS SDK v2 (`rekognition` 2.31.21), MongoDB via Spring Data, Thumbnailator 0.4.20, JUnit 5 + Mockito + AssertJ.

**Spec:** `docs/superpowers/specs/2026-09-16-upload-image-moderation-design.md`

## Global Constraints

- **Java 21 is required.** Maven picks up the system default, which is JDK 22 on this machine and will fail the build. Every `mvn` command in this plan must run in a shell that has first exported:
  ```bash
  export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
  ```
- AWS SDK v2 modules are pinned to **2.31.21**, matching the existing `s3` and `ses` dependencies. Do not let Maven resolve a different version.
- Follow the existing code style: Lombok `@Data` + `@Accessors(chain = true)` on DAOs and DTOs, `@Service` + `@Slf4j` + `@AllArgsConstructor` on services, package root `com.av.pixel`.
- Tests use `@ExtendWith(MockitoExtension.class)` with AssertJ assertions, matching `ImagePrivacyServiceImplTest`.
- The user-facing rejection message is exactly: `This image can't be used. Please upload a different photo.`
- Never log or return the specific moderation label to the end user.
- Default configuration: `moderation.enabled=true`, `moderation.fail-open=false`.
- `moderation.blocked-labels` default value, verbatim:
  `Explicit:60,Non-Explicit Nudity:75,Obstructed Intimate Parts:75,Swimwear or Underwear:80`
- `Kissing on the Lips` must never be blocked. It is the core feature of the app.
- Do not commit `src/main/resources/service-account-key.json` or the `.DS_Store` files that are currently untracked in the working tree. Stage only the files each task names.

## File Structure

| File | Responsibility |
|---|---|
| `pom.xml` | Adds the `rekognition` dependency |
| `config/RekognitionConfig.java` | Builds the `RekognitionClient` bean from existing AWS credentials |
| `dto/ModerationResult.java` | Immutable verdict: allowed, failure, labels |
| `service/ModerationPolicy.java` | Parses `moderation.blocked-labels` and decides whether a set of returned labels is blocked. Pure logic, no AWS types |
| `service/ImageModerationService.java` | The interface the rest of the app depends on |
| `service/impl/ImageModerationServiceImpl.java` | Normalises bytes, calls Rekognition, applies the policy, handles failure |
| `enums/ModerationSourceEnum.java` | Which endpoint the upload came from |
| `enums/ModerationDecisionEnum.java` | Why the upload was refused |
| `dao/UploadModerationLog.java` | The audit document |
| `repository/UploadModerationLogRepository.java` | Its repository |
| `service/impl/GenerationsServiceImpl.java` | The `assertUploadIsSafe` guard and its two call sites |
| `src/main/resources/application.properties` | The `moderation.*` and `aws.rekognition.region` keys |

`ModerationPolicy` is split out from `ImageModerationServiceImpl` deliberately: the label-matching rules are the part most likely to be tuned under appeal pressure, and keeping them free of AWS SDK types means they can be tested without mocking anything.

---

### Task 1: Rekognition dependency and client bean

**Files:**
- Modify: `pom.xml:104` (after the `ses` dependency block)
- Create: `src/main/java/com/av/pixel/config/RekognitionConfig.java`
- Modify: `src/main/resources/application.properties`

**Interfaces:**
- Consumes: nothing.
- Produces: a Spring bean of type `software.amazon.awssdk.services.rekognition.RekognitionClient`, and the properties `moderation.enabled`, `moderation.fail-open`, `moderation.blocked-labels`, `aws.rekognition.region`, all consumed by Tasks 2 and 3.

This task has no behaviour to test, so its gate is that the project compiles with the new dependency resolved and the config class present.

- [ ] **Step 1: Add the Rekognition dependency**

In `pom.xml`, immediately after the closing `</dependency>` of the `ses` block (around line 104), add:

```xml
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>rekognition</artifactId>
            <version>2.31.21</version>
        </dependency>
```

- [ ] **Step 2: Verify the dependency resolves**

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
mvn -q dependency:get -Dartifact=software.amazon.awssdk:rekognition:2.31.21
```

Expected: BUILD SUCCESS. If Maven cannot resolve it offline, run the same command without `-q` to see the repository it is reaching for.

- [ ] **Step 3: Create the client bean**

Create `src/main/java/com/av/pixel/config/RekognitionConfig.java`. This mirrors `S3Config` exactly, including the `StaticCredentialsProvider` pattern:

```java
package com.av.pixel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;

@Configuration
public class RekognitionConfig {

    @Value("${aws.access-key}")
    private String accessKey;

    @Value("${aws.secret-key}")
    private String secretKey;

    @Value("${aws.rekognition.region:${aws.region}}")
    private String region;

    @Bean
    public RekognitionClient rekognitionClient() {
        return RekognitionClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }
}
```

The `${aws.rekognition.region:${aws.region}}` form means the property is optional and falls back to the S3 region.

- [ ] **Step 4: Add the configuration properties**

Append to `src/main/resources/application.properties`:

```properties

# Uploaded image moderation (AWS Rekognition DetectModerationLabels)
# Rejects nude / sexually explicit reference-image uploads before generation.
# Required for Google Play Sexual Content and AI-Generated Content policy compliance.
moderation.enabled=true
moderation.fail-open=false
moderation.blocked-labels=Explicit:60,Non-Explicit Nudity:75,Obstructed Intimate Parts:75,Swimwear or Underwear:80
# Optional. Defaults to aws.region when unset.
#aws.rekognition.region=ap-south-1
```

- [ ] **Step 5: Verify the project compiles**

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
mvn -q clean compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add pom.xml src/main/java/com/av/pixel/config/RekognitionConfig.java src/main/resources/application.properties
git commit -m "feat: add AWS Rekognition client and moderation config"
```

---

### Task 2: Moderation policy

**Files:**
- Create: `src/main/java/com/av/pixel/service/ModerationPolicy.java`
- Test: `src/test/java/com/av/pixel/service/ModerationPolicyTest.java`

**Interfaces:**
- Consumes: the `moderation.blocked-labels` property from Task 1.
- Produces, used by Task 3:
  - `ModerationPolicy(String blockedLabelsProperty)` — a constructor taking the raw property string, so tests can build one without Spring.
  - `double lowestThreshold()` — returns the smallest configured confidence, or `100.0d` when the blocklist is empty. Task 3 passes this to Rekognition as `MinConfidence`.
  - `Optional<String> firstBlocked(List<DetectedLabel> labels)` — returns the name of the highest-confidence label that violates the policy, or empty.
  - `record DetectedLabel(String name, String parentName, double confidence)` — a nested record, deliberately free of AWS SDK types so the policy can be tested without mocks.

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/com/av/pixel/service/ModerationPolicyTest.java`:

```java
package com.av.pixel.service;

import com.av.pixel.service.ModerationPolicy.DetectedLabel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ModerationPolicyTest {

    private static final String DEFAULT_BLOCKLIST =
            "Explicit:60,Non-Explicit Nudity:75,Obstructed Intimate Parts:75,Swimwear or Underwear:80";

    private final ModerationPolicy policy = new ModerationPolicy(DEFAULT_BLOCKLIST);

    @Test
    void blocksExplicitNudityAboveThreshold() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Exposed Female Nipple", "Explicit Nudity", 94.2d),
                new DetectedLabel("Explicit Nudity", "Explicit", 94.2d)));

        assertThat(blocked).contains("Explicit Nudity");
    }

    @Test
    void allowsBlockedLabelBelowItsThreshold() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Non-Explicit Nudity", "Non-Explicit Nudity of Intimate parts and Kissing", 70.0d)));

        assertThat(blocked).isEmpty();
    }

    @Test
    void allowsKissingOnTheLipsAtHighConfidence() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Kissing on the Lips", "Non-Explicit Nudity of Intimate parts and Kissing", 99.0d)));

        assertThat(blocked).isEmpty();
    }

    @Test
    void blocksSwimwearAtOrAboveEighty() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Female Swimwear Or Underwear", "Swimwear or Underwear", 85.0d)));

        assertThat(blocked).contains("Female Swimwear Or Underwear");
    }

    @Test
    void matchesOnParentNameWhenChildIsNotListed() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Sex Toys", "Explicit", 88.0d)));

        assertThat(blocked).contains("Sex Toys");
    }

    @Test
    void allowsUnrelatedCategories() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Alcoholic Beverages", "Alcohol", 99.0d),
                new DetectedLabel("Weapon Violence", "Violence", 97.0d)));

        assertThat(blocked).isEmpty();
    }

    @Test
    void reportsHighestConfidenceViolationFirst() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Non-Explicit Nudity", "Non-Explicit Nudity of Intimate parts and Kissing", 80.0d),
                new DetectedLabel("Explicit Nudity", "Explicit", 96.0d)));

        assertThat(blocked).contains("Explicit Nudity");
    }

    @Test
    void lowestThresholdIsTheSmallestConfiguredConfidence() {
        assertThat(policy.lowestThreshold()).isEqualTo(60.0d);
    }

    @Test
    void toleratesWhitespaceAndSkipsMalformedEntries() {
        ModerationPolicy messy = new ModerationPolicy("  Explicit : 60 , garbage , Violence:notanumber , Swimwear or Underwear:80 ");

        assertThat(messy.lowestThreshold()).isEqualTo(60.0d);
        assertThat(messy.firstBlocked(List.of(new DetectedLabel("Explicit Nudity", "Explicit", 61.0d)))).contains("Explicit Nudity");
        assertThat(messy.firstBlocked(List.of(new DetectedLabel("Weapon Violence", "Violence", 99.0d)))).isEmpty();
    }

    @Test
    void emptyBlocklistBlocksNothingAndReportsMaximumThreshold() {
        ModerationPolicy empty = new ModerationPolicy("");

        assertThat(empty.lowestThreshold()).isEqualTo(100.0d);
        assertThat(empty.firstBlocked(List.of(new DetectedLabel("Explicit Nudity", "Explicit", 99.0d)))).isEmpty();
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
mvn -q test -Dtest=ModerationPolicyTest
```

Expected: COMPILATION ERROR — `cannot find symbol: class ModerationPolicy`.

- [ ] **Step 3: Write the implementation**

Create `src/main/java/com/av/pixel/service/ModerationPolicy.java`:

```java
package com.av.pixel.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Decides whether Rekognition's moderation labels violate the upload policy.
 *
 * <p>Deliberately free of AWS SDK types so the rules — the part most likely to be
 * retuned while the app is under Play review — can be tested without mocks.
 */
@Slf4j
public class ModerationPolicy {

    /** Returned by {@link #lowestThreshold()} when nothing is blocked, so Rekognition returns as little as possible. */
    private static final double NO_BLOCKLIST_THRESHOLD = 100.0d;

    private final Map<String, Double> thresholdsByLabel;

    public ModerationPolicy(String blockedLabelsProperty) {
        this.thresholdsByLabel = parse(blockedLabelsProperty);
        log.info("moderation policy loaded with {} blocked labels: {}", thresholdsByLabel.size(), thresholdsByLabel);
    }

    private static Map<String, Double> parse(String property) {
        Map<String, Double> parsed = new LinkedHashMap<>();
        if (StringUtils.isBlank(property)) {
            return parsed;
        }
        for (String entry : property.split(",")) {
            String[] parts = entry.split(":");
            if (parts.length != 2) {
                log.warn("ignoring malformed moderation.blocked-labels entry: {}", entry);
                continue;
            }
            String label = parts[0].trim();
            try {
                parsed.put(label.toLowerCase(), Double.parseDouble(parts[1].trim()));
            } catch (NumberFormatException e) {
                log.warn("ignoring moderation.blocked-labels entry with non-numeric threshold: {}", entry);
            }
        }
        return parsed;
    }

    /**
     * The smallest configured confidence, suitable as Rekognition's MinConfidence so
     * that every label we might act on is returned and nothing else is.
     */
    public double lowestThreshold() {
        return thresholdsByLabel.values().stream()
                .min(Double::compareTo)
                .orElse(NO_BLOCKLIST_THRESHOLD);
    }

    /**
     * @return the name of the highest-confidence label that violates the policy, or
     *         empty when every label is acceptable.
     */
    public Optional<String> firstBlocked(List<DetectedLabel> labels) {
        if (labels == null || labels.isEmpty()) {
            return Optional.empty();
        }
        return labels.stream()
                .filter(this::isBlocked)
                .max(Comparator.comparingDouble(DetectedLabel::confidence))
                .map(DetectedLabel::name);
    }

    private boolean isBlocked(DetectedLabel label) {
        return exceedsThreshold(label.name(), label.confidence())
                || exceedsThreshold(label.parentName(), label.confidence());
    }

    private boolean exceedsThreshold(String name, double confidence) {
        if (StringUtils.isBlank(name)) {
            return false;
        }
        Double threshold = thresholdsByLabel.get(name.toLowerCase());
        return threshold != null && confidence >= threshold;
    }

    /** One moderation label, decoupled from the AWS SDK's ModerationLabel type. */
    public record DetectedLabel(String name, String parentName, double confidence) {
    }
}
```

Matching is case-insensitive so a taxonomy casing change at AWS cannot silently disable a rule.

- [ ] **Step 4: Run the tests to verify they pass**

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
mvn -q test -Dtest=ModerationPolicyTest
```

Expected: PASS, 10 tests.

If `org.apache.commons.lang3.StringUtils` does not resolve, it is already used elsewhere in this codebase (`GenerationsServiceImpl` imports it) and comes in transitively; confirm with `grep -rn "org.apache.commons.lang3" src/main/java | head -1` before adding any dependency.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/av/pixel/service/ModerationPolicy.java src/test/java/com/av/pixel/service/ModerationPolicyTest.java
git commit -m "feat: add moderation label policy with configurable blocklist"
```

---

### Task 3: Image moderation service

**Files:**
- Create: `src/main/java/com/av/pixel/dto/ModerationResult.java`
- Create: `src/main/java/com/av/pixel/service/ImageModerationService.java`
- Create: `src/main/java/com/av/pixel/service/impl/ImageModerationServiceImpl.java`
- Test: `src/test/java/com/av/pixel/service/impl/ImageModerationServiceImplTest.java`

**Interfaces:**
- Consumes: `ModerationPolicy` and its nested `DetectedLabel` record from Task 2; the `RekognitionClient` bean and `moderation.*` properties from Task 1.
- Produces, used by Task 4:
  - `ImageModerationService.moderate(byte[] imageBytes)` returning `ModerationResult`.
  - `ModerationResult` with getters `isAllowed()`, `isFailure()`, `getTopLabel()`, `getTopConfidence()`, `getLabels()`, and the static factories `allowed()`, `rejected(String topLabel, double topConfidence, List<String> labels)`, and `failure(boolean allowed)`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/com/av/pixel/service/impl/ImageModerationServiceImplTest.java`:

```java
package com.av.pixel.service.impl;

import com.av.pixel.dto.ModerationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.ModerationLabel;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImageModerationServiceImplTest {

    private static final String DEFAULT_BLOCKLIST =
            "Explicit:60,Non-Explicit Nudity:75,Obstructed Intimate Parts:75,Swimwear or Underwear:80";

    @Mock private RekognitionClient rekognitionClient;

    private byte[] image;

    @BeforeEach
    void setUp() throws IOException {
        image = pngOf(200, 200);
    }

    private ImageModerationServiceImpl service(boolean enabled, boolean failOpen) {
        return new ImageModerationServiceImpl(rekognitionClient, enabled, failOpen, DEFAULT_BLOCKLIST);
    }

    private static byte[] pngOf(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", out);
        return out.toByteArray();
    }

    private void respondWith(ModerationLabel... labels) {
        when(rekognitionClient.detectModerationLabels(any(DetectModerationLabelsRequest.class)))
                .thenReturn(DetectModerationLabelsResponse.builder().moderationLabels(labels).build());
    }

    private static ModerationLabel label(String name, String parent, float confidence) {
        return ModerationLabel.builder().name(name).parentName(parent).confidence(confidence).build();
    }

    @Test
    void rejectsExplicitNudity() {
        respondWith(label("Explicit Nudity", "Explicit", 94.2f));

        ModerationResult result = service(true, false).moderate(image);

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.isFailure()).isFalse();
        assertThat(result.getTopLabel()).isEqualTo("Explicit Nudity");
        assertThat(result.getTopConfidence()).isCloseTo(94.2d, org.assertj.core.data.Offset.offset(0.01d));
        assertThat(result.getLabels()).containsExactly("Explicit Nudity:94.2");
    }

    @Test
    void allowsKissingOnTheLips() {
        respondWith(label("Kissing on the Lips", "Non-Explicit Nudity of Intimate parts and Kissing", 99.0f));

        ModerationResult result = service(true, false).moderate(image);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.isFailure()).isFalse();
    }

    @Test
    void rejectsSwimwear() {
        respondWith(label("Female Swimwear Or Underwear", "Swimwear or Underwear", 85.0f));

        ModerationResult result = service(true, false).moderate(image);

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getTopLabel()).isEqualTo("Female Swimwear Or Underwear");
    }

    @Test
    void allowsWhenNoLabelsReturned() {
        respondWith();

        assertThat(service(true, false).moderate(image).isAllowed()).isTrue();
    }

    @Test
    void sendsLowestThresholdAsMinConfidence() {
        respondWith();

        service(true, false).moderate(image);

        ArgumentCaptor<DetectModerationLabelsRequest> captor =
                ArgumentCaptor.forClass(DetectModerationLabelsRequest.class);
        verify(rekognitionClient).detectModerationLabels(captor.capture());
        assertThat(captor.getValue().minConfidence()).isEqualTo(60.0f);
    }

    @Test
    void skipsRekognitionEntirelyWhenDisabled() {
        ModerationResult result = service(false, false).moderate(image);

        assertThat(result.isAllowed()).isTrue();
        verify(rekognitionClient, never()).detectModerationLabels(any(DetectModerationLabelsRequest.class));
    }

    @Test
    void failsClosedWhenRekognitionThrows() {
        when(rekognitionClient.detectModerationLabels(any(DetectModerationLabelsRequest.class)))
                .thenThrow(SdkClientException.create("boom"));

        ModerationResult result = service(true, false).moderate(image);

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void failsOpenWhenConfigured() {
        when(rekognitionClient.detectModerationLabels(any(DetectModerationLabelsRequest.class)))
                .thenThrow(SdkClientException.create("boom"));

        ModerationResult result = service(true, true).moderate(image);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void treatsNullOrEmptyBytesAsAFailure() {
        ModerationResult result = service(true, false).moderate(new byte[0]);

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.isFailure()).isTrue();
        verify(rekognitionClient, never()).detectModerationLabels(any(DetectModerationLabelsRequest.class));
    }

    @Test
    void treatsUndecodableBytesAsAFailure() {
        ModerationResult result = service(true, false).moderate("not an image".getBytes());

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void downscalesOversizedImagesBeforeCallingRekognition() throws IOException {
        respondWith();
        byte[] huge = pngOf(6000, 6000);
        assertThat(huge.length).isGreaterThan(5 * 1024 * 1024);

        service(true, false).moderate(huge);

        ArgumentCaptor<DetectModerationLabelsRequest> captor =
                ArgumentCaptor.forClass(DetectModerationLabelsRequest.class);
        verify(rekognitionClient).detectModerationLabels(captor.capture());
        assertThat(captor.getValue().image().bytes().asByteArray().length).isLessThanOrEqualTo(5 * 1024 * 1024);
    }
}
```

The `downscalesOversizedImagesBeforeCallingRekognition` test builds a real 6000x6000 PNG. A blank `TYPE_INT_RGB` image of that size compresses well, so if the assertion `isGreaterThan(5MB)` fails, switch `pngOf` to fill the image with random pixel data before writing.

- [ ] **Step 2: Run the tests to verify they fail**

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
mvn -q test -Dtest=ImageModerationServiceImplTest
```

Expected: COMPILATION ERROR — `cannot find symbol: class ImageModerationServiceImpl`.

- [ ] **Step 3: Create the result type**

Create `src/main/java/com/av/pixel/dto/ModerationResult.java`:

```java
package com.av.pixel.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * The verdict on one uploaded image.
 *
 * <p>{@code failure} distinguishes "the moderator said no" from "the moderator could
 * not be reached". When {@code failure} is true, {@code allowed} reflects the
 * configured fail-open setting rather than any judgement about the image.
 */
@Getter
@AllArgsConstructor
public class ModerationResult {

    private final boolean allowed;
    private final boolean failure;
    private final String topLabel;
    private final Double topConfidence;
    private final List<String> labels;

    public static ModerationResult allowed() {
        return new ModerationResult(true, false, null, null, List.of());
    }

    public static ModerationResult rejected(String topLabel, double topConfidence, List<String> labels) {
        return new ModerationResult(false, false, topLabel, topConfidence, labels);
    }

    public static ModerationResult failure(boolean allowed) {
        return new ModerationResult(allowed, true, null, null, List.of());
    }
}
```

- [ ] **Step 4: Create the interface**

Create `src/main/java/com/av/pixel/service/ImageModerationService.java`:

```java
package com.av.pixel.service;

import com.av.pixel.dto.ModerationResult;

public interface ImageModerationService {

    /**
     * Inspects an uploaded image for restricted content.
     *
     * <p>Never throws. A policy rejection and an infrastructure failure are both
     * ordinary return values, so callers have exactly one path to handle and a
     * provider outage can never surface as an unhandled error that skips the check.
     */
    ModerationResult moderate(byte[] imageBytes);
}
```

- [ ] **Step 5: Write the implementation**

Create `src/main/java/com/av/pixel/service/impl/ImageModerationServiceImpl.java`:

```java
package com.av.pixel.service.impl;

import com.av.pixel.dto.ModerationResult;
import com.av.pixel.service.ImageModerationService;
import com.av.pixel.service.ModerationPolicy;
import com.av.pixel.service.ModerationPolicy.DetectedLabel;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Slf4j
public class ImageModerationServiceImpl implements ImageModerationService {

    /** Rekognition rejects inline image bytes larger than 5 MB. */
    private static final int MAX_BYTES = 5 * 1024 * 1024;

    /** Rekognition rejects images smaller than 80x80. */
    private static final int MIN_DIMENSION = 80;

    /** Longest edge to downscale to when the payload is over MAX_BYTES. */
    private static final int DOWNSCALE_EDGE = 1600;

    private final RekognitionClient rekognitionClient;
    private final boolean enabled;
    private final boolean failOpen;
    private final ModerationPolicy policy;

    public ImageModerationServiceImpl(
            RekognitionClient rekognitionClient,
            @Value("${moderation.enabled:true}") boolean enabled,
            @Value("${moderation.fail-open:false}") boolean failOpen,
            @Value("${moderation.blocked-labels:}") String blockedLabels) {
        this.rekognitionClient = rekognitionClient;
        this.enabled = enabled;
        this.failOpen = failOpen;
        this.policy = new ModerationPolicy(blockedLabels);
        log.info("image moderation enabled={} failOpen={}", enabled, failOpen);
    }

    @Override
    public ModerationResult moderate(byte[] imageBytes) {
        if (!enabled) {
            return ModerationResult.allowed();
        }
        if (imageBytes == null || imageBytes.length == 0) {
            log.error("[CRITICAL] moderation received empty image bytes");
            return ModerationResult.failure(failOpen);
        }
        try {
            byte[] normalised = normaliseForRekognition(imageBytes);

            DetectModerationLabelsResponse response = rekognitionClient.detectModerationLabels(
                    DetectModerationLabelsRequest.builder()
                            .image(Image.builder().bytes(SdkBytes.fromByteArray(normalised)).build())
                            .minConfidence((float) policy.lowestThreshold())
                            .build());

            List<DetectedLabel> detected = response.moderationLabels().stream()
                    .map(l -> new DetectedLabel(l.name(), l.parentName(), l.confidence()))
                    .toList();

            Optional<String> blocked = policy.firstBlocked(detected);
            if (blocked.isEmpty()) {
                return ModerationResult.allowed();
            }

            String topLabel = blocked.get();
            double topConfidence = detected.stream()
                    .filter(l -> topLabel.equals(l.name()))
                    .mapToDouble(DetectedLabel::confidence)
                    .max()
                    .orElse(0d);

            return ModerationResult.rejected(topLabel, topConfidence, describe(detected));
        } catch (Exception e) {
            log.error("[CRITICAL] image moderation failed, failOpen={}", failOpen, e);
            return ModerationResult.failure(failOpen);
        }
    }

    private static List<String> describe(List<DetectedLabel> detected) {
        return detected.stream()
                .map(l -> String.format(Locale.ROOT, "%s:%.1f", l.name(), l.confidence()))
                .toList();
    }

    /**
     * Rekognition accepts only JPEG and PNG, at most 5 MB inline, at least 80x80.
     * Re-encode and downscale so that an unusual or oversized upload is actually
     * checked rather than erroring into a fail-closed rejection.
     */
    private static byte[] normaliseForRekognition(byte[] imageBytes) throws Exception {
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (decoded == null) {
            throw new IllegalArgumentException("upload is not a decodable image");
        }
        if (decoded.getWidth() < MIN_DIMENSION || decoded.getHeight() < MIN_DIMENSION) {
            throw new IllegalArgumentException("upload is smaller than Rekognition's 80x80 minimum");
        }
        if (imageBytes.length <= MAX_BYTES) {
            return imageBytes;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(decoded)
                .size(DOWNSCALE_EDGE, DOWNSCALE_EDGE)
                .keepAspectRatio(true)
                .outputFormat("JPEG")
                .outputQuality(0.85f)
                .toOutputStream(out);
        byte[] resized = out.toByteArray();
        log.info("downscaled upload for moderation: {} bytes -> {} bytes", imageBytes.length, resized.length);
        return resized;
    }
}
```

Note the `normaliseForRekognition` short-circuit: an image already under 5 MB is passed through untouched, so the common path does no re-encoding work.

- [ ] **Step 6: Run the tests to verify they pass**

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
mvn -q test -Dtest=ImageModerationServiceImplTest
```

Expected: PASS, 11 tests.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/av/pixel/dto/ModerationResult.java \
        src/main/java/com/av/pixel/service/ImageModerationService.java \
        src/main/java/com/av/pixel/service/impl/ImageModerationServiceImpl.java \
        src/test/java/com/av/pixel/service/impl/ImageModerationServiceImplTest.java
git commit -m "feat: add Rekognition-backed image moderation service"
```

---

### Task 4: Audit log and generation flow integration

**Files:**
- Create: `src/main/java/com/av/pixel/enums/ModerationSourceEnum.java`
- Create: `src/main/java/com/av/pixel/enums/ModerationDecisionEnum.java`
- Create: `src/main/java/com/av/pixel/dao/UploadModerationLog.java`
- Create: `src/main/java/com/av/pixel/repository/UploadModerationLogRepository.java`
- Modify: `src/main/java/com/av/pixel/service/impl/GenerationsServiceImpl.java` — add two fields, the guard helper, and two call sites

**Interfaces:**
- Consumes: `ImageModerationService.moderate(byte[])` and `ModerationResult` from Task 3.
- Produces: the enforced guard. Nothing later depends on it.

This is the task that actually closes the hole. The two call sites must go **before `locker.tryLock()`** — that placement is what makes a rejection free for the user.

- [ ] **Step 1: Create the enums**

Create `src/main/java/com/av/pixel/enums/ModerationSourceEnum.java`:

```java
package com.av.pixel.enums;

public enum ModerationSourceEnum {
    IMAGE_GENERATION,
    VIDEO_EFFECT
}
```

Create `src/main/java/com/av/pixel/enums/ModerationDecisionEnum.java`:

```java
package com.av.pixel.enums;

public enum ModerationDecisionEnum {
    /** The image violated the content policy. */
    REJECTED,
    /** The image could not be checked and was refused under fail-closed. */
    ERROR
}
```

- [ ] **Step 2: Create the audit document and repository**

Create `src/main/java/com/av/pixel/dao/UploadModerationLog.java`, following the `ImageFlag` pattern:

```java
package com.av.pixel.dao;

import com.av.pixel.dao.base.BaseEntity;
import com.av.pixel.enums.ModerationDecisionEnum;
import com.av.pixel.enums.ModerationSourceEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * One refused upload. Only rejections are stored, which keeps write volume near zero
 * while still producing the evidence the Google Play appeal needs.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "upload_moderation_logs")
@Accessors(chain = true)
public class UploadModerationLog extends BaseEntity {

    private String userCode;
    private ModerationSourceEnum source;
    private ModerationDecisionEnum decision;
    private String topLabel;
    private Double topConfidence;
    private List<String> labels;
    private String contentType;
    private long sizeBytes;
}
```

Create `src/main/java/com/av/pixel/repository/UploadModerationLogRepository.java`:

```java
package com.av.pixel.repository;

import com.av.pixel.dao.UploadModerationLog;
import com.av.pixel.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UploadModerationLogRepository extends BaseRepository<UploadModerationLog, String> {
}
```

- [ ] **Step 3: Add the dependencies to GenerationsServiceImpl**

In `src/main/java/com/av/pixel/service/impl/GenerationsServiceImpl.java`, add to the imports:

```java
import com.av.pixel.dao.UploadModerationLog;
import com.av.pixel.dto.ModerationResult;
import com.av.pixel.enums.ModerationDecisionEnum;
import com.av.pixel.enums.ModerationSourceEnum;
import com.av.pixel.repository.UploadModerationLogRepository;
import com.av.pixel.service.ImageModerationService;
import java.util.concurrent.atomic.AtomicLong;
```

Add two fields after `private final VideoThumbnailService videoThumbnailService;` (around line 123). The class uses `@AllArgsConstructor`, so constructor injection happens automatically:

```java
    private final ImageModerationService imageModerationService;
    private final UploadModerationLogRepository uploadModerationLogRepository;
```

- [ ] **Step 4: Add the guard helper**

In the same file, add these constants next to `IMAGE_UNSAFE_LOGO` (around line 125):

```java
    private static final String UPLOAD_REJECTED_MESSAGE = "This image can't be used. Please upload a different photo.";
    private static final long MODERATION_ALERT_INTERVAL_MS = 5 * 60 * 1000L;
    private static final AtomicLong LAST_MODERATION_ALERT_AT = new AtomicLong(0L);
```

Then add the helper method next to `safeUploadRefImage` (around line 237):

```java
    /**
     * Rejects restricted uploads before any work is done.
     *
     * <p>Called before the generation lock is acquired, so a rejection debits no
     * credits, writes nothing to S3, and sends nothing to Ideogram or GoEnhance.
     */
    void assertUploadIsSafe(String userCode, MultipartFile file, ModerationSourceEnum source) {
        if (file == null || file.isEmpty()) {
            return;
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            log.error("[CRITICAL] could not read upload for moderation, user={}", userCode, e);
            recordModerationRejection(userCode, file, source, ModerationDecisionEnum.ERROR, null);
            throw new Error(HttpStatus.UNPROCESSABLE_ENTITY, UPLOAD_REJECTED_MESSAGE);
        }

        ModerationResult result = imageModerationService.moderate(bytes);

        if (result.isFailure()) {
            alertModerationFailure(userCode, source);
        }
        if (result.isAllowed()) {
            return;
        }

        ModerationDecisionEnum decision = result.isFailure()
                ? ModerationDecisionEnum.ERROR
                : ModerationDecisionEnum.REJECTED;
        log.warn("upload rejected user={} source={} decision={} label={}",
                userCode, source, decision, result.getTopLabel());
        recordModerationRejection(userCode, file, source, decision, result);
        throw new Error(HttpStatus.UNPROCESSABLE_ENTITY, UPLOAD_REJECTED_MESSAGE);
    }

    private void recordModerationRejection(String userCode, MultipartFile file, ModerationSourceEnum source,
                                           ModerationDecisionEnum decision, ModerationResult result) {
        try {
            uploadModerationLogRepository.save(new UploadModerationLog()
                    .setUserCode(userCode)
                    .setSource(source)
                    .setDecision(decision)
                    .setTopLabel(result == null ? null : result.getTopLabel())
                    .setTopConfidence(result == null ? null : result.getTopConfidence())
                    .setLabels(result == null ? List.of() : result.getLabels())
                    .setContentType(file.getContentType())
                    .setSizeBytes(file.getSize()));
        } catch (Exception e) {
            // Never let an audit-write failure turn a rejection into a 500 that lets the image through.
            log.error("could not persist upload moderation log for user={}", userCode, e);
        }
    }

    private void alertModerationFailure(String userCode, ModerationSourceEnum source) {
        long now = DateUtil.currentTimeMillis();
        long last = LAST_MODERATION_ALERT_AT.get();
        if (now - last < MODERATION_ALERT_INTERVAL_MS || !LAST_MODERATION_ALERT_AT.compareAndSet(last, now)) {
            return;
        }
        try {
            sesEmailService.sendErrorMail("[CRITICAL] image moderation unavailable"
                    + "\n\n source : " + source
                    + "\n\n user Code : " + userCode
                    + "\n\n Uploads are being refused while moderation is down (fail-closed).");
        } catch (Exception e) {
            log.error("could not send moderation failure alert", e);
        }
    }
```

The `compareAndSet` guard means a burst of concurrent failures sends exactly one email, not one per thread.

- [ ] **Step 5: Wire the two call sites**

In `generate()`, the line after `Validator.validateGenerateRequest(generateRequest);` (around line 130) and **before** `String key = "generation_" + userDTO.getCode();`, insert:

```java
        assertUploadIsSafe(userDTO.getCode(), file, ModerationSourceEnum.IMAGE_GENERATION);
```

In `generateVideoEffect()`, after the `request.getEffect()` blank check closes (around line 628) and **before** `String effectId = request.getEffect();`, insert:

```java
        assertUploadIsSafe(userDTO.getCode(), file, ModerationSourceEnum.VIDEO_EFFECT);
```

Verify the placement — this is the whole point of the change:

```bash
grep -n "assertUploadIsSafe\|tryLock\|getAvailable() <" src/main/java/com/av/pixel/service/impl/GenerationsServiceImpl.java
```

Expected: in both methods, the `assertUploadIsSafe` line number is **lower** than the `tryLock` line number that follows it, and lower than any credit check.

- [ ] **Step 6: Compile and run the full test suite**

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
mvn -q clean test
```

Expected: BUILD SUCCESS, all tests passing including the four pre-existing test classes.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/av/pixel/enums/ModerationSourceEnum.java \
        src/main/java/com/av/pixel/enums/ModerationDecisionEnum.java \
        src/main/java/com/av/pixel/dao/UploadModerationLog.java \
        src/main/java/com/av/pixel/repository/UploadModerationLogRepository.java \
        src/main/java/com/av/pixel/service/impl/GenerationsServiceImpl.java
git commit -m "feat: reject restricted image uploads before generation"
```

---

## Verification

After Task 4, confirm the whole thing holds together:

- [ ] `mvn -q clean test` passes with `JAVA_HOME` on JDK 21.
- [ ] `grep -n "assertUploadIsSafe" src/main/java/com/av/pixel/service/impl/GenerationsServiceImpl.java` shows exactly three hits: the definition and two call sites.
- [ ] In both methods the guard precedes `tryLock` and the credit check.
- [ ] `git status` shows no accidental staging of `service-account-key.json` or `.DS_Store`.

## Deployment notes

- The IAM user behind `aws.access-key` needs the `rekognition:DetectModerationLabels` permission. Without it, every upload is refused under fail-closed, which is loud but total — grant it before deploying.
- Confirm Rekognition is available in the configured region. If `aws.region` is a region without it, set `aws.rekognition.region` explicitly.
- Rekognition costs roughly $1 per 1,000 images, with 5,000 images/month free for the first 12 months. This is absorbed by the business; no user credits are involved.

## Out of scope — resolve before resubmitting to Google

Recorded in the spec's Known gaps and repeated here because they affect whether the appeal succeeds:

1. **Prompt text is unmoderated.** Nothing stops a prompt such as "nude photo of…" reaching Ideogram. `checkForSafeImages()` reacts only after generation.
2. **Intimate GoEnhance effects ship today**, implied by the `excludeIntimate` parameter on `GET /images/effects`.

Upload moderation closes the identified hole but may not be sufficient for reinstatement on its own.
