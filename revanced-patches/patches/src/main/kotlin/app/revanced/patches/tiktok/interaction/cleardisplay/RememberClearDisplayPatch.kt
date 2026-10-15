package app.revanced.patches.tiktok.interaction.cleardisplay

import app.revanced.patcher.extensions.ExternalLabel
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.firstClassDef
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.shared.onRenderFirstFrameMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
val rememberClearDisplayPatch = bytecodePatch(
    name = "Remember clear display",
    description = "Remembers the clear display configurations in between videos.",
) {
    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    apply {
        onClearDisplayEventMethod.apply {
            // region Hook the "Clear display" configuration save event to remember the state of clear display.

            val isEnabledIndex = indexOfFirstInstructionOrThrow(Opcode.IGET_BOOLEAN) + 1
            val isEnabledRegister = getInstruction<TwoRegisterInstruction>(isEnabledIndex - 1).registerA

            addInstructions(
                isEnabledIndex,
                "invoke-static { v$isEnabledRegister }, " +
                    "Lapp/revanced/extension/tiktok/cleardisplay/RememberClearDisplayPatch;->rememberClearDisplayState(Z)V",
            )

            // endregion

            // region Override the "Clear display" configuration load event to load the state of clear display.

            val clearDisplayEventClass = parameters[0].type

            // The event constructor's argument order changed in 47.x; pick the one this version has.
            // Registers: v0 = event, v1 = type, v2 = enter method, v3 = type name, v4 = state.
            val eventConstructors = firstClassDef(clearDisplayEventClass).methods
                .filter { it.name == "<init>" }
                .map { it.parameterTypes.joinToString("") }
            val constructEvent = when {
                // Up to 46.x: (type, enterMethod, typeName, isClean).
                "ILjava/lang/String;Ljava/lang/String;Z" in eventConstructors ->
                    "invoke-direct { v0, v1, v2, v3, v4 }, " +
                        "$clearDisplayEventClass-><init>(ILjava/lang/String;Ljava/lang/String;Z)V"
                // 47.x: (isClean, type, enterMethod, typeName).
                "ZILjava/lang/String;Ljava/lang/String;" in eventConstructors ->
                    "invoke-direct { v0, v4, v1, v2, v3 }, " +
                        "$clearDisplayEventClass-><init>(ZILjava/lang/String;Ljava/lang/String;)V"
                else -> throw PatchException(
                    "No known clear display event constructor in $clearDisplayEventClass: $eventConstructors",
                )
            }
            onRenderFirstFrameMethod.addInstructionsWithLabels(
                0,
                """
                    # Create a new clearDisplayEvent and post it to the EventBus (https://github.com/greenrobot/EventBus)

                    # Clear display type such as 0 = LONG_PRESS, 1 = SCREEN_RECORD etc.
                    const/4 v1, 0x0

                    # Enter method (Such as "pinch", "swipe_exit", or an empty string (unknown, what it means)).
                    const-string v2, ""

                    # Name of the clear display type which is equivalent to the clear display type.
                    const-string v3, "long_press"
                    
                     # The state of clear display.
                    invoke-static { }, Lapp/revanced/extension/tiktok/cleardisplay/RememberClearDisplayPatch;->getClearDisplayState()Z
                    move-result v4
                    if-eqz v4, :clear_display_disabled

                    new-instance v0, $clearDisplayEventClass
                    $constructEvent
                    invoke-virtual { v0 }, $clearDisplayEventClass->post()Lcom/ss/android/ugc/governance/eventbus/IEvent;
                    """,
                ExternalLabel("clear_display_disabled", onRenderFirstFrameMethod.getInstruction(0)),
            )

            // endregion
        }
    }
}
