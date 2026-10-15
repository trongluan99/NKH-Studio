package com.ads.nkh.ads.wrapper

import com.applovin.mediation.ads.MaxRewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd

class ApRewardAd : ApAdBase {
    private var admobReward: RewardedAd? = null
    private var admobRewardInter: RewardedInterstitialAd? = null
    private var maxReward: MaxRewardedAd? = null

    constructor() : super()

    constructor(status: StatusAd) : super(status)

    constructor(maxReward: MaxRewardedAd?) : super() {
        this.maxReward = maxReward
        status = StatusAd.AD_LOADED
    }

    constructor(admobRewardInter: RewardedInterstitialAd?) : super() {
        this.admobRewardInter = admobRewardInter
        status = StatusAd.AD_LOADED
    }

    constructor(admobReward: RewardedAd?) : super() {
        this.admobReward = admobReward
        status = StatusAd.AD_LOADED
    }

    fun setAdmobReward(admobReward: RewardedAd?) {
        this.admobReward = admobReward
        status = StatusAd.AD_LOADED
    }

    fun setAdmobReward(admobRewardInter: RewardedInterstitialAd?) {
        this.admobRewardInter = admobRewardInter
    }

    fun setMaxReward(maxReward: MaxRewardedAd?) {
        this.maxReward = maxReward
        status = StatusAd.AD_LOADED
    }

    fun getAdmobReward(): RewardedAd? {
        return admobReward
    }

    fun getAdmobRewardInter(): RewardedInterstitialAd? {
        return admobRewardInter
    }

    fun getMaxReward(): MaxRewardedAd? {
        return maxReward
    }

    /**
     * Clean reward when shown
     */
    fun clean() {
        maxReward = null
        admobReward = null
        admobRewardInter = null
    }

    val isRewardInterstitial: Boolean
        @JvmName("_isRewardInterstitial_prop") get() = isRewardInterstitial()

    override fun isReady(): Boolean {
        return admobReward != null || admobRewardInter != null || (maxReward != null && maxReward!!.isReady)
    }

    fun isRewardInterstitial(): Boolean {
        return admobRewardInter != null
    }
}
