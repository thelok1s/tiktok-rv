# TikTok ReVanced Automated Patcher

This repository contains a fully automated GitHub Actions pipeline for downloading, patching, signing, and releasing a modded TikTok application every two weeks.

## Overview

The pipeline executes the following steps:
1. **Download APK**: Automatically fetches the latest split APKs for TikTok directly from the Google Play Store using [`gplaydl`](https://github.com/hkalina/gplaydl) which utilizes an anonymous session token to bypass account requirements.
2. **Remove Split Restrictions**: Uses a python script to parse the base `AndroidManifest.xml` and strips `android:isSplitRequired="true"` along with fused module metadata, bypassing the split restriction error without requiring a full `apktool` resource compilation (which frequently breaks on complex apps like TikTok).
3. **Build Custom Patches**: Compiles the modified `revanced-patches` source tree included in this repository.
4. **Apply Patches**: Uses `revanced-cli` to inject the patches into the bytecode of the stripped Base APK.
5. **Sign & Release**: Signs the patched Base APK and the original configuration split APKs (en, arm64_v8a, xxhdpi) with a PKCS12 keystore and packages them into a ZIP file uploaded to GitHub Releases.

## Applied Patches

The patched TikTok application permanently includes the following features (bypassing the need for an in-app settings menu):

*   **Disable login requirement**: Bypasses the mandatory login/sign-up screen, allowing instant access to the app's content without an account.
*   **Feed filter**: Removes advertisements from the video feed.
*   **Downloads**: Force-enables downloading for all videos, removes the TikTok watermark from downloaded videos, and modifies the default download directory to `/sdcard/Pictures/TikTok`.
*   **Playback speed**: Adds playback speed controls (modified to support TikTok v45.3.3+ by updating the `getCurrentAweme` method signature to `LJII()`).
*   **Show seekbar**: Forces the video seekbar to be visible, allowing scrubbing through any video.
*   **Remember clear display**: Remembers your preference for "Clear Display" mode across videos.
*   **SIM spoof**: Spoofs the SIM card region (defaults to USA) to bypass regional restrictions on content.

*Note: The "Settings" and "Sanitize sharing links" patches are intentionally omitted from this build pipeline due to compatibility issues with TikTok's latest obfuscation and resource compilation pipeline.*

## License

The patch source code in the `revanced-patches` directory is licensed under the **GNU General Public License v3.0 (GPLv3)**, inheriting from the original [ReVanced Patches](https://github.com/ReVanced/revanced-patches) project. See the [LICENSE](LICENSE) file for more details.
