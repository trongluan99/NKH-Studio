package com.ads.nkh.funtion

import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem

interface RewardCallback {
    fun onUserEarnedReward(var1: RewardItem?)

    fun onRewardedAdClosed()

    fun onRewardedAdFailedToShow(codeError: Int)

    fun onAdClicked()

    fun onAdClicked(adUnitId: String?, mediationAdapterClassName: String?, adType: AdType?)

    fun onAdImpression()

    fun onAdLogRev(
        adValue: AdValue?,
        adUnitId: String?,
        mediationAdapterClassName: String?,
        adType: AdType?
    )
}
