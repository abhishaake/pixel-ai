# Uploaded image moderation — design

**Date:** 2026-09-16
**Status:** Approved for planning
**Repos:** `pixel-ai` (Spring Boot backend)
**Trigger:** Google Play suspension of `com.rishyash_studio.pixel_ai` (version code 44)

## Problem

Google Play suspended the app for violating the Sexual Content and Profanity policy
and the AI-Generated Content policy, citing the in-app experience.

The backend accepts user-uploaded reference images on two multipart endpoints and
never inspects their content:

- `POST /api/v1/images/generate/v2` — `character_reference_images`
- `POST /api/v1/images/generate/goenhance` — `reference_image`

In `GenerationsServiceImpl.generate()` the ordering is: acquire lock, check credits,
call Ideogram, then `safeUploadRefImage()` writes the file to S3. The photo reaches a
third-party generation API and the S3 bucket before anything looks at it. A user can
upload a nude photograph as a character reference, and in the GoEnhance flow have it
animated into a video.

Existing safety covers only *generated output*: Ideogram returns `isImageSafe`, and
`checkForSafeImages()` substitutes a placeholder logo and emails an alert after the
fact. Nothing prevents a restricted *input*.

Play's AI-Generated Content policy asks for two things: prevention of restricted
content, and in-app reporting. Reporting already exists (`POST /images/report` →
`image_flags`). Prevention does not.

## Goal

Reject nude and sexually explicit uploaded images before they reach any generation
provider, S3, or the user's credit balance — and keep a record of each rejection as
evidence for the Play appeal.

## Scope

In scope:

- Content moderation of the uploaded file on both multipart generation endpoints.
- An audit record of every rejection.
- Configuration to tune the policy without a code change.

Out of scope (deliberate, see Known gaps):

- Moderation of the generation prompt text.
- Gating or removing the intimate GoEnhance video effects.
- Any change to post-generation `checkForSafeImages()` handling.
- User strikes, account blocking, or an appeal flow for rejected uploads.
- Moderation of the JSON-only `POST /api/v1/images` endpoint, which accepts no file.

## Key decisions

**Backend: AWS Rekognition `DetectModerationLabels`.** Reuses the existing
`aws.access-key` / `aws.secret-key` / `aws.region` credentials, so it adds one SDK
dependency and no new vendor account. Its label taxonomy distinguishes explicit
nudity from kissing, which this product needs. Google Cloud Vision SafeSearch returns
coarser likelihood buckets; a local ONNX model is materially less accurate and adds a
model file to the deploy. Under a suspension deadline, Rekognition is the fastest
defensible path.

**Cost is never passed to the user.** Moderation runs before the lock is taken and
before any credit check, so a rejected upload debits nothing. There is no refund path
to get wrong.

**Fail closed.** If Rekognition errors or times out, the upload is rejected. An
outage that blocks generation is a better failure than one that admits nudes while
the app is suspended. A `moderation.fail-open` property (default `false`) exists as an
escape hatch.

**Reject at sub-label granularity, not category.** Rekognition's top-level category
"Non-Explicit Nudity of Intimate parts and Kissing" contains `Kissing on the Lips`.
Blocking that category wholesale would break the core feature of an app named
*Kiss Hug AI Video*. The blocklist names sub-labels.

**Swimwear stays blocked.** Confirmed by the product owner. It costs some false
rejections on innocent beach photos, but over-blocking is recoverable during an
appeal and under-blocking is what caused the suspension. Revisit after reinstatement.

**Store rejections only.** Allowed uploads are not persisted, keeping write volume
near zero while still producing the evidence the appeal needs.

## Architecture

### Component: `ImageModerationService`

```java
public interface ImageModerationService {
    ModerationResult moderate(byte[] imageBytes);
}
```

Takes bytes, returns a verdict. It knows nothing about generations, credits, users,
or HTTP, so it is unit-testable in isolation and reusable if prompt or output
moderation is added later.

**It never throws.** Both a policy rejection and an infrastructure failure are normal
return values, so the caller has exactly one code path to handle and can never leak a
Rekognition exception into a `500` that bypasses the guard.

`ModerationResult` is a value object: `allowed`, `failure`, `topLabel`,
`topConfidence`, `List<String> labels`. The `failure` flag distinguishes "Rekognition
said no" from "Rekognition could not be reached", which is what lets the caller
record `REJECTED` versus `ERROR`. When `failure` is true, `allowed` reflects the
`moderation.fail-open` setting.

`ImageModerationServiceImpl` responsibilities:

