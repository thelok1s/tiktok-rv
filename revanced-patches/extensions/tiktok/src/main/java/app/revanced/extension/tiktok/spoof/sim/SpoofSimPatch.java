package app.revanced.extension.tiktok.spoof.sim;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.Utils;
import app.revanced.extension.tiktok.settings.Settings;

@SuppressWarnings("unused")
public class SpoofSimPatch {

    /**
     * {@link android.telephony.TelephonyManager#SIM_STATE_READY}.
     * Hardcoded to avoid a framework import in the extension.
     */
    private static final int SIM_STATE_READY = 5;

    /**
     * During app startup native code can be called with no obvious way to set the context.
     * Cannot check if sim spoofing is enabled or the app will crash since no context is set.
     */
    private static boolean isContextNotSet(String fieldSpoofed) {
        if (Utils.getContext() != null) {
            return false;
        }

        Logger.printException(() -> "Context is not yet set, cannot spoof: " + fieldSpoofed, null);
        return true;
    }

    public static String getCountryIso(String value) {
        if (isContextNotSet("countryIso")) return value;

        if (Settings.SIM_SPOOF.get()) {
            String iso = Settings.SIM_SPOOF_ISO.get();
            Logger.printDebug(() -> "Spoofing countryIso from: " + value + " to: " + iso);
            return iso;
        }

        return value;
    }

    public static String getOperator(String value) {
        if (isContextNotSet("MCC-MNC")) return value;

        if (Settings.SIM_SPOOF.get()) {
            String mcc_mnc = Settings.SIMSPOOF_MCCMNC.get();
            Logger.printDebug(() -> "Spoofing sim MCC-MNC from: " + value + " to: " + mcc_mnc);
            return mcc_mnc;
        }

        return value;
    }

    public static String getOperatorName(String value) {
        if (isContextNotSet("operatorName")) return value;

        if (Settings.SIM_SPOOF.get()) {
            String operator = Settings.SIMSPOOF_OP_NAME.get();
            Logger.printDebug(() -> "Spoofing sim operatorName from: " + value + " to: " + operator);
            return operator;
        }

        return value;
    }

    /**
     * Make a SIM-less device report a present, ready SIM so the spoofed operator/country
     * stays consistent (TikTok checks the SIM state alongside the operator info).
     */
    public static int getSimState(int value) {
        if (isContextNotSet("simState")) return value;

        if (Settings.SIM_SPOOF.get()) {
            Logger.printDebug(() -> "Spoofing simState from: " + value + " to: " + SIM_STATE_READY);
            return SIM_STATE_READY;
        }

        return value;
    }

    public static boolean hasIccCard(boolean value) {
        if (isContextNotSet("hasIccCard")) return value;

        if (Settings.SIM_SPOOF.get()) {
            Logger.printDebug(() -> "Spoofing hasIccCard from: " + value + " to: true");
            return true;
        }

        return value;
    }
}
