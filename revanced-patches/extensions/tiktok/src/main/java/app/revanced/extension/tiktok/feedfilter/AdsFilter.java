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
        // getAwemeRawAd() != null is the broadest ad signal: it is non-null for any Aweme
        // carrying an ad payload, including brand-takeover / soft ads where isAd() returns
        // false (isAd() requires BOTH the isAd flag and awemeRawAd). isWithPromotionalMusic()
        // covers branded-music posts that may not carry a raw ad.
        return item.getAwemeRawAd() != null || item.isAd() || item.isWithPromotionalMusic();
    }
}
