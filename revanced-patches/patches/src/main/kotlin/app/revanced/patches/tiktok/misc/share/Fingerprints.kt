package app.revanced.patches.tiktok.misc.share

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.urlShorteningMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returnType("LX/")
    parameterTypes(
        "I",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;"
    )
    opcodes(Opcode.RETURN_OBJECT)
    instructions("shorten_network_timeout_experiment"(String::contains))
}