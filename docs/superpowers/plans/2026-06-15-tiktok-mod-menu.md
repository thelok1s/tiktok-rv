# TikTok Mod Menu Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Revive the existing-but-disabled ReVanced Settings menu in the tiktok-rv mod so users can toggle features in-app, and add one new "Skip ads at render" sub-toggle.

**Architecture:** The full settings UI already exists in the extension (`TikTokPreferenceFragment` + categories + `Settings.java` keys the features already read). The `Settings` bytecode patch — which injects a row into TikTok's native settings list and hijacks `AdPersonalizationActivity` to host the fragment — is simply not enabled in our build, and its fingerprints may have drifted on TikTok 45.5.x. We enable it, repair any broken fingerprints, add one new toggle, and ship.

**Tech Stack:** Kotlin ReVanced patches (smali via dexlib2 fingerprints), Java extension (`android.preference`), revanced-cli, APKEditor, gradle. No unit-test harness exists for smali patches — verification is "patch applies cleanly" + on-device behavior.

---

## Conventions used by every build/probe step

- **Repo root:** `/Users/lok1s/tiktokrevanced`
- **Base APK for probing:** `raw-base-4553.apk` (clean 45.5.3 base, already present)
- **Build the patch bundle** (needed after ANY change under `revanced-patches/`):
  ```bash
  cd /Users/lok1s/tiktokrevanced/revanced-patches && \
  ORG_GRADLE_PROJECT_githubPackagesUsername="${GH_USER:-thelok1s}" \
  ORG_GRADLE_PROJECT_githubPackagesPassword="$(gh auth token)" \
  ./gradlew :patches:build -x test -q
  ```
- **Resolve the freshly built bundle path** (excludes the -sources rvp):
  ```bash
  BUNDLE="$(find /Users/lok1s/tiktokrevanced/revanced-patches/patches/build/libs -name 'patches-*.rvp' ! -name '*sources*' | head -n1)"
  ```
- **Commit convention:** messages end with the trailer
  `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`

---

## File Structure

| File | Responsibility | Action |
|---|---|---|
| `revanced-patches/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/settings/SettingsPatch.kt` | inject entry row + hijack `AdPersonalizationActivity` | enable (no edit unless Task 2 finds drift) |
| `.../tiktok/misc/settings/Fingerprints.kt` | the 4 fingerprints the patch resolves | re-anchor any broken ones (Task 2) |
| `revanced-patches/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/settings/Settings.java` | setting keys | add `SKIP_ADS_AT_RENDER` (Task 3) |
| `.../extension/tiktok/feedfilter/FeedAdSkip.java` | render-time auto-skip | extend guard with the new sub-toggle (Task 3) |
| `.../extension/tiktok/settings/preference/categories/FeedFilterPreferenceCategory.java` | feed-filter toggles UI | add the "Skip ads at render" toggle (Task 4) |
| `scripts/build-local.sh` | local enable list | add `-e "Settings"` (Task 5) |
| `.github/workflows/tiktok-patcher.yml` | CI enable list | add `Settings` to the patch list (Task 5) |
| `README.md` | user-facing patch list (RU + EN) | document the menu (Task 7) |

---

## Task 1: Probe whether the Settings patch resolves on 45.5.x

**Purpose:** The single biggest unknown is whether the 4 fingerprints still resolve. This task is diagnostic only — no source changes. Its output decides whether Task 2 is needed.

**Files:** none (diagnostic).

- [ ] **Step 1: Build the current patch bundle**

Run the bundle build command from "Conventions" above. Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Attempt to apply ONLY the Settings patch against the clean base**

```bash
cd /Users/lok1s/tiktokrevanced && \
BUNDLE="$(find revanced-patches/patches/build/libs -name 'patches-*.rvp' ! -name '*sources*' | head -n1)" && \
java -jar revanced-cli.jar patch raw-base-4553.apk \
  -p "$BUNDLE" -o /tmp/probe-settings.apk --purge \
  --exclusive -e "Settings" 2>&1 | tee /tmp/settings-probe.log
```

- [ ] **Step 3: Read the probe result**

```bash
grep -iE "could not|unable to resolve|fingerprint|failed|Exception|Successfully" /tmp/settings-probe.log
```

Expected outcomes (record which one occurred):
- **PASS:** log shows successful patching and `/tmp/probe-settings.apk` exists → all 4 fingerprints resolve. **Skip Task 2 entirely.**
- **FAIL:** log names a failing fingerprint/method (e.g. `initUnitManger`, `AdPersonalizationActivity`, or one of the string anchors) → note exactly which → proceed to Task 2 to repair only those.

- [ ] **Step 4: No commit** (diagnostic only). Record the outcome in the task notes / PR description.

---

## Task 2: Repair broken Settings fingerprints (CONDITIONAL — only if Task 1 FAILED)

**Purpose:** Re-anchor whichever of the 4 fingerprints no longer resolves on 45.5.x. Skip this entire task if Task 1 PASSED.

**Files:**
- Modify: `revanced-patches/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/settings/Fingerprints.kt`
- Possibly modify: `revanced-patches/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/settings/SettingsPatch.kt` (only if the `initUnitManger` body shape changed — see Step 4)

The 4 fingerprints and their current anchors (from `Fingerprints.kt`):
1. `addSettingsEntryMethod` — `name("initUnitManger")` in `/SettingNewVersionFragment;`
2. `adPersonalizationActivityOnCreateMethod` — `name("onCreate")` in `/AdPersonalizationActivity;`
3. `settingsEntryMethod` — string anchor `"pls pass item or extends the EventUnit"`
4. `settingsEntryInfoMethod` — string anchors `"ExposeItem(title="`, `", icon="`

- [ ] **Step 1: Locate the real method/class for the failing fingerprint in the base APK**

Decompile the base once and search. Example for the `initUnitManger`/`SettingNewVersionFragment` case:

```bash
cd /tmp && rm -rf base-decomp && \
# Decompile dex -> smali with APKEditor:
java -jar /Users/lok1s/tiktokrevanced/tools/APKEditor.jar d -i /Users/lok1s/tiktokrevanced/raw-base-4553.apk -o /tmp/base-decomp
grep -rl "SettingNewVersionFragment" /tmp/base-decomp/smali* | head
# For a string anchor instead:
grep -rln "pls pass item or extends the EventUnit" /tmp/base-decomp/smali* | head
grep -rln "ExposeItem(title=" /tmp/base-decomp/smali* | head
```

Expected: the grep returns at least one smali file. If a **string anchor** still appears verbatim, that fingerprint's anchor is fine and the failure is elsewhere — re-check Task 1's log. If a **class name** changed (e.g. `SettingNewVersionFragment` renamed), note the new defining class.

- [ ] **Step 2: Update the failing anchor in `Fingerprints.kt`**

Edit only the broken fingerprint. Patterns:

- Class renamed → update the `definingClass("/NewName;")` substring.
- Method renamed but a stable string remains in its body → switch from `name(...)` to a `strings(...)` / `string(...)` anchor (match the style already used by `settingsEntryMethod`/`settingsEntryInfoMethod` in this file).
- A helper string moved methods → update the literal in the `gettingFirstImmutableMethod(...)` call.

Show the actual before/after in the commit. (Exact new value depends on Step 1 findings — do not guess; use what grep returned.)

- [ ] **Step 3: Rebuild the bundle and re-run the Task 1 probe**

Run the bundle build, then re-run Task 1 Step 2's `revanced-cli ... -e "Settings"` command.
Expected: now PASSES (produces `/tmp/probe-settings.apk`). If a *different* fingerprint now fails, repeat Steps 1–3 for it.

- [ ] **Step 4: If the probe passes but `initUnitManger` index math is suspect, verify the injection site**

`SettingsPatch.kt` finds `markIndex` via `IGET_OBJECT` of field `headerUnit` then reuses the two following instructions. If the patch APPLIED but on-device the entry row never appears (caught later in Task 6), the body shape changed. Inspect the resolved method's smali:

```bash
SF="$(grep -rl 'initUnitManger' /tmp/base-decomp | grep -i SettingNewVersionFragment | head -n1)"; echo "$SF"; \
grep -nA40 'initUnitManger' "$SF" | grep -nE 'headerUnit|iget-object|invoke' | head
```

