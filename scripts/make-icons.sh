#!/usr/bin/env bash
# Generates the launcher icons and TV banner from ONE logo file.
#   usage: scripts/make-icons.sh [logo.png]      (default: brand/logo.png, square, transparent, >= 512 px)
# Needs ImageMagick (`convert`). Colours/name are read from brand/res/values.
set -euo pipefail
cd "$(dirname "$0")/.."
LOGO="${1:-brand/logo.png}"
OUT="brand/res"
[ -f "$LOGO" ] || { echo "Logo not found: $LOGO"; exit 1; }
command -v convert >/dev/null || { echo "ImageMagick 'convert' is required"; exit 1; }

BG=$(grep -o 'name="ic_launcher_background">#[0-9A-Fa-f]\{6\}' brand/res/values/brand_colors.xml | grep -o '#[0-9A-Fa-f]\{6\}')
NAME=$(grep -o '<string name="app_name">[^<]*' brand/res/values/brand_strings.xml | sed 's/.*>//')
[ -n "$BG" ] || BG="#1B1B2F"

# density  scale(x10)
for spec in mdpi:10 hdpi:15 xhdpi:20 xxhdpi:30 xxxhdpi:40; do
  d=${spec%%:*}; k=${spec##*:}
  fg=$(( 108 * k / 10 )); inner=$(( fg * 62 / 100 )); leg=$(( 48 * k / 10 )); legin=$(( leg * 70 / 100 ))
  mkdir -p "$OUT/mipmap-$d"
  # adaptive foreground: logo inside the 66% safe zone on a transparent canvas
  convert "$LOGO" -resize ${inner}x${inner} -background none -gravity center -extent ${fg}x${fg} "$OUT/mipmap-$d/ic_launcher_foreground.png"
  # legacy icon (Android 6-7.x): logo on the background colour
  convert -size ${leg}x${leg} "xc:$BG" \( "$LOGO" -resize ${legin}x${legin} \) -gravity center -composite "$OUT/mipmap-$d/ic_launcher.png"
done

# TV banner 320x180
mkdir -p "$OUT/drawable-xhdpi"
convert -size 320x180 "xc:$BG" \( "$LOGO" -resize 120x120 \) -gravity west -geometry +24+0 -composite "$OUT/drawable-xhdpi/banner.png"
if [ -n "$NAME" ]; then
  convert "$OUT/drawable-xhdpi/banner.png" -gravity east -fill white -pointsize 26 -annotate +16+0 "$(echo "$NAME" | fold -s -w 14)" "$OUT/drawable-xhdpi/banner_t.png" 2>/dev/null \
    && mv "$OUT/drawable-xhdpi/banner_t.png" "$OUT/drawable-xhdpi/banner.png" || true
fi
echo "Icons and banner written to $OUT (background $BG)."
