package app.revanced.extension.tiktok;

import static app.revanced.extension.shared.Utils.isDarkModeEnabled;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;

public class Utils {

    // Colors picked by hand. These should be replaced with the styled resources TikTok uses.
    private static final @ColorInt int TEXT_DARK_MODE_TITLE = Color.WHITE;
    private static final @ColorInt int TEXT_DARK_MODE_SUMMARY
            = Color.argb(255, 170, 170, 170);

    private static final @ColorInt int TEXT_LIGHT_MODE_TITLE = Color.BLACK;
    private static final @ColorInt int TEXT_LIGHT_MODE_SUMMARY
            = Color.argb(255, 80, 80, 80);

    public static void setTitleAndSummaryColor(View view) {
        final boolean darkModeEnabled = isDarkModeEnabled();

        TextView title = view.findViewById(android.R.id.title);
        title.setTextColor(darkModeEnabled
                ? TEXT_DARK_MODE_TITLE
                : TEXT_LIGHT_MODE_TITLE);

        TextView summary = view.findViewById(android.R.id.summary);
        summary.setTextColor(darkModeEnabled
                ? TEXT_DARK_MODE_SUMMARY
                : TEXT_LIGHT_MODE_SUMMARY);
    }
}