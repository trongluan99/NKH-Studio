package com.ads.nkh.ads.wrapper

import com.google.android.libraries.ads.mobile.sdk.common.ExperimentalApi
import com.google.android.libraries.ads.mobile.sdk.squeezeback.SqueezebackAd

@OptIn(ExperimentalApi::class)
class ApSqueezebackAd : ApAdBase {
    var squeezebackAd: SqueezebackAd? = null
        set(value) {
            field = value
            if (value != null) {
                status = StatusAd.AD_LOADED
            } else if (status == StatusAd.AD_LOADED || status == StatusAd.AD_RENDER_SUCCESS) {
                status = StatusAd.AD_INIT
            }
        }

    constructor(status: StatusAd) : super(status)

    constructor() : super()

    constructor(squeezebackAd: SqueezebackAd?) : super() {
        this.squeezebackAd = squeezebackAd
    }

    override fun isReady(): Boolean {
        return squeezebackAd != null
    }

    fun hide() {
        squeezebackAd?.hide()
    }

    fun destroy() {
        squeezebackAd?.destroy()
        squeezebackAd = null
        status = StatusAd.AD_INIT
    }
}
