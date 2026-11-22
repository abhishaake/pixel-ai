# Play Relaunch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship Pixel AI under the Hastara listing with restricted uploads blocked, private content genuinely private, and reported feed content auto-hidden.

**Architecture:** Three backend changes in `pixel-ai` — a config-only blocklist widening guarded by a test that reads the shipped properties file, a pure static privacy resolver extracted from the feed path of `GenerationsServiceImpl`, and a new `ReportTakedownService` that owns report handling. One app change in `PixelAI` — repoint `applicationId` and the store-listing link at the Hastara package. Then release and submission checklists.

**Tech Stack:** Spring Boot 3.4.4, Java 21, MongoDB (Spring Data + `MongoTemplate`), AWS Rekognition, JUnit 5 + Mockito + AssertJ; Flutter with Android Gradle Plugin 8.6.1.

**Spec:** `docs/superpowers/specs/2026-11-22-play-relaunch-design.md`

## Global Constraints

- Target package: `com.rishyash_studio.icc_world_cup_2024_app`
- App name: `Pixel AI – Kiss Hug AI Video`
- Blocklist value, exactly: `moderation.blocked-labels=Explicit:60,Explicit Nudity:60,Explicit Sexual Activity:60,Sex Toys:60`
- Report hide threshold: `3` distinct reporters, property `moderation.report-hide-threshold`
- Upload rejection message, unchanged: `This image can't be used. Please upload a different photo.`
- `targetSdk = 36` and `compileSdk = 36` (already set — must stay)
- Private uploads are **not** moderated. Do not add moderation to the private path.
- Java builds need JDK 21: `export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`
- **Never `git add` `src/main/resources/application.properties` wholesale.** The working copy holds live credentials that have never been committed. Task 1 shows the only safe way to stage a change to it.

## Prerequisites from the product owner

Tasks 4–6 are blocked until these exist. Tasks 1–3 are not.

1. Hastara's upload keystore and its passwords, or a completed upload-key reset from Google.
2. Hastara's highest uploaded versionCode (Play Console → Hastara → App bundle explorer).
3. A `google-services.json` that contains an Android client for `com.rishyash_studio.icc_world_cup_2024_app` (Firebase console → project settings → add Android app → re-download).
4. Store listing assets, and Privacy policy + Terms URLs that prohibit generating restricted content.
5. Rotated credentials for everything currently substituted into the local `application.properties` (Mongo, both AWS key pairs, Ideogram, GoEnhance, Gmail app password), set in the deployment environment before Task 5.

Outside these tasks but on the same account: the trader status declaration is due February 2027, and missing it removes the developer profile and all its apps globally.

## File Structure

| File | Repo | Change | Responsibility |
|---|---|---|---|
| `src/main/resources/application.properties` | pixel-ai | Modify one line | Blocklist value |
| `src/test/java/com/av/pixel/service/ModerationPolicyTest.java` | pixel-ai | Modify | Guards the shipped blocklist |
| `src/main/java/com/av/pixel/service/impl/GenerationsServiceImpl.java` | pixel-ai | Modify | Feed privacy resolution; delegate reports |
| `src/test/java/com/av/pixel/service/impl/GenerationsServiceImplFeedPrivacyTest.java` | pixel-ai | Create | Feed privacy rules |
| `src/main/java/com/av/pixel/service/ReportTakedownService.java` | pixel-ai | Create | Report-handling contract |
| `src/main/java/com/av/pixel/service/impl/ReportTakedownServiceImpl.java` | pixel-ai | Create | Dedupe, count distinct reporters, hide |
| `src/main/java/com/av/pixel/repository/ImageFlagRepository.java` | pixel-ai | Modify | Two derived queries |
| `src/test/java/com/av/pixel/service/impl/ReportTakedownServiceImplTest.java` | pixel-ai | Create | Takedown rules |
| `android/app/build.gradle` | PixelAI | Modify one line | `applicationId` |
| `lib/presentation/widget/bottom_navigation_widget.dart` | PixelAI | Modify one line | Store listing link |
| `android/app/google-services.json` | PixelAI | Replace | Firebase client for new package |
| `pubspec.yaml` | PixelAI | Modify one line | versionCode above Hastara's |

---

### Task 1: Blocklist covers the whole Explicit branch

**Why:** `ModerationPolicy.isBlocked` matches a label's own name or its **direct** parent only. With `Explicit Nudity:60`, labels under the sibling `Explicit Sexual Activity` and `Sex Toys` pass the upload gate. Every level is named so third-level labels match through their direct parent even if Rekognition omits ancestor labels.

**Files:**
- Modify: `src/main/resources/application.properties` (the `moderation.blocked-labels=` line only)
- Test: `src/test/java/com/av/pixel/service/ModerationPolicyTest.java`

**Interfaces:**
- Consumes: `ModerationPolicy(String blockedLabelsProperty)`, `Optional<String> firstBlocked(List<DetectedLabel>)`, `record DetectedLabel(String name, String parentName, double confidence)` — all existing.
- Produces: nothing new.

