package com.ads.nkh.ads.wrapper

import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAd

class ApPipAd : ApAdBase {
    var pictureInPictureAd: PictureInPictureAd? = null
        set(value) {
            field = value
            if (value != null) {
                status = StatusAd.AD_LOADED
            }
        }

    constructor(status: StatusAd) : super(status)

    constructor() : super()

    constructor(pictureInPictureAd: PictureInPictureAd?) : super() {
        this.pictureInPictureAd = pictureInPictureAd
        if (pictureInPictureAd != null) {
            status = StatusAd.AD_LOADED
        }
    }

    override fun isReady(): Boolean {
        return pictureInPictureAd != null
    }

    fun hide() {
        pictureInPictureAd?.hide()
    }

    fun destroy() {
        pictureInPictureAd?.destroy()
        pictureInPictureAd = null
    }
}
