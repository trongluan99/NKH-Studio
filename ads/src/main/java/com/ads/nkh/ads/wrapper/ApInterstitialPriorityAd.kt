package com.ads.nkh.ads.wrapper

class ApInterstitialPriorityAd(
    high1PriorityId: String,
    high2PriorityId: String,
    high3PriorityId: String,
    normalPriorityId: String
) {
    var high1PriorityId: String = high1PriorityId
        set(value) {
            field = value
            if (field.isNotEmpty() && high1PriorityInterstitialAd == null) {
                high1PriorityInterstitialAd = ApInterstitialAd()
            }
        }

    var high2PriorityId: String = high2PriorityId
        set(value) {
            field = value
            if (field.isNotEmpty() && high2PriorityInterstitialAd == null) {
                high2PriorityInterstitialAd = ApInterstitialAd()
            }
        }

    var high3PriorityId: String = high3PriorityId
        set(value) {
            field = value
            if (field.isNotEmpty() && high3PriorityInterstitialAd == null) {
                high3PriorityInterstitialAd = ApInterstitialAd()
            }
        }

    var normalPriorityId: String = normalPriorityId
        set(value) {
            field = value
            if (field.isNotEmpty() && normalPriorityInterstitialAd == null) {
                normalPriorityInterstitialAd = ApInterstitialAd()
            }
        }

    var high1PriorityInterstitialAd: ApInterstitialAd? = null
        private set

    var high2PriorityInterstitialAd: ApInterstitialAd? = null
        private set

    var high3PriorityInterstitialAd: ApInterstitialAd? = null
        private set

    var normalPriorityInterstitialAd: ApInterstitialAd? = null
        private set

    init {
        if (this.high1PriorityId.isNotEmpty() && this.high1PriorityInterstitialAd == null) {
            this.high1PriorityInterstitialAd = ApInterstitialAd()
        }
        if (this.high2PriorityId.isNotEmpty() && this.high2PriorityInterstitialAd == null) {
            this.high2PriorityInterstitialAd = ApInterstitialAd()
        }
        if (this.high3PriorityId.isNotEmpty() && this.high3PriorityInterstitialAd == null) {
            this.high3PriorityInterstitialAd = ApInterstitialAd()
        }
        if (this.normalPriorityId.isNotEmpty() && this.normalPriorityInterstitialAd == null) {
            this.normalPriorityInterstitialAd = ApInterstitialAd()
        }
    }
}
