package app.revanced.extension.tiktok.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.Utils;
import app.revanced.extension.tiktok.settings.preference.TikTokPreferenceFragment;

import com.bytedance.ies.ugc.aweme.commercialize.compliance.personalization.AdPersonalizationActivity;

import java.lang.reflect.Constructor;

/**
 * Hooks AdPersonalizationActivity to inject a custom {@link TikTokPreferenceFragment}.
 */
@SuppressWarnings({"deprecation", "unused"})
public class TikTokActivityHook {
    /**
     * Flags/visibility bitmask TikTok passes as the trailing int of the ExposeItem constructor
     * for plain clickable settings rows (observed verbatim in AboutPage.onViewCreated on 45.5.3).
     */
    private static final int EXPOSE_ITEM_FLAGS = 0xFFFFF0;

    /**
     * Builds a TikTok settings entry ("ExposeItem" wrapped in an "EventUnit") that opens the
     * ReVanced settings when tapped, and returns it so the Settings patch can add it to a
     * settings page's unit list.
     *
     * <p>The ExposeItem constructor is heavily version-specific. On older TikToks it was
     * {@code (title, icon, OnClickListener, key)}; on 45.5.3 it has a long "real" constructor and
     * a shorter convenience one, both starting {@code (String title, <icon>, View.OnClickListener, ...)}.
     * Rather than hardcode a signature, we pick the SHORTEST constructor matching that prefix and
     * mirror the argument values TikTok itself uses for simple rows (see AboutPage): an icon
     * instance, our click listener, type-default zeros/nulls for the middle params, and the
     * {@link #EXPOSE_ITEM_FLAGS} mask for the trailing int. Returns {@code null} (logged) on
     * failure rather than throwing, so a future signature change degrades to "no entry" instead
     * of crashing the host settings page.
     *
     * @param entryClazzName     obfuscated EventUnit class (wraps the ExposeItem)
     * @param entryInfoClazzName obfuscated ExposeItem class (the entry's data)
     */
    public static Object createSettingsEntry(String entryClazzName, String entryInfoClazzName) {
        try {
            Class<?> entryClazz = Class.forName(entryClazzName);
            Class<?> entryInfoClazz = Class.forName(entryInfoClazzName);

            Constructor<?> entryInfoConstructor = null;
            for (Constructor<?> candidate : entryInfoClazz.getDeclaredConstructors()) {
                Class<?>[] params = candidate.getParameterTypes();
                boolean matchesPrefix = params.length >= 3
                        && params[0] == String.class
                        && params[2] == View.OnClickListener.class;
                if (matchesPrefix && (entryInfoConstructor == null
                        || params.length < entryInfoConstructor.getParameterTypes().length)) {
                    entryInfoConstructor = candidate;
                }
            }
            if (entryInfoConstructor == null) {
                throw new NoSuchMethodException(
                        entryInfoClazzName + ": no (String, icon, View.OnClickListener, ...) constructor");
            }
            entryInfoConstructor.setAccessible(true);

            Class<?>[] params = entryInfoConstructor.getParameterTypes();
            Object[] args = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                args[i] = defaultValue(params[i]);
            }
            args[0] = "ReVanced settings";
            args[1] = newInstanceOrNull(params[1]); // icon object (no-arg ctor, as in native code)
            args[2] = (View.OnClickListener) view -> startSettingsActivity();
            int last = params.length - 1;
            if (params[last] == int.class) {
                args[last] = EXPOSE_ITEM_FLAGS;
            }

            Object buttonInfo = entryInfoConstructor.newInstance(args);
            return entryClazz.getConstructor(entryInfoClazz).newInstance(buttonInfo);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Logger.printException(() -> "createSettingsEntry failed", e);
            return null;
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return Boolean.FALSE;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return (char) 0;
        return null;
    }

    private static Object newInstanceOrNull(Class<?> type) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            Logger.printException(() -> "Could not instantiate icon " + type, e);
            return null;
        }
    }

    /***
     * Initialize the settings menu.
     * @param base The activity to initialize the settings menu on.
     * @return Whether the settings menu should be initialized.
     */
    public static boolean initialize(AdPersonalizationActivity base) {
        Bundle extras = base.getIntent().getExtras();
        if (extras != null && !extras.getBoolean("revanced", false)) return false;

        SettingsStatus.load();

        LinearLayout linearLayout = new LinearLayout(base);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setFitsSystemWindows(true);
        linearLayout.setTransitionGroup(true);
        // Solid dark background so the dark-mode text colours (see TikTokPreferenceFragment) are
        // always readable, regardless of the host activity's theme.
        linearLayout.setBackgroundColor(0xFF121212);

        FrameLayout fragment = new FrameLayout(base);
        fragment.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        int fragmentId = View.generateViewId();
        fragment.setId(fragmentId);

        linearLayout.addView(fragment);
        base.setContentView(linearLayout);

        PreferenceFragment preferenceFragment = new TikTokPreferenceFragment();
        base.getFragmentManager().beginTransaction().replace(fragmentId, preferenceFragment).commit();

        return true;
    }

    private static void startSettingsActivity() {
        Context appContext = Utils.getContext();
        if (appContext != null) {
            Intent intent = new Intent(appContext, AdPersonalizationActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("revanced", true);
            appContext.startActivity(intent);
        } else {
            Logger.printDebug(() -> "Utils.getContext() return null");
        }
    }
}
