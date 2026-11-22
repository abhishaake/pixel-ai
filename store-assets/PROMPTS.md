# Image prompts for the store graphics

Generate each image in ChatGPT and save it into `store-assets/images/` under the **file name** shown. JPG or PNG both work. Then run `./store-assets/export.sh`.

## How to get consistent faces

- **Use one ChatGPT chat per screenshot.** Generate the input photo(s) first, then the result in the same chat, starting with *"Using the exact same person/people from the previous image…"*. This keeps the face identical between the "before" polaroid and the result.
- **Ask for portrait 2:3** (1024×1536) unless the prompt says otherwise. The templates crop to fit, so keep the subject centred with some space around it.
- **If a result looks plastic,** reply with: *"More natural: real skin texture, slight imperfections, less retouching, iPhone photo."*

## Style line (paste at the end of every prompt)

```
Photorealistic, shot on iPhone 15 Pro, natural skin texture, soft cinematic lighting, vivid but natural colours, vertical 2:3, no text, no watermark, no logos, no borders.
```

## Rules so Play accepts these

- **People:** adults in their mid-20s to 30s, fictional, never celebrities. The grandmother is the only exception (an older adult).
- **Clothing:** everyday outfits in every image. No swimwear, lingerie, crop tops or tight focus on hips or chest.
- **Kisses:** closed-mouth kisses only, framed from the waist up.
- **Dances:** full body, front or three-quarter view, never from behind.

---

## 01 · AI Kiss

**`kiss_in1.jpg`**
```
Casual selfie of a white American woman, 26, long honey-blonde wavy hair, freckles, warm smile, wearing a light blue denim shirt, standing on a city street in golden hour, phone held at arm's length, head and shoulders framing.
```

**`kiss_in2.jpg`**
```
Casual selfie of a white American man, 28, short tousled brown hair, light stubble, friendly smile, wearing a white linen shirt, outdoor café background, head and shoulders framing.
```

**`kiss.jpg`**
```
Using the exact same woman and man from the previous two images: a romantic moment as a still frame from a video. They share a gentle closed-mouth kiss on the lips, eyes closed, his hand softly on her cheek. Framed waist-up, same outfits, rooftop at sunset with warm city bokeh lights behind them. Tender, cinematic, wholesome.
```

## 02 · AI Hug

**`hug_in1.jpg`** (old photo; the template adds a sepia filter)
```
A slightly faded 1990s family snapshot: a white American grandmother, about 60, short silver-blonde curls, wearing a soft green knitted cardigan and pearl necklace, sitting on a floral home sofa and smiling kindly at the camera. Film grain, warm colour cast, mild flash, slight blur, like a scanned print from 1998.
```

**`hug_in2.jpg`**
```
Modern selfie of a white American man, 27, short light-brown hair, wearing a navy hoodie, gentle smile, indoors near a window with soft daylight, head and shoulders.
```

**`hug.jpg`**
```
Using the same grandmother and the same young man from the previous images: an emotional reunion as a still frame from a video. The grandson hugs his grandmother warmly, her eyes closed and smiling, his chin resting on her shoulder. Waist-up, cosy living room with warm lamp light and soft bokeh. Heartfelt, joyful, family.
```

## 03 · Dance

Use the same woman for all six images.

**`dance_in.jpg`**
```
Full-body casual photo of a white American woman, 25, wavy shoulder-length auburn hair, wearing a white t-shirt, high-waisted blue jeans and white sneakers, standing relaxed and smiling in a bright urban plaza.
```

**`dance.jpg`**
```
Using the exact same woman and outfit: a still frame from a fun dance video. She is mid-dance with arms raised joyfully and one knee lifted, big laugh, full body, front view, colourful neon-lit street at dusk, motion energy, slight motion blur on her hands.
```

**`twerk.jpg`**
```
Same woman, same outfit: still frame from an energetic hip-hop dance video. Full body, three-quarter FRONT view facing the camera, knees bent in a confident dance stance, hands on knees, playful grin. Bright studio with a pink and purple gradient wall. Fun, upbeat, not suggestive.
```

**`hip_shake.jpg`**
```
Same woman, same outfit: still frame from a Latin dance video. Full body, front view, hips swaying to one side, one arm up and one on her waist, laughing. Warm orange studio light.
```

**`jiggle.jpg`**
```
Same woman, same outfit: still frame from a bouncy party-dance video. Full body, front view, jumping slightly with shoulders shimmying and arms loose, joyful expression. Confetti in the air, purple party lights.
```

**`squat_shake.jpg`**
```
Same woman, same outfit: still frame from a fitness-style dance video. Full body, front view, low squat with arms extended forward, energetic smile. Teal and blue gym-studio lighting.
```

## 04 · Muscle Show

**`muscle_in.jpg`**
```
Casual photo of a white American man, 27, slim build, short blond hair, wearing an oversized grey hoodie, standing in a city park and smiling a little shyly, waist-up.
```

