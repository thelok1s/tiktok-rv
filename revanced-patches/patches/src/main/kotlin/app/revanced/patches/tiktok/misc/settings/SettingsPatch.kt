package app.revanced.patches.tiktok.misc.settings

import app.revanced.patcher.extensions.*
import app.revanced.patcher.immutableClassDef
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.misc.extension.sharedExtensionPatch
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/tiktok/settings/TikTokActivityHook;"

val settingsPatch = bytecodePatch(
    name = "Settings",
    description = "Adds ReVanced settings to TikTok.",
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    apply {
        val initializeSettingsMethodDescriptor =
            "$EXTENSION_CLASS_DESCRIPTOR->initialize(" +
                "Lcom/bytedance/ies/ugc/aweme/commercialize/compliance/personalization/AdPersonalizationActivity;" +
                ")Z"

        val createSettingsEntryMethodDescriptor =
            "$EXTENSION_CLASS_DESCRIPTOR->createSettingsEntry(" +
                "Ljava/lang/String;" +
                "Ljava/lang/String;" +
                ")Ljava/lang/Object;"

        fun String.toClassName() = substring(1, this.length - 1).replace("/", ".")

        // Find the class name of classes which construct a settings entry
        val settingsButtonClass = settingsEntryMethod.immutableClassDef.type.toClassName()
        val settingsButtonInfoClass = settingsEntryInfoMethod.immutableClassDef.type.toClassName()

        // Create a settings entry for 'revanced settings' and add it to the settings page.
        //
        // On TikTok 45.5.x the settings UI no longer exposes a `headerUnit` field. Instead each
        // `*Page` (here `AboutPage.onViewCreated`) populates a shared "unit manager" by repeatedly
        // calling `<this>.getManager()` (an obfuscated no-arg getter returning the manager type)
        // followed by `manager.add(unit)` (a one-arg `void` call), and finishing with a no-arg
        // `void` commit call on the manager. We derive these obfuscated members from the bytecode
        // itself (so we never hardcode the rename-churned names), then inject one more
        // get-manager + add pair for the ReVanced entry just before the commit call.
        addSettingsEntryMethod.apply {
            val pageClass = definingClass

            // The get-manager call: first `invoke-virtual {this}` on a method declared by the page
            // class that takes no parameters and returns a reference type (the unit manager).
            val getManagerIndex = indexOfFirstInstruction {
                opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.let { ref ->
                        ref.definingClass == pageClass &&
                            ref.parameterTypes.isEmpty() &&
                            ref.returnType.startsWith("L")
                    } == true
            }
            val getManager = getInstruction(getManagerIndex)
            val managerType = getManager.getReference<MethodReference>()!!.returnType

            // The add-unit call: first `invoke-virtual` on the manager type taking a single
            // parameter (the unit) and returning void.
            val addEntryIndex = indexOfFirstInstruction(getManagerIndex) {
                opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.let { ref ->
                        ref.definingClass == managerType &&
                            ref.parameterTypes.size == 1 &&
                            ref.returnType == "V"
                    } == true
            }
            val addEntry = getInstruction(addEntryIndex)

            // The commit call: the no-arg void call on the manager type (`manager.commit()`),
            // emitted once after every unit has been added. We inject right before its
            // get-manager so the ReVanced entry is the last unit added before rendering.
            val commitIndex = indexOfFirstInstruction(addEntryIndex) {
                opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.let { ref ->
                        ref.definingClass == managerType &&
                            ref.parameterTypes.isEmpty() &&
                            ref.returnType == "V"
                    } == true
            }
            // Walk back from the commit to its own get-manager (`invoke-virtual {this}` +
            // `move-result`); we insert our get-manager + add pair just before it.
            val commitGetManagerIndex = indexOfFirstInstructionReversed(commitIndex) {
                opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.let { ref ->
                        ref.definingClass == pageClass &&
                            ref.parameterTypes.isEmpty() &&
                            ref.returnType == managerType
                    } == true
            }

            // `this` is the sole register live into the commit get-manager.
            val thisRegister = getInstruction<FiveRegisterInstruction>(commitGetManagerIndex).registerC

            // The add immediately preceding the commit gives us two registers (its manager and its
            // unit operand) that are provably dead at the injection point, so we can safely reuse
            // them as scratch without disturbing `this`.
            val lastAddIndex = indexOfFirstInstructionReversed(commitGetManagerIndex) {
                opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.let { ref ->
                        ref.definingClass == managerType &&
                            ref.parameterTypes.size == 1 &&
                            ref.returnType == "V"
                    } == true
            }
            val lastAdd = getInstruction<FiveRegisterInstruction>(lastAddIndex)
            val managerRegister = lastAdd.registerC
            val entryRegister = lastAdd.registerD

            // If createSettingsEntry returns null (e.g. a future ExposeItem signature change it
            // can't satisfy), skip the add so the host page still renders its own rows instead of
            // failing to build the unit list.
            addInstructionsWithLabels(
                commitGetManagerIndex,
                """
                    const-string v$entryRegister, "$settingsButtonClass"
                    const-string v$managerRegister, "$settingsButtonInfoClass"
                    invoke-static {v$entryRegister, v$managerRegister}, $createSettingsEntryMethodDescriptor
                    move-result-object v$entryRegister
                    if-eqz v$entryRegister, :revanced_skip_entry
                    check-cast v$entryRegister, ${settingsEntryMethod.immutableClassDef.type}
                    invoke-virtual {v$thisRegister}, ${getManager.getReference<MethodReference>()}
                    move-result-object v$managerRegister
                    invoke-virtual {v$managerRegister, v$entryRegister}, ${addEntry.getReference<MethodReference>()}
                """,
                ExternalLabel("revanced_skip_entry", getInstruction(commitGetManagerIndex)),
            )
        }

        // Initialize the settings menu once the replaced setting entry is clicked.
        adPersonalizationActivityOnCreateMethod.apply {
            val initializeSettingsIndex = indexOfFirstInstruction(Opcode.INVOKE_SUPER) + 1

            val thisRegister = getInstruction<FiveRegisterInstruction>(initializeSettingsIndex - 1).registerC
            val usableRegister = implementation!!.registerCount - parameters.size - 2

            addInstructionsWithLabels(
                initializeSettingsIndex,
                """
                    invoke-static { v$thisRegister }, $initializeSettingsMethodDescriptor
                    move-result v$usableRegister
                    if-eqz v$usableRegister, :do_not_open
                    return-void
                """,
                ExternalLabel("do_not_open", getInstruction(initializeSettingsIndex)),
            )
        }
    }
}
