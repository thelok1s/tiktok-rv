# TikTok Mod Menu — Design Spec

**Date**: 2026-06-15
**Target**: TikTok 45.5.x (`com.zhiliaoapp.musically` / `com.ss.android.ugc.trill`)
**Approach**: A — revive the existing (disabled) ReVanced Settings menu and harden it for 45.5.x.

## Problem

Every shipped tiktok-rv feature is force-enabled with no user control. A complete settings
UI already exists in the extension (`TikTokPreferenceFragment` + 4 categories + preference
widgets, all wired to `Settings.java` keys that the features already read), but the
**`Settings` patch is disabled** in our build and its fingerprints may have drifted on
45.5.x. We revive it, give it a clearly-labeled entry, and add the one safe missing toggle.

## Goal

Ship an in-app settings menu that exposes every setting that already drives behavior, plus
one new toggle for render-time ad-skip, surfaced from TikTok's own settings list — with the
minimum new code and zero risk to the always-on patches we intend to keep always-on.

## Non-Goals

- No toggles for **disable-login-requirement** or **show-seekbar**: they stay always-on,
  bytecode untouched. Gating them would mean adding extension methods + changing
  `returnEarly` edits, which risks instability for little benefit.
- No floating button / overlay / gesture entry point (deferred; the injected settings-list
  row is the entry).
- No new custom UI framework — reuse the existing `android.preference`-based fragment.

## Architecture

Three existing layers; we repair seams, not rebuild:

```
TikTok native Settings list (SettingNewVersionFragment.initUnitManger)
        │  ← [Settings patch] injects a "ReVanced" entry row
        ▼
AdPersonalizationActivity.onCreate  ← [Settings patch] hijacks: if our entry was
        │                              tapped, host TikTokPreferenceFragment instead
        ▼
TikTokPreferenceFragment  (extension UI, android.preference, pure Java)
        ├── FeedFilterPreferenceCategory      (SettingsStatus.feedFilterEnabled)
        ├── DownloadsPreferenceCategory        (downloadEnabled)
        ├── SimSpoofPreferenceCategory         (simSpoofEnabled)
        └── ExtensionPreferenceCategory        (always-on)
        ▼
Settings.java keys  ←→  read by each feature's extension code (AdsFilter, FeedAdSkip, ...)
```

### Key files

| File | Role | Change |
|---|---|---|
| `patches/.../tiktok/misc/settings/SettingsPatch.kt` | injects entry + hijacks activity | enable in build; repair if fingerprints broke |
| `patches/.../tiktok/misc/settings/Fingerprints.kt` | 4 fingerprints | re-anchor any that don't resolve on 45.5.x |
| `extensions/.../settings/Settings.java` | setting keys | add `SKIP_ADS_AT_RENDER` |
| `extensions/.../settings/preference/categories/FeedFilterPreferenceCategory.java` | feed toggles | add "Skip ads at render" toggle |
| `extensions/.../feedfilter/FeedAdSkip.java` | render-time skip | extend existing `REMOVE_ADS` guard with `SKIP_ADS_AT_RENDER` sub-toggle |
| `scripts/build-local.sh` | local enable list | add `-e "Settings"` |
| `.github/workflows/tiktok-patcher.yml` | CI enable list | add `Settings` to patch list |

## The 4 fingerprints (verify-first)

The `Settings` patch resolves these; any failure fails the whole build:

1. `addSettingsEntryMethod` — `name("initUnitManger")`, `definingClass("/SettingNewVersionFragment;")`
2. `adPersonalizationActivityOnCreateMethod` — `name("onCreate")`, `definingClass("/AdPersonalizationActivity;")`
3. `settingsEntryMethod` — string anchor `"pls pass item or extends the EventUnit"`
4. `settingsEntryInfoMethod` — string anchors `"ExposeItem(title="`, `", icon="`

`SettingsPatch.kt` also depends on the `markIndex` heuristic inside `initUnitManger`
(`IGET_OBJECT` of field `headerUnit`, then reuses the two following instructions). If
`initUnitManger`'s body changed shape on 45.5.x, this index math may need adjustment even if
the fingerprint resolves.

## Data flow / toggle wiring

Working pattern (unchanged): `TogglePreference(Setting)` → persisted to SharedPreferences →
feature extension reads `Setting.get()` at runtime.

**Force-on patches reviewed:**

