package com.ads.nkh.ads.wrapper

import android.view.View
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd

class ApNativeAd : ApAdBase {
    var layoutCustomNative: Int = 0
    var nativeView: View? = null
    var admobNativeAd: NativeAd? = null
        set(value) {
            field = value
            if (value != null) {
                status = StatusAd.AD_LOADED
            }
        }

    constructor(status: StatusAd) : super(status)

    constructor(layoutCustomNative: Int, nativeView: View?) : super() {
        this.layoutCustomNative = layoutCustomNative
        this.nativeView = nativeView
        status = StatusAd.AD_LOADED
    }

    constructor(layoutCustomNative: Int, admobNativeAd: NativeAd?) : super() {
        this.layoutCustomNative = layoutCustomNative
        this.admobNativeAd = admobNativeAd
        status = StatusAd.AD_LOADED
    }

    constructor() : super()

    override fun isReady(): Boolean {
        return nativeView != null || admobNativeAd != null
    }

    override fun toString(): String {
        return "Status:$status == nativeView:$nativeView == admobNativeAd:$admobNativeAd"
    }
}
