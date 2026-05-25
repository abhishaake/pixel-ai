# Video Effect API — App Integration Guide

Branch: `go_enahance_ai`

---

## Overview

The Video Effect feature lets users apply animated effects to a reference image (their photo or any image) and receive a short video clip. The backend submits the job to **GoEnhance**, polls for the result, and returns a `GenerationsDTO` with the video URL — the same shape used for image generations.

**Fixed cost:** 200 credits per video effect generation.

---

## Flow Summary

```
App                              Backend                        GoEnhance
 │                                  │                               │
 ├─ GET /effects ─────────────────► │                               │
 │◄──── [effectId, label, url] ─── │                               │
 │                                  │                               │
 ├─ POST /generate/goenhance ─────► │                               │
 │   (multipart: body + image)      ├─ upload image to S3           │
 │                                  ├─ POST /videoeffect/generate ► │
 │                                  │◄── imgUuid ─────────────────── │
 │                                  │                               │
 │                                  │  poll up to 20x / 3s each     │
 │                                  ├─ GET /jobs/detail?img_uuid ──►│
 │                                  │◄── status: pending/success ── │
 │                                  │                               │
 │  If ready within ~60s:           │                               │
 │◄── GenerationsDTO (video URL) ── │                               │
 │                                  │                               │
 │  If still processing:            │                               │
 │◄── { message: "We will         ──│                               │
 │      inform you when ready" }   │                               │
 │                                  │ (scheduler retries every 5min)│
```

---

## Endpoints

### 1. Get Available Effects

```
GET /api/v1/images/effects
Authorization: Bearer <token>
```

**Response `200 OK`**

```json
{
  "data": [
    {
      "effectId": "hug",
      "label": "Hug",
      "url": "https://cdn.goenhance.ai/..."
    },
    {
      "effectId": "twerk-dance",
      "label": "Twerk Dance",
      "url": "https://cdn.goenhance.ai/..."
    }
  ]
}
```

| Field | Type | Description |
|---|---|---|
| `effectId` | `String` | Pass this as `effect` in the generate request |
| `label` | `String` | Display name for the UI |
| `url` | `String` | Preview video/GIF URL to show users before they pick |

**Known effect IDs** (fetched dynamically from GoEnhance, refreshed daily at 2 AM):

| Effect ID | Display |
|---|---|
| `hug` | Hug |
| `kiss` | Kiss |
| `french-kiss` | French Kiss |
| `blow-kiss` | Blow Kiss |
| `twerk-dance` | Twerk Dance |
| `hip-shake` | Hip Shake |
| `jiggle-dance` | Jiggle Dance |
| `squat-shake` | Squat Shake |
| `phut-hon-dance` | Phut Hon Dance |
| `turning-metal` | Turning Metal |
| `anime2real` | Anime to Real |
| `muscle-show` | Muscle Show |
| `mermaid-spell` | Mermaid Spell |
| `thunder-god` | Thunder God |
| `spitfire` | Spitfire |
| `set-on-fire` | Set on Fire |
| `alien-kidnap` | Alien Kidnap |

> Always fetch from `/effects` at runtime rather than hardcoding — the list is synced from GoEnhance and may change.

---

### 2. Generate Video Effect

```
POST /api/v1/images/generate/goenhance
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

**Parts**

| Part | Type | Required | Description |
|---|---|---|---|
| `body` | `String` (JSON) | Yes | JSON-serialized `VideoEffectRequest` |
| `reference_image` | `File` | Yes | The image to apply the effect to (JPEG/PNG) |

**`body` JSON schema**

```json
{
  "effect": "hug",
  "resolution": "720p",
  "privateImage": false
}
```

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `effect` | `String` | **Yes** | — | Effect ID from `/effects` endpoint |
| `resolution` | `String` | No | `"720p"` | `"480p"`, `"540p"`, or `"720p"` |
| `privateImage` | `Boolean` | No | `null` | `true` to keep off the public feed |

**Example (multipart, Swift pseudocode)**

```swift
let request = MultipartFormData()
request.add(
    value: #"{"effect":"hug","resolution":"720p","privateImage":false}"#,
    name: "body"
)
request.add(
    file: imageData,
    name: "reference_image",
    fileName: "image.jpg",
    mimeType: "image/jpeg"
)
POST("/api/v1/images/generate/goenhance", headers: [.bearer(token)])
```

**Example (multipart, Kotlin pseudocode)**

```kotlin
val body = MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addFormDataPart("body", """{"effect":"hug","resolution":"720p","privateImage":false}""")
    .addFormDataPart(
        "reference_image", "image.jpg",
        RequestBody.create("image/jpeg".toMediaType(), imageBytes)
    )
    .build()