**`muscle.jpg`**
```
Using the exact same man's face: a still frame from a transformation video. He now has a huge superhero-level muscular physique in a fitted black tank top, flexing both biceps with a proud, playful grin. Waist-up, dramatic orange and red rim lighting, gym background with bokeh. Fun and exaggerated, like a viral effect.
```

## 05 · Fire & Thunder

Use the same man for all four images.

**`power_in.jpg`**
```
Casual selfie of a white British man, 29, short dark hair with a fade, trimmed beard, wearing a dark green bomber jacket, neutral confident expression, urban rooftop, head and shoulders.
```

**`spitfire.jpg`**
```
Using the exact same man: cinematic still from a VFX video. He breathes a powerful stream of bright orange fire from his mouth toward the side of the frame like a dragon, embers flying, intense eyes, dark night street. Waist-up, epic movie-poster lighting. Fantasy special effect, no injury.
```

**`set_on_fire.jpg`**
```
Same man: cinematic still from a VFX video. His body is surrounded by swirling magical orange flames like a fire superhero, calm and powerful expression, glowing embers, dark background. Waist-up. Fantasy effect, he is unharmed.
```

**`thunder_god.jpg`**
```
Same man: cinematic still from a VFX video. Blue-white lightning crackles around him and from his raised fist, glowing electric eyes, storm clouds behind. Waist-up, epic god-of-thunder vibe, original design (no comic-book costumes).
```

## 06 · Magic

Use the same woman for all four images.

**`magic_in.jpg`**
```
Casual selfie of a white European woman, 24, long straight light-brown hair, blue eyes, white sweater, soft smile, cafe window light, head and shoulders.
```

**`mermaid.jpg`**
```
Using the exact same woman: magical still from a VFX video. She is transformed into a mermaid with a shimmering teal and violet tail, sitting on a rock in a glowing underwater cave, hair floating. Her upper body is fully covered by a long-sleeved, high-neck top made of iridescent fish scales that matches the tail, no bare midriff, no cleavage. Fairy-tale, family-friendly, full body. Vertical 9:16.
```

**`alien.jpg`**
```
Same woman: funny sci-fi still from a VFX video. A UFO hovers above her at night, shining a green tractor beam as she floats up off a country road, arms out, surprised expression. Full body, cinematic. Vertical 9:16.
```

**`metal.jpg`**
```
Same woman: still from a VFX video. Liquid chrome metal is spreading up from her hands and forearms, both arms raised in front of her chest with palms open, the mirror-like metal rippling over her sleeves. Her loose white sweater and face are still normal, amazed smile. Studio with blue light. Waist-up. Vertical 9:16.
```

## 07 · Anime to Real

Both halves must line up: same pose, same framing, same background layout.

**`anime.jpg`**
```
Original anime character illustration (not from any existing anime): young woman with silver-lavender bob hair, big violet eyes, wearing a red bomber jacket, standing in a neon Tokyo-style street at night, facing camera, waist-up, centred. Clean modern anime style. Vertical 2:3.
```

**`real.jpg`**
```
Turn the previous anime character into a real person, keeping exactly the same pose, framing, outfit, hair colour and background layout: a real young woman with a silver-lavender bob, violet-tinted contact lenses, red bomber jacket, neon street at night. Photorealistic cosplay-quality photo, waist-up, centred.
```

## 08 · AI Art

The template shows the prompt "a girl in a neon city at night, rain". Use that subject in all four styles. **Best option:** generate these in your own app's AI Art feature (Pixel 4.0), so they are real app output.

**`art_realistic.jpg`**
```
A girl in a neon city at night, rain, photorealistic, reflections on wet pavement, umbrella, cinematic.
```

**`art_anime.jpg`**
```
A girl in a neon city at night, rain, anime illustration style, vibrant pink and cyan, detailed background.
```

**`art_3d.jpg`**
```
A girl in a neon city at night, rain, 3D Pixar-like render, cute stylised character, glossy lighting.
```

**`art_fiction.jpg`**
```
A girl in a neon city at night, rain, cyberpunk science-fiction concept art, holograms, flying cars, epic.
```

## Icon · `icon.jpg` (square 1:1)

```
Square app icon photo, 1:1. Close-up of a young white couple (blonde woman, brown-haired man) embracing cheek-to-cheek, faces almost touching, eyes closed, gentle smiles, a moment just before a kiss. Warm sunset bokeh in orange and pink, glowing rim light. Subjects centred in the middle 70% of the frame, faces large, simple background. Leave the bottom-right corner fairly empty for an "AI" label. Photorealistic, no text.
```

## Feature graphic cards

No new images needed. Copy three results you already have:

- `hug.jpg` → `fg_hug.jpg`
- `kiss.jpg` → `fg_kiss.jpg`
- `dance.jpg` → `fg_dance.jpg`

---

### Before you publish

These are mock-ups of what each effect does. Play requires screenshots to show the real app experience. Once the app is live, run `kiss_in1` + `kiss_in2`, `power_in` and the other input photos through your real effects. Replace any frame that looks noticeably different from what the app actually produces.
