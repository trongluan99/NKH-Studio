package com.ads.nkh.funtion

import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem

interface RewardCallbackPreload {
    fun onNextAction() {}

    fun onAdClosed() {}

    fun onAdFailedToShow(adError: FullScreenContentError?) {}

    fun onAdClicked() {}

    fun onAdImpression() {}

    fun onUserEarnedReward(var1: RewardItem?) {}
}
