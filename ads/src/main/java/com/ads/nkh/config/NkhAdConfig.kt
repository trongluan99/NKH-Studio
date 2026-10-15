package com.ads.nkh.config

import android.app.Application
import java.util.ArrayList

class NkhAdConfig {
    @get:JvmName("_isVariantDev_prop")
    var isVariantDev: Boolean = false
        private set
    var adjustConfig: AdjustConfig = AdjustConfig(false)
    var eventNamePurchase: String = ""
        private set
    var idAdResume: String? = null
        set(value) {
            field = value
            isEnableAdResume = !value.isNullOrEmpty()
        }
    private val _listDeviceTest: MutableList<String> = ArrayList()
    var listDeviceTest: List<String>
        get() = _listDeviceTest
        set(value) {
            _listDeviceTest.clear()
            _listDeviceTest.addAll(value)
        }
    var application: Application
        private set
    @get:JvmName("_isEnableAdResume_prop")
    var isEnableAdResume: Boolean = false
        private set
    var facebookClientToken: String = DEFAULT_TOKEN_FACEBOOK_SDK
    var adjustTokenTiktok: String? = null
        set(value) {
            ADJUST_TOKEN_TIKTOK = value ?: ""
            field = value
        }
    var intervalInterstitialAd: Int = 0
    @get:JvmName("_isShowAdDebugDialog_prop")
    @set:JvmName("_setShowAdDebugDialog_prop")
    var isShowAdDebugDialog: Boolean = false
    var prepareLoadingAdsDialogLayout: Int = -1
    var resumeLoadingDialogLayout: Int = -1

    constructor(application: Application) {
        this.application = application
    }

    constructor(application: Application, environment: String) {
        this.isVariantDev = environment == ENVIRONMENT_DEVELOP
        this.application = application
    }

    @Deprecated("Use setEnvironment instead")
    fun setVariant(isVariantDev: Boolean) {
        this.isVariantDev = isVariantDev
    }

    fun setEnvironment(environment: String) {
        this.isVariantDev = environment == ENVIRONMENT_DEVELOP
    }

    fun addDeviceTest(deviceId: String) {
        _listDeviceTest.add(deviceId)
    }

    fun isVariantDev(): Boolean = isVariantDev

    fun isEnableAdResume(): Boolean = isEnableAdResume

    fun isShowAdDebugDialog(): Boolean = isShowAdDebugDialog

    fun setShowAdDebugDialog(showAdDebugDialog: Boolean) {
        this.isShowAdDebugDialog = showAdDebugDialog
    }

    @get:JvmName("_isEnableAdjust_prop")
    val isEnableAdjust: Boolean
        get() = adjustConfig.isEnableAdjust

    fun isEnableAdjust(): Boolean = isEnableAdjust

    companion object {
        const val ENVIRONMENT_DEVELOP = "develop"
        const val ENVIRONMENT_PRODUCTION = "production"
        const val DEFAULT_TOKEN_FACEBOOK_SDK = "client_token"
        const val ADJUST_TOKEN_TIKTOK_DEFAULT = "client_token_adjust_tiktok"

        @JvmField
        var ADJUST_TOKEN_TIKTOK: String = ADJUST_TOKEN_TIKTOK_DEFAULT
    }
}
