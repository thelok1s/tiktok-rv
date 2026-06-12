# tiktok-rv — Patch refresh for 45.5.3 + in-app control menu

**Date:** 2026-06-12
**Target:** TikTok `original` (`com.zhiliaoapp.musically`) **45.5.3** (versionCode 2024505030); keep `asia` (`com.ss.android.ugc.trill`) source-compatible.
**Scope:** Phase A (fix & verify existing patches) + Phase B (in-app control menu). Phase C is out of scope, designed separately later.

## Background

All patches live in `revanced-patches/` and locate code in TikTok's obfuscated bytecode via **declarative fingerprints** (method name, defining-class suffix, string constants, access flags). When TikTok bumps versions, obfuscated names drift, so a fingerprint either:
- fails to resolve → revanced-cli errors / skips the patch, or
- resolves to the wrong method → patch "applies" but does nothing (silent breakage).

The current pipeline (`.github/workflows/tiktok-patcher.yml`) downloads, patches with `--exclusive -e <patch>`, merges splits via APKEditor, signs, and releases — **with no verification that patches applied or work at runtime**. All patches declare `compatibleWith("...")("45.3.3")`; the device runs 45.5.3.

The workflow applies: `Feed filter`, `Downloads`, `SIM spoof`, `Remember clear display`, `Show seekbar`, `Playback speed`, `Disable login requirement`. It does **not** apply the `Settings` patch, so the app is force-everything-on with no menu. ReVanced's `Settings` patch (injects a row into `SettingNewVersionFragment` and hosts the screen in `AdPersonalizationActivity`) is outdated and non-functional on current TikTok.

**Known broken:** ads removal (feed filter). **Suspect / unverified:** downloads path change, watermark removal, others — none are runtime-verified.

## Goals

1. Every applied patch is **verified** on 45.5.3 across three layers: apply-time, frida runtime, on-device UI.
2. Ads/feed filtering actually removes ads.
3. Downloads land in the configured path, watermark-free.
4. A working **in-app control menu** to toggle each mod, backed by SharedPreferences, with patches reading those flags.
5. `compatibleWith` bumped to 45.5.3 for all touched patches.

## Non-goals (Phase C, later)

Stream/live removal from feed, analytics/ads stripping for APK size reduction, alternative download source. Donor mods (`TikTok_You-3.0`, `TikTok 45.5.3 Fix`) are **reference-only** for injection technique; their PUP-flagged code is never shipped.

---

## Pillar A — Fix & verify existing patches

### A1. Local build → install → verify loop

Replace CI iteration with a local script. Inputs already present in repo: `revanced-cli.jar`, `APKEditor.jar`, `apktool.jar`, `raw-base.apk`, config split APKs, a keystore.

Loop:
1. `cd revanced-patches && ./gradlew build` → produces `patches/build/libs/patches-*.rvp`.
2. `java -jar revanced-cli.jar patch raw-base.apk -p <rvp> -b -o unsigned-patched.apk --exclusive -e <patches…>`.
3. Merge `unsigned-patched.apk` + `config.*.apk` via `APKEditor.jar m`.
4. Sign with the local keystore (`apksigner`).
5. `adb install -r` to the Pixel 8 Pro (husky).

Wrap as a single script (e.g. `scripts/build-local.sh`) parameterized by patch list, so individual patches can be built/tested in isolation.

### A2. Verification harness (three layers)

For **each** patch, all three must pass before it is "working":

- **Apply-time:** capture revanced-cli stdout; assert the patch is listed as applied (not skipped/failed). A patch whose fingerprint fails must surface as an error, never a silent pass.
- **Runtime (frida):** a per-patch JS hook attached to the running app that confirms the extension method fires with expected effect, e.g.:
  - `FeedItemsFilter.filter(...)` is invoked and removes ≥1 item when ads are present.
  - `DownloadsPatch.getDownloadPath()` returns the configured path.
  - watermark branch in `ACLCommonShare.getTranscode` is taken (returns our value).
  - `SpoofSimPatch` returns the configured region.
  Scripts live in `scripts/frida/<patch>.js`.
- **UI (android-mcp):** drive the real app — scroll the feed checking for ad markers, perform a download and confirm the file path + absence of watermark, toggle seekbar/speed and observe.

Verification status recorded in a checklist in the spec/PR.

### A3. Per-patch fingerprint audit on 45.5.3

Disassemble `raw-base.apk` with jadx (decompiled Java for reading) and apktool (smali for ground truth). For each fingerprint, re-resolve against 45.5.3 and fix drift. Bump `compatibleWith(...)("45.5.3")`.

