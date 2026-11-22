#!/usr/bin/env bash
# Re-encodes the GoEnhance demo clips (public/clips/*.mp4) into flicker-free versions (public/clips30/).
# Each output starts at the moment the scene needs, already sped up, at a constant 30 fps,
# and every frame is a keyframe, so Remotion never has to seek or retime video while rendering.
# Format per line: <name> <start seconds> <speed> <seconds to keep>
set -euo pipefail
cd "$(dirname "$0")/public"
mkdir -p clips30

while read -r name from rate keep; do
  [[ -z "$name" || "$name" == \#* ]] && continue
  ffmpeg -nostdin -v error -y -ss "$from" -i "clips/$name.mp4" -t "$(echo "$keep * $rate" | bc -l)" \
    -vf "setpts=PTS/$rate,fps=30,scale=trunc(iw/2)*2:trunc(ih/2)*2,format=yuv420p" \
    -an -c:v libx264 -preset slow -crf 14 -g 1 -keyint_min 1 -sc_threshold 0 -movflags +faststart \
    "clips30/$name.mp4"
  echo "clips30/$name.mp4"
done <<'EOF'
# name             from  speed keep(s)
kiss               0     1.4   3.7
hug                0     1.6   3.2
old_photo_revival  0     1.6   3.2
phut_dance         0     1.2   3.4
spitfire           0.6   1.0   1.4
thunder_god        3.4   1.1   1.2
set_on_fire        2.0   1.3   1.2
mermaid            3.3   1.2   1.3
alien              1.4   1.3   1.1
metal              2.6   1.3   1.1
EOF
