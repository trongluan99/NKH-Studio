package com.ads.nkh.ads.wrapper

class ApInterstitialPriority2Ad(
    highPriorityId: String,
    normalPriorityId: String
) {
    var highPriorityId: String = highPriorityId
        set(value) {
            field = value
            if (field.isNotEmpty() && highPriorityInterstitialAd == null) {
                highPriorityInterstitialAd = ApInterstitialAd()
            }
        }

    var normalPriorityId: String = normalPriorityId
        set(value) {
            field = value
            if (field.isNotEmpty() && normalPriorityInterstitialAd == null) {
                normalPriorityInterstitialAd = ApInterstitialAd()
            }
        }

    var highPriorityInterstitialAd: ApInterstitialAd? = null
        private set

    var normalPriorityInterstitialAd: ApInterstitialAd? = null
        private set

    init {
        if (this.highPriorityId.isNotEmpty() && this.highPriorityInterstitialAd == null) {
            this.highPriorityInterstitialAd = ApInterstitialAd()
        }
        if (this.normalPriorityId.isNotEmpty() && this.normalPriorityInterstitialAd == null) {
            this.normalPriorityInterstitialAd = ApInterstitialAd()
        }
    }
}
