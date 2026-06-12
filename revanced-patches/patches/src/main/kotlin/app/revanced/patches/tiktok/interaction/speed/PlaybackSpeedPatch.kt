package app.revanced.patches.tiktok.interaction.speed

import app.revanced.patcher.classDef
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.firstClassDef
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.shared.getEnterFromMethod
import app.revanced.patches.tiktok.shared.onRenderFirstFrameMethod
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.returnEarly
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction11x
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val playbackSpeedPatch = bytecodePatch(
    name = "Playback speed",
    description = "Enables the playback speed option for all videos and " +
        "retains the speed configurations in between videos.",
) {
    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    apply {
        getSpeedMethod.apply {
            val injectIndex =
                indexOfFirstInstructionOrThrow { getReference<MethodReference>()?.returnType == "F" } + 2
            val register = getInstruction<Instruction11x>(injectIndex - 1).registerA

            addInstruction(
                injectIndex,
                "invoke-static { v$register }," +
                    " Lapp/revanced/extension/tiktok/speed/PlaybackSpeedPatch;->rememberPlaybackSpeed(F)V",
            )
        }

        // By default, the playback speed will reset to 1.0 at the start of each video.
        // Instead, override it with the desired playback speed.
        onRenderFirstFrameMethod.addInstructions(
            0,
            """
                # Video playback location (e.g. home page, following page or search result page) retrieved using getEnterFrom method.
                const/4 v0, 0x1
                invoke-virtual { p0, v0 },  $getEnterFromMethod
                move-result-object v0

                # Model of current video (getCurrentAweme). Obfuscated, version-specific method
                # name on BaseListFragmentPanel that returns the current Aweme.
                # 45.3.3: LJII()  ->  45.5.3: LJIIIIZZ()
                # Re-derive on a TikTok bump: in BaseListFragmentPanel find the no-arg method
                # returning Lcom/ss/android/ugc/aweme/feed/model/Aweme; whose body reads field
                # LLJJIJIIJIL and delegates to the ViewPagerComponentTemp getter that calls the
                # IViewPagerComponentAbility/0QVT->getAweme path (NOT the getCurrentItem/getItem one).
                invoke-virtual { p0 }, Lcom/ss/android/ugc/aweme/feed/panel/BaseListFragmentPanel;->LJIIIIZZ()Lcom/ss/android/ugc/aweme/feed/model/Aweme;
                move-result-object v1

                # Desired playback speed retrieved using getPlaybackSpeed method.
                invoke-static { }, Lapp/revanced/extension/tiktok/speed/PlaybackSpeedPatch;->getPlaybackSpeed()F
                move-result v2
                invoke-static { v2, v1, v0, v0 }, ${setSpeedMethod.definingClass}->${setSpeedMethod.name}(FLcom/ss/android/ugc/aweme/feed/model/Aweme;Ljava/lang/String;Ljava/lang/String;)V
            """,
        )

        // Force enable the playback speed option for all videos.
        setSpeedMethod.classDef.methods.find { method -> method.returnType == "Z" }?.returnEarly(true)
    }
}