| Feature | Type | Decision |
|---|---|---|
| Render-time ad-skip (`FeedAdSkip.onAwemeRendered`) | extension method | already gated by `REMOVE_ADS` (line 33); add a **sub-toggle** for the auto-swipe behavior — see below |
| Show seekbar | bytecode `returnEarly` | **left always-on, no toggle** |
| Disable login requirement | bytecode `returnEarly` | **left always-on, no toggle** |

Render-skip is **not** force-on independent of settings: `FeedAdSkip` already returns early
when `REMOVE_ADS` is off. The auto-swipe it performs (synthesizing a swipe to advance the
feed when an ad reaches render) is more intrusive than passive filtering — it physically
moves the feed. So the new `SKIP_ADS_AT_RENDER` toggle is a **sub-control**: it lets a user
keep ad removal (`REMOVE_ADS`) but disable the auto-swipe specifically. The guard becomes:

```java
if (!Settings.REMOVE_ADS.get() || !Settings.SKIP_ADS_AT_RENDER.get()) return;
```

New `SKIP_ADS_AT_RENDER` defaults `TRUE` (preserves current behavior). It belongs in the
Feed-filter category beside `REMOVE_ADS`, with a description making clear it controls the
auto-skip, not ad detection.

### Orphan keys — intentionally NOT surfaced

`REMEMBERED_SPEED` (`FloatSetting`) and `CLEAR_DISPLAY` (`BooleanSetting`) have no UI **by
design**, not oversight. Their extension code shows they are auto-managed remembered state:

- `PlaybackSpeedPatch.rememberPlaybackSpeed(newSpeed)` persists whatever speed the user last
  picked in-app; `getPlaybackSpeed()` restores it next launch.
- `RememberClearDisplayPatch.rememberClearDisplayState(newState)` persists the in-app
  clear-display toggle; `getClearDisplayState()` restores it.

Exposing them as editable preferences would be redundant (a second control fighting the
in-app one) or meaningless (an editable "last speed" number). They stay orphan. No Playback
category is added. The "always-on canary proves the fragment renders" role is already filled
by the existing always-on `ExtensionPreferenceCategory` (Miscellaneous).

## Menu surface (final)

| Category | Items | Status |
|---|---|---|
| Feed filter | Remove feed ads, **Skip ads at render (new)**, Hide Shop/Live/Story/Image, Min/Max views, Min/Max likes | exists + 1 new |
| Downloads | Download path, Remove watermark | as-is |
| Bypass regional restriction | Fake SIM, Country ISO, mcc+mnc, Operator name | as-is |
| Miscellaneous | About, Sanitize sharing links, Debug log | as-is |

**Total new code**: 1 Settings key, 1 extension guard edit, 1 toggle added to an existing
category, 2 build-list edits. Everything else is revive + fingerprint repair.

## Error handling / failure modes

- **Fingerprint unresolved** → whole build aborts (revanced-cli). Mitigation: first impl step
  is a resolution probe (apply only `Settings` against `raw-base-4553.apk`, read the error)
  to learn which fingerprints need re-anchoring before writing fixes.
- **Entry row present, tap is a no-op** → `AdPersonalizationActivity` hijack mis-resolved or
  `initialize()` returns false. Verified on-device.
- **Menu opens, a feature category missing** → that feature's `SettingsStatus.enableX()`
  wasn't injected (feature patch didn't run). The always-on Miscellaneous category always
  renders, so if even *it* is absent the fragment itself failed to load.
- **Toggle flips, no effect** → live vs. restart mismatch; route restart-required settings
  through the restart dialog.

## Testing

1. **Build**: `Settings` patch applies cleanly against the 45.5.3 base (probe green); full
   `build-local.sh` produces a signed APK.
2. **Smoke (device)**: native Settings → "ReVanced" row visible → opens → all 4 categories
   render.
3. **Functional**: *Remove feed ads* off → ads reappear (and no auto-swipe); *Remove feed
   ads* on + *Skip ads at render* off → ad still detected but feed does **not** auto-advance.
4. **Regression**: seekbar still shows, login still bypassed (both untouched).

## Rollout

Land behind the existing branch/PR flow. Defaults preserve current behavior
(`SKIP_ADS_AT_RENDER=true`, `REMOVE_ADS=true`), so an existing user sees no change until they
open the menu and flip something. Update README patch list (RU+EN) to mention the settings
menu.
