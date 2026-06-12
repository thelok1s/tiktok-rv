package app.revanced.patches.tiktok.misc.spoof.sim

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.firstMethod
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.tiktok.misc.extension.sharedExtensionPatch

import app.revanced.patches.tiktok.misc.settings.settingsStatusLoadMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val sIMSpoofPatch = bytecodePatch(
    name = "SIM spoof",
    description = "Spoofs the information which is retrieved from the SIM card.",
    use = false,
) {
    dependsOn(
        sharedExtensionPatch,

    )

    compatibleWith(
        "com.ss.android.ugc.trill",
        "com.zhiliaoapp.musically",
    )

    apply {
        // TelephonyManager method name -> Triple(extension method, smali type, isReferenceType).
        // The smali type is used both for the parameter and the return value; isReferenceType
        // picks move-result-object (true) vs move-result (false).
        val replacements = hashMapOf(
            "getSimCountryIso" to Triple("getCountryIso", "Ljava/lang/String;", true),
            "getNetworkCountryIso" to Triple("getCountryIso", "Ljava/lang/String;", true),
            "getSimOperator" to Triple("getOperator", "Ljava/lang/String;", true),
            "getNetworkOperator" to Triple("getOperator", "Ljava/lang/String;", true),
            "getSimOperatorName" to Triple("getOperatorName", "Ljava/lang/String;", true),
            "getNetworkOperatorName" to Triple("getOperatorName", "Ljava/lang/String;", true),
            // Make a SIM-less device look like it has a ready SIM, so the spoofed operator stays consistent.
            "getSimState" to Triple("getSimState", "I", false),
            "hasIccCard" to Triple("hasIccCard", "Z", false),
        )

        // Find all api call to check sim information.
        buildMap {
            classDefs.forEach { classDef ->
                classDef.methods.let { methods ->
                    buildMap methodList@{
                        methods.forEach methods@{ method ->
                            with(method.implementation?.instructions ?: return@methods) {
                                ArrayDeque<Pair<Int, Triple<String, String, Boolean>>>().also { patchIndices ->
                                    this.forEachIndexed { index, instruction ->
                                        if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@forEachIndexed

                                        val methodRef =
                                            (instruction as Instruction35c).reference as MethodReference
                                        if (methodRef.definingClass != "Landroid/telephony/TelephonyManager;") return@forEachIndexed

                                        replacements[methodRef.name]?.let { replacement ->
                                            patchIndices.add(index to replacement)
                                        }
                                    }
                                }.also { if (it.isEmpty()) return@methods }.let { patches ->
                                    put(method, patches)
                                }
                            }
                        }
                    }
                }.also { if (it.isEmpty()) return@forEach }.let { methodPatches ->
                    put(classDef, methodPatches)
                }
            }
        }.forEach { (classDef, methods) ->
            methods.forEach { (method, patches) ->
                with(classDef.firstMethod(method)) {
                    while (!patches.isEmpty()) {
                        val (index, target) = patches.removeLast()
                        val (extensionMethod, descriptor, isReferenceType) = target

                        val resultReg = getInstruction<OneRegisterInstruction>(index + 1).registerA
                        val moveResult = if (isReferenceType) "move-result-object" else "move-result"

                        // Patch Android API and return fake sim information.
                        addInstructions(
                            index + 2,
                            """
                                invoke-static {v$resultReg}, Lapp/revanced/extension/tiktok/spoof/sim/SpoofSimPatch;->$extensionMethod($descriptor)$descriptor
                                $moveResult v$resultReg
                            """,
                        )
                    }
                }
            }
        }

        // Enable patch in settings.
        settingsStatusLoadMethod.addInstruction(
            0,
            "invoke-static {}, Lapp/revanced/extension/tiktok/settings/SettingsStatus;->enableSimSpoof()V",
        )
    }
}