Confirm a `headerUnit` `iget-object` exists and is followed by the add-entry + get-unit-manager calls the patch reuses. Adjust the `markIndex + N` offsets in `SettingsPatch.kt` only if the ordering differs. (Defer this step's on-device confirmation to Task 6.)

- [ ] **Step 5: Commit**

```bash
cd /Users/lok1s/tiktokrevanced && git add revanced-patches/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/settings/ && \
git commit -m "$(printf 'fix(tiktok): re-anchor Settings patch fingerprints for 45.5.x\n\nCo-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>')"
```

---

## Task 3: Add the `SKIP_ADS_AT_RENDER` setting and gate the render-skip

**Purpose:** Add the new sub-toggle key and make `FeedAdSkip` honor it, without changing default behavior (defaults `TRUE`).

**Files:**
- Modify: `revanced-patches/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/settings/Settings.java`
- Modify: `revanced-patches/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/feedfilter/FeedAdSkip.java`

- [ ] **Step 1: Add the setting key**

In `Settings.java`, immediately after the `REMOVE_ADS` line, add:

```java
public static final BooleanSetting SKIP_ADS_AT_RENDER = new BooleanSetting("skip_ads_at_render", TRUE, true);
```

(The third `true` arg matches `REMOVE_ADS` — marks it as requiring the value sync, consistent with sibling feed-filter toggles.)

- [ ] **Step 2: Extend the guard in `FeedAdSkip.onAwemeRendered`**

The method currently has (around line 33):

```java
if (!Settings.REMOVE_ADS.get()) return;
```

Replace that single line with:

```java
if (!Settings.REMOVE_ADS.get() || !Settings.SKIP_ADS_AT_RENDER.get()) return;
```

This keeps ad *removal* tied to `REMOVE_ADS` while letting users disable only the intrusive auto-swipe via the new key. No other lines change.

- [ ] **Step 3: Compile the extension to verify it builds**

```bash
cd /Users/lok1s/tiktokrevanced/revanced-patches && \
ORG_GRADLE_PROJECT_githubPackagesUsername="${GH_USER:-thelok1s}" \
ORG_GRADLE_PROJECT_githubPackagesPassword="$(gh auth token)" \
./gradlew :extensions:tiktok:build -x test -q
```

Expected: `BUILD SUCCESSFUL`. A typo in the setting name or guard will fail javac here.

- [ ] **Step 4: Commit**

```bash
cd /Users/lok1s/tiktokrevanced && \
git add revanced-patches/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/settings/Settings.java \
        revanced-patches/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/feedfilter/FeedAdSkip.java && \
git commit -m "$(printf 'feat(tiktok): add Skip-ads-at-render sub-toggle (gates FeedAdSkip auto-swipe)\n\nCo-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>')"
```

---

## Task 4: Surface the new toggle in the Feed-filter category

**Purpose:** Add a `TogglePreference` for `SKIP_ADS_AT_RENDER` to the menu, beside "Remove feed ads", with a description that makes clear it controls auto-skip, not detection.

**Files:**
- Modify: `revanced-patches/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/settings/preference/categories/FeedFilterPreferenceCategory.java`

- [ ] **Step 1: Add the toggle right after the existing "Remove feed ads" preference**

In `addPreferences(Context context)`, the first `addPreference(...)` adds `REMOVE_ADS`. Immediately after that block, insert:

```java
addPreference(new TogglePreference(
        context,
        "Skip ads at render", "Auto-swipe past an ad that slips through the filter. Turn off to stop the feed from auto-advancing (ads may still appear).",
        Settings.SKIP_ADS_AT_RENDER
));
```

(`TogglePreference(Context, String title, String summary, BooleanSetting)` — matches the existing constructor used throughout this file. `Settings` is already imported here.)

- [ ] **Step 2: Compile the extension**

```bash
cd /Users/lok1s/tiktokrevanced/revanced-patches && \
ORG_GRADLE_PROJECT_githubPackagesUsername="${GH_USER:-thelok1s}" \
ORG_GRADLE_PROJECT_githubPackagesPassword="$(gh auth token)" \
./gradlew :extensions:tiktok:build -x test -q
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
cd /Users/lok1s/tiktokrevanced && \
git add revanced-patches/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/settings/preference/categories/FeedFilterPreferenceCategory.java && \
git commit -m "$(printf 'feat(tiktok): expose Skip-ads-at-render toggle in feed-filter settings\n\nCo-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>')"
```

---

## Task 5: Enable the Settings patch in both build lists

**Purpose:** The patch does nothing until it's in the enable list of the local script AND the CI workflow.

**Files:**
- Modify: `scripts/build-local.sh` (the `PATCHES=(...)` default array, lines ~22–26)
- Modify: `.github/workflows/tiktok-patcher.yml` (the `--exclusive -e ...` patch list)

- [ ] **Step 1: Add `Settings` to the local default patch array**

In `scripts/build-local.sh`, change the default `PATCHES` array to include Settings. Current:

```bash
  PATCHES=(
    -e "Feed filter" -e "Downloads" -e "SIM spoof"
    -e "Remember clear display" -e "Show seekbar"
    -e "Playback speed" -e "Disable login requirement"
  )
```

Replace with (adds `-e "Settings"`):

```bash
  PATCHES=(
    -e "Settings"
    -e "Feed filter" -e "Downloads" -e "SIM spoof"
    -e "Remember clear display" -e "Show seekbar"
    -e "Playback speed" -e "Disable login requirement"
  )
```

- [ ] **Step 2: Add `Settings` to the CI workflow patch list**

In `.github/workflows/tiktok-patcher.yml`, find the `-e "Feed filter"` line in the revanced-cli `patch` invocation (around line 365) and add `-e "Settings"` to that same list (place it first, mirroring the local script). Confirm the surrounding `--exclusive` / continuation-backslash formatting is preserved.

```bash
grep -n 'Feed filter\|Disable login requirement\|--exclusive' .github/workflows/tiktok-patcher.yml
```

Use the result to edit the exact lines. Expected after edit: the list contains `-e "Settings"` alongside the other 7 patches.

- [ ] **Step 3: Sanity-check the workflow still parses**

```bash
cd /Users/lok1s/tiktokrevanced && python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/tiktok-patcher.yml')); print('yaml ok')"
```

Expected: `yaml ok`.

- [ ] **Step 4: Commit**

```bash
cd /Users/lok1s/tiktokrevanced && git add scripts/build-local.sh .github/workflows/tiktok-patcher.yml && \
git commit -m "$(printf 'build(tiktok): enable Settings patch in local + CI patch lists\n\nCo-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>')"
```

---

## Task 6: Full local build + on-device verification

**Purpose:** Prove the whole stack: patch applies in the real pipeline, the menu opens, categories render, and the new toggle behaves.

**Files:** none (verification).

- [ ] **Step 1: Run the full local build**

```bash
cd /Users/lok1s/tiktokrevanced && ./scripts/build-local.sh 2>&1 | tee /tmp/build-local.log
```

Expected: ends with `==> Done: .../tiktok-rv-local.apk (...)`. If the **Settings** patch fails to apply here (but Task 1 passed), the cause is a different patch interaction — read `/tmp/build-local.log`.

- [ ] **Step 2: Install on the connected device**

```bash
adb install -r /Users/lok1s/tiktokrevanced/tiktok-rv-local.apk
```

Expected: `Success`. (If signature conflict, `adb uninstall com.zhiliaoapp.musically` first — note this wipes app data.)

- [ ] **Step 3: Verify the entry row + menu render**

Open TikTok → go to the native Settings screen → look for the injected "ReVanced" row → tap it.

Expected: the ReVanced settings screen opens showing **4 categories**: Feed filter, Downloads, Bypass regional restriction, Miscellaneous.
- If the row is **absent**: the `initUnitManger` injection mis-fired → revisit Task 2 Step 4 (index math).
- If the row is present but **tapping does nothing**: the `AdPersonalizationActivity.onCreate` hijack mis-resolved → revisit that fingerprint (Task 2).
- If the screen opens but a **feature category is missing**: that feature's `SettingsStatus.enableX()` wasn't injected — confirm the feature patch is in the enable list (it is, from Task 5). Miscellaneous always renders.

- [ ] **Step 4: Verify the new toggle's two states**

In Feed filter, confirm "Skip ads at render" appears under "Remove feed ads".
- With both ON: when an ad reaches render, the feed auto-advances (current behavior).
- Turn "Skip ads at render" OFF (leave "Remove feed ads" ON): an ad that slips through is **not** auto-swiped — the feed stays put.
- Turn "Remove feed ads" OFF: ads reappear and there is no auto-swipe.

- [ ] **Step 5: Regression check (untouched always-on features)**

Confirm the video seekbar still shows and the app does not demand login — neither was modified, so both should behave exactly as before.

- [ ] **Step 6: No commit** (verification). Record results in the PR description.

---

## Task 7: Document the menu in the README (RU + EN)

**Purpose:** The README lists patches in both Russian and English; add the settings menu.

**Files:**
- Modify: `README.md` (RU patch list around line 34; EN patch list around line 118)

- [ ] **Step 1: Locate both patch-list sections**

```bash
grep -n "Feed filter\|Фильтр\|Disable login\|Playback speed\|SIM spoof" /Users/lok1s/tiktokrevanced/README.md
```

- [ ] **Step 2: Add a "Settings menu" bullet to the EN list**

Match the existing bullet style (bold lead-in + sentence). Add near the top of the EN patch list:

```markdown
- **Settings menu:** Adds an in-app "ReVanced" entry to TikTok's settings where every feature can be toggled — including a new "Skip ads at render" sub-toggle that controls whether the feed auto-swipes past ads that slip through the filter.
```

- [ ] **Step 3: Add the equivalent bullet to the RU list**

Match the existing Russian bullet style:

```markdown
- **Меню настроек:** Добавляет в настройки TikTok пункт «ReVanced», где можно включать и отключать функции — включая новый переключатель «Пропускать рекламу при показе», управляющий автопролистыванием рекламы, прошедшей фильтр.
```

- [ ] **Step 4: Commit**

```bash
cd /Users/lok1s/tiktokrevanced && git add README.md && \
git commit -m "$(printf 'docs: document in-app settings menu (RU+EN)\n\nCo-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>')"
```

---

## Done criteria

- Task 1 probe passed (with Task 2 repairs if it didn't).
- `./scripts/build-local.sh` produces a signed APK with the Settings patch applied.
- On-device: "ReVanced" row → menu with 4 categories → "Skip ads at render" toggle works in both states.
- Seekbar + login-bypass regressions clean.
- README updated in both languages.
- All changes committed; ready to push/PR per the user's normal flow.
