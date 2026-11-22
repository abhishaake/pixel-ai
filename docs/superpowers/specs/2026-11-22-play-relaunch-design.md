# Play relaunch after suspension — design

**Date:** 2026-11-22
**Status:** Approved for planning
**Repos:** `pixel-ai` (Spring Boot backend), `PixelAI` (Flutter app)
**Trigger:** Google Play suspension of `com.rishyash_studio.pixel_ai` (version code 44), appeal rejected 2026-09-24

## Problem

Pixel.AI – Kiss Hug AI Video was suspended on 2026-09-16 under the Sexual Content
and Profanity policy and the AI-Generated Content policy. An appeal was filed the
same evening (ticket 4-8710000041332) and **rejected on 2026-09-24**. The Play
Console no longer offers an appeal control for this app.

### What the reviewer actually saw

The screenshot attached to the rejection (`com.rishyash_studio.pixel_ai.png`) shows a
full nude photograph rendered **inside the app's public feed** — the like heart, the
share control and the `@username` watermark are all in frame. The violation is
restricted content reaching the community feed, not an intimate video effect and not
a kiss/hug animation.

### Why it was reachable

1. `GenerationsServiceImpl.isPublic()` returns `true` unless the caller explicitly
   sets `privateImage`, so **public is the default**.
2. Version 44 shipped with no upload moderation at all. `assertUploadAllowed` was
   written after the suspension and has never been released.
3. The **generated output is never moderated** before entering the feed.
   `ContentModerationService.assertGenerationAllowedPublic` exists and does check
   output, but is wired only to the privacy-toggle endpoint
   (`ImagePrivacyServiceImpl`), not to generation.
4. `reportImage` writes an `ImageFlag` row that nothing reads. Reported content stays
   in the feed indefinitely.
5. **The feed's privacy filter is chosen by the client.** The feed request carries a
   `privacy` field that `ImagePrivacyEnum.getEnumByName` maps straight into the Mongo
   query, and `Validator.validateFilterImageRequest` checks only page and size. A
   request with `privacy: "private"` or `privacy: "both"` returns **other users'
   private generations**. Private uploads are deliberately not moderated, so this is
   a direct route from an unmoderated private image to anyone's screen. It also
   defeats the paid image-privacy feature: users pay to make an image private, and
   the server will hand it to any caller who asks.

Google's rejection instructs: publish a new app under a new package name and a new
app name. Reinstatement of `com.rishyash_studio.pixel_ai` is not available.

## Goal

Return the product to Google Play with restricted **uploads** blocked before
generation, private content genuinely private, and reported feed content removed
automatically.

## Decisions

All product decisions below were made by the product owner on 2026-11-22. Several were
taken against the recommendation recorded here; they are written down so that a later
reader does not mistake them for oversights and "fix" them.

| Decision | Choice | Recommended |
|---|---|---|
| Product positioning | Unchanged from v44 | — |
| Upload moderation | Block restricted uploads on public generations, 422 + error message | same |
| Output moderation | **Not implemented** | implement |
| Intimate video effects | **Unchanged, served by default** | remove |
| Prompt moderation | Not implemented | — |
| Report handling | Auto-hide at 3 distinct reporters | same |
| Package name | **`com.rishyash_studio.icc_world_cup_2024_app`** (the live Hastara listing) | a brand-new package |
| App name | **`Pixel AI – Kiss Hug AI Video`** (the suspended title) | a new name |

Server-side enforcement of feed privacy (Architecture §3) is not a product choice. The
decision to skip moderation for private uploads is only safe if private content cannot
be retrieved by other users, so it is a correctness requirement of the upload-only
design the owner chose.

## Accepted risks

These are consequences of the decisions above, accepted by the product owner.

1. **The flagged screen remains reachable.** Only uploads are moderated. A
   prompt-only generation, or a clothed reference image transformed by Ideogram or
   GoEnhance, can still place restricted content in the public feed — the exact
   screen in the reviewer's screenshot. One Rekognition call on the generated result
   before it becomes public would close this; it was declined on cost.
2. **A live production app is being overwritten.** The chosen package belongs to
   Hastara: Palm Reading & Tarot — Production, 57 monthly active devices, 30
   acquisitions in the trailing 28 days. Its users receive an unrelated app on their
   next update, and the listing ceases to exist as a tarot app. Any active Hastara
   subscriptions keep renewing for a product that no longer exists unless cancelled
   first.
3. **The suspended app's exact title is reused**, contrary to the written instruction
   in the rejection. Title matching against enforcement records is trivial.
4. **Account-level exposure.** Rishyash Studio already carries two suspensions
   (`com.rishyash_studio.pixel_ai`, `com.rishyash_studio.ipl_2025`). Google's reply
   states that suspensions count as strikes and that multiple violations can result
   in account suspension. If this relaunch is identified as republishing a suspended
   app, the plausible outcome is loss of Hastara and of the developer account, not
   merely of this app.