- [ ] **Step 1: Write the failing tests**

In `ModerationPolicyTest.java`, replace the `DEFAULT_BLOCKLIST` constant:

```java
    private static final String DEFAULT_BLOCKLIST =
            "Explicit:60,Explicit Nudity:60,Explicit Sexual Activity:60,Sex Toys:60";
```

Add these imports at the top of the file:

```java
import java.io.InputStream;
import java.util.Properties;
```

Add these tests inside the class:

```java
    @Test
    void blocksExplicitSexualActivity() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Explicit Sexual Activity", "Explicit", 70.0d)));
        assertThat(blocked).contains("Explicit Sexual Activity");
    }

    @Test
    void blocksSexToys() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Sex Toys", "Explicit", 65.0d)));
        assertThat(blocked).contains("Sex Toys");
    }

    @Test
    void blocksThirdLevelNudityEvenWithoutAncestorLabels() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Exposed Female Genitalia", "Explicit Nudity", 80.0d)));
        assertThat(blocked).contains("Exposed Female Genitalia");
    }

    @Test
    void shippedConfigurationMatchesTheGuardedBlocklist() throws Exception {
        Properties shipped = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
            shipped.load(in);
        }
        assertThat(shipped.getProperty("moderation.blocked-labels")).isEqualTo(DEFAULT_BLOCKLIST);
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
mvn -q test -Dtest=ModerationPolicyTest
```

Expected: FAIL. `shippedConfigurationMatchesTheGuardedBlocklist` fails with expected `"Explicit:60,Explicit Nudity:60,..."` but was `"Explicit Nudity:60"`. The three `blocks*` tests pass — they exercise the constant, which is the point: the policy logic is already correct and the defect was purely the shipped value.

- [ ] **Step 3: Change the working copy's blocklist line**

```bash
cd /Users/mohit/Documents/GitHub/pixel-ai
F=src/main/resources/application.properties
NEW='moderation.blocked-labels=Explicit:60,Explicit Nudity:60,Explicit Sexual Activity:60,Sex Toys:60'
sed -i '' "s|^moderation.blocked-labels=.*|$NEW|" "$F"
grep -n '^moderation.blocked-labels=' "$F"
```

Expected: one line, with the new value.

- [ ] **Step 4: Run the tests to verify they pass**

```bash
mvn -q test -Dtest=ModerationPolicyTest
```

Expected: PASS, all tests including the pre-existing `allowsKissingOnTheLipsAtHighConfidence`.

- [ ] **Step 5: Stage only this line — never the local credentials**

The working copy of this file contains live credentials. Build the staged version from `HEAD` plus only the blocklist change:

```bash
cd /Users/mohit/Documents/GitHub/pixel-ai
F=src/main/resources/application.properties
NEW='moderation.blocked-labels=Explicit:60,Explicit Nudity:60,Explicit Sexual Activity:60,Sex Toys:60'
TMP=$(mktemp)
git show HEAD:"$F" | sed "s|^moderation.blocked-labels=.*|$NEW|" > "$TMP"
git update-index --cacheinfo 100644,"$(git hash-object -w "$TMP")","$F"
rm "$TMP"
git diff --cached -- "$F"
```

Expected: exactly one removed line (`moderation.blocked-labels=Explicit Nudity:60`) and one added line (the new value). **If the staged diff shows anything else — a URI, a key, a password — stop and run `git restore --staged "$F"`.**

- [ ] **Step 6: Commit**

```bash
git add src/test/java/com/av/pixel/service/ModerationPolicyTest.java
git commit -m "fix: block the whole Explicit moderation branch on uploads"
```

---

### Task 2: Feed privacy is decided by the server

**Why:** The feed request's `privacy` field goes straight into the Mongo query. A request with `privacy: "private"` or `"both"` returns other users' private generations. `isSelfProfile` also accepts a request listing the caller **plus** someone else, which would extend that access. Private uploads are not moderated, so this is the route from an unchecked private image to anyone's screen, and it defeats paid privacy.

**Files:**
- Modify: `src/main/java/com/av/pixel/service/impl/GenerationsServiceImpl.java:409-418` (inside `filterImages`)
- Create: `src/test/java/com/av/pixel/service/impl/GenerationsServiceImplFeedPrivacyTest.java`

**Interfaces:**
- Consumes: `ImagePrivacyEnum` (`DEFAULT`→`false`, `PUBLIC`→`false`, `PRIVATE`→`true`, `BOTH`→`null` via `getPrivateImage()`).
- Produces:
  - `static boolean GenerationsServiceImpl.isSelfProfile(String userCode, List<String> requestedUserCodes)`
  - `static ImagePrivacyEnum GenerationsServiceImpl.resolveFeedPrivacy(ImagePrivacyEnum requested, boolean isSelfProfile, String userCode)`

  Both package-private, so the test in the same package can call them without constructing the service.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/av/pixel/service/impl/GenerationsServiceImplFeedPrivacyTest.java`:

```java
package com.av.pixel.service.impl;

import com.av.pixel.enums.ImagePrivacyEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GenerationsServiceImplFeedPrivacyTest {

    private static final String ME = "P100";
    private static final String OTHER = "P200";

    private static Boolean privacyFilter(ImagePrivacyEnum requested, boolean self, String userCode) {
        return GenerationsServiceImpl.resolveFeedPrivacy(requested, self, userCode).getPrivateImage();
    }

    @Test
    void strangerAskingForPrivateReceivesOnlyPublic() {
        assertThat(privacyFilter(ImagePrivacyEnum.PRIVATE, false, ME)).isFalse();
    }

    @Test
    void strangerAskingForBothReceivesOnlyPublic() {
        assertThat(privacyFilter(ImagePrivacyEnum.BOTH, false, ME)).isFalse();
    }

    @Test
    void anonymousCallerAskingForPrivateReceivesOnlyPublic() {
        assertThat(privacyFilter(ImagePrivacyEnum.PRIVATE, false, null)).isFalse();
    }

    @Test
    void defaultFeedIsPublic() {
        assertThat(privacyFilter(ImagePrivacyEnum.DEFAULT, false, ME)).isFalse();
    }

    @Test
    void ownProfileByDefaultShowsPublicAndPrivate() {
        assertThat(privacyFilter(ImagePrivacyEnum.DEFAULT, true, ME)).isNull();
    }

    @Test
    void ownProfileMayAskForPrivateOnly() {
        assertThat(privacyFilter(ImagePrivacyEnum.PRIVATE, true, ME)).isTrue();
    }

    @Test
    void preservedAllPrivacyCodesAreUnchanged() {
        assertThat(privacyFilter(ImagePrivacyEnum.DEFAULT, false, "P108")).isNull();
        assertThat(privacyFilter(ImagePrivacyEnum.PRIVATE, false, "P125")).isNull();
        assertThat(privacyFilter(ImagePrivacyEnum.PUBLIC, false, "P109")).isNull();
    }

    @Test
    void ownCodeAloneIsSelfProfile() {
        assertThat(GenerationsServiceImpl.isSelfProfile(ME, List.of(ME))).isTrue();
    }

    @Test
    void ownCodeMixedWithAnotherIsNotSelfProfile() {
        assertThat(GenerationsServiceImpl.isSelfProfile(ME, List.of(ME, OTHER))).isFalse();
    }

    @Test
    void emptyRequestOrAnonymousCallerIsNotSelfProfile() {
        assertThat(GenerationsServiceImpl.isSelfProfile(ME, List.of())).isFalse();
        assertThat(GenerationsServiceImpl.isSelfProfile(ME, null)).isFalse();
        assertThat(GenerationsServiceImpl.isSelfProfile(null, List.of(ME))).isFalse();
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
mvn -q test -Dtest=GenerationsServiceImplFeedPrivacyTest
```

Expected: COMPILATION ERROR — `cannot find symbol: method resolveFeedPrivacy` and `isSelfProfile`.

- [ ] **Step 3: Replace the privacy resolution in `filterImages`**

In `GenerationsServiceImpl.java`, replace this block (currently lines 409–418):

```java
        boolean isSelfProfile = StringUtils.isNotEmpty(userCode) && !CollectionUtils.isEmpty(generationsFilterRequest.getUserCodes())
                && generationsFilterRequest.getUserCodes().contains(userCode);

        ImagePrivacyEnum privacyEnum = ImagePrivacyEnum.getEnumByName(generationsFilterRequest.getPrivacy());

        if (isSelfProfile && ImagePrivacyEnum.DEFAULT.equals(privacyEnum)) {
            privacyEnum = ImagePrivacyEnum.BOTH;
        } else if (StringUtils.isNotEmpty(userCode) && List.of("P108", "P125", "P109").contains(userCode)) {
            privacyEnum = ImagePrivacyEnum.BOTH;
        }
```

with:

```java
        boolean isSelfProfile = isSelfProfile(userCode, generationsFilterRequest.getUserCodes());
        ImagePrivacyEnum privacyEnum = resolveFeedPrivacy(
                ImagePrivacyEnum.getEnumByName(generationsFilterRequest.getPrivacy()), isSelfProfile, userCode);
```

- [ ] **Step 4: Add the two resolvers**

In the same class, directly below the existing `isPublic(Boolean)` method, add:

```java
    /** User codes that see every user's private content in the feed. Preserved as found; purpose undocumented. */
    private static final List<String> FEED_ALL_PRIVACY_CODES = List.of("P108", "P125", "P109");

    /** True only when every requested profile is the caller's own. */
    static boolean isSelfProfile(String userCode, List<String> requestedUserCodes) {
        return StringUtils.isNotEmpty(userCode)
                && !CollectionUtils.isEmpty(requestedUserCodes)
                && requestedUserCodes.stream().allMatch(userCode::equals);
    }

    /**
     * The privacy filter the feed query actually applies. A client may ask for private
     * content but receives it only for its own profile.
     */
    static ImagePrivacyEnum resolveFeedPrivacy(ImagePrivacyEnum requested, boolean isSelfProfile, String userCode) {
        if (isSelfProfile && ImagePrivacyEnum.DEFAULT.equals(requested)) {
            return ImagePrivacyEnum.BOTH;
        }
        if (StringUtils.isNotEmpty(userCode) && FEED_ALL_PRIVACY_CODES.contains(userCode)) {
            return ImagePrivacyEnum.BOTH;
        }
        if (isSelfProfile) {
            return requested;
        }
        return ImagePrivacyEnum.PUBLIC;
    }
```

`StringUtils` (commons-lang3, line 75) and `CollectionUtils` (Spring, line 86) are already imported.

- [ ] **Step 5: Run the test to verify it passes**

```bash
mvn -q test -Dtest=GenerationsServiceImplFeedPrivacyTest
```

Expected: PASS, 10 tests.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/av/pixel/service/impl/GenerationsServiceImpl.java \
        src/test/java/com/av/pixel/service/impl/GenerationsServiceImplFeedPrivacyTest.java
git commit -m "fix: serve private generations only on the owner's profile"
```

---

### Task 3: Reported generations leave the feed at 3 distinct reporters

**Why:** `reportImage` saves an `ImageFlag` that nothing reads. Reported content stays in the feed indefinitely.

**Files:**
- Create: `src/main/java/com/av/pixel/service/ReportTakedownService.java`
- Create: `src/main/java/com/av/pixel/service/impl/ReportTakedownServiceImpl.java`
- Modify: `src/main/java/com/av/pixel/repository/ImageFlagRepository.java`
- Modify: `src/main/java/com/av/pixel/service/impl/GenerationsServiceImpl.java` (field at line 119, `reportImage` at lines 864–877, two imports)
- Create: `src/test/java/com/av/pixel/service/impl/ReportTakedownServiceImplTest.java`

**Interfaces:**
- Consumes: `ImageFlag` (`genId`, `imageId`, `userCode`, `reason`; chained setters), `ImageReportRequest` (`genId`, `imageId`, `reason`), `SesEmailService.sendErrorMail(String)`, `Generations` (`_id` is an `ObjectId`; field `privateImage`).
- Produces:
  - `void ReportTakedownService.report(String userCode, ImageReportRequest request)`
  - `boolean ImageFlagRepository.existsByGenIdAndUserCode(String genId, String userCode)`
  - `List<ImageFlag> ImageFlagRepository.findByGenId(String genId)`

- [ ] **Step 1: Add the repository queries**

Replace the body of `ImageFlagRepository.java` with:

```java
package com.av.pixel.repository;

import com.av.pixel.dao.ImageFlag;
import com.av.pixel.repository.base.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageFlagRepository  extends BaseRepository<ImageFlag, String> {

    boolean existsByGenIdAndUserCode(String genId, String userCode);

    List<ImageFlag> findByGenId(String genId);
}
```

- [ ] **Step 2: Add the service interface**

Create `src/main/java/com/av/pixel/service/ReportTakedownService.java`:

```java
package com.av.pixel.service;

import com.av.pixel.request.ImageReportRequest;

/**
 * Records user reports, and withdraws a generation from the public feed once enough
 * distinct users have reported it.
 */
public interface ReportTakedownService {

    void report(String userCode, ImageReportRequest request);
}
```

- [ ] **Step 3: Write the failing test**

Create `src/test/java/com/av/pixel/service/impl/ReportTakedownServiceImplTest.java`:

```java
package com.av.pixel.service.impl;

import com.av.pixel.dao.Generations;
import com.av.pixel.dao.ImageFlag;
import com.av.pixel.repository.ImageFlagRepository;
import com.av.pixel.request.ImageReportRequest;
import com.av.pixel.service.SesEmailService;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportTakedownServiceImplTest {

    @Mock private ImageFlagRepository imageFlagRepository;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private SesEmailService sesEmailService;

    private ReportTakedownServiceImpl service;
    private String genId;

    @BeforeEach
    void setUp() {
        service = new ReportTakedownServiceImpl(imageFlagRepository, mongoTemplate, sesEmailService, 3);
        genId = new ObjectId().toHexString();
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Generations.class)))
                .thenReturn(UpdateResult.acknowledged(1L, 1L, null));
    }

    private void storedReporters(String... userCodes) {
        List<ImageFlag> flags = Arrays.stream(userCodes)
                .map(code -> new ImageFlag().setGenId(genId).setUserCode(code))
                .toList();
        when(imageFlagRepository.findByGenId(genId)).thenReturn(flags);
    }

    private ImageReportRequest requestFor(String id) {
        ImageReportRequest request = new ImageReportRequest();
        request.setGenId(id);
        request.setReason("nudity");
        return request;
    }

    private void verifyNotHidden() {
        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), eq(Generations.class));
    }

    @Test
    void secondDistinctReporterDoesNotHide() {
        storedReporters("P1", "P2");
        service.report("P2", requestFor(genId));
        verifyNotHidden();
    }

    @Test
    void thirdDistinctReporterSetsGenerationPrivateAndAlerts() {
        storedReporters("P1", "P2", "P3");

        service.report("P3", requestFor(genId));

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(query.capture(), update.capture(), eq(Generations.class));
        assertThat(query.getValue().getQueryObject().get("_id")).isEqualTo(new ObjectId(genId));
        Document set = (Document) update.getValue().getUpdateObject().get("$set");
        assertThat(set.get("privateImage")).isEqualTo(true);
        verify(sesEmailService).sendErrorMail(anyString());
    }

    @Test
    void repeatReportFromSameUserIsIgnored() {
        when(imageFlagRepository.existsByGenIdAndUserCode(genId, "P1")).thenReturn(true);
        storedReporters("P1", "P2", "P3");

        service.report("P1", requestFor(genId));

        verify(imageFlagRepository, never()).save(any(ImageFlag.class));
        verifyNotHidden();
    }

    @Test
    void duplicateRowsFromOneUserCountOnce() {
        storedReporters("P1", "P1", "P1", "P2");
        service.report("P2", requestFor(genId));
        verifyNotHidden();
    }

    @Test
    void anonymousReportsDoNotCount() {
        storedReporters(null, null, null, "P1", "P2");
        service.report(null, requestFor(genId));
        verifyNotHidden();
    }

    @Test
    void alreadyPrivateGenerationSendsNoAlert() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Generations.class)))
                .thenReturn(UpdateResult.acknowledged(1L, 0L, null));
        storedReporters("P1", "P2", "P3");

        service.report("P3", requestFor(genId));

        verify(sesEmailService, never()).sendErrorMail(anyString());
    }

    @Test
    void hidingNeverDeletesTheGeneration() {
        storedReporters("P1", "P2", "P3");
        service.report("P3", requestFor(genId));
        verify(mongoTemplate, never()).remove(any(Query.class), eq(Generations.class));
    }

    @Test
    void mongoFailureWhileHidingDoesNotReachTheCaller() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Generations.class)))
                .thenThrow(new RuntimeException("mongo down"));
        storedReporters("P1", "P2", "P3");

        assertThatCode(() -> service.report("P3", requestFor(genId))).doesNotThrowAnyException();
    }

    @Test
    void malformedGenIdDoesNotReachTheCaller() {
        String bad = "not-an-object-id";
        when(imageFlagRepository.findByGenId(bad)).thenReturn(List.of(
                new ImageFlag().setGenId(bad).setUserCode("P1"),
                new ImageFlag().setGenId(bad).setUserCode("P2"),
                new ImageFlag().setGenId(bad).setUserCode("P3")));

        assertThatCode(() -> service.report("P3", requestFor(bad))).doesNotThrowAnyException();
        verifyNotHidden();
    }

    @Test
    void missingGenIdIsIgnored() {
        service.report("P1", requestFor(null));
        verify(imageFlagRepository, never()).save(any(ImageFlag.class));
    }
}
```

- [ ] **Step 4: Run the test to verify it fails**

```bash
mvn -q test -Dtest=ReportTakedownServiceImplTest
```

Expected: COMPILATION ERROR — `cannot find symbol: class ReportTakedownServiceImpl`.

- [ ] **Step 5: Implement the service**

Create `src/main/java/com/av/pixel/service/impl/ReportTakedownServiceImpl.java`:

```java
package com.av.pixel.service.impl;

import com.av.pixel.dao.Generations;
import com.av.pixel.dao.ImageFlag;
import com.av.pixel.repository.ImageFlagRepository;
import com.av.pixel.request.ImageReportRequest;
import com.av.pixel.service.ReportTakedownService;
import com.av.pixel.service.SesEmailService;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
public class ReportTakedownServiceImpl implements ReportTakedownService {

    private final ImageFlagRepository imageFlagRepository;
    private final MongoTemplate mongoTemplate;
    private final SesEmailService sesEmailService;
    private final int hideThreshold;

    public ReportTakedownServiceImpl(ImageFlagRepository imageFlagRepository,
                                     MongoTemplate mongoTemplate,
                                     SesEmailService sesEmailService,
                                     @Value("${moderation.report-hide-threshold:3}") int hideThreshold) {
        this.imageFlagRepository = imageFlagRepository;
        this.mongoTemplate = mongoTemplate;
        this.sesEmailService = sesEmailService;
        this.hideThreshold = hideThreshold;
    }

    @Override
    public void report(String userCode, ImageReportRequest request) {
        if (Objects.isNull(request) || StringUtils.isEmpty(request.getGenId())) {
            return;
        }
        String genId = request.getGenId();

        // One report per user per generation; repeats must not move the count.
        if (StringUtils.isNotEmpty(userCode) && imageFlagRepository.existsByGenIdAndUserCode(genId, userCode)) {
            return;
        }
        imageFlagRepository.save(new ImageFlag()
                .setGenId(genId)
                .setImageId(request.getImageId())
                .setReason(request.getReason())
                .setUserCode(userCode));

        // Distinct, identified reporters only: anonymous reports and duplicate rows
        // must not let one person hide someone else's post.
        long reporters = imageFlagRepository.findByGenId(genId).stream()
                .map(ImageFlag::getUserCode)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .count();
        if (reporters >= hideThreshold) {
            hideFromFeed(genId, reporters);
        }
    }

    private void hideFromFeed(String genId, long reporters) {
        try {
            Query query = new Query(Criteria.where("_id").is(new ObjectId(genId))
                    .and("privateImage").ne(true));
            UpdateResult result = mongoTemplate.updateFirst(
                    query, new Update().set("privateImage", true), Generations.class);
            if (result.getModifiedCount() > 0) {
                log.warn("generation hidden from feed after {} distinct reports, genId={}", reporters, genId);
                alert(genId, reporters);
            }
        } catch (Exception e) {
            log.error("[CRITICAL] could not hide reported generation genId={}", genId, e);
        }
    }

    private void alert(String genId, long reporters) {
        try {
            sesEmailService.sendErrorMail("[MODERATION] generation hidden from public feed"
                    + "\n\n genId : " + genId
                    + "\n\n distinct reporters : " + reporters
                    + "\n\n It has been set private. Review the reports in image_flags.");
        } catch (Exception e) {
            log.error("could not send report takedown alert", e);
        }
    }
}
```

The `privateImage ne true` condition makes the update idempotent: once hidden, further reports match nothing, `modifiedCount` is 0, and no second alert is sent.

- [ ] **Step 6: Run the test to verify it passes**

```bash
mvn -q test -Dtest=ReportTakedownServiceImplTest
```

Expected: PASS, 10 tests.

- [ ] **Step 7: Delegate `reportImage` to the new service**

In `GenerationsServiceImpl.java`:

Replace the field (line 119):

```java
    private final ImageFlagRepository imageFlagRepository;
```

with:

```java
    private final ReportTakedownService reportTakedownService;
```

Replace the whole `reportImage` method (lines 864–877):

```java
    @Override
    public String reportImage (UserDTO userDTO, ImageReportRequest imageReportRequest) {
        if (Objects.isNull(imageReportRequest) || StringUtils.isEmpty(imageReportRequest.getGenId())) {
            return "SUCCESS";
        }
        String userCode = Objects.nonNull(userDTO) && StringUtils.isNotEmpty(userDTO.getCode()) ? userDTO.getCode() : null;
        ImageFlag imageFlag = new ImageFlag()
                .setGenId(imageReportRequest.getGenId())
                .setImageId(imageReportRequest.getImageId())
                .setReason(imageReportRequest.getReason())
                .setUserCode(userCode);
        imageFlagRepository.save(imageFlag);
        return "SUCCESS";
    }
```

with:

```java
    @Override
    public String reportImage (UserDTO userDTO, ImageReportRequest imageReportRequest) {
        String userCode = Objects.nonNull(userDTO) && StringUtils.isNotEmpty(userDTO.getCode()) ? userDTO.getCode() : null;
        reportTakedownService.report(userCode, imageReportRequest);
        return "SUCCESS";
    }
```

Remove these two imports, which are now unused:

```java
import com.av.pixel.dao.ImageFlag;
import com.av.pixel.repository.ImageFlagRepository;
```

Add:

```java
import com.av.pixel.service.ReportTakedownService;
```

`GenerationsServiceImpl` is built by Lombok `@AllArgsConstructor`, so swapping the field type needs no constructor edit.

- [ ] **Step 8: Confirm nothing else referenced the removed field**

```bash
grep -n "imageFlagRepository\|ImageFlag\b" src/main/java/com/av/pixel/service/impl/GenerationsServiceImpl.java
```

Expected: no output.

- [ ] **Step 9: Run the full suite**

```bash
mvn -q clean test
```

Expected: BUILD SUCCESS. Includes the pre-existing `SmokeTest`, `ContentModerationServiceImplTest`, `ImageModerationServiceImplTest`, `ImagePrivacyServiceImplTest`, `GenerationsMapPrivacyTest`, `AdminConfigServiceImplPrivacyCostTest`.

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/av/pixel/service/ReportTakedownService.java \
        src/main/java/com/av/pixel/service/impl/ReportTakedownServiceImpl.java \
        src/main/java/com/av/pixel/repository/ImageFlagRepository.java \
        src/main/java/com/av/pixel/service/impl/GenerationsServiceImpl.java \
        src/test/java/com/av/pixel/service/impl/ReportTakedownServiceImplTest.java
git commit -m "feat: hide a generation from the feed after 3 distinct reports"
```

---

### Task 4: Repoint the Android app at the Hastara package

**Blocked on:** Prerequisites 1, 2 and 3.

**Why only `applicationId`:** Play identifies an app solely by `applicationId`. `namespace` (`build.gradle:37`) is the Java package for generated `R` and `BuildConfig` classes, and `MainActivity.kt` lives in it. Changing it forces a file move and achieves nothing. The `package=` attribute in `AndroidManifest.xml` is ignored by AGP 8 in favour of `namespace`. Leave all three.

**Files (all in `/Users/mohit/Documents/GitHub/PixelAI`):**
- Modify: `android/app/build.gradle:52`
- Modify: `lib/presentation/widget/bottom_navigation_widget.dart:1197`
- Replace: `android/app/google-services.json`
- Modify: `pubspec.yaml:19`
- Create (gitignored, never committed): `android/key.properties`

**Interfaces:** none — configuration only.

- [ ] **Step 1: Branch from `main`**

```bash
cd /Users/mohit/Documents/GitHub/PixelAI
git checkout main && git pull
git checkout -b feature/relaunch-hastara-package
```

- [ ] **Step 2: Confirm the API 36 target is already in place**

```bash
grep -n "compileSdk\|targetSdk" android/app/build.gradle
```

Expected: `compileSdk = 36` and `targetSdk = 36`. This is what clears Hastara's open policy issue ("App must target Android 16 (API level 36) or higher"). If either is lower, raise it to 36 before continuing.

- [ ] **Step 3: Change the applicationId**

```bash
sed -i '' 's|applicationId = "com.rishyash_studio.pixel_ai"|applicationId = "com.rishyash_studio.icc_world_cup_2024_app"|' android/app/build.gradle
grep -n "applicationId\|namespace" android/app/build.gradle
```

Expected: `applicationId = "com.rishyash_studio.icc_world_cup_2024_app"`, and `namespace = "com.rishyash_studio.pixel_ai"` unchanged.

- [ ] **Step 4: Point "Rate us" at the new listing**

```bash
sed -i '' "s|appStoreId: 'com.rishyash_studio.pixel_ai'|appStoreId: 'com.rishyash_studio.icc_world_cup_2024_app'|" lib/presentation/widget/bottom_navigation_widget.dart
grep -rn "com.rishyash_studio.pixel_ai" lib
```

Expected: no output. Without this, "Rate us" opens the suspended listing.

- [ ] **Step 5: Install the new `google-services.json`**

Copy the product owner's file over `android/app/google-services.json`, then:

```bash
grep -c '"package_name": "com.rishyash_studio.icc_world_cup_2024_app"' android/app/google-services.json
```

Expected: `1` or more. If `0`, the Android build fails with "No matching client found for package name" — the file is wrong; do not proceed.

- [ ] **Step 6: Set a versionCode above Hastara's**

```bash
grep -n '^version:' pubspec.yaml
```

Currently `version: 7.0.4+44`. The number after `+` is the versionCode and must be **greater than Hastara's highest uploaded versionCode** (Prerequisite 2), or Play rejects the upload. Set it, e.g. if Hastara's highest is 12:

```bash
sed -i '' 's|^version: .*|version: 7.0.5+45|' pubspec.yaml
```

Use 45 only if it exceeds Hastara's highest; otherwise use Hastara's highest + 1.

- [ ] **Step 7: Configure signing with Hastara's upload key**

Create `android/key.properties` (path relative to the `android/` directory, matching `rootProject.file("key.properties")`):

```properties
storeFile=/absolute/path/to/hastara-upload-keystore.jks
storePassword=<from product owner>
keyAlias=<from product owner>
keyPassword=<from product owner>
```

Confirm it will never be committed:

```bash
git check-ignore -v android/key.properties
```

Expected: `android/.gitignore:11:key.properties	android/key.properties`.

- [ ] **Step 8: Build a release APK and verify package, version and target**

```bash
flutter build apk --release
AAPT2="$ANDROID_HOME/build-tools/$(ls "$ANDROID_HOME/build-tools" | sort -V | tail -1)/aapt2"
"$AAPT2" dump badging build/app/outputs/flutter-apk/app-release.apk | grep -E "^package:|targetSdkVersion"
```

Expected:
```
package: name='com.rishyash_studio.icc_world_cup_2024_app' versionCode='<your N>' ...
targetSdkVersion:'36'
```

- [ ] **Step 9: Verify it is signed with Hastara's upload key**

```bash
keytool -printcert -jarfile build/app/outputs/flutter-apk/app-release.apk | grep SHA256
```

Expected: identical to Play Console → Hastara → Test and release → App integrity → **Upload key certificate** → SHA-256. If it differs, Play will reject the bundle. Stop and fix `key.properties`.

- [ ] **Step 10: Build the release bundle**

```bash
flutter build appbundle --release
ls -la build/app/outputs/bundle/release/app-release.aab
```

Expected: the `.aab` exists.

- [ ] **Step 11: Commit**

```bash
git add android/app/build.gradle lib/presentation/widget/bottom_navigation_widget.dart \
        android/app/google-services.json pubspec.yaml
git status --short
git commit -m "chore: publish under com.rishyash_studio.icc_world_cup_2024_app"
```

Before committing, `git status --short` must not list `android/key.properties` or any `.jks`.

---

### Task 5: Release the backend

**Blocked on:** Tasks 1–3 merged.

The backend must be live before the app ships: the app validates purchases against it, and the upload gate and feed fix must be in place before a reviewer opens the app.

- [ ] **Step 1: Confirm the old app can no longer buy credits**

Play Console → Pixel.AI – Kiss Hug AI Video → Order management. Filter to orders after **2026-09-16** (the suspension date).

- **No orders:** the single `app.package-name` value is sufficient. Continue.
- **Orders exist:** stop. The old builds can still purchase, and their tokens will fail validation once the package changes. `app.package-name` must become a list first; raise this before deploying.

- [ ] **Step 2: Confirm the deploy credentials can call Rekognition**

With the deployment's AWS credentials exported in your shell (not pasted here), and any small JPEG:

```bash
aws rekognition detect-moderation-labels --image-bytes fileb:///path/to/any.jpg --region ap-south-1
```

Expected: JSON containing `"ModerationLabels"`. `AccessDeniedException` means the IAM user lacks `rekognition:DetectModerationLabels` — grant it first. Without it, **every** public upload is refused (fail-closed).

- [ ] **Step 3: Set the package for purchase validation**

In the deployment environment (not in any committed file):

```
app_package_name=com.rishyash_studio.icc_world_cup_2024_app
```

The committed `application.properties` reads `app.package-name=${app_package_name}`.

- [ ] **Step 4: Merge and deploy**

Merge `feature/upload-image-moderation` to `master` and deploy through the existing pipeline.

- [ ] **Step 5: Verify the running configuration**

In the deployed application's startup log:

```
moderation policy loaded with 4 blocked labels: {explicit=60.0, explicit nudity=60.0, explicit sexual activity=60.0, sex toys=60.0}
```

`1 blocked labels` means the old config shipped. Do not submit the app.

---

### Task 6: Prepare the listing, verify on a Play install, then release

**Blocked on:** Tasks 4 and 5, and Prerequisite 4.

Play Billing only works in a build installed from Play, so every check runs on the internal-testing install, after the products exist.

- [ ] **Step 1: Prepare the listing (product owner)**

On the Hastara listing:

- In-app products: create `pixel_product_coin_80`, `pixel_product_coin_450`, `pixel_product_coin_2500`, `pixel_product_coin_5800` as one-time consumables, matching current prices. These are the IDs in `PixelAI/lib/presentation/view/in_app_purchase_screen.dart:36-41`.
- Store listing: title `Pixel AI – Kiss Hug AI Video`, descriptions, icon, feature graphic, screenshots — none depicting nudity or intimate effects.
- App content: privacy policy URL; content rating questionnaire, declaring user-generated content; Data safety form; AI-generated content declaration.

- [ ] **Step 2: Cancel Hastara subscriptions**

Play Console → Hastara → Order management → active subscriptions. Cancel and refund any that remain. Otherwise those users keep paying for a tarot product that no longer exists.

- [ ] **Step 3: Install from internal testing**

Upload `build/app/outputs/bundle/release/app-release.aab` (Task 4) to the **Internal testing** track. Add your test accounts as testers and as license testers. Install through the internal-test opt-in link.

- [ ] **Step 4: Upload gate**

On a **public** generation, attach a nude reference image.

Expected: "This image can't be used. Please upload a different photo." Credits unchanged. A new document in `upload_moderation_logs` with `decision: "REJECTED"`.

- [ ] **Step 5: Feed privacy**

Account A creates a private generation. From account B, request account A's profile with `privacy: "private"`, then with `privacy: "both"`, then with `userCodes` set to both A and B and `privacy: "private"`.

Expected: none of the three responses contains A's private generation.

- [ ] **Step 6: Report takedown**

Make a public generation. Report it from three different accounts.

Expected: after the third report it disappears from the public feed; the owner still sees it on their own profile; one alert mail arrives. A fourth report sends no second mail.

- [ ] **Step 7: Purchase**

Buy `pixel_product_coin_80`.

Expected: credits awarded. A failure usually means `app_package_name` (Task 5 Step 3) is wrong or the product from Step 1 is not active yet.

- [ ] **Step 8: Release to production**

Only when Steps 4–7 all pass, promote the internal-testing release to Production.

---

## Not in this plan, by decision

Recorded so they are not added during execution:

- Moderation of generated output.
- Moderation of private uploads.
- Prompt-text moderation.
- Removing or gating intimate video effects.
- Credit refunds for rejected uploads.

See the spec's Decisions and Accepted risks sections.