1. Short-circuit to allowed when `moderation.enabled=false`.
2. Normalise the input for Rekognition (see Input normalisation).
3. Call `DetectModerationLabels` with the lowest configured confidence as
   `MinConfidence`.
4. Match returned labels against the blocklist by label name *or* by taxonomy
   parent name, comparing each against its own threshold.
5. On `SdkException` or any unexpected failure: log `[CRITICAL]`, fire a throttled
   alert email, and return a result with `failure=true` and `allowed` set from
   `moderation.fail-open`.

### Component: `RekognitionConfig`

A `@Configuration` mirroring the existing `S3Config`: a `RekognitionClient` bean
built from `aws.access-key`, `aws.secret-key`, and `aws.rekognition.region`
(defaulting to `${aws.region}`), so Rekognition can be pointed at a different region
if the S3 region ever lacks the service.

### Component: `UploadModerationLog`

A Mongo document in collection `upload_moderation_logs`, extending `BaseEntity`,
following the existing `ImageFlag` pattern:

| Field | Type | Notes |
|---|---|---|
| `userCode` | `String` | who uploaded |
| `source` | `ModerationSourceEnum` | `IMAGE_GENERATION` or `VIDEO_EFFECT` |
| `decision` | `ModerationDecisionEnum` | `REJECTED` or `ERROR` |
| `labels` | `List<String>` | e.g. `["Explicit Nudity:94.2"]` |
| `topLabel` | `String` | highest-confidence blocked label |
| `topConfidence` | `Double` | its confidence |
| `contentType` | `String` | as reported by the upload |
| `sizeBytes` | `long` | original size before normalisation |

`UploadModerationLogRepository extends BaseRepository<UploadModerationLog, String>`.

### Integration into the generation flows

A private helper in `GenerationsServiceImpl`:

```java
private void assertUploadIsSafe(String userCode, MultipartFile file, ModerationSourceEnum source)
```

It returns immediately when `file == null`. Otherwise it reads `file.getBytes()` and
calls the moderation service. When the result is allowed it returns and the flow
continues. When the result is not allowed it persists an `UploadModerationLog` —
`decision = ERROR` if `result.failure` is set, otherwise `REJECTED` — and throws
`Error(UNPROCESSABLE_ENTITY, ...)`.

Reading `file.getBytes()` is itself wrapped: an `IOException` there is treated as a
rejection with `decision = ERROR`, since an unreadable upload must not fall through
unchecked.

Call sites, both placed **before `locker.tryLock()`**:

- `generate()` — immediately after `Validator.validateGenerateRequest(generateRequest)`
- `generateVideoEffect()` — immediately after the `request.getEffect()` blank check

Placement before the lock means a rejection needs no unlock bookkeeping, performs no
credit check or debit, sends nothing to Ideogram or GoEnhance, and writes nothing to
S3.

`file.getBytes()` is called twice overall — once here and once in the existing
`safeUploadRefImage()`. Spring's `StandardMultipartFile` supports repeated reads, so
this needs no change to `ImageMap.validateAndGetImageRequest()` or to `ImageRequest`,
keeping the diff contained.

## Policy configuration

```properties
moderation.enabled=true
moderation.fail-open=false
moderation.blocked-labels=Explicit:60,Non-Explicit Nudity:75,Obstructed Intimate Parts:75,Swimwear or Underwear:80
aws.rekognition.region=${aws.region}
```

`moderation.blocked-labels` is a comma-separated list of `LabelName:MinConfidence`
pairs, parsed at startup. A label matches when the returned label's own name or its
taxonomy parent name equals the configured name, and its confidence is greater than
or equal to the configured threshold.

Resulting behaviour against the Rekognition v7 taxonomy:

| Rekognition label | Blocked | Threshold |
|---|---|---|
| `Explicit` branch — exposed genitalia, sexual activity, sex toys | yes | 60 |
| `Non-Explicit Nudity` — partial exposure, implied nudity | yes | 75 |
| `Obstructed Intimate Parts` | yes | 75 |
| `Swimwear or Underwear` | yes | 80 |
| `Kissing on the Lips` | **no** | — |
| Violence, Alcohol, Gambling, Drugs, Hate Symbols, Rude Gestures | **no** | — |

The `Explicit` threshold is deliberately the lowest: the cost of a miss in that
category is the suspension recurring.

## Input normalisation

Rekognition accepts JPEG and PNG only, at most 5 MB when bytes are passed directly,
and at least 80x80 pixels. A private `normaliseForRekognition(byte[])` in the impl
uses Thumbnailator — already a dependency, used by `ImageCompressionServiceImpl` —
to transcode to PNG and downscale until the payload fits, so a large or unusual
upload is actually checked rather than erroring into a fail-closed rejection.

