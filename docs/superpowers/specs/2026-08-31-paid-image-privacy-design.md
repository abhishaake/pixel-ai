# Paid image privacy unlock — design

**Date:** 2026-08-31
**Status:** Approved for planning
**Repos:** `pixel-ai` (Spring Boot backend), `PixelAI` (Flutter app)

## Problem

Image visibility is decided once, at generation time. `GenerateRequest.privateImage`
is written to `Generations.privateImage` and never changes again. A user who
generates a public image and later regrets it has no way to hide it, and a user who
paid the generation-time `privacyCost` cannot make that image public again.

## Goal

Add a control on the image details screen that lets an image's owner change its
visibility after creation. The first time an owner makes a given image private they
are charged a one-time fee in tokens. After that, they may switch that image between
public and private as often as they like, free.

## Scope

In scope:

- One new backend endpoint to set an image's visibility, charging on first unlock.
- A `privacyUnlocked` flag on `Generations`, an admin-configurable price, and a new
  transaction order type.
- An owner-only control on the Flutter image details screen: a priced button while
  locked, a free switch once unlocked.
- Video-effect generations, which are rows in the same `generations` collection and
  are covered by the same endpoint and the same control.

Out of scope:

- Bulk or profile-wide privacy changes.
- Refunding an unlock.
- Any change to generation-time privacy pricing (`ModelPricing.privacyCost`).
- Pushing visibility changes to other users' already-loaded feeds.

## Key decisions

### Generation-time payment already counts as an unlock

An image generated with `privateImage = true` cost its owner
`ModelPricingDTO.getFinalPrivacyCost()` at creation. That owner is treated as having
already paid, and toggles that image freely — they are never charged the unlock fee.

This is why a separate persisted flag is required rather than reading `privateImage`
alone. Once such an owner switches the image to public, `privateImage` becomes
`false` and can no longer imply payment. The flag records the entitlement
independently of the current visibility.

Effective unlocked state is therefore:

```java
Boolean.TRUE.equals(generation.getPrivacyUnlocked())
    || Boolean.TRUE.equals(generation.getPrivateImage())
```

The second clause covers every pre-existing private image, so **no data migration is
needed**. Every write through the new endpoint also sets `privacyUnlocked = true`,
which is what keeps the entitlement alive across a flip back to public.

### One endpoint, not two

A single `PUT` both charges (when required) and applies the new visibility. A
separate purchase step followed by a separate apply step would let a client crash
between the two, leaving a user charged with nothing to show for it.

### Price lives in `AdminConfig`

`AdminConfig` is already loaded into `Cache.adminConfigMap` at startup and read
through `AdminConfigService`, so the price is tunable in production without a deploy
and is the same for every image regardless of the model that produced it. Reusing
the per-model `ModelPricing.privacyCost` was rejected: the same button showing
different prices on different images is confusing.

The server sends the price to the app in the generation payload rather than the app
assuming `50`, so there is one source of truth and no client/server drift.

## Backend design (`pixel-ai`)

### Data model

`com.av.pixel.dao.Generations` — one new field:

```java
Boolean privacyUnlocked;
```

`com.av.pixel.dao.AdminConfig` — one new field:

```java
private Integer privacyUnlockCost;
```

`com.av.pixel.enums.OrderTypeEnum` — add `PRIVACY_UNLOCK`, so the debit is
distinguishable from `IMAGE_GENERATION` and `VIDEO_EFFECT` in the `transactions`
collection.

`com.av.pixel.constants.Constants` — add:

```java
public static final Integer DEFAULT_PRIVACY_UNLOCK_COST = 50;
```

### Price resolution

`AdminConfigService.getPrivacyUnlockCost()` returns
`adminConfig.getPrivacyUnlockCost()` when the cached config holds a non-null value,
and `Constants.DEFAULT_PRIVACY_UNLOCK_COST` otherwise.

Note the deliberate difference from the sibling `getDefaultCredits()`, which returns
`0` when unset. Returning `0` here would give the feature away for free the moment
the config document was missing a field. Falling back to the default price fails
closed instead.

### Endpoint

```
PUT /api/v1/images/privacy      @Authenticated
```

Request — `com.av.pixel.request.UpdateImagePrivacyRequest`:

```java
String generationId;
Boolean privateImage;
```

Response — `com.av.pixel.response.ImagePrivacyResponse`:

```java
String  generationId;
Boolean privateImage;      // visibility after the call
Boolean privacyUnlocked;   // always true on success
Integer chargedCredits;    // 0 when the change was free
Integer availableCredits;  // balance after the call
```

`availableCredits` lets the app refresh its token display without a second request.
`chargedCredits` tells the app whether to show a "50 ✦ spent" confirmation or stay
silent.

Wired into `ImagesController` alongside the existing `PUT /action` and `PUT /view`
methods, delegating to `GenerationsService.updateImagePrivacy(UserDTO, UpdateImagePrivacyRequest)`.

### Service logic

`GenerationsServiceImpl.updateImagePrivacy`:

1. Validate: `generationId` non-empty and a valid `ObjectId`, `privateImage`
   non-null. Otherwise `400`.
2. Load the generation; `404` if absent or soft-deleted.
3. **Ownership:** `generation.getUserCode()` must equal `userDTO.getCode()`, else
   `403`. Without this check any authenticated user can privatise anyone's image.
4. Already unlocked (per the expression above) → set `privateImage`, set
   `privacyUnlocked = true`, save, return with `chargedCredits = 0`.
5. Locked and the request asks for `privateImage = false` → the image is already
   public and nothing was paid for. No-op, return current state with
   `chargedCredits = 0`. Never charge to make something public that already is.
6. Locked and the request asks for `privateImage = true` → the charge path:
   1. `locker.tryLock("privacy_unlock_" + generationId, 10)`, mirroring the
      `"generation_" + userCode` lock in `generate`. This is the double-tap and
      concurrent-request guard; failure to acquire throws
      `new Error(HttpStatus.CONFLICT, "Privacy change already in progress, please wait..")`.
   2. Resolve cost from `AdminConfigService.getPrivacyUnlockCost()`.
   3. Load the user's credit, creating it via `createNewUserCredit` if absent, as
      `generate` does.
   4. `available < cost` → `throw new Error(HttpStatus.PAYMENT_REQUIRED, "Not enough credits")`.
      This matches the existing message and status at `GenerationsServiceImpl:159`,
      so the app's established 402-to-paywall path applies with no new client
      handling.
   5. Set `privateImage = true` and `privacyUnlocked = true`; save the generation.
   6. Debit via
      `userCreditService.debitUserCredit(userCode, cost, OrderTypeEnum.PRIVACY_UNLOCK, "SERVER", generationId)`,
      **synchronously**. `generate` fires its debit and forgets, but here the
      resulting balance is part of the response.
   7. Unlock in a `finally`, as the surrounding code does.

**Write-order tradeoff.** The generation is saved before the debit. If the debit then
fails, the user receives one unlock for free; the alternative order risks charging a
user whose save then fails, which is worse for them. The failure is logged at error
level and is monetarily trivial.

### DTO exposure

`GenerationsDTO` gains:

```java
Boolean privacyUnlocked;
Integer privacyUnlockCost;
```

Both are populated in `GenerationsMap.toGenerationsDTO`, which already maps
`privateImage`. `privacyUnlockCost` is set unconditionally from
`AdminConfigService` so every generation in a feed page carries the price and the
app never hardcodes it.

`GenerationsDTO` is annotated `@JsonInclude(NON_EMPTY)`. For `Boolean` and `Integer`
that behaves as `NON_NULL`, so `false` and `0` still serialise; only `null` is
omitted. The app must therefore treat an absent `privacyUnlocked` as `false`.

## App design (`PixelAI`)

### Model

`Generation` in `lib/domain/model/response/image_filter_response_model.dart`
currently does not parse `privateImage` at all. Add three fields:

```dart
bool? privateImage;      // mutable
bool? privacyUnlocked;   // mutable
final int? privacyUnlockCost;
```

parsed in `fromJson` and emitted in `toJson`. The first two are non-`final` so a
successful toggle can mutate the instance in place. The details screen and
`ImageListingPaginationBloc` hold the same `Generation` object, so an in-place
mutation is reflected in the profile grid with no refetch.

### Data layer

