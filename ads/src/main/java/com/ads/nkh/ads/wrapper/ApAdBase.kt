package com.ads.nkh.ads.wrapper

abstract class ApAdBase {
    @get:JvmName("_getStatus_prop")
    @set:JvmName("_setStatus_prop")
    var status: StatusAd = StatusAd.AD_INIT

    constructor(status: StatusAd) {
        this.status = status
    }

    constructor()

    fun getStatus(): StatusAd {
        return status
    }

    fun setStatus(status: StatusAd) {
        this.status = status
    }

    val isReady: Boolean
        @JvmName("_isReady_prop") get() = isReady()

    val isNotReady: Boolean
        @JvmName("_isNotReady_prop") get() = isNotReady()

    val isLoading: Boolean
        @JvmName("_isLoading_prop") get() = isLoading()

    val isLoadFail: Boolean
        @JvmName("_isLoadFail_prop") get() = isLoadFail()

    val isRenderSuccess: Boolean
        @JvmName("_isRenderSuccess_prop") get() = isRenderSuccess()

    abstract fun isReady(): Boolean

    fun isNotReady(): Boolean {
        return !isReady()
    }

    fun isLoading(): Boolean {
        return status == StatusAd.AD_LOADING
    }

    fun isLoadFail(): Boolean {
        return status == StatusAd.AD_LOAD_FAIL
    }

    fun isRenderSuccess(): Boolean {
        return status == StatusAd.AD_RENDER_SUCCESS
    }
}
