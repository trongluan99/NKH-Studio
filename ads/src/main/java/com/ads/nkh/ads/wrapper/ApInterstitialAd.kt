package com.ads.nkh.ads.wrapper

import com.applovin.mediation.ads.MaxInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd

class ApInterstitialAd : ApAdBase {
    var interstitialAd: InterstitialAd? = null
        set(value) {
            field = value
            status = StatusAd.AD_LOADED
        }

    var maxInterstitialAd: MaxInterstitialAd? = null
        set(value) {
            field = value
            status = StatusAd.AD_LOADED
        }

    constructor(status: StatusAd) : super(status)

    constructor() : super()

    constructor(maxInterstitialAd: MaxInterstitialAd?) : super() {
        this.maxInterstitialAd = maxInterstitialAd
        status = StatusAd.AD_LOADED
    }

    constructor(interstitialAd: InterstitialAd?) : super() {
        this.interstitialAd = interstitialAd
        status = StatusAd.AD_LOADED
    }

    override fun isReady(): Boolean {
        if (maxInterstitialAd != null && maxInterstitialAd!!.isReady) {
            return true
        }
        return interstitialAd != null
    }
}