- `lib/domain/model/request/update_image_privacy_request_model.dart` — a new request
  model.
- `pixel_ai_service.dart` — `@PUT("/api/v1/images/privacy")`, returning
  `HttpResponse<BaseAPIResponse<ImagePrivacyData>>`.
- `repository.dart` — `updateImagePrivacy(...)` wrapping it in `handleResponse`,
  following the shape of `reportImage`.

### State

A new `ImagePrivacyCubit` with states `initial / loading / success(privateImage,
availableCredits, chargedCredits) / error(message, showBuyFlow)`, provided at the
details screen.

The nearby `reportImage` calls `locator<PixelRepository>()` straight from the
widget, but that call is fire-and-forget. This one needs loading, error, and 402
states; threading those through `setState` inside an already 1713-line file would
make it worse. `showBuyFlow` is set on status `402`, matching the flag that
`GenerateImageBloc` and `GenerateVideoBloc` already use.

### UI

In `ImageDetailUserInfoWidget`, beside the existing Download and Block-user buttons
(`image_detail_view_screen.dart:1254`), rendered only when
`BlocProvider.of<LoginBloc>(context).loginResponseModel?.user?.code == imageDetail.userCode`.

**Locked** (`privacyUnlocked != true && privateImage != true`) — a `MyFlatButton`
reading `Make private · <cost> ✦`, styled like its Download/Block siblings. Tapping
opens a confirmation dialog before any charge:

> Make this image private for 50 ✦?
> After this you can switch it between public and private anytime, free.
>
> [Cancel] [Confirm]

The dialog uses the live `privacyUnlockCost` from the payload, not a literal.

**Unlocked** — a `Switch` labelled `Private`, consistent with the privacy switches
already in `generate_image_screen` and `generate_video_screen`. Flips fire
immediately with no dialog. While in flight the control is disabled; on failure it
reverts to its previous position.

**Feedback** — success shows `MyFlushbarHelper.showSuccessSnackbar`; failure shows
`showFailureSnackbar`. When `showBuyFlow` is set, navigate to
`RouteName.inAppPurchase` as `home_screen.dart:142` does.

Video-effect generations get the identical control; nothing keys off
`videoEffect`.

## Error handling

| Condition | Status | App behaviour |
|---|---|---|
| Missing/invalid `generationId` or null `privateImage` | 400 | Failure snackbar |
| Generation not found or deleted | 404 | Failure snackbar |
| Caller is not the owner | 403 | Failure snackbar (control is hidden for non-owners, so this is defence in depth) |
| Insufficient credits | 402 | Failure snackbar, then navigate to the purchase screen |
| Concurrent change in progress (lock not acquired) | 409 | Failure snackbar; switch reverts |

## Testing

Backend, against `GenerationsServiceImpl.updateImagePrivacy`:

- Locked public image → private: debits exactly the configured cost, sets both
  flags, writes a `PRIVACY_UNLOCK` transaction.
- Second toggle on the same image (either direction): charges `0`.
- Image generated private (`privateImage = true`, `privacyUnlocked` null) → public:
  charges `0` and sets `privacyUnlocked = true`.
- That same image back to private afterwards: still charges `0`.
- Non-owner caller: `403`, no debit, no write.
- Balance below cost: `402`, no debit, generation unchanged.
- Locked image, request `privateImage = false`: no-op, no debit.
- `AdminConfig.privacyUnlockCost` null: falls back to `50`, not `0`.

App:

- `Generation.fromJson` parses all three new fields and defaults a missing
  `privacyUnlocked` to false.
- `ImagePrivacyCubit` emits `error(showBuyFlow: true)` on a 402 and
  `error(showBuyFlow: false)` on other failures.
- A failed toggle leaves `Generation.privateImage` at its original value.

## Known limitations

**Other users' loaded feeds are stale.** Making an image private removes it from
subsequent `filterImages` results, but users who already scrolled past it keep it in
memory until they refetch. Accepted.

**Legacy rows with `privateImage: null`.** `findByFilters` filters on
`Criteria.where("privateImage").is(false)`, which does not match `null`. Any such
rows are already absent from the public feed today — pre-existing behaviour, not
introduced here. Toggling one to public writes an explicit `false` and surfaces it,
which is the correct outcome.
