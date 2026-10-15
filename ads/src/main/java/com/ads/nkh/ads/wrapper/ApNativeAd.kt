package com.ads.nkh.ads.wrapper

import android.view.View
import com.ads.nkh.ads.native_ads.NativeAdConfig
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd

class ApNativeAd : ApAdBase {
    var layoutCustomNative: Int = 0
    var nativeAdConfig: NativeAdConfig? = null

    var nativeView: View? = null
        set(value) {
            field = value
            if (value != null) {
                status = StatusAd.AD_LOADED
            } else if (admobNativeAd == null &&
                (status == StatusAd.AD_LOADED || status == StatusAd.AD_RENDER_SUCCESS)
            ) {
                status = StatusAd.AD_INIT
            }
        }

    var admobNativeAd: NativeAd? = null
        set(value) {
            field = value
            if (value != null) {
                status = StatusAd.AD_LOADED
            } else if (nativeView == null &&
                (status == StatusAd.AD_LOADED || status == StatusAd.AD_RENDER_SUCCESS)
            ) {
                status = StatusAd.AD_INIT
            }
        }

    constructor(status: StatusAd) : super(status)

    constructor(layoutCustomNative: Int, nativeView: View?) : super() {
        this.layoutCustomNative = layoutCustomNative
        this.nativeView = nativeView
    }

    constructor(layoutCustomNative: Int, admobNativeAd: NativeAd?) : super() {
        this.layoutCustomNative = layoutCustomNative
        this.admobNativeAd = admobNativeAd
    }

    constructor(layoutCustomNative: Int, admobNativeAd: NativeAd?, nativeAdConfig: NativeAdConfig?) : super() {
        this.layoutCustomNative = layoutCustomNative
        this.admobNativeAd = admobNativeAd
        this.nativeAdConfig = nativeAdConfig
    }

    constructor() : super()

    override fun isReady(): Boolean {
        return nativeView != null || admobNativeAd != null
    }

    fun markAsImpressed() {
        if (isReady()) {
            status = StatusAd.AD_IMPRESSED
        }
    }

    fun isImpressed(): Boolean {
        return status == StatusAd.AD_IMPRESSED
    }

    fun isReadyToImpress(): Boolean {
        return isReady() && status == StatusAd.AD_LOADED
    }

    override fun toString(): String {
        return "Status:$status == nativeView:$nativeView == admobNativeAd:$admobNativeAd == config:$nativeAdConfig"
    }
}
