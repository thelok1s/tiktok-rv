package app.revanced.patches.tiktok.misc.settings

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext

// TikTok 45.5.x obfuscated away the old `SettingNewVersionFragment.initUnitManger`
// (+ the `headerUnit` field the patch used to anchor on). The settings UI is now built
// by per-screen `*Page` classes whose `onViewCreated` populates a shared unit manager
// (`LX/0SqZ`) via repeated `hS()` (get manager) + `LX/0SqZ;->LIZ(LX/0Sqb;)V` (add unit)
// calls, finishing with `LX/0SqZ;->LJ()V` (commit). AboutPage is a stable, always-present
// settings screen that follows this pattern, so we anchor the ReVanced entry there.
internal val BytecodePatchContext.addSettingsEntryMethod by gettingFirstMethodDeclaratively {
    name("onViewCreated")
    definingClass("Lcom/ss/android/ugc/aweme/setting/page/AboutPage;")
}

internal val BytecodePatchContext.adPersonalizationActivityOnCreateMethod by gettingFirstMethodDeclaratively {
    name("onCreate")
    definingClass("/AdPersonalizationActivity;")
}

internal val BytecodePatchContext.settingsEntryMethod by gettingFirstImmutableMethodDeclaratively(
    "pls pass item or extends the EventUnit",
)

internal val BytecodePatchContext.settingsEntryInfoMethod by gettingFirstImmutableMethod("ExposeItem(title=", ", icon=")

internal val BytecodePatchContext.settingsStatusLoadMethod by gettingFirstMethodDeclaratively {
    name("load")
    definingClass("Lapp/revanced/extension/tiktok/settings/SettingsStatus;")
}
