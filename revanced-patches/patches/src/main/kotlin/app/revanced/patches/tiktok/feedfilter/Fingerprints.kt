package app.revanced.patches.tiktok.feedfilter

import app.revanced.patcher.*
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val BytecodePatchContext.feedApiServiceLIZMethod by gettingFirstMethodDeclaratively {
    name("fetchFeedList")
    definingClass("/FeedApiService;")
}

internal val BytecodePatchContext.followFeedMethod by gettingFirstMethodDeclaratively {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    returnType("Lcom/ss/android/ugc/aweme/follow/presenter/FollowFeedList;")
    instructions("feed"(String::equals))
}