package app.revanced.extension.tiktok.feedfilter;

import app.revanced.extension.tiktok.settings.Settings;
import com.ss.android.ugc.aweme.feed.model.Aweme;

public class AdsFilter implements IFilter {
    @Override
    public boolean getEnabled() {
        return Settings.REMOVE_ADS.get();
    }

    @Override
    public boolean getFiltered(Aweme item) {
        // getAdAwemeSource() != 0 marks an ad slot at fetchFeedList time, BEFORE the awemeRawAd
        // payload is lazily attached at render — so getAwemeRawAd()/isAd() are still null/false
        // there. It reliably removes video brand-takeover ads (e.g. the For-You "Реклама"
        // placements) that the other signals miss. NOTE: photo-mode ads and other lazily-marked
        // ads are still not caught at this hook — those need a render-time or network approach.
        // getAwemeRawAd()/isAd()/isWithPromotionalMusic() cover paths where the ad payload is
        // already present (e.g. paginated loads, branded-music posts).
        return item.getAdAwemeSource() != 0
                || item.getAwemeRawAd() != null
                || item.isAd()
                || item.isWithPromotionalMusic();
    }
}