5. **Intimate effects remain live**, so the category cited in both notices is still
   served by default.

## Scope

In scope:

- Release the existing upload moderation on both multipart generation endpoints.
- Make the moderation blocklist cover the whole `Explicit` branch.
- Enforce feed privacy on the server.
- Auto-hide reported generations from the public feed.
- Verify the Flutter app targets Android API 36 (already set).
- Repoint the Android build and the backend's Play validation at the new package.

Out of scope, by decision: output moderation, prompt moderation, removal or gating of
intimate video effects, credit refunds on rejected uploads.

## Architecture

### 1. Upload moderation — release, do not rewrite

`ContentModerationServiceImpl.assertUploadAllowed` already implements the required
behaviour and is called from both `generate()` and `generateVideoEffect()` before
`locker.tryLock()` and before any credit check. A rejected upload therefore debits no
credits, reaches no generation provider and writes nothing to S3.

No code change. The work is deployment and verification:

- The IAM user behind `aws.access-key` needs `rekognition:DetectModerationLabels`.
  Without it every upload fails closed — loud, but total.
- `moderation.enabled=true`, `moderation.fail-open=false` as configured.

### 2. Blocklist coverage

Current: `moderation.blocked-labels=Explicit Nudity:60`

`ModerationPolicy.isBlocked` matches a returned label when the label's **own name or
its direct parent name** is in the blocklist — not a grandparent. In the Rekognition
v7 taxonomy, `Explicit Sexual Activity` and `Sex Toys` are siblings of
`Explicit Nudity` under `Explicit`, so neither they nor their children match today,
and sex-act imagery passes the upload gate.

New:

```properties
moderation.blocked-labels=Explicit:60,Explicit Nudity:60,Explicit Sexual Activity:60,Sex Toys:60
```

Naming every level, rather than only `Explicit`, is deliberate. With `Explicit` alone,
a third-level label such as `Exposed Female Genitalia` (parent `Explicit Nudity`) is
caught only if Rekognition also returns its `Explicit Nudity` ancestor as a separate
label. Listing the second-level names means every third-level label matches through
its own direct parent, whether or not ancestors appear in the response.

`Kissing on the Lips` sits under `Non-Explicit Nudity of Intimate parts and Kissing`,
a different branch, and remains allowed — `ModerationPolicyTest` already asserts this
and is the regression guard for the core product feature.

### 3. Feed privacy enforcement

In the feed path of `GenerationsServiceImpl`, after `privacyEnum` is resolved from the
request: unless the request is for the caller's own profile (`isSelfProfile`), force
`privacyEnum` to `PUBLIC`. The client may still *ask* for private content; it receives
it only for its own profile.

`isSelfProfile` is tightened at the same time. Today it is true when the requested
`userCodes` merely *contain* the caller's code, so a request listing the caller and a
victim with `privacy: "private"` would still return the victim's private content. It
becomes true only when every requested code is the caller's own.

The existing exception for user codes `P108`, `P125` and `P109`, which always receive
`BOTH`, is preserved unchanged. Those accounts see every user's private content; this
is recorded under Known gaps rather than altered, since its purpose is not documented.

This is what makes "skip moderation for private uploads" safe, and what makes the paid
privacy feature hold.

### 4. Report-driven takedown

New behaviour in `reportImage`, which currently saves an `ImageFlag` and returns.

After saving the flag, count **distinct** `userCode` values that have flagged the same
`genId`. At the threshold, set the generation's `privateImage` to `true` and send one
alert mail via `SesEmailService`.

The public feed queries `privateImage == false` (`ImagePrivacyEnum.DEFAULT` and
`PUBLIC` both map to `false`), so setting it to `true` removes the generation from the
feed. With §3 in place, a crafted feed request cannot bring it back. The takedown also sets `reportHidden=true`, and the
privacy toggle refuses to make a `reportHidden` generation public (422), so its owner cannot republish it either.
Clearing `reportHidden` is a manual database operation.

Distinct-reporter counting is the whole point: counting rows would let one user hide
any post by calling the endpoint three times. A report from a user who has already
flagged the generation is not saved, and the count takes distinct non-empty
`userCode` values, so pre-existing duplicate rows and anonymous reports cannot move it.
No index is required for correctness; a concurrent double-report can at worst add one
duplicate row, which the distinct count ignores.

The generation is not deleted. The owner keeps it privately; only public visibility is
withdrawn.

Threshold `3` is a property, `moderation.report-hide-threshold`, tunable without a
deploy.

### 5. Package migration

**Flutter app (`PixelAI`):**

- `android/app/build.gradle`: `applicationId = "com.rishyash_studio.icc_world_cup_2024_app"`.
- `targetSdk` must be 36. The Hastara listing's open policy issue is "App must target
  Android 16 (API level 36) or higher", enforced 2026-11-01, and every update to that
  listing is rejected until it is met. `android/app/build.gradle` already sets
  `compileSdk = 36` and `targetSdk = 36`, so this needs verifying, not changing.
- versionCode must exceed Hastara's highest uploaded versionCode, or Play rejects the
  upload. `pubspec.yaml` is currently `7.0.4+44`.
- New `google-services.json` for a Firebase app registered against the new
  applicationId.
- Signing: an update to an existing listing must be signed with that listing's upload
  key. Check Play Console → App integrity for how Hastara is signed. If Hastara's
  upload keystore is unavailable, request an upload-key reset from Google before the
  build is needed.

**Backend (`pixel-ai`):**

`GooglePlayServiceImpl` validates one-time product purchases (credit packs) through the
Play Developer API products endpoint, using a single `app.package-name`. Change the
value to the new package.

Open verification item: the suspended Pixel AI app has been removed from Play. If its
installed builds can no longer complete purchases, a single value is sufficient. If
they can, their tokens will fail validation against the new package, and the property
must become a list. Confirm before release rather than building the list speculatively.

**In-app products** must exist under the target listing with the product IDs the
Pixel AI client requests. Any Hastara products on that listing are unrelated to this
backend and go unused.

## Error handling

| Condition | Behaviour |
|---|---|
| Restricted upload on a public generation | 422, "This image can't be used. Please upload a different photo.", `upload_moderation_logs` row with `REJECTED` |
| Restricted upload on a private generation | Allowed; not sent to Rekognition |
| Rekognition unavailable | 422, row with `ERROR`, `[CRITICAL]` log, throttled alert mail |
| Feed request for another user's private content | Served as `PUBLIC`; private items not returned |
| Third distinct report on a generation | Generation set private, removed from feed, alert mail |
| Repeat report from a user who already flagged it | No additional count; idempotent |
| Mongo failure while hiding a reported generation | Logged `[CRITICAL]`; the flag row still stands |

## Testing

Unit tests in the existing Mockito + AssertJ style:

1. `Explicit Sexual Activity` at 70 is blocked — the gap this change closes.
2. A response containing only `Exposed Female Genitalia` (parent `Explicit Nudity`),
   with no ancestor labels, is blocked — guards against Rekognition omitting ancestors.
3. `Kissing on the Lips` at 99 remains allowed. Regression guard on the core feature.
4. Feed request with `privacy: "private"` for another user's profile returns only
   public generations.
5. Feed request with `privacy: "both"` for the general feed returns only public
   generations.
6. Self-profile request still returns the caller's own private generations.
7. Third distinct reporter hides the generation; the second does not.
8. Same reporter three times does not hide the generation.
9. Hiding sets `privateImage=true` and leaves the generation present.
10. Mongo failure during hide does not propagate to the caller.

Manual verification before submission, on the build that will be uploaded:

- Upload a nude reference image on a public generation → rejected, no credit debited.
- Confirm `upload_moderation_logs` receives the row.
- From a second account, request the first account's profile with `privacy: "private"`
  → no private items returned.
- Confirm the app builds and runs targeting API 36.
- Complete a test purchase under the new package and confirm credits are awarded.

## What is required from the product owner

1. Store listing assets for the target listing: title, short and full description,
   icon, feature graphic, screenshots. None may depict nudity or intimate effects.
2. Privacy policy and Terms URLs that explicitly prohibit generating restricted
   content. The rejected appeal told Google these already exist.
3. Hastara's upload keystore, or an upload-key reset request to Google.
4. Firebase app registration for the target applicationId → `google-services.json`.
5. In-app product IDs and prices to create under the target listing.
6. Cancellation of any active Hastara subscriptions (Play Console → Order management)
   before the update ships.
7. Play Console: content rating questionnaire (declaring user-generated content),
   Data safety form, and the AI-generated content declaration under App content.
8. Trader status declaration — due February 2027; missing it removes the developer
   profile and all its apps globally.
9. Rotation of the credentials currently substituted into the working copy of
   `src/main/resources/application.properties`. They have never been committed and
   must not be.

## Known gaps

1. Generated output reaching the public feed is unmoderated. See Accepted risks 1.
2. Prompt text is unmoderated.
3. Intimate video effects are served by default. See Accepted risks 5.
4. User codes `P108`, `P125` and `P109` receive every user's private content in the
   feed. Preserved as found; purpose undocumented.
5. `Interceptor` accepts a client-supplied `user` JSON header as the authenticated identity without checking a
   token. Anyone can impersonate any user code, including `P108`/`P125`/`P109`, which defeats §3 and §4 for a caller
   outside the app. The Flutter client never sends this header. Left in place by product-owner decision on
   2026-11-22.
