package com.ads.nkh.ads.wrapper

import android.app.Activity
import android.view.ViewGroup
import com.google.android.libraries.ads.mobile.sdk.swipeableinterstitial.SwipeableInterstitialAd

class ApSwipeableAd : ApAdBase {
    var swipeableInterstitialAd: SwipeableInterstitialAd? = null
        set(value) {
            field = value
            if (value != null) {
                status = StatusAd.AD_LOADED
            }
        }

    constructor(status: StatusAd) : super(status)

    constructor() : super()

    constructor(swipeableInterstitialAd: SwipeableInterstitialAd?) : super() {
        this.swipeableInterstitialAd = swipeableInterstitialAd
        if (swipeableInterstitialAd != null) {
            status = StatusAd.AD_LOADED
        }
    }

    override fun isReady(): Boolean {
        return swipeableInterstitialAd != null
    }

    fun destroy(activity: Activity? = null) {
        swipeableInterstitialAd?.let { ad ->
            if (activity != null) {
                val adView = ad.getView(activity)
                (adView.parent as? ViewGroup)?.removeView(adView)
            }
            ad.destroy()
        }
        swipeableInterstitialAd = null
    }
}
