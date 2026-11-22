# App content declarations for Pixel AI on the Hastara listing

Listing: `com.rishyash_studio.icc_world_cup_2024_app`. Hastara's current declarations are stale (most last edited May 2024, Data safety Feb 2025) and describe a different app. Every answer below is derived from the PixelAI app code and the pixel-ai backend as of 2026-11-22. Items marked **CONFIRM** are inferences the code cannot prove.

## Status in Play Console (2026-11-22)

Approved by the product owner and entered. Everything is saved and **not sent for review**.

| Declaration | Status |
|---|---|
| Privacy policy | Saved — Pixel AI URL (was `sites.google.com/view/hastara-palm-reading-tarot/home`) |
| Ads | Unchanged — already "contains ads" |
| Advertising ID | Saved — Yes; Analytics + Advertising only |
| Data safety | Saved — as the table below, plus **User IDs** (collected, required, app functionality + account management) |
| Sign in details | **Not entered — product owner must add a reviewer Google account** (see below) |
| Content ratings | Saved (IARC terms accepted with product-owner consent). Result: IARC **12+**, ESRB **Teen**, PEGI **Parental guidance**, USK **12+**, ClassInd **14+** |
| Target audience | Saved — **16-17 and 18 and over** (was 13-15, 16-17, 18+) |

### Content rating answers as submitted

Two answers differ from the draft below, after checking the app code:
- **User-generated content is the primary source of content: Yes.** The app opens on the Explore feed (`PixelAI/lib/presentation/view/home_screen.dart:71`), so other users' creations are the first thing shown. This unlocked follow-ups: public nudity sharing No; graphic violence No; block users Yes; report users Yes; chat moderation No; invite-only No.
- **Online content: Yes.** IARC lists "generated AI content" as an example. Catalog violence, sexual material/nudity, offensive language and drugs: all No.
- Age-restricted products No; precise location No; digital purchases Yes (not random items); cash rewards/NFTs No; browser No; news/education No.

### Why 16+ and not 18+

The product owner asked to match competitors. Play does not publish target-audience declarations, only IARC ratings. The top 11 results for "AI kiss hug video" (India, 2026-11-22) are rated 3+ (7 apps), 12+ (3 apps, including the 1M+ leader) and 18+ (1 app). Pixel AI's own Terms set the floor: "You must be at least 16 years old to use this App." Declaring 13-15 would contradict the Terms, so the audience is 16-17 and 18+.
| Store listing text | Saved as draft (see `full-description.txt`, `short-description.txt`) |
| One-time products | Created and **Active**: `pixel_product_coin_80` ₹20, `_450` ₹99, `_2500` ₹499, `_5800` ₹999 (India); other regions auto-converted |

### Sign in details — text to paste

Google states reviewers cannot create accounts or use their own, so add a dedicated reviewer Google account (email + password) in Play Console → App content → Sign in details → Yes → Add details, top up its credits, and paste this into "Any other information" (427 of 500 characters):

```
Sign-in is required. Tap "Continue with Google" and sign in with the account above. It already has credits, so no purchase is needed.

Safety checks:
- Create a PUBLIC image with a reference photo. Photos with nudity or sexual content are rejected ("This image can't be used...") and no credits are charged.
- Use Report on any image in the public feed. Images reported by several users are removed from the feed automatically.
```

Then tick "Sign in details in this declaration provide full access…" — only true once the account works.

## Privacy policy
`https://sites.google.com/view/pixel-ai-privacy-polocy/home` — the URL the app itself links to.

## Ads
**Yes, the app contains ads.** `google_mobile_ads` 5.2.0 (AdMob) is a dependency.

## Advertising ID
**Yes.** Used for **Advertising or marketing** (AdMob) and **Analytics** (Firebase Analytics). The AdMob SDK merges the `AD_ID` permission into the manifest.

## App access (Sign in details)
**All or some functionality is restricted.** Instructions for the reviewer:
> Sign in with any Google account (or Apple). New accounts receive starter credits, enough to create images and video effects. No test account or code is needed.

## Target audience and content
**16-17 and 18 and over** (as entered; the original draft proposed 18+ only, see "Why 16+ and not 18+" above). Matches the Terms' minimum age of 16. Not designed for children; no appeal to children.

## Content ratings (IARC questionnaire)
- Category: **All other app types**
- Violence, fear, gambling, controlled substances, crude humour: **No**
- Sexuality / nudity: **No.** Sexual content is prohibited by the Terms, and restricted uploads for public creations are rejected. Kissing and hugging animations are not sexual content under IARC. **CONFIRM** you are comfortable answering No given the kiss/hug effects.
- Profanity: **No** (none in app-authored content)
- Users can interact or exchange content: **Yes** (public feed of user creations)
- Shares user's current location: **No**
- Digital purchases: **Yes** (credit packs)
- Unrestricted internet / web browser: **No**

## Data safety
Encrypted in transit: **Yes** (HTTPS backend, Firebase). **CONFIRM** the backend is served only over HTTPS.
Users can request deletion: **Yes** — `https://forms.gle/4rBrT89Zjof7Lcnm8` (the app's "Delete account" option).

| Data type | Collected | Shared | Required? | Purposes |
|---|---|---|---|---|
| Personal info › Name | Yes | No | Required | Account management, App functionality |
| Personal info › Email address | Yes | No | Required | Account management, App functionality |
| Photos and videos › Photos | Yes | No | Optional | App functionality |
| Photos and videos › Videos | Yes | No | Optional | App functionality (generated video effects) |
| Financial info › Purchase history | Yes | No | Optional | App functionality |
| App activity › App interactions | Yes | No | Required | Analytics |
| App activity › Other user-generated content (prompts) | Yes | No | Optional | App functionality |
| App info and performance › Crash logs | Yes | No | Required | Analytics |
| App info and performance › Diagnostics | Yes | No | Required | Analytics |
| Device or other IDs | Yes | **Yes** (Google AdMob) | Required | Advertising or marketing, Analytics, Fraud prevention |
| Location › Approximate location | Yes | **Yes** (Google AdMob, from IP) | Required | Advertising or marketing, Analytics |

Notes:
- Uploads and prompts sent to Ideogram and GoEnhance are processing by service providers acting on your behalf, which Play does not count as "sharing".
- Creations a user posts to the public feed are shared at the user's direction, which Play also exempts.
- The Device-ID and approximate-location rows follow Google's own published AdMob disclosure guidance.

## Health apps / Financial features / Government apps
**None / Not applicable** — unchanged from Hastara's answers.
