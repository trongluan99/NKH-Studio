package com.ads.nkh.ads.wrapper

import com.applovin.mediation.MaxError
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

class ApAdError {
    private var maxError: MaxError? = null
    private var loadAdError: LoadAdError? = null
    private var fullScreenContentError: FullScreenContentError? = null
    private var message: String = ""

    constructor(fullScreenContentError: FullScreenContentError?) {
        this.fullScreenContentError = fullScreenContentError
    }

    constructor(loadAdError: LoadAdError?) {
        this.loadAdError = loadAdError
    }

    constructor(maxError: MaxError?) {
        this.maxError = maxError
    }

    constructor(message: String?) {
        this.message = message ?: ""
    }

    fun setMessage(message: String?) {
        this.message = message ?: ""
    }

    fun getMessage(): String {
        if (maxError != null) {
            return maxError!!.message
        }
        if (loadAdError != null) {
            return loadAdError!!.message
        }
        if (fullScreenContentError != null) {
            return fullScreenContentError!!.message
        }
        if (message.isNotEmpty()) {
            return message
        }
        return "unknown error"
    }
}
