package app.revanced.extension.tiktok.settings.preference.categories;

import android.content.Context;
import android.preference.PreferenceScreen;
import app.revanced.extension.tiktok.settings.Settings;
import app.revanced.extension.tiktok.settings.SettingsStatus;
import app.revanced.extension.tiktok.settings.preference.TogglePreference;

@SuppressWarnings("deprecation")
public class FeedFilterPreferenceCategory extends ConditionalPreferenceCategory {
    public FeedFilterPreferenceCategory(Context context, PreferenceScreen screen) {
        super(context, screen);
        setTitle("Feed filter");
    }

    @Override
    public boolean getSettingsStatus() {
        return SettingsStatus.feedFilterEnabled;
    }

    @Override
    public void addPreferences(Context context) {
        addPreference(new TogglePreference(
                context,
                "Remove feed ads", "Remove ads from feed.",
                Settings.REMOVE_ADS
        ));
        addPreference(new TogglePreference(
                context,
                "Skip ads at render", "Auto-swipe past an ad that slips through the filter. Turn off to stop the feed from auto-advancing (ads may still appear).",
                Settings.SKIP_ADS_AT_RENDER
        ));
        addPreference(new TogglePreference(
                context,
                "Hide TikTok Shop", "Hide TikTok shop from feed.",
                Settings.HIDE_SHOP
        ));
        addPreference(new TogglePreference(
                context,
                "Hide livestreams", "Hide livestreams from feed.",
                Settings.HIDE_LIVE
        ));
        addPreference(new TogglePreference(
                context,
                "Hide story", "Hide story from feed.",
                Settings.HIDE_STORY
        ));
        addPreference(new TogglePreference(
                context,
                "Hide image video", "Hide image video from feed.",
                Settings.HIDE_IMAGE
        ));
    }
}
