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
    -e "Feed filter" -e "Downloads" -e "SIM spoof"
    -e "Remember clear display" -e "Show seekbar"
    -e "Playback speed" -e "Disable login requirement"
  )
fi

echo "==> Building patch bundle"
( cd revanced-patches && \
  ORG_GRADLE_PROJECT_githubPackagesUsername="${GH_USER:-thelok1s}" \
  ORG_GRADLE_PROJECT_githubPackagesPassword="$(gh auth token)" \
  ./gradlew :patches:build -x test -q )

BUNDLE="$(find revanced-patches/patches/build/libs -name 'patches-*.rvp' | head -n1)"
echo "==> Bundle: $BUNDLE"

echo "==> Patching base ($BASE_APK)"
java -jar revanced-cli.jar patch "$BASE_APK" \
  -p "$BUNDLE" -b -o "$ROOT/.local-patched-base.apk" --purge \
  --exclusive "${PATCHES[@]}"

echo "==> Merging splits"
SPLITS_DIR="$ROOT/.local-splits"
rm -rf "$SPLITS_DIR"; mkdir -p "$SPLITS_DIR"
cp "$ROOT/.local-patched-base.apk" "$SPLITS_DIR/base.apk"
for c in config.arm64_v8a config.xxxhdpi config.en config.ru; do
  [ -f "$SPLITS_SRC/$c.apk" ] && cp "$SPLITS_SRC/$c.apk" "$SPLITS_DIR/"
done
rm -f "$ROOT/.local-merged.apk"
java -jar APKEditor.jar m -i "$SPLITS_DIR" -o "$ROOT/.local-merged.apk"

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
