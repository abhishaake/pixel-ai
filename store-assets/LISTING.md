# Google Play store listing text

Limits: title 30 characters, short description 80, full description 4,000.
Play bans in metadata: "best", "#1", "top", "new", "free", prices, ranking claims, ALL CAPS words, emoji in the title or short description, repeated keyword stuffing.

Facts used below (confirmed 22 Nov 2026):
- No free video or daily credit for new users, so the listing never says "free" or "try before you pay".
- Video credits are only taken when a video finishes (backend: `completeVideoEffectJob` debits `VIDEO_EFFECT_COST` = 200 on success only).
- Photos are kept until the user asks for deletion or deletes their profile, and are never used to train AI.
- Support email: mohitpassivework2024@gmail.com.

## App name (30 max) — 29

```
AI Kiss & Hug Video: Pixel AI
```

## Short description (80 max) — 75

```
AI kiss video & hug video maker. Two photos in, video out in about a minute
```

## Full description

```
Pixel AI is an AI kiss video and AI hug video maker with more than 50 photo to video effects. Upload two photos and get a romantic kiss video or a warm hug video, usually in about a minute. One photo is all you need to dance, flex, breathe fire, zoom out to space or step into a magic scene.

You only pay for videos that finish. If a video fails, no credits are used.

AI Kiss Video
Turn two photos into an AI kiss video. Pick Kiss Your Love, Kiss Me or Kiss You! for a romantic moment, or Blow Kiss for a playful one. Great for couples, anniversaries and long-distance surprises.

AI Hug Video
Hug someone you miss. Make an AI hug video with a partner, a friend, a parent or a grandparent.

Old Photos, Alive Again
Old Photo Revival and Old Photo Motion animate a printed family photo, so a favourite memory smiles and moves again.

Tips for old printed photos
1. Lay the photo flat in good daylight.
2. Take a picture of it with your phone, straight on and without glare.
3. Crop it so the face is clear, then upload it.

Couples and Family
Strike a Heart Pose together, try Boyfriends and Girlfriends, or announce good news with Baby Arrived.

AI Dance Videos
Turn any photo into a dance video for Reels and Shorts: Phut Hon Dance, Let's Dance, Girl Dance, plus hip-hop, Latin groove, party bounce and shake-it moves.

Fire, Thunder and Power
Breathe fire with Spitfire, light up with Set on Fire, call lightning with Thunder God, or power up with Go Super and Titan Stride.

Muscle Effects
Turn any photo into a superhero flex with Muscle Show and Muscle Bodybuilding.

Magic and Fantasy
Become a mermaid with Mermaid Spell, get beamed up by a UFO with Alien Kidnap, turn into liquid chrome with Turning Metal, spread your wings with Dark Angel, or become a robot with Anything, Robot.

Viral Video Effects
Zoom from your photo out to space with Earth Zoom Out, or dive back in with Earth Zoom In. Flatten anything with Squish It, spin it in the 360° Microwave, melt it with Melting, or blow it up with Explosion, Powder Explosion and Smash It. Fill the frame with Sakura Flood, fly away with Go Fly!, or walk the red carpet with Red Carpet Time.

Fun Face and Style Effects
Try Gender Swap, Long Hair or Sheep Curls, pull a Tongue Out Face, drift off with AI Sleep, or add a pro look with Focus Lens and Sleek Split. Pets can join the chaos with Pet-pocalypse.

Anime to Real
Turn your anime art into a lifelike photo of a real person.

AI Art
Type an idea and get an AI image in Realistic, Anime, 3D Art, Fiction or Design style.

How it works
1. Choose an effect: AI kiss, AI hug, old photo, dance, fire, muscle, magic, viral effects or anime to real.
2. Upload one or two photos. Everyday phone selfies work.
3. Save your AI video and share it to your favourite social apps.

Credits
Each video effect uses 200 credits. Credits are only used when your video is ready. If a video fails, no credits are used and you can try again.

Your photos
Your photos are never used to train AI. Your videos stay private unless you choose to make them public. Uploaded photos are kept with your account until you ask us to delete them or you delete your profile.

Please only use photos of people who have agreed to it. Pixel AI is a photo to video app for fun, creative content.

Questions, or a video that didn't work? Email us at mohitpassivework2024@gmail.com.
```

## Screenshot order (store-assets/export)

1. ss1 — 2 Photos. 1 AI Kiss.
2. ss2 — Hug Someone You Miss
3. ss3 — Video Failed? No Charge.
4. ss4 — Any Photo Can Dance
5. ss5 — Flex Like a Superhero
6. ss6 — Breathe Fire. Call Thunder.
7. ss7 — Become a Mermaid.
8. ss8 — Anime to Real Life

Spare (not in the store set): AI Art, export with `./store-assets/export.sh art`.

## Effects left out of the store text, and why

All 61 effects in `video_effect_configs` (export of 28 Jun 2026) stay in the app. These are just not named in the listing:

| Effect | Reason |
|---|---|
| Bikini Up, Drunk Pole Dance | Sexual content in metadata (Play metadata policy) |
| Twerk Dance, Jiggle Dance, Squat Shake, Hip Shake | Named neutrally instead ("hip-hop, Latin groove, party bounce, shake-it") |
| French Kiss | Removed after the policy review |
| Squid Game, Oscar Winner, Venom, Jellycat | Third-party trademarks in the listing invite IP complaints |
| Face Punch, Fight, Fire the Gun! | Violence and weapons in metadata |
| AI Jesus Hug | Religious figure; your call whether to name it |
