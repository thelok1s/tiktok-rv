package app.revanced.extension.tiktok.feedfilter;

import app.revanced.extension.tiktok.settings.Settings;
import com.ss.android.ugc.aweme.feed.model.Aweme;
import com.ss.android.ugc.aweme.feed.model.AwemeExtKt;

public class AdsFilter implements IFilter {
    @Override
    public boolean getEnabled() {
        return Settings.REMOVE_ADS.get();
    }

    @Override
    public boolean getFiltered(Aweme item) {
        // Ads on the For-You feed take several forms that each need a different signal:
        //  - getAdAwemeSource() != 0: ad slot flagged at fetchFeedList time, BEFORE the awemeRawAd
        //    payload is attached at render (video brand-takeover placements).
        //  - AwemeExtKt.isPseudoAd(): pseudo / front-end ads (photo-mode, brand-takeover) whose
        //    payload lives in getCommerceVideoAuthInfo().pseudoAdData, not awemeRawAd — this is
        //    TikTok's own classifier, matching what their ad pipeline uses.
        //  - getAwemeRawAd()/isAd(): standard ads where the payload is already present.
        //  - isWithPromotionalMusic(): branded-music posts.
        return item.getAdAwemeSource() != 0
                || AwemeExtKt.isPseudoAd(item)
                || item.getAwemeRawAd() != null
                || item.isAd()
                || item.isWithPromotionalMusic();
    }
}
