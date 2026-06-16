package app.revanced.extension.tiktok.settings.preference;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.view.View;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.tiktok.Utils;

/**
 * About entry for the tiktok-rv settings menu. Shows the current versions and opens the
 * project's GitHub repository when tapped.
 *
 * <p>Resources cannot be compiled into TikTok (aapt fails on its stripped resources), so this is
 * built in code rather than from XML.
 */
@SuppressWarnings("deprecation")
public class ReVancedTikTokAboutPreference extends Preference {

    /**
     * tiktok-rv release version. Bump this on each release (the releases are date-based, e.g.
     * {@code v2026.06.16}). There is no runtime source for it, unlike the ReVanced Patches version.
     */
    public static final String TIKTOK_RV_VERSION = "v2026.06.16";

    private static final String REPO_URL = "https://github.com/thelok1s/tiktok-rv";

    public ReVancedTikTokAboutPreference(Context context) {
        super(context);

        String patchesVersion = app.revanced.extension.shared.Utils.getPatchesReleaseVersion();

        setTitle("About tiktok-rv");
        setSummary("tiktok-rv " + TIKTOK_RV_VERSION
                + " · ReVanced Patches " + patchesVersion + "\n"
                + "Based on ReVanced. Tap to open the GitHub repository.");
    }

    @Override
    protected void onClick() {
        try {
            Context context = getContext();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception ex) {
            Logger.printException(() -> "Could not open " + REPO_URL, ex);
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        Utils.setTitleAndSummaryColor(view);
    }
}