retrofit.generateVideoEffect(authHeader, body)
```

---

## Response Shapes

### Case A — Video ready (synchronous, ~60s window)

HTTP `201 Created`

```json
{
  "data": {
    "generationId": "6657a1f3e4b0f3a1d2c3b4a5",
    "userCode": "P101",
    "userName": "Alex",
    "userImgUrl": "https://...",
    "images": [
      {
        "imageId": 1,
        "url": "https://av-pixel.s3.ap-south-1.amazonaws.com/P101_1716800000.mp4",
        "thumbnail": "https://av-pixel.s3.ap-south-1.amazonaws.com/P101_1716800000.mp4"
      }
    ],
    "model": "hug",
    "privateImage": false,
    "characterRefImageUrl": "https://av-pixel.s3.ap-south-1.amazonaws.com/P101_ref_1716800000.jpeg"
  }
}
```

Key fields:

| Field | Description |
|---|---|
| `generationId` | Unique ID for this generation |
| `images[0].url` | **The video URL** (mp4 hosted on S3) |
| `images[0].thumbnail` | Same as `url` for videos |
| `model` | Echo of the `effect` ID used |
| `characterRefImageUrl` | The uploaded reference image URL |
| `message` | **Absent** — video is ready |

### Case B — Still processing (async fallback)

HTTP `201 Created`

```json
{
  "data": {
    "message": "We will inform you when your video is ready"
  }
}
```

The backend will complete the job via the **scheduler** (runs every 5 minutes). The app should show a "processing" state and poll the user's generation feed or use push notifications when ready.

> **Detection:** check `data.message != null`. If present, the video is not ready yet; `images` will be empty/absent.

---

## Error Responses

| HTTP Status | Scenario | `error` message |
|---|---|---|
| `400 Bad Request` | `effect` field missing or blank | `"Effect is required"` |
| `400 Bad Request` | `body` JSON invalid | `"Invalid request"` |
| `400 Bad Request` | No image file attached | `"Reference image is required"` |
| `402 Payment Required` | User has < 200 credits | `"Not enough credits"` |
| `423` (lock) | Another generation is in progress | `"1 Generation already in progress, Please wait.."` |
| `500` | GoEnhance job submission failed | `"Failed to submit video effect job, please try again"` |
| `500` | Other server error | `"Some error occurred, please try again"` |

---

## Feed Integration — Fetching Video Effects

By default, the `/filter` feed **excludes** video effects. To include them (e.g. on a user's own profile page showing all content):

```
POST /api/v1/images/filter?includeVideoEffects=true
Authorization: Bearer <token>
Content-Type: application/json
```

Without the query param (or `includeVideoEffects=false`), video effect generations are filtered out of the public image feed.

**Identifying a video generation in the feed:**

The `Generations` document has `videoEffect: true` for all video effect entries — use this to render a video player instead of an image view. The `model` field will contain the effect ID (e.g. `"hug"`).

---

## Credit Cost

| Action | Cost |
|---|---|
| Generate video effect | **200 credits** (fixed) |

Credits are debited asynchronously after the video is successfully stored. Check the user's credit balance before showing the generate button; surface a "buy credits" flow on `402`.

---

## Background Job / Scheduler Behaviour

If the GoEnhance job doesn't complete within the synchronous poll window (~60 seconds, 20 × 3s polls), the backend returns Case B and the job is tracked in the `video_effect_jobs` collection.

The `VideoEffectJobScheduler` runs every **5 minutes** and picks up any `PENDING` jobs, completes them, and debits credits. The app should either:

- **Poll** `GET /api/v1/images/filter?includeVideoEffects=true` on the user's profile until the generation appears, or
- **Listen for a push notification** (when that integration is wired).

Job status progression: `PENDING → COMPLETED | FAILED`

---

## Dev / Test — Mock Mode

The GoEnhance client has a mock mode controlled by:

```properties
goenhance.mock.enabled=true
```

When enabled, the client returns a fixed mock video URL and skips the real API call. Use this in staging/dev to test the full flow without consuming GoEnhance credits.
