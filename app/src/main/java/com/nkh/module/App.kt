package com.nkh.module

import com.ads.nkh.admob.Admob
import com.ads.nkh.admob.AppOpenManager
import com.ads.nkh.ads.NkhAd
import com.ads.nkh.application.AdsMultiDexApplication
import com.ads.nkh.billing.AppPurchase
import com.ads.nkh.config.AdjustConfig
import com.ads.nkh.config.NkhAdConfig

class App : AdsMultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        initAds()
        initBilling()
    }

    private fun initAds() {
        val environment = if (BuildConfig.DEBUG) {
            NkhAdConfig.ENVIRONMENT_DEVELOP
        } else {
            NkhAdConfig.ENVIRONMENT_PRODUCTION
        }
        val config = NkhAdConfig(this, environment).apply {
            adjustConfig = AdjustConfig(true, getString(R.string.adjust_token))
            facebookClientToken = getString(R.string.facebook_client_token)
            adjustTokenTiktok = getString(R.string.tiktok_token)
            setIdAdResumeHighFloor(BuildConfig.ad_appopen_resume, BuildConfig.ad_appopen_resume)
            isShowAdDebugDialog = false
        }
        mNkhAdConfig = config

        NkhAd.getInstance().init(this, config)
        Admob.getInstance().setDisableAdResumeWhenClickAds(true)
        Admob.getInstance().setOpenActivityAfterShowInterAds(false)
        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)
    }

    private fun initBilling() {
        val listIAP = ArrayList<String>().apply {
            add("android.test.purchased")
        }
        val listSub = ArrayList<String>()
        AppPurchase.getInstance().initBilling(this, listIAP, listSub)
    }
}
