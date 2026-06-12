package app.revanced.patches.tiktok.interaction.speed

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

// getCurrentAweme on BaseListFragmentPanel: the no-arg method returning the current Aweme
// by delegating to the ViewPagerComponentTemp getter. The obfuscated name drifts per
// version/variant (45.3.3 global: LJII; 45.5.x global: LJIIIIZZ; trill 37.5.22: getCurrentAweme),
// so it is resolved by fingerprint instead of hardcoded.
//
// There are three no-arg ()Aweme methods on the panel; this fingerprint isolates the right one:
//   - the correct getter is PUBLIC FINAL with the exact body below (iget field -> null check ->
//     delegate to ViewPagerComponentTemp -> return),
//   - a structurally identical decoy is PUBLIC (NOT final) and delegates to a different
//     ViewPagerComponentTemp method (getCurrentItem/getItem path) — excluded by FINAL,
//   - a thin wrapper is PUBLIC FINAL but only 3 instructions — excluded by the opcode sequence.
internal val BytecodePatchContext.getCurrentAwemeMethod by gettingFirstImmutableMethodDeclaratively {
    definingClass("/BaseListFragmentPanel;")
    returnType("Lcom/ss/android/ugc/aweme/feed/model/Aweme;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    parameterTypes()
    opcodes(
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.CONST_4,
        Opcode.RETURN_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.RETURN_OBJECT,
    )
}

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
