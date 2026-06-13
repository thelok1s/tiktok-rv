package app.revanced.patches.tiktok.feedfilter

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.misc.extension.sharedExtensionPatch

import app.revanced.patches.tiktok.interaction.speed.getCurrentAwemeMethod
import app.revanced.patches.tiktok.misc.settings.settingsStatusLoadMethod
import app.revanced.patches.tiktok.shared.onRenderFirstFrameMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/revanced/extension/tiktok/feedfilter/FeedItemsFilter;"

@Suppress("unused")
val feedFilterPatch = bytecodePatch(
    name = "Feed filter",
    description = "Removes ads, livestreams, stories, image videos " +
        "and videos with a specific amount of views or likes from the feed.",
) {
    dependsOn(
        sharedExtensionPatch,

    )

    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    apply {
        arrayOf(
            feedApiServiceLIZMethod to "$EXTENSION_CLASS_DESCRIPTOR->filter(Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;)V",
            followFeedMethod to "$EXTENSION_CLASS_DESCRIPTOR->filter(Lcom/ss/android/ugc/aweme/follow/presenter/FollowFeedList;)V",
        ).forEach { (method, filterSignature) ->
            val returnInstruction = method.instructions.first { it.opcode == Opcode.RETURN_OBJECT }
            val register = (returnInstruction as OneRegisterInstruction).registerA
            method.addInstruction(
                returnInstruction.location.index,
                "invoke-static { v$register }, $filterSignature",
            )
        }

        settingsStatusLoadMethod.addInstruction(
            0,
            "invoke-static {}, Lapp/revanced/extension/tiktok/settings/SettingsStatus;->enableFeedFilter()V",
        )

        // Render-time ad skip. For-You ads inserted by the ad engine after fetchFeedList (photo /
        // brand-takeover / clustered ad-pods) never reach the feed-list filter above, but by the
        // time a video's first frame renders all ad markers are populated. Detect the current
        // Aweme here and, if it is an ad, advance to the next video.
        onRenderFirstFrameMethod.addInstructions(
            0,
            """
                invoke-virtual { p0 }, Lcom/ss/android/ugc/aweme/feed/panel/BaseListFragmentPanel;->${getCurrentAwemeMethod.name}()Lcom/ss/android/ugc/aweme/feed/model/Aweme;
                move-result-object v0
                invoke-static { v0 }, Lapp/revanced/extension/tiktok/feedfilter/FeedAdSkip;->onAwemeRendered(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)V
            """,
        )
    }
}
