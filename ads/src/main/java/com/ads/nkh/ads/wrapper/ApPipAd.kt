package com.ads.nkh.ads.wrapper

import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAd

class ApPipAd : ApAdBase {
    var pictureInPictureAd: PictureInPictureAd? = null
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

    constructor(pictureInPictureAd: PictureInPictureAd?) : super() {
        this.pictureInPictureAd = pictureInPictureAd
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
        status = StatusAd.AD_INIT
    }
}