Priority order:
1. **Feed filter / ads** (`feedfilter/Fingerprints.kt`: `fetchFeedList`/`FeedApiService`, follow-feed method) — known broken.
2. **Downloads path** (`downloads/Fingerprints.kt`: `downloadUriMethod` keyed on `/Camera/`, `video/mp4`).
3. **Watermark** (`ACLCommonShare.getTranscode/getCode/getShowType`).
4. **Seekbar, Playback speed, Remember clear display, Disable login requirement, SIM spoof.**

### A4. Ads deep-dive

If the feed fingerprint resolves but ads persist, frida-trace the live feed response (`FeedItemList` / follow feed) to inspect the current ad item model, then update `AdsFilter` (and siblings in `extensions/tiktok/feedfilter/`) so matching reflects the current schema. Verify a known ad-heavy feed shows none after patch.

---

## Pillar B — In-app control menu

### B1. Settings backend

Reuse the extension preference UI already in the repo (`extensions/tiktok/settings/`: `TikTokPreferenceFragment`, `Settings.java`, preference classes, categories). Back it with SharedPreferences. **Default policy: sensible defaults, user opts in** — not all-on. Define a default table per mod (e.g. login-bypass + watermark-free ON; aggressive feed filters OFF until enabled). Each extension filter/patch reads its flag at runtime and no-ops when disabled.

### B2. Menu entry-point injection — "hijack" approach (primary)

Rather than reconstruct a new list row in an obfuscated RecyclerView (the exact thing that broke in ReVanced's `Settings` patch), **hijack an existing, low-value settings/profile row** and re-point its click handler to launch our settings screen. Robust because it needs one stable clickable target, not feed-list reconstruction.

Method:
1. Mine donor mods (`TikTok_You-3.0`, `Fix`) via jadx/apktool to learn how they inject their menus and which host they use.
2. Statically + dynamically (frida) locate candidate hosts:
   - **Profile sidebar drawer** (Resources / Personal tools list).
   - **Settings & Privacy screen** rows.
   - A reusable host Activity (ReVanced used `AdPersonalizationActivity`; re-evaluate if still viable as the screen host even if the entry row is hijacked elsewhere).
3. Pick the host with the most stable hook; write a new injection patch that swaps the target's intent/click to open our preference screen.
4. Backup: if no clean hijack exists, replace a known-useless submenu entry's destination.

### B3. Wire toggles to patches

Each fixed patch from Pillar A reads its SharedPreferences flag (via the extension `Settings`), so the menu actually controls behavior. `SettingsStatus.enable*()` hooks already exist and are extended here rather than removed.

---

## Architecture / data flow

```
revanced-patches (Kotlin, fingerprints + bytecode edits)
        │  inject calls to ↓
extensions/tiktok (Java, ships inside APK)
   ├── settings/ (PreferenceFragment, Settings, SettingsStatus)  ← reads SharedPreferences
   ├── feedfilter/ (FeedItemsFilter, AdsFilter, …)               ← gated by flags
   ├── download/ (DownloadsPatch: path, watermark)               ← gated by flags
   └── spoof/sim, speed/, cleardisplay/, …                       ← gated by flags

Verification (not shipped):
   scripts/build-local.sh   — patch+merge+sign+install loop
   scripts/frida/*.js       — runtime hook assertions
   android-mcp              — on-device UI checks
```

## Risks

- **Obfuscation drift mid-effort:** fingerprints may break again on the next TikTok release; mitigated by the verification harness catching silent failures.
- **Menu host instability:** chosen injection host may move; backup hijack target reduces risk.
- **Donor mod PUP:** strictly read-only analysis; nothing copied verbatim.
- **Anti-tamper / integrity checks** in TikTok may detect resigning; existing releases install fine, so assumed tolerable, but watch for runtime crashes during frida verification.

## Verification checklist (per patch)

| Patch | Apply | Frida | UI | compatibleWith 45.5.3 |
|---|---|---|---|---|
| Feed filter / ads | ☐ | ☐ | ☐ | ☐ |
| Downloads path | ☐ | ☐ | ☐ | ☐ |
| Watermark removal | ☐ | ☐ | ☐ | ☐ |
| Show seekbar | ☐ | ☐ | ☐ | ☐ |
| Playback speed | ☐ | ☐ | ☐ | ☐ |
| Remember clear display | ☐ | ☐ | ☐ | ☐ |
| Disable login requirement | ☐ | ☐ | ☐ | ☐ |
| SIM spoof | ☐ | ☐ | ☐ | ☐ |
| In-app menu (entry + screen) | ☐ | ☐ | ☐ | ☐ |
