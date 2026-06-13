package com.ss.android.ugc.aweme.feed.model;

// Dummy class — real is TikTok's Kotlin facade com.ss.android.ugc.aweme.feed.model.AwemeExtKt.
// isPseudoAd(Aweme) is TikTok's own check for pseudo / front-end ads (brand-takeover, photo
// ads) whose payload lives in Aweme.getCommerceVideoAuthInfo().pseudoAdData rather than
// awemeRawAd — so getAwemeRawAd()/isAd() miss them. Real signature:
// public static final boolean isPseudoAd(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z
public class AwemeExtKt {
    public static boolean isPseudoAd(Aweme aweme) {
        throw new UnsupportedOperationException("Stub");
    }
}
