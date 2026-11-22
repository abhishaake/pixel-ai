#!/usr/bin/env bash
# Renders every store graphic to exact Google Play sizes with headless Chrome.
#   ./store-assets/export.sh            -> all assets
#   ./store-assets/export.sh ss1 icon   -> only those
# Output: store-assets/export/
#   ss1..ss8.png   1080x1920, 24-bit PNG (no alpha), phone screenshots
#   icon.png       512x512, 32-bit PNG, app icon (Play rounds the corners itself)
#   feature.png    1024x500, 24-bit PNG (no alpha), feature graphic
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
OUT="$DIR/export"
CHROME="${CHROME:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"
PAGE="file://$DIR/templates/index.html"

ALL=(ss1 ss2 ss3 ss4 ss5 ss6 ss7 ss8 icon feature)
IDS=("${@:-${ALL[@]}}")
mkdir -p "$OUT"

size() {
  case "$1" in
    icon) echo "512,512" ;;
    feature) echo "1024,500" ;;
    *) echo "1080,1920" ;;
  esac
}

for id in "${IDS[@]}"; do
  png="$OUT/$id.png"
  "$CHROME" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 \
    --allow-file-access-from-files --virtual-time-budget=8000 \
    --window-size="$(size "$id")" --screenshot="$png" "$PAGE?only=$id" >/dev/null 2>&1

  if [[ "$id" == "icon" ]]; then
    # Play's icon spec is a 32-bit PNG (with alpha channel).
    python3 -I -c "from PIL import Image; import sys; Image.open(sys.argv[1]).convert('RGBA').save(sys.argv[1])" "$png"
  else
    # Play wants screenshots and the feature graphic without an alpha channel.
    sips -s format jpeg -s formatOptions 100 "$png" --out "$OUT/$id.tmp.jpg" >/dev/null
    sips -s format png "$OUT/$id.tmp.jpg" --out "$png" >/dev/null
    rm -f "$OUT/$id.tmp.jpg"
  fi
  dims=$(sips -g pixelWidth -g pixelHeight "$png" | awk '/pixel/{printf "%s ", $2}')
  echo "$id.png  ${dims% }"
done
