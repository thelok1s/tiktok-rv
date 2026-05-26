package app.revanced.patches.tiktok.interaction.speed

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags

internal val BytecodePatchContext.getSpeedMethod by gettingFirstMethodDeclaratively {
    name("onFeedSpeedSelectedEvent")
    definingClass("/BaseListFragmentPanel;")
}

internal val BytecodePatchContext.setSpeedMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returnType("V")
    parameterTypes("F", "Lcom/ss/android/ugc/aweme/feed/model/Aweme;", "Ljava/lang/String;", "Ljava/lang/String;")
    instructions("swipe_up_lock_persist"(String::contains))
}
