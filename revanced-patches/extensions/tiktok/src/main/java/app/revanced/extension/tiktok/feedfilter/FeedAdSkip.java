package app.revanced.extension.tiktok.feedfilter;

import android.app.Activity;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import app.revanced.extension.shared.Utils;
import app.revanced.extension.tiktok.settings.Settings;

import com.ss.android.ugc.aweme.feed.model.Aweme;
import com.ss.android.ugc.aweme.feed.model.AwemeExtKt;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Skips feed ads that the {@link AdsFilter} cannot remove at fetch time.
 *
 * Many For-You ads (photo / brand-takeover / clustered ad-pods) are inserted into the feed by
 * TikTok's commercialize ad engine AFTER FeedApiService.fetchFeedList returns, so they never pass
 * through the feed-list filter. By the time a video's first frame renders, though, all ad markers
 * are populated (that is when the "Реклама" tag is shown). This hook runs at onRenderFirstFrame:
 * if the now-current item is an ad, it synthesizes a swipe-up to advance to the next video.
 */
public final class FeedAdSkip {
    private static Aweme lastHandled;

    /** Injected at BaseListFragmentPanel.onRenderFirstFrame with the current Aweme. */
    public static void onAwemeRendered(Aweme aweme) {
        try {
            if (aweme == null || aweme == lastHandled) return;
            if (!Settings.REMOVE_ADS.get() || !Settings.SKIP_ADS_AT_RENDER.get()) return;
            if (!isAd(aweme)) return;
            lastHandled = aweme;
            Activity activity = getResumedActivity();
            if (activity == null) return;
            View decor = activity.getWindow().getDecorView();
            // Small delay so the rendered frame settles before we fling past it.
            Utils.runOnMainThreadDelayed(() -> swipeToNext(decor), 60);
        } catch (Throwable ex) {
            // Never break the feed because of ad skipping.
        }
    }

    /** Current resumed Activity via ActivityThread (works even when registered late). */
    private static Activity getResumedActivity() {
        try {
            Class<?> atClass = Class.forName("android.app.ActivityThread");
            Object activityThread = atClass.getMethod("currentActivityThread").invoke(null);
            Field activitiesField = atClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            Map<?, ?> activities = (Map<?, ?>) activitiesField.get(activityThread);
            if (activities == null) return null;
            for (Object record : activities.values()) {
                Class<?> recordClass = record.getClass();
                Field pausedField = recordClass.getDeclaredField("paused");
                pausedField.setAccessible(true);
                if (!pausedField.getBoolean(record)) {
                    Field activityField = recordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    return (Activity) activityField.get(record);
                }
            }
        } catch (Throwable ex) {
            // ignore
        }
        return null;
    }

    private static boolean isAd(Aweme item) {
        try {
            return item.getAdAwemeSource() != 0
                    || AwemeExtKt.isPseudoAd(item)
                    || item.getAwemeRawAd() != null
                    || item.isAd()
                    || item.isSoftAd();
        } catch (Throwable ex) {
            return false;
        }
    }

    private static void swipeToNext(View view) {
        try {
            int w = view.getWidth();
            int h = view.getHeight();
            if (w <= 0 || h <= 0) return;
            float x = w / 2f;
            float yStart = h * 0.80f;
            float yEnd = h * 0.20f;
            long t0 = SystemClock.uptimeMillis();
            dispatch(view, MotionEvent.ACTION_DOWN, x, yStart, t0, t0);
            int steps = 10;
            for (int i = 1; i <= steps; i++) {
                float y = yStart + (yEnd - yStart) * i / steps;
                dispatch(view, MotionEvent.ACTION_MOVE, x, y, t0, t0 + i * 6L);
            }
            dispatch(view, MotionEvent.ACTION_UP, x, yEnd, t0, t0 + steps * 6L + 6L);
        } catch (Throwable ex) {
            // ignore
        }
    }

    private static void dispatch(View view, int action, float x, float y, long downTime, long eventTime) {
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
        view.dispatchTouchEvent(event);
        event.recycle();
    }
}
