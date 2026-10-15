package com.ads.nkh.ads.wrapper

import com.applovin.mediation.ads.MaxInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd

class ApInterstitialAd : ApAdBase {
    var interstitialAd: InterstitialAd? = null
        set(value) {
            field = value
            if (value != null) {
                status = StatusAd.AD_LOADED
            } else if (maxInterstitialAd == null &&
                (status == StatusAd.AD_LOADED || status == StatusAd.AD_RENDER_SUCCESS)
            ) {
                status = StatusAd.AD_INIT
            }
        }

    var maxInterstitialAd: MaxInterstitialAd? = null
        set(value) {
            field = value
            if (value != null) {
                status = StatusAd.AD_LOADED
            } else if (interstitialAd == null &&
                (status == StatusAd.AD_LOADED || status == StatusAd.AD_RENDER_SUCCESS)
            ) {
                status = StatusAd.AD_INIT
            }
        }

    constructor(status: StatusAd) : super(status)

    constructor() : super()

    constructor(maxInterstitialAd: MaxInterstitialAd?) : super() {
        this.maxInterstitialAd = maxInterstitialAd
    }

    constructor(interstitialAd: InterstitialAd?) : super() {
        this.interstitialAd = interstitialAd
    }

    override fun isReady(): Boolean {
        if (maxInterstitialAd != null && maxInterstitialAd!!.isReady) {
            return true
        }
        return interstitialAd != null
    }
}
