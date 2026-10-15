package com.ads.nkh.config

class AdjustConfig {
    var isEnableAdjust: Boolean = false
    var adjustToken: String = ""
    var eventNamePurchase: String = ""
    var eventAdImpression: String = ""

    constructor(enableAdjust: Boolean) {
        this.isEnableAdjust = enableAdjust
    }

    constructor(enableAdjust: Boolean, adjustToken: String) {
        this.isEnableAdjust = enableAdjust
        this.adjustToken = adjustToken
    }
}
