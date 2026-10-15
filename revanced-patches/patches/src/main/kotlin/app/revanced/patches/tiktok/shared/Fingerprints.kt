package app.revanced.patches.tiktok.shared

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.getEnterFromMethod by gettingFirstImmutableMethodDeclaratively {
    definingClass("/BaseListFragmentPanel;")
    returnType("Ljava/lang/String;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes("Z")
    opcodes(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.RETURN_OBJECT,
    )
}

// Anchored by name: up to 46.x the body contained "method_enable_viewpager_preload_duration",
// but 47.x moved that preload logic into ViewPagerPanelComponent, leaving the method itself
// (BaseListFragmentPanel.onRenderFirstFrame(String)) intact and un-obfuscated.
internal val BytecodePatchContext.onRenderFirstFrameMethod by gettingFirstMethodDeclaratively {
    name("onRenderFirstFrame")
    definingClass("/BaseListFragmentPanel;")
    returnType("V")
    parameterTypes("Ljava/lang/String;")
}
