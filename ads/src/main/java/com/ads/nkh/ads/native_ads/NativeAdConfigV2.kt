package com.ads.nkh.ads.native_ads

/**
 * Configuration class for customizing Native Ad appearance
 */
class NativeAdConfigV2 {
    var colorCTA: String? = "default"
    var heightCTA: Int = 40

    constructor(colorCTA: String?, heightCTA: Int) {
        this.colorCTA = colorCTA
        this.heightCTA = heightCTA
    }

    constructor()
}
