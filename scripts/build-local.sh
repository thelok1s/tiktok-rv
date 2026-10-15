#!/usr/bin/env bash
# Local patch -> merge -> sign loop for fast iteration on a connected device.
# Mirrors .github/workflows/tiktok-patcher.yml but uses a pre-downloaded clean
# stock base + config splits and a local keystore.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

BASE_APK="${BASE_APK:-$ROOT/raw-base-4553.apk}"
SPLITS_SRC="${SPLITS_SRC:-$ROOT/tiktok_stock_4553/extracted}"
OUT="${OUT:-$ROOT/tiktok-rv-local.apk}"
KS="${KS:-$ROOT/scripts/local.keystore}"
KS_PASS="${KS_PASS:-android}"
KS_ALIAS="${KS_ALIAS:-local}"
SDK="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
APKSIGNER="$(ls "$SDK"/build-tools/*/apksigner | sort -V | tail -1)"

# Patches to enable (override by passing args).
PATCHES=("$@")
if [ ${#PATCHES[@]} -eq 0 ]; then
  PATCHES=(
    -e "Settings"
    -e "Feed filter" -e "Downloads" -e "SIM spoof"
    -e "Remember clear display" -e "Show seekbar"
    -e "Playback speed" -e "Disable login requirement"
  )
fi

# Extra gradle flags (e.g. GRADLE_FLAGS=--offline when jitpack is unreachable but
# the SNAPSHOT plugin is already cached locally).
GRADLE_FLAGS="${GRADLE_FLAGS:-}"

echo "==> Building patch bundle"
( cd revanced-patches && \
  ORG_GRADLE_PROJECT_githubPackagesUsername="${GH_USER:-thelok1s}" \
  ORG_GRADLE_PROJECT_githubPackagesPassword="$(gh auth token)" \
  ./gradlew :patches:build -x test -q $GRADLE_FLAGS )

# Exclude the -sources rvp: it sorts before the real bundle and contains no compiled patches.
BUNDLE="$(find revanced-patches/patches/build/libs -name 'patches-*.rvp' ! -name '*sources*' | head -n1)"
echo "==> Bundle: $BUNDLE"

# Merge before patching (mirrors CI): some TikTok builds keep code in df_* feature
# splits, which patches can only see once everything is in one APK.
echo "==> Merging splits"
SPLITS_DIR="$ROOT/.local-splits"
rm -rf "$SPLITS_DIR"; mkdir -p "$SPLITS_DIR"
cp "$BASE_APK" "$SPLITS_DIR/base.apk"
for c in config.arm64_v8a config.xxxhdpi config.en config.ru; do
  [ -f "$SPLITS_SRC/$c.apk" ] && cp "$SPLITS_SRC/$c.apk" "$SPLITS_DIR/"
done
for df in "$SPLITS_SRC"/df_*.apk; do
  [ -f "$df" ] && cp "$df" "$SPLITS_DIR/"
done
rm -f "$ROOT/.local-merged-raw.apk"
if [ -f "$ROOT/tools/APKEditor.jar" ]; then APKEDITOR="$ROOT/tools/APKEditor.jar"; else APKEDITOR="$ROOT/APKEditor.jar"; fi
java -jar "$APKEDITOR" m -i "$SPLITS_DIR" -o "$ROOT/.local-merged-raw.apk"

echo "==> Patching merged APK"
java -jar revanced-cli.jar patch "$ROOT/.local-merged-raw.apk" \
  -p "$BUNDLE" -b -o "$ROOT/.local-merged.apk" --purge \
  --exclusive "${PATCHES[@]}"

if [ ! -f "$KS" ]; then
  echo "==> Generating local keystore"
  keytool -genkeypair -v -keystore "$KS" -alias "$KS_ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$KS_PASS" -keypass "$KS_PASS" \
    -dname "CN=tiktok-rv-local"
fi

echo "==> Signing -> $OUT"
"$APKSIGNER" sign --ks "$KS" --ks-pass "pass:$KS_PASS" \
  --ks-key-alias "$KS_ALIAS" --out "$OUT" "$ROOT/.local-merged.apk"

echo "==> Done: $OUT ($(du -h "$OUT" | cut -f1))"