This is a private method rather than an addition to `ImageCompressionService`,
whose `ImageCompressionConfig` thresholds are tuned for ~900 KB feed thumbnails and
do not fit this purpose.

## Error handling

| Condition | Behaviour |
|---|---|
| Blocked label at or above threshold | `422`, log persisted with `REJECTED` |
| No blocked label | proceed to the normal generation flow |
| `file == null` | proceed; Rekognition is not called |
| Rekognition failure, `fail-open=false` | `422`, log persisted with `ERROR`, `[CRITICAL]` log line, throttled alert email |
| Rekognition failure, `fail-open=true` | proceed, `[CRITICAL]` log line, throttled alert email |
| Mongo failure while saving the log | swallowed in try/catch; the rejection still stands |
| Normalisation failure | treated as a Rekognition failure |

User-facing rejection message:

> This image can't be used. Please upload a different photo.

Neutral and non-accusatory, and it does not name the label that fired — naming it
would teach users how to tune around the filter.

Alert emails reuse `sesEmailService.sendErrorMail`, throttled to at most one every
five minutes via an `AtomicLong` holding the last-sent timestamp, so a Rekognition
outage cannot flood the inbox.

## Testing

Unit tests with a mocked `RekognitionClient`, in the existing Mockito + AssertJ style
(`@ExtendWith(MockitoExtension.class)`), in
`ImageModerationServiceImplTest`:

1. Blocked label above threshold → rejected.
2. Same label below its threshold → allowed.
3. `Kissing on the Lips` at 99% confidence → allowed. Regression guard on the core
   product feature.
4. `Swimwear or Underwear` at 85% → rejected. Guard on the confirmed strict setting.
5. Label matched via its taxonomy parent name → rejected.
6. `moderation.enabled=false` → allowed, Rekognition never called.
7. Rekognition client throws, `fail-open=false` → result is `allowed=false`,
   `failure=true`; the service itself does not propagate the exception.
8. Rekognition client throws, `fail-open=true` → result is `allowed=true`,
   `failure=true`.
9. Oversized input → normalised under 5 MB, Rekognition still called.
10. Blocklist property parsing, including whitespace and malformed entries.

`GenerationsServiceImpl` is 865 lines with roughly fifteen collaborators, so the
decision logic lives entirely in the moderation service where it can be tested
cheaply, and the caller invokes a single guard. Correct call-site ordering — before
the lock, before credits — is verified by reading the two methods rather than by
standing up a heavy harness for that class.

## Files

New:

- `config/RekognitionConfig.java`
- `service/ImageModerationService.java`
- `service/impl/ImageModerationServiceImpl.java`
- `dto/ModerationResult.java`
- `dao/UploadModerationLog.java`
- `repository/UploadModerationLogRepository.java`
- `enums/ModerationSourceEnum.java`
- `enums/ModerationDecisionEnum.java`
- `src/test/java/com/av/pixel/service/impl/ImageModerationServiceImplTest.java`

Modified:

- `pom.xml` — add `software.amazon.awssdk:rekognition:2.31.21`, matching the pinned
  SDK version used by the existing `s3` and `ses` dependencies
- `src/main/resources/application.properties` — the `moderation.*` and
  `aws.rekognition.region` keys
- `service/impl/GenerationsServiceImpl.java` — the `assertUploadIsSafe` helper and
  two call sites

## Known gaps

These are out of scope by decision, and are recorded because the suspension notice
cites the in-app experience broadly rather than uploads alone. A reviewer who reached
restricted content likely had routes other than an upload:

1. **Prompt text is unmoderated.** Nothing stops a prompt such as "nude photo of…"
   reaching Ideogram. `checkForSafeImages()` reacts only after generation, replacing
   the image with a placeholder and emailing an alert — it does not block the user or
   record a violation. A server-side denylist on `GenerateRequest.prompt` is the
   obvious next increment.
2. **Intimate GoEnhance effects ship today.** The `excludeIntimate` parameter on
   `GET /images/effects` implies a set of intimate effects exists and is reachable.
   Whether those survive is a product decision.

Both should be resolved before resubmitting to Google. Upload moderation alone closes
the identified hole but may not be sufficient for reinstatement.

## Operational note for the appeal

Once deployed, `upload_moderation_logs` supports a concrete claim in the Play appeal:
the number of uploads blocked before generation since the deploy date, broken down by
label. That is materially more persuasive than an assertion that moderation was
added.
