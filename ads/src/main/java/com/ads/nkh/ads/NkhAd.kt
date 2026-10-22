package com.ads.nkh.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.LogLevel
import com.ads.nkh.R
import com.ads.nkh.admob.Admob
import com.ads.nkh.admob.AppOpenManager
import com.ads.nkh.ads.native_ads.NativeAdConfig
import com.ads.nkh.ads.wrapper.ApInterstitialAd
import com.ads.nkh.ads.wrapper.ApInterstitialPriority2Ad
import com.ads.nkh.ads.wrapper.ApInterstitialPriorityAd
import com.ads.nkh.ads.wrapper.ApNativeAd
import com.ads.nkh.ads.wrapper.ApPipAd
import com.ads.nkh.ads.wrapper.ApSwipeableAd
import com.ads.nkh.ads.wrapper.StatusAd
import com.ads.nkh.config.NkhAdConfig
import com.ads.nkh.debug.NkhAdDebugManager
import com.ads.nkh.dialog.FullScreenNativeAdDialog
import com.ads.nkh.dialog.SwipeableInterstitialFeedDialog
import com.ads.nkh.event.NkhAdjust
import com.ads.nkh.funtion.AdCallback
import com.ads.nkh.funtion.AdType
import com.ads.nkh.funtion.RewardCallback
import com.ads.nkh.util.AppUtil
import com.ads.nkh.util.SharePreferenceUtils
import com.facebook.FacebookSdk
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAd
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAdPosition
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAdPresentationScope
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.swipeableinterstitial.SwipeableInterstitialAd
import org.json.JSONException
import java.io.BufferedReader
import java.io.InputStreamReader

class NkhAd {
    var adConfig: NkhAdConfig? = null
        private set
    private var initCallback: NkhInitCallback? = null
    private var initAdSuccess: Boolean = false

    var isForceOrganic: Boolean = false

    val organic: Boolean
        get() = SharePreferenceUtils.getIsOrganic(adConfig?.application)

    val shouldDisplayNativeOnboardingNormal1: Boolean
        get() = getShouldDisplayNativeOnboardingNormal1(isForceOrganic)

    val shouldDisplayNativeOnboardingFull1: Boolean
        get() = getShouldDisplayNativeOnboardingFull1(isForceOrganic)

    val shouldDisplayNativeOnboardingFull2: Boolean
        get() = getShouldDisplayNativeOnboardingFull2(isForceOrganic)

    val shouldDisplayNativeOnboardingNormal2: Boolean
        get() = getShouldDisplayNativeOnboardingNormal2(isForceOrganic)

    val shouldDisplayNativeHome: Boolean
        get() = getShouldDisplayNativeHome(isForceOrganic)

    val shouldDisplayNativePermission: Boolean
        get() = getShouldDisplayNativePermission(isForceOrganic)

    val shouldDisplayInterOnboarding: Boolean
        get() = getShouldDisplayInterOnboarding(isForceOrganic)

    val shouldDisplayNativeWelcomeBack: Boolean
        get() = getShouldDisplayNativeWelcomeBack(isForceOrganic)

    val shouldDisplayInterWelcomeBack: Boolean
        get() = getShouldDisplayInterWelcomeBack(isForceOrganic)

    val shouldDisplayWidgetUninstall: Boolean
        get() = getShouldDisplayWidgetUninstall(isForceOrganic)

    val shouldDisplayHighCTA: Boolean
        get() = getShouldDisplayHighCTA(isForceOrganic)

    fun getShouldDisplayNativeOnboardingNormal1(isForceOrganic: Boolean): Boolean =
        if (isForceOrganic) !organic else true

    fun getShouldDisplayNativeOnboardingFull1(isForceOrganic: Boolean): Boolean =
        if (isForceOrganic) !organic else true

    fun getShouldDisplayNativeOnboardingFull2(isForceOrganic: Boolean): Boolean =
        if (isForceOrganic) !organic else true

    fun getShouldDisplayNativeOnboardingNormal2(isForceOrganic: Boolean): Boolean =
        if (isForceOrganic) !organic else true

    fun getShouldDisplayNativeHome(isForceOrganic: Boolean): Boolean =
        if (isForceOrganic) !organic else true

    fun getShouldDisplayNativePermission(isForceOrganic: Boolean): Boolean =
        if (isForceOrganic) !organic else true

    fun getShouldDisplayInterOnboarding(isForceOrganic: Boolean): Boolean =
        if (isForceOrganic) !organic else true

    fun getShouldDisplayNativeWelcomeBack(isForceOrganic: Boolean): Boolean =
        if (isForceOrganic) !organic else true

    fun getShouldDisplayInterWelcomeBack(isForceOrganic: Boolean): Boolean =
        if (isForceOrganic) !organic else true

    fun getShouldDisplayWidgetUninstall(isForceOrganic: Boolean): Boolean =
        if (isForceOrganic) !organic else true

    fun getShouldDisplayHighCTA(isForceOrganic: Boolean): Boolean =
        if (isForceOrganic) !organic else true

    fun setCountClickToShowAds(countClickToShowAds: Int) {
        Admob.getInstance().setNumToShowAds(countClickToShowAds)
    }

    fun setCountClickToShowAds(countClickToShowAds: Int, currentClicked: Int) {
        Admob.getInstance().setNumToShowAds(countClickToShowAds, currentClicked)
    }

    var resumeLoadingDialogLayout: Int
        get() = adConfig?.resumeLoadingDialogLayout ?: -1
        set(value) {
            adConfig?.resumeLoadingDialogLayout = value
        }

    var prepareLoadingAdsDialogLayout: Int
        get() = adConfig?.prepareLoadingAdsDialogLayout ?: -1
        set(value) {
            adConfig?.prepareLoadingAdsDialogLayout = value
        }

    fun init(context: Application, adConfig: NkhAdConfig?) {
        if (adConfig == null) {
            throw RuntimeException("Cant not set GamAdConfig null")
        }
        this.adConfig = adConfig
        AppUtil.VARIANT_DEV = adConfig.isVariantDev
        if (adConfig.isEnableDebugMode) {
            NkhAdDebugManager.registerLifecycleIfNeeded(context)
        }
        if (adConfig.isEnableAdjust) {
            NkhAdjust.enableAdjust = true
            setupAdjust(adConfig.isVariantDev, adConfig.adjustConfig.adjustToken)
        }

        val appContext = context.applicationContext ?: context
        val fbToken = adConfig.facebookClientToken?.trim()
        val hasValidFbToken = !fbToken.isNullOrEmpty() &&
            fbToken != NkhAdConfig.DEFAULT_TOKEN_FACEBOOK_SDK &&
            !fbToken.startsWith("XXXX", ignoreCase = true)
        if (hasValidFbToken) {
            Admob.getInstance().runOnBackgroundThread(Runnable {
                try {
                    FacebookSdk.setClientToken(fbToken!!)
                    @Suppress("DEPRECATION")
                    FacebookSdk.sdkInitialize(appContext)
                } catch (e: Exception) {
                    Log.w(TAG, "FacebookSdk background init skipped: ${e.message}")
                }
            })
        } else {
            Admob.getInstance().runOnBackgroundThread(Runnable {
                try {
                    FacebookSdk.setAutoInitEnabled(false)
                    FacebookSdk.setAutoLogAppEventsEnabled(false)
                    FacebookSdk.setAdvertiserIDCollectionEnabled(false)
                } catch (_: Throwable) {
                }
            })
        }

        Admob.getInstance().init(context, adConfig.listDeviceTest, adConfig.adjustTokenTiktok)
        AppOpenManager.getInstance().registerApplication(adConfig.application)
        if (adConfig.isEnableAdResume) {
            AppOpenManager.getInstance().init(
                adConfig.application,
                adConfig.idAdResumeHigh,
                adConfig.idAdResume
            )
        }

        initAdSuccess = true
        initCallback?.initAdSuccess()
    }

    fun openDebugInspector(activity: Activity) {
        NkhAdDebugManager.showDebugInspectorDialog(activity)
    }

    fun setEnableDebugMode(enable: Boolean, showFloatingButton: Boolean = true) {
        adConfig?.let { cfg ->
            cfg.isEnableDebugMode = enable
            cfg.isShowFloatingDebugButton = showFloatingButton
            if (enable) {
                NkhAdDebugManager.registerLifecycleIfNeeded(cfg.application)
            }
        }
    }

    fun loadNativeConfigFromAssets(context: Context, remoteFile: String): NativeAdConfig {
        return try {
            val isStream = context.assets.open(remoteFile)
            val br = BufferedReader(InputStreamReader(isStream))
            val sb = StringBuilder()
            var line: String?
            while (br.readLine().also { line = it } != null) {
                sb.append(line)
            }
            br.close()
            isStream.close()
            NativeAdConfig.fromJson(context, sb.toString())
        } catch (e: Exception) {
            NativeAdConfig()
        }
    }

    fun loadNativeConfigFromString(context: Context, remoteFile: String): NativeAdConfig {
        return try {
            NativeAdConfig.fromJson(context, remoteFile)
        } catch (e: JSONException) {
            NativeAdConfig()
        }
    }

    fun enableAdResume(application: Application, id: String) {
        AppOpenManager.getInstance().init(application, id)
    }

    fun enableAdResumeHighFloor(application: Application, idHigh: String?, idNormal: String?) {
        AppOpenManager.getInstance().init(application, idHigh, idNormal)
    }

    fun enableAdResume(application: Application, idHigh: String?, idNormal: String?) {
        AppOpenManager.getInstance().init(application, idHigh, idNormal)
    }

    fun setAppResumeHighFloor(idHigh: String?, idNormal: String?) {
        adConfig?.application?.let { AppOpenManager.getInstance().registerApplication(it) }
        AppOpenManager.getInstance().setAppResumeHighFloor(idHigh, idNormal)
    }

    fun setAppResumeHighFloor(listIdResume: List<String>) {
        adConfig?.application?.let { AppOpenManager.getInstance().registerApplication(it) }
        AppOpenManager.getInstance().setAppResumeHighFloor(listIdResume)
    }

    fun setInitCallback(initCallback: NkhInitCallback?) {
        this.initCallback = initCallback
        if (initAdSuccess) {
            initCallback?.initAdSuccess()
        }
    }

    private fun setupAdjust(buildDebug: Boolean, adjustToken: String?) {
        val app = adConfig?.application ?: return
        val environment = if (buildDebug) {
            AdjustConfig.ENVIRONMENT_SANDBOX
        } else {
            AdjustConfig.ENVIRONMENT_PRODUCTION
        }
        val config = AdjustConfig(app, adjustToken, environment)

        // Change the log level.
        config.setLogLevel(LogLevel.VERBOSE)
        config.enablePreinstallTracking()
        config.enableSendingInBackground()
        config.setOnAttributionChangedListener { adjustAttribution ->
            val isOrganic = "Organic" == adjustAttribution.trackerName ||
                (adjustAttribution.network != null && adjustAttribution.network.equals("organic", ignoreCase = true))
            SharePreferenceUtils.setIsOrganic(app, isOrganic)
        }
        Adjust.initSdk(config)
        app.registerActivityLifecycleCallbacks(AdjustLifecycleCallbacks())
    }

    private class AdjustLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            Adjust.onResume()
        }

        override fun onActivityPaused(activity: Activity) {
            Adjust.onPause()
        }

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {}

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityStarted(activity: Activity) {}
    }

    @JvmOverloads
    fun loadBanner(mActivity: Activity, id: String, adCallback: AdCallback? = null) {
        Admob.getInstance().loadBanner(mActivity, id, adCallback)
    }

    @JvmOverloads
    fun loadInlineBanner(
        activity: Activity,
        id: String,
        inlineStyle: String = Admob.BANNER_INLINE_LARGE_STYLE,
        adCallback: AdCallback? = null
    ) {
        Admob.getInstance().loadInlineBanner(activity, id, inlineStyle, adCallback)
    }

    @JvmOverloads
    fun loadCollapsibleBanner(
        activity: Activity,
        id: String,
        gravity: String,
        adCallback: AdCallback? = null
    ) {
        Admob.getInstance().loadCollapsibleBanner(activity, id, gravity, adCallback)
    }

    @JvmOverloads
    fun loadCollapsibleBannerFragment(
        activity: Activity,
        id: String,
        rootView: View,
        gravity: String,
        adCallback: AdCallback? = null
    ) {
        Admob.getInstance().loadCollapsibleBannerFragment(activity, id, rootView, gravity, adCallback)
    }

    @JvmOverloads
    fun loadBannerFragment(
        mActivity: Activity,
        id: String,
        rootView: View,
        adCallback: AdCallback? = null
    ) {
        Admob.getInstance().loadBannerFragment(mActivity, id, rootView, adCallback)
    }

    fun loadSplashInterstitialAds(
        context: Context,
        id: String,
        timeOut: Long,
        timeDelay: Long,
        adListener: AdCallback
    ) {
        Admob.getInstance().loadSplashInterstitialAds(context, id, timeOut, timeDelay, true, adListener)
    }

    /**
     * Load Interstitial Splash với HighFloor tuần tự (Sequential):
     * Request [idAdsHigh] trước -> nếu fail mới request [idAdsNormal].
     * (Không bắn đồng thời như SameTime).
     */
    @JvmOverloads
    fun loadSplashInterstitialAdsHighFloor(
        context: Context,
        idAdsHigh: String,
        idAdsNormal: String,
        timeOut: Long,
        timeDelay: Long,
        showSplashIfReady: Boolean = true,
        adListener: AdCallback
    ) {
        Admob.getInstance().loadSplashInterstitialAdsHighFloor(
            context,
            idAdsHigh,
            idAdsNormal,
            timeOut,
            timeDelay,
            showSplashIfReady,
            adListener
        )
    }

    @JvmOverloads
    fun loadSplashInterstitialAdsHighFloor(
        context: Context,
        idAdsHigh1: String,
        idAdsHigh2: String,
        idAdsNormal: String,
        timeOut: Long,
        timeDelay: Long,
        showSplashIfReady: Boolean = true,
        adListener: AdCallback
    ) {
        Admob.getInstance().loadSplashInterstitialAdsHighFloor(
            context,
            idAdsHigh1,
            idAdsHigh2,
            idAdsNormal,
            timeOut,
            timeDelay,
            showSplashIfReady,
            adListener
        )
    }

    @JvmOverloads
    fun loadSplashInterstitialAdsHighFloor(
        context: Context,
        listIdAds: List<String>,
        timeOut: Long,
        timeDelay: Long,
        showSplashIfReady: Boolean = true,
        adListener: AdCallback
    ) {
        Admob.getInstance().loadSplashInterstitialAdsHighFloor(
            context,
            listIdAds,
            timeOut,
            timeDelay,
            showSplashIfReady,
            adListener
        )
    }

    @JvmOverloads
    fun loadSplashHighFloor(
        context: Context,
        idAdsHigh: String,
        idAdsNormal: String,
        timeOut: Long,
        timeDelay: Long,
        showSplashIfReady: Boolean = true,
        adListener: AdCallback
    ) {
        loadSplashInterstitialAdsHighFloor(
            context,
            idAdsHigh,
            idAdsNormal,
            timeOut,
            timeDelay,
            showSplashIfReady,
            adListener
        )
    }

    /**
     * Load App Open Splash với HighFloor tuần tự (Sequential):
     * Request [idResumeSplashHigh] trước -> nếu fail mới request [idResumeSplashNormal].
     */
    @JvmOverloads
    fun loadOpenAppAdSplashHighFloor(
        context: Context,
        idResumeSplashHigh: String,
        idResumeSplashNormal: String,
        timeDelay: Long,
        timeOut: Long,
        isShowAdIfReady: Boolean = true,
        adCallback: AdCallback
    ) {
        AppOpenManager.getInstance().loadOpenAppAdSplashHighFloor(
            context,
            idResumeSplashHigh,
            idResumeSplashNormal,
            timeDelay,
            timeOut,
            isShowAdIfReady,
            adCallback
        )
    }

    @JvmOverloads
    fun loadOpenAppAdSplashHighFloor(
        context: Context,
        listIdResumeSplash: List<String>,
        timeDelay: Long,
        timeOut: Long,
        isShowAdIfReady: Boolean = true,
        adCallback: AdCallback
    ) {
        AppOpenManager.getInstance().loadOpenAppAdSplashHighFloor(
            context,
            listIdResumeSplash,
            timeDelay,
            timeOut,
            isShowAdIfReady,
            adCallback
        )
    }

    fun onCheckShowSplashWhenFail(activity: AppCompatActivity, callback: AdCallback, timeDelay: Int) {
        Admob.getInstance().onCheckShowSplashWhenFail(activity, callback, timeDelay)
    }

    fun getInterstitialAds(context: Context, id: String, adListener: AdCallback): ApInterstitialAd {
        val apInterstitialAd = ApInterstitialAd(StatusAd.AD_LOADING)
        Admob.getInstance().getInterstitialAds(context, id, object : AdCallback() {
            override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                super.onInterstitialLoad(interstitialAd)
                apInterstitialAd.interstitialAd = interstitialAd
                if (interstitialAd == null) {
                    apInterstitialAd.setStatus(StatusAd.AD_LOAD_FAIL)
                }
                adListener.onApInterstitialLoad(apInterstitialAd)
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                Log.d(TAG, "Admob onAdFailedToLoad")
                apInterstitialAd.setStatus(StatusAd.AD_LOAD_FAIL)
                adListener.onAdFailedToLoad(i)
            }

            override fun onAdFailedToShow(adError: FullScreenContentError?) {
                super.onAdFailedToShow(adError)
                Log.d(TAG, "Admob onAdFailedToShow")
                apInterstitialAd.setStatus(StatusAd.AD_LOAD_FAIL)
                adListener.onAdFailedToShow(adError)
            }

            override fun onAdLogRev(
                adValue: AdValue?,
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                adListener.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
            }
        })
        return apInterstitialAd
    }

    fun forceShowInterstitial(
        context: Context,
        mInterstitialAd: ApInterstitialAd?,
        callback: AdCallback,
        shouldReloadAds: Boolean
    ) {
        val intervalMs = (getInstance().adConfig?.intervalInterstitialAd ?: 0) * 1000L
        if (System.currentTimeMillis() - SharePreferenceUtils.getLastImpressionInterstitialTime(context) < intervalMs) {
            callback.onNextAction()
            return
        }
        forceShowInterstitialNotDelay(context, mInterstitialAd, callback, shouldReloadAds)
    }

    fun forceShowInterstitialNotDelay(
        context: Context,
        mInterstitialAd: ApInterstitialAd?,
        callback: AdCallback,
        shouldReloadAds: Boolean
    ) {
        if (mInterstitialAd == null || mInterstitialAd.isNotReady()) {
            callback.onNextAction()
            return
        }
        val currentAd = mInterstitialAd.interstitialAd
        val adUnitId = currentAd?.adUnitId
        val adCallback = object : AdCallback() {
            override fun onAdClosed() {
                super.onAdClosed()
                callback.onAdClosed()
                if (shouldReloadAds && adUnitId != null) {
                    mInterstitialAd.interstitialAd = null
                    mInterstitialAd.setStatus(StatusAd.AD_LOADING)
                    Admob.getInstance().getInterstitialAds(context, adUnitId, object : AdCallback() {
                        override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                            super.onInterstitialLoad(interstitialAd)
                            mInterstitialAd.interstitialAd = interstitialAd
                            if (interstitialAd == null) {
                                mInterstitialAd.setStatus(StatusAd.AD_LOAD_FAIL)
                            }
                            callback.onInterstitialLoad(mInterstitialAd.interstitialAd)
                        }

                        override fun onAdFailedToLoad(i: LoadAdError?) {
                            super.onAdFailedToLoad(i)
                            mInterstitialAd.interstitialAd = null
                            mInterstitialAd.setStatus(StatusAd.AD_LOAD_FAIL)
                            callback.onAdFailedToLoad(i)
                        }

                        override fun onAdFailedToShow(adError: FullScreenContentError?) {
                            super.onAdFailedToShow(adError)
                            mInterstitialAd.setStatus(StatusAd.AD_LOAD_FAIL)
                            callback.onAdFailedToShow(adError)
                        }
                    })
                } else {
                    mInterstitialAd.interstitialAd = null
                    mInterstitialAd.setStatus(StatusAd.AD_INIT)
                }
            }

            override fun onNextAction() {
                super.onNextAction()
                callback.onNextAction()
            }

            override fun onAdFailedToShow(adError: FullScreenContentError?) {
                super.onAdFailedToShow(adError)
                callback.onAdFailedToShow(adError)
                if (shouldReloadAds && adUnitId != null) {
                    mInterstitialAd.interstitialAd = null
                    mInterstitialAd.setStatus(StatusAd.AD_LOADING)
                    Admob.getInstance().getInterstitialAds(context, adUnitId, object : AdCallback() {
                        override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                            super.onInterstitialLoad(interstitialAd)
                            mInterstitialAd.interstitialAd = interstitialAd
                            if (interstitialAd == null) {
                                mInterstitialAd.setStatus(StatusAd.AD_LOAD_FAIL)
                            }
                            callback.onInterstitialLoad(mInterstitialAd.interstitialAd)
                        }

                        override fun onAdFailedToLoad(i: LoadAdError?) {
                            super.onAdFailedToLoad(i)
                            mInterstitialAd.interstitialAd = null
                            mInterstitialAd.setStatus(StatusAd.AD_LOAD_FAIL)
                            callback.onAdFailedToLoad(i)
                        }

                        override fun onAdFailedToShow(adError: FullScreenContentError?) {
                            super.onAdFailedToShow(adError)
                            mInterstitialAd.setStatus(StatusAd.AD_LOAD_FAIL)
                            callback.onAdFailedToShow(adError)
                        }

                        override fun onAdLogRev(
                            adValue: AdValue?,
                            adUnitId: String?,
                            mediationAdapterClassName: String?,
                            adType: AdType?
                        ) {
                            super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                            callback.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                        }
                    })
                } else {
                    mInterstitialAd.interstitialAd = null
                    mInterstitialAd.setStatus(StatusAd.AD_LOAD_FAIL)
                }
            }

            override fun onAdClicked() {
                super.onAdClicked()
                callback.onAdClicked()
            }

            override fun onAdClicked(
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                callback.onAdClicked(adUnitId, mediationAdapterClassName, adType)
            }

            override fun onAdLogRev(
                adValue: AdValue?,
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                callback.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
            }

            override fun onInterstitialShow() {
                super.onInterstitialShow()
                mInterstitialAd.setStatus(StatusAd.AD_RENDER_SUCCESS)
                callback.onInterstitialShow()
            }

            override fun onAdImpression() {
                super.onAdImpression()
                mInterstitialAd.setStatus(StatusAd.AD_RENDER_SUCCESS)
                callback.onAdImpression()
            }
        }
        Admob.getInstance().forceShowInterstitial(context, currentAd, adCallback)
    }

    fun loadNativeAdResultCallback(
        activity: Activity,
        id: String,
        layoutCustomNative: Int,
        callback: AdCallback
    ) {
        Admob.getInstance().loadNativeAd(activity as Context, id, object : AdCallback() {
            override fun onUnifiedNativeAdLoaded(unifiedNativeAd: NativeAd) {
                super.onUnifiedNativeAdLoaded(unifiedNativeAd)
                callback.onNativeAdLoaded(ApNativeAd(layoutCustomNative, unifiedNativeAd))
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                callback.onAdFailedToLoad(i)
            }

            override fun onAdFailedToShow(adError: FullScreenContentError?) {
                super.onAdFailedToShow(adError)
                callback.onAdFailedToShow(adError)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                callback.onAdClicked()
            }

            override fun onAdLogRev(
                adValue: AdValue?,
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                callback.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
            }

            override fun onAdClicked(
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                callback.onAdClicked(adUnitId, mediationAdapterClassName, adType)
            }
        })
    }

    fun loadNativeAd(
        activity: Activity,
        id: String,
        layoutCustomNative: Int,
        adPlaceHolder: FrameLayout,
        containerShimmerLoading: ShimmerFrameLayout?,
        callback: AdCallback
    ) {
        Admob.getInstance().loadNativeAd(activity as Context, id, object : AdCallback() {
            override fun onUnifiedNativeAdLoaded(unifiedNativeAd: NativeAd) {
                super.onUnifiedNativeAdLoaded(unifiedNativeAd)
                val apNativeAd = ApNativeAd(layoutCustomNative, unifiedNativeAd)
                callback.onNativeAdLoaded(apNativeAd)
                populateNativeAdView(
                    activity,
                    apNativeAd,
                    adPlaceHolder,
                    containerShimmerLoading
                )
            }

            override fun onAdImpression() {
                super.onAdImpression()
                callback.onAdImpression()
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                callback.onAdFailedToLoad(i)
            }

            override fun onAdFailedToShow(adError: FullScreenContentError?) {
                super.onAdFailedToShow(adError)
                callback.onAdFailedToShow(adError)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                callback.onAdClicked()
            }

            override fun onAdLogRev(
                adValue: AdValue?,
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                callback.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
            }

            override fun onAdClicked(
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                callback.onAdClicked(adUnitId, mediationAdapterClassName, adType)
            }
        })
    }

    fun loadNativeAd(
        activity: Activity,
        id: String,
        layoutCustomNative: Int,
        adPlaceHolder: FrameLayout,
        containerShimmerLoading: ShimmerFrameLayout?,
        isShowClose: Boolean,
        callback: AdCallback
    ) {
        Admob.getInstance().loadNativeAd(activity as Context, id, object : AdCallback() {
            override fun onUnifiedNativeAdLoaded(unifiedNativeAd: NativeAd) {
                super.onUnifiedNativeAdLoaded(unifiedNativeAd)
                val apNativeAd = ApNativeAd(layoutCustomNative, unifiedNativeAd)
                callback.onNativeAdLoaded(apNativeAd)
                populateNativeAdView(
                    activity,
                    apNativeAd,
                    adPlaceHolder,
                    containerShimmerLoading,
                    isShowClose
                )
            }

            override fun onAdImpression() {
                super.onAdImpression()
                callback.onAdImpression()
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                callback.onAdFailedToLoad(i)
            }

            override fun onAdFailedToShow(adError: FullScreenContentError?) {
                super.onAdFailedToShow(adError)
                callback.onAdFailedToShow(adError)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                callback.onAdClicked()
            }

            override fun onAdLogRev(
                adValue: AdValue?,
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                callback.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
            }

            override fun onAdClicked(
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                callback.onAdClicked(adUnitId, mediationAdapterClassName, adType)
            }
        })
    }

    fun loadNativeAd(
        activity: Activity,
        id: String,
        layoutCustomNative: Int,
        adPlaceHolder: FrameLayout,
        containerShimmerLoading: ShimmerFrameLayout?,
        nativeAdConfig: NativeAdConfig,
        callback: AdCallback
    ) {
        Admob.getInstance().loadNativeAd(activity as Context, id, object : AdCallback() {
            override fun onUnifiedNativeAdLoaded(unifiedNativeAd: NativeAd) {
                super.onUnifiedNativeAdLoaded(unifiedNativeAd)
                val apNativeAd = ApNativeAd(layoutCustomNative, unifiedNativeAd, nativeAdConfig)
                callback.onNativeAdLoaded(apNativeAd)
                populateNativeAdView(
                    activity,
                    apNativeAd,
                    adPlaceHolder,
                    containerShimmerLoading,
                    nativeAdConfig
                )
            }

            override fun onAdImpression() {
                super.onAdImpression()
                callback.onAdImpression()
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                callback.onAdFailedToLoad(i)
            }

            override fun onAdFailedToShow(adError: FullScreenContentError?) {
                super.onAdFailedToShow(adError)
                callback.onAdFailedToShow(adError)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                callback.onAdClicked()
            }

            override fun onAdLogRev(
                adValue: AdValue?,
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                callback.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
            }

            override fun onAdClicked(
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                callback.onAdClicked(adUnitId, mediationAdapterClassName, adType)
            }
        })
    }

    fun loadNativeAd(
        activity: Activity,
        id: String,
        layoutCustomNative: Int,
        adPlaceHolder: FrameLayout,
        containerShimmerLoading: ShimmerFrameLayout?,
        colorCTA: String,
        heightCTA: Int,
        radiusCTA: Int,
        callback: AdCallback
    ) {
        Admob.getInstance().loadNativeAd(activity as Context, id, object : AdCallback() {
            override fun onUnifiedNativeAdLoaded(unifiedNativeAd: NativeAd) {
                super.onUnifiedNativeAdLoaded(unifiedNativeAd)
                val apNativeAd = ApNativeAd(layoutCustomNative, unifiedNativeAd)
                callback.onNativeAdLoaded(apNativeAd)
                populateNativeAdView(
                    activity,
                    apNativeAd,
                    adPlaceHolder,
                    containerShimmerLoading,
                    colorCTA,
                    heightCTA,
                    radiusCTA
                )
            }

            override fun onAdImpression() {
                super.onAdImpression()
                callback.onAdImpression()
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                callback.onAdFailedToLoad(i)
            }

            override fun onAdFailedToShow(adError: FullScreenContentError?) {
                super.onAdFailedToShow(adError)
                callback.onAdFailedToShow(adError)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                callback.onAdClicked()
            }

            override fun onAdLogRev(
                adValue: AdValue?,
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                callback.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
            }

            override fun onAdClicked(
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                callback.onAdClicked(adUnitId, mediationAdapterClassName, adType)
            }
        })
    }

    private fun prepareNativeAdView(
        activity: Activity,
        apNativeAd: ApNativeAd,
        containerShimmerLoading: ShimmerFrameLayout?
    ): NativeAdView? {
        if (apNativeAd.admobNativeAd == null && apNativeAd.nativeView == null) {
            containerShimmerLoading?.visibility = View.GONE
            return null
        }
        @SuppressLint("InflateParams")
        val adView = LayoutInflater.from(activity).inflate(apNativeAd.layoutCustomNative, null) as NativeAdView
        if (containerShimmerLoading != null) {
            try {
                containerShimmerLoading.stopShimmer()
                containerShimmerLoading.visibility = View.GONE
            } catch (e: Exception) {
                Log.w(TAG, "Shimmer stop error: ${e.message}")
            }
        }
        return adView
    }

    fun populateNativeAdView(
        activity: Activity,
        apNativeAd: ApNativeAd,
        adPlaceHolder: FrameLayout,
        containerShimmerLoading: ShimmerFrameLayout?
    ) {
        val adView = prepareNativeAdView(activity, apNativeAd, containerShimmerLoading) ?: return
        adPlaceHolder.visibility = View.VISIBLE
        Admob.getInstance().populateUnifiedNativeAdView(apNativeAd.admobNativeAd, adView)
        adPlaceHolder.removeAllViews()
        adPlaceHolder.addView(adView)
        apNativeAd.setStatus(StatusAd.AD_RENDER_SUCCESS)
    }

    fun populateNativeAdView(
        activity: Activity,
        apNativeAd: ApNativeAd,
        adPlaceHolder: FrameLayout,
        containerShimmerLoading: ShimmerFrameLayout?,
        isShowClose: Boolean
    ) {
        val adView = prepareNativeAdView(activity, apNativeAd, containerShimmerLoading) ?: return
        adPlaceHolder.visibility = View.VISIBLE
        Admob.getInstance().populateUnifiedNativeAdView(apNativeAd.admobNativeAd, adView)
        adPlaceHolder.removeAllViews()
        adPlaceHolder.addView(adView)
        apNativeAd.setStatus(StatusAd.AD_RENDER_SUCCESS)

        if (isShowClose) {
            val marginInDp = 10
            val marginInPx = (marginInDp * activity.resources.displayMetrics.density).toInt()
            val img = ImageView(activity)
            img.setImageResource(R.drawable.ic_close_ads)
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            params.gravity = Gravity.TOP or Gravity.END
            params.topMargin = marginInPx
            params.rightMargin = marginInPx

            img.setOnClickListener {
                adPlaceHolder.removeAllViews()
            }

            adPlaceHolder.addView(img, params)
        }
    }

    fun populateNativeAdView(
        activity: Activity,
        apNativeAd: ApNativeAd,
        adPlaceHolder: FrameLayout,
        containerShimmerLoading: ShimmerFrameLayout?,
        colorCTA: String,
        heightCTA: Int,
        radiusCTA: Int
    ) {
        val adView = prepareNativeAdView(activity, apNativeAd, containerShimmerLoading) ?: return
        adPlaceHolder.visibility = View.VISIBLE
        Admob.getInstance().populateUnifiedNativeAdView(
            apNativeAd.admobNativeAd,
            adView,
            colorCTA,
            heightCTA,
            radiusCTA
        )
        adPlaceHolder.removeAllViews()
        adPlaceHolder.addView(adView)
        apNativeAd.setStatus(StatusAd.AD_RENDER_SUCCESS)
    }

    fun populateNativeAdView(
        activity: Activity,
        apNativeAd: ApNativeAd,
        adPlaceHolder: FrameLayout,
        containerShimmerLoading: ShimmerFrameLayout?,
        nativeAdConfig: NativeAdConfig
    ) {
        val adView = prepareNativeAdView(activity, apNativeAd, containerShimmerLoading) ?: return
        adPlaceHolder.visibility = View.VISIBLE
        Admob.getInstance().populateUnifiedNativeAdView(apNativeAd.admobNativeAd, adView, nativeAdConfig)
        adPlaceHolder.removeAllViews()
        adPlaceHolder.addView(adView)
        apNativeAd.setStatus(StatusAd.AD_RENDER_SUCCESS)
    }

    fun initRewardAds(context: Context, id: String, callback: RewardCallback) {
        Admob.getInstance().initRewardAds(context, id, callback)
    }

    fun initRewardAds(context: Context, id: String, callback: AdCallback) {
        Admob.getInstance().initRewardAds(context, id, callback)
    }

    fun getRewardInterstitial(context: Context, id: String, callback: AdCallback) {
        Admob.getInstance().getRewardInterstitial(context, id, callback)
    }

    fun showRewardInterstitial(
        activity: Activity,
        rewardedInterstitialAd: RewardedInterstitialAd?,
        adCallback: RewardCallback
    ) {
        Admob.getInstance().showRewardInterstitial(activity, rewardedInterstitialAd, adCallback)
    }

    fun showRewardAds(context: Activity, adCallback: RewardCallback) {
        Admob.getInstance().showRewardAds(context, adCallback)
    }

    fun showRewardAds(context: Activity, rewardedAd: RewardedAd?, adCallback: RewardCallback) {
        Admob.getInstance().showRewardAds(context, rewardedAd, adCallback)
    }

    fun loadInterSplashPriority4SameTime(
        context: Context,
        idAdsHigh1: String,
        idAdsHigh2: String,
        idAdsHigh3: String,
        idAdsNormal: String,
        timeOut: Long,
        timeDelay: Long,
        adListener: AdCallback
    ) {
        Admob.getInstance().loadInterSplashPriority4SameTime(
            context,
            idAdsHigh1,
            idAdsHigh2,
            idAdsHigh3,
            idAdsNormal,
            timeOut,
            timeDelay,
            adListener
        )
    }

    fun loadInterSplashPriority4SameTime(
        context: Context,
        idAdsHigh: String,
        idAdsNormal: String,
        timeOut: Long,
        timeDelay: Long,
        adListener: AdCallback
    ) {
        Admob.getInstance().loadInterSplashPriority2SameTime(
            context,
            idAdsHigh,
            idAdsNormal,
            timeOut,
            timeDelay,
            adListener
        )
    }

    fun loadInterSplashPriority4SameTime(
        context: Context,
        idAdsHigh: String,
        idAdsNormal: String,
        timeOut: Long,
        adListener: AdCallback
    ) {
        Admob.getInstance().loadInterSplashPriority2SameTime(
            context,
            idAdsHigh,
            idAdsNormal,
            timeOut,
            0L,
            adListener
        )
    }

    fun onShowSplashPriority4(activity: AppCompatActivity, adListener: AdCallback) {
        Admob.getInstance().onShowSplashPriority4(activity, adListener)
    }

    fun onCheckShowSplashPriority4WhenFail(
        activity: AppCompatActivity,
        callback: AdCallback,
        timeDelay: Int
    ) {
        Admob.getInstance().onCheckShowSplashPriority4WhenFail(activity, callback, timeDelay)
    }

    fun loadInterSplashPriority2SameTime(
        context: Context,
        idAdsHigh: String,
        idAdsNormal: String,
        timeOut: Long,
        timeDelay: Long,
        adListener: AdCallback
    ) {
        Admob.getInstance().loadInterSplashPriority2SameTime(
            context,
            idAdsHigh,
            idAdsNormal,
            timeOut,
            timeDelay,
            adListener
        )
    }

    fun loadInterSplashPriority2SameTime(
        context: Context,
        idAdsHigh: String,
        idAdsNormal: String,
        timeOut: Long,
        adListener: AdCallback
    ) {
        Admob.getInstance().loadInterSplashPriority2SameTime(
            context,
            idAdsHigh,
            idAdsNormal,
            timeOut,
            0L,
            adListener
        )
    }

    fun onShowSplashPriority2(activity: AppCompatActivity, adListener: AdCallback) {
        Admob.getInstance().onShowSplashPriority2(activity, adListener)
    }

    fun onCheckShowSplashPriority2WhenFail(
        activity: AppCompatActivity,
        callback: AdCallback,
        timeDelay: Int
    ) {
        Admob.getInstance().onCheckShowSplashPriority2WhenFail(activity, callback, timeDelay)
    }

    @JvmOverloads
    fun loadInterSplashPrioritySequentially(
        context: Context,
        listId: List<String>?,
        timeOut: Long,
        timeDelay: Long = 0L,
        adListener: AdCallback?
    ) {
        Admob.getInstance().loadInterSplashPrioritySequentially(context, listId, timeOut, timeDelay, adListener)
    }

    @JvmOverloads
    fun loadInterSplashPriority2Sequentially(
        context: Context,
        idAdsHigh: String?,
        idAdsNormal: String?,
        timeOut: Long,
        timeDelay: Long = 0L,
        adListener: AdCallback?
    ) {
        Admob.getInstance().loadInterSplashPriority2Sequentially(context, idAdsHigh, idAdsNormal, timeOut, timeDelay, adListener)
    }

    fun onShowSplashPrioritySequentially(activity: AppCompatActivity, adListener: AdCallback?) {
        Admob.getInstance().onShowSplashPrioritySequentially(activity, adListener)
    }

    private var isFinishLoadNativeAdHigh1 = false
    private var isFinishLoadNativeAdHigh2 = false
    private var isFinishLoadNativeAdHigh3 = false
    private var isFinishLoadNativeAdNormal = false

    private var apNativeAdHigh2: ApNativeAd? = null
    private var apNativeAdHigh3: ApNativeAd? = null
    private var apNativeAdNormal: ApNativeAd? = null

    fun loadNative4SameTime(
        activity: Activity,
        idAdHigh1: String,
        idAdHigh2: String,
        idAdHigh3: String,
        idAdNormal: String,
        layoutCustomNative: Int,
        adCallback: AdCallback
    ) {
        isFinishLoadNativeAdHigh1 = false
        isFinishLoadNativeAdHigh2 = false
        isFinishLoadNativeAdHigh3 = false
        isFinishLoadNativeAdNormal = false

        apNativeAdHigh2 = null
        apNativeAdHigh3 = null
        apNativeAdNormal = null

        loadNativeAdResultCallback(activity, idAdHigh1, layoutCustomNative, object : AdCallback() {
            override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                super.onNativeAdLoaded(nativeAd)
                adCallback.onNativeAdLoaded(nativeAd)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                adCallback.onAdClicked()
            }

            override fun onAdLogRev(
                adValue: AdValue?,
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                adCallback.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
            }

            override fun onAdClicked(
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                adCallback.onAdClicked(adUnitId, mediationAdapterClassName, adType)
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                val high2 = apNativeAdHigh2
                val high3 = apNativeAdHigh3
                val normal = apNativeAdNormal
                if (isFinishLoadNativeAdHigh2 && high2 != null) {
                    adCallback.onNativeAdLoaded(high2)
                } else if (isFinishLoadNativeAdHigh3 && high3 != null) {
                    adCallback.onNativeAdLoaded(high3)
                } else if (isFinishLoadNativeAdNormal && normal != null) {
                    adCallback.onNativeAdLoaded(normal)
                } else {
                    // waiting for ads loaded
                    isFinishLoadNativeAdHigh1 = true
                }
            }
        })

        loadNativeAdResultCallback(activity, idAdHigh2, layoutCustomNative, object : AdCallback() {
            override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                super.onNativeAdLoaded(nativeAd)
                if (isFinishLoadNativeAdHigh1) {
                    adCallback.onNativeAdLoaded(nativeAd)
                } else {
                    isFinishLoadNativeAdHigh2 = true
                    apNativeAdHigh2 = nativeAd
                }
            }

            override fun onAdClicked() {
                super.onAdClicked()
                adCallback.onAdClicked()
            }

            override fun onAdLogRev(
                adValue: AdValue?,
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                adCallback.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
            }

            override fun onAdClicked(
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                adCallback.onAdClicked(adUnitId, mediationAdapterClassName, adType)
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                if (isFinishLoadNativeAdHigh1) {
                    val high3 = apNativeAdHigh3
                    val normal = apNativeAdNormal
                    if (isFinishLoadNativeAdHigh3 && high3 != null) {
                        adCallback.onNativeAdLoaded(high3)
                    } else if (isFinishLoadNativeAdNormal && normal != null) {
                        adCallback.onNativeAdLoaded(normal)
                    } else {
                        isFinishLoadNativeAdHigh2 = true
                    }
                } else {
                    isFinishLoadNativeAdHigh2 = true
                    apNativeAdHigh2 = null
                }
            }
        })

        loadNativeAdResultCallback(activity, idAdHigh3, layoutCustomNative, object : AdCallback() {
            override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                super.onNativeAdLoaded(nativeAd)
                if (isFinishLoadNativeAdHigh1 && isFinishLoadNativeAdHigh2) {
                    adCallback.onNativeAdLoaded(nativeAd)
                } else {
                    isFinishLoadNativeAdHigh3 = true
                    apNativeAdHigh3 = nativeAd
                }
            }

            override fun onAdClicked() {
                super.onAdClicked()
                adCallback.onAdClicked()
            }

            override fun onAdLogRev(
                adValue: AdValue?,
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                adCallback.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
            }

            override fun onAdClicked(
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                adCallback.onAdClicked(adUnitId, mediationAdapterClassName, adType)
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                if (isFinishLoadNativeAdHigh1 && isFinishLoadNativeAdHigh2) {
                    val normal = apNativeAdNormal
                    if (isFinishLoadNativeAdNormal && normal != null) {
                        adCallback.onNativeAdLoaded(normal)
                    } else {
                        isFinishLoadNativeAdHigh3 = true
                    }
                } else {
                    isFinishLoadNativeAdHigh3 = true
                    apNativeAdHigh3 = null
                }
            }
        })

        loadNativeAdResultCallback(activity, idAdNormal, layoutCustomNative, object : AdCallback() {
            override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                super.onNativeAdLoaded(nativeAd)
                if (isFinishLoadNativeAdHigh1 && isFinishLoadNativeAdHigh2 && isFinishLoadNativeAdHigh3) {
                    adCallback.onNativeAdLoaded(nativeAd)
                } else {
                    isFinishLoadNativeAdNormal = true
                    apNativeAdNormal = nativeAd
                }
            }

            override fun onAdClicked() {
                super.onAdClicked()
                adCallback.onAdClicked()
            }

            override fun onAdLogRev(
                adValue: AdValue?,
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                adCallback.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
            }

            override fun onAdClicked(
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                adCallback.onAdClicked(adUnitId, mediationAdapterClassName, adType)
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                if (isFinishLoadNativeAdHigh1 && isFinishLoadNativeAdHigh2 && isFinishLoadNativeAdHigh3) {
                    apNativeAdNormal?.let { adCallback.onNativeAdLoaded(it) } ?: adCallback.onAdFailedToLoad(i)
                } else {
                    isFinishLoadNativeAdNormal = true
                    apNativeAdNormal = null
                }
            }
        })
    }

    fun loadPriorityInterstitialAds(
        context: Context,
        apInterstitialPriorityAd: ApInterstitialPriorityAd,
        adCallback: AdCallback
    ) {
        loadPriorityInterstitialAdsFromAdmob(context, apInterstitialPriorityAd, adCallback)
    }

    fun loadPriorityInterstitialAdsFromAdmob(
        context: Context,
        apInterstitialPriorityAd: ApInterstitialPriorityAd,
        adCallback: AdCallback
    ) {
        if (apInterstitialPriorityAd.high1PriorityId.isNotEmpty() &&
            apInterstitialPriorityAd.high1PriorityInterstitialAd?.isReady() != true
        ) {
            loadAdsInterHigh1Priority(context, apInterstitialPriorityAd, adCallback)
        }

        if (apInterstitialPriorityAd.high2PriorityId.isNotEmpty() &&
            apInterstitialPriorityAd.high2PriorityInterstitialAd?.isReady() != true
        ) {
            loadAdsInterHigh2Priority(context, apInterstitialPriorityAd, adCallback)
        }

        if (apInterstitialPriorityAd.high3PriorityId.isNotEmpty() &&
            apInterstitialPriorityAd.high3PriorityInterstitialAd?.isReady() != true
        ) {
            loadAdsInterHigh3Priority(context, apInterstitialPriorityAd, adCallback)
        }

        if (apInterstitialPriorityAd.normalPriorityId.isNotEmpty() &&
            apInterstitialPriorityAd.normalPriorityInterstitialAd?.isReady() != true
        ) {
            loadInterNormalPriority(context, apInterstitialPriorityAd, adCallback)
        }
    }

    private fun loadPriorityInterSlot(
        context: Context,
        adUnitId: String,
        targetAd: ApInterstitialAd?,
        logLabel: String,
        adCallback: AdCallback
    ) {
        targetAd?.setStatus(StatusAd.AD_LOADING)
        Admob.getInstance().getInterstitialAds(context, adUnitId, object : AdCallback() {
            override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                super.onInterstitialLoad(interstitialAd)
                Log.d(TAG, "onInterstitialLoad $logLabel")
                targetAd?.interstitialAd = interstitialAd
                if (interstitialAd == null) {
                    targetAd?.setStatus(StatusAd.AD_LOAD_FAIL)
                }
                adCallback.onApInterstitialLoad(targetAd)
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                Log.e(TAG, "onAdFailedToLoad: $logLabel: $i")
                targetAd?.setStatus(StatusAd.AD_LOAD_FAIL)
                adCallback.onAdFailedToLoad(i)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                adCallback.onAdClicked()
            }

            override fun onAdClicked(
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                adCallback.onAdClicked(adUnitId, mediationAdapterClassName, adType)
            }

            override fun onAdLogRev(
                adValue: AdValue?,
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                adCallback.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
            }

            override fun onAdImpression() {
                super.onAdImpression()
                adCallback.onAdImpression()
            }
        })
    }

    private fun loadAdsInterHigh1Priority(
        context: Context,
        apInterstitialPriorityAd: ApInterstitialPriorityAd,
        adCallback: AdCallback
    ) {
        loadPriorityInterSlot(
            context,
            apInterstitialPriorityAd.high1PriorityId,
            apInterstitialPriorityAd.high1PriorityInterstitialAd,
            "idAdsNormalPriority",
            adCallback
        )
    }

    private fun loadAdsInterHigh2Priority(
        context: Context,
        apInterstitialPriorityAd: ApInterstitialPriorityAd,
        adCallback: AdCallback
    ) {
        loadPriorityInterSlot(
            context,
            apInterstitialPriorityAd.high2PriorityId,
            apInterstitialPriorityAd.high2PriorityInterstitialAd,
            "idAdsNormalPriority",
            adCallback
        )
    }

    private fun loadAdsInterHigh3Priority(
        context: Context,
        apInterstitialPriorityAd: ApInterstitialPriorityAd,
        adCallback: AdCallback
    ) {
        loadPriorityInterSlot(
            context,
            apInterstitialPriorityAd.high3PriorityId,
            apInterstitialPriorityAd.high3PriorityInterstitialAd,
            "idAdsNormalPriority",
            adCallback
        )
    }

    private fun loadInterNormalPriority(
        context: Context,
        apInterstitialPriorityAd: ApInterstitialPriorityAd,
        adCallback: AdCallback
    ) {
        loadPriorityInterSlot(
            context,
            apInterstitialPriorityAd.normalPriorityId,
            apInterstitialPriorityAd.normalPriorityInterstitialAd,
            "idAdsNormalPriority",
            adCallback
        )
    }

    fun forceShowInterstitialPriority(
        context: Context,
        apInterstitialPriorityAd: ApInterstitialPriorityAd,
        adCallback: AdCallback,
        isReloadAds: Boolean
    ) {
        val high1 = apInterstitialPriorityAd.high1PriorityInterstitialAd
        val high2 = apInterstitialPriorityAd.high2PriorityInterstitialAd
        val high3 = apInterstitialPriorityAd.high3PriorityInterstitialAd
        val normal = apInterstitialPriorityAd.normalPriorityInterstitialAd
        val interstitialAd: ApInterstitialAd = when {
            high1 != null && high1.isReady() -> high1
            high2 != null && high2.isReady() -> high2
            high3 != null && high3.isReady() -> high3
            normal != null && normal.isReady() -> normal
            else -> {
                adCallback.onNextAction()
                if (isReloadAds) {
                    loadPriorityInterstitialAds(context, apInterstitialPriorityAd, AdCallback())
                }
                return
            }
        }
        forceShowInterstitial(
            context,
            interstitialAd,
            object : AdCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    adCallback.onNextAction()
                }

                override fun onAdClosed() {
                    super.onAdClosed()
                    interstitialAd.interstitialAd = null
                    interstitialAd.setStatus(StatusAd.AD_INIT)
                    adCallback.onAdClosed()
                    if (isReloadAds) {
                        loadPriorityInterstitialAds(context, apInterstitialPriorityAd, AdCallback())
                    }
                }

                override fun onInterstitialShow() {
                    super.onInterstitialShow()
                    adCallback.onInterstitialShow()
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    adCallback.onAdClicked()
                }

                override fun onAdClicked(
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                    super.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                    adCallback.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                }

                override fun onAdLogRev(
                    adValue: AdValue?,
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                    super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                    adCallback.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                }

                override fun onAdFailedToShow(adError: FullScreenContentError?) {
                    super.onAdFailedToShow(adError)
                    adCallback.onAdFailedToShow(adError)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    adCallback.onAdImpression()
                }
            },
            false
        )
    }

    // ==================== Priority 2 Interstitial Ads ====================

    fun loadPriority2InterstitialAds(
        context: Context,
        apInterstitialPriority2Ad: ApInterstitialPriority2Ad,
        adCallback: AdCallback
    ) {
        loadPriority2InterstitialAdsFromAdmob(context, apInterstitialPriority2Ad, adCallback)
    }

    fun loadPriority2InterstitialAdsFromAdmob(
        context: Context,
        apInterstitialPriority2Ad: ApInterstitialPriority2Ad,
        adCallback: AdCallback
    ) {
        if (apInterstitialPriority2Ad.highPriorityId.isNotEmpty() &&
            apInterstitialPriority2Ad.highPriorityInterstitialAd?.isReady() != true
        ) {
            loadAdsInterHighPriority2(context, apInterstitialPriority2Ad, adCallback)
        }

        if (apInterstitialPriority2Ad.normalPriorityId.isNotEmpty() &&
            apInterstitialPriority2Ad.normalPriorityInterstitialAd?.isReady() != true
        ) {
            loadInterNormalPriority2(context, apInterstitialPriority2Ad, adCallback)
        }
    }

    private fun loadAdsInterHighPriority2(
        context: Context,
        apInterstitialPriority2Ad: ApInterstitialPriority2Ad,
        adCallback: AdCallback
    ) {
        loadPriorityInterSlot(
            context,
            apInterstitialPriority2Ad.highPriorityId,
            apInterstitialPriority2Ad.highPriorityInterstitialAd,
            "idAdsHighPriority",
            adCallback
        )
    }

    private fun loadInterNormalPriority2(
        context: Context,
        apInterstitialPriority2Ad: ApInterstitialPriority2Ad,
        adCallback: AdCallback
    ) {
        loadPriorityInterSlot(
            context,
            apInterstitialPriority2Ad.normalPriorityId,
            apInterstitialPriority2Ad.normalPriorityInterstitialAd,
            "idAdsNormalPriority",
            adCallback
        )
    }

    fun forceShowInterstitialPriority2(
        context: Context,
        apInterstitialPriority2Ad: ApInterstitialPriority2Ad,
        adCallback: AdCallback,
        isReloadAds: Boolean
    ) {
        val high = apInterstitialPriority2Ad.highPriorityInterstitialAd
        val normal = apInterstitialPriority2Ad.normalPriorityInterstitialAd
        val interstitialAd: ApInterstitialAd = when {
            high != null && high.isReady() -> high
            normal != null && normal.isReady() -> normal
            else -> {
                adCallback.onNextAction()
                if (isReloadAds) {
                    loadPriority2InterstitialAds(context, apInterstitialPriority2Ad, AdCallback())
                }
                return
            }
        }
        forceShowInterstitial(
            context,
            interstitialAd,
            object : AdCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    adCallback.onNextAction()
                }

                override fun onAdClosed() {
                    super.onAdClosed()
                    interstitialAd.interstitialAd = null
                    interstitialAd.setStatus(StatusAd.AD_INIT)
                    adCallback.onAdClosed()
                    if (isReloadAds) {
                        loadPriority2InterstitialAds(context, apInterstitialPriority2Ad, AdCallback())
                    }
                }

                override fun onInterstitialShow() {
                    super.onInterstitialShow()
                    adCallback.onInterstitialShow()
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    adCallback.onAdClicked()
                }

                override fun onAdClicked(
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                    super.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                    adCallback.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                }

                override fun onAdLogRev(
                    adValue: AdValue?,
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                    super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                    adCallback.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                }

                override fun onAdFailedToShow(adError: FullScreenContentError?) {
                    super.onAdFailedToShow(adError)
                    adCallback.onAdFailedToShow(adError)
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    adCallback.onAdImpression()
                }
            },
            false
        )
    }

    fun loadAndShowInterstitial(
        context: Context,
        id: String,
        timeOut: Long,
        isDelayInterval: Boolean,
        callback: AdCallback
    ) {
        val intervalMs = (getInstance().adConfig?.intervalInterstitialAd ?: 0) * 1000L
        if (System.currentTimeMillis() - SharePreferenceUtils.getLastImpressionInterstitialTime(context) < intervalMs &&
            isDelayInterval
        ) {
            callback.onNextAction()
            return
        }
        Admob.getInstance().loadAndShowInterstitial(context, id, timeOut, callback)
    }

    // =========================================================================
    // Picture-in-Picture (PiP) Ads
    // =========================================================================

    @JvmOverloads
    fun getPipAd(
        context: Context,
        id: String,
        adCallback: AdCallback? = null
    ): ApPipAd {
        val apPipAd = ApPipAd(StatusAd.AD_LOADING)
        Admob.getInstance().loadPipAd(context, id, object : AdCallback() {
            override fun onPipAdLoaded(pipAd: PictureInPictureAd?) {
                apPipAd.pictureInPictureAd = pipAd
                if (pipAd == null) {
                    apPipAd.setStatus(StatusAd.AD_LOAD_FAIL)
                }
                adCallback?.onPipAdLoaded(pipAd)
                adCallback?.onApPipAdLoaded(apPipAd)
                adCallback?.onAdLoaded()
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                apPipAd.setStatus(StatusAd.AD_LOAD_FAIL)
                adCallback?.onAdFailedToLoad(i)
            }

            override fun onPipAdShown() {
                apPipAd.setStatus(StatusAd.AD_RENDER_SUCCESS)
                adCallback?.onPipAdShown()
            }

            override fun onPipAdHidden() {
                adCallback?.onPipAdHidden()
            }

            override fun onAdImpression() {
                apPipAd.setStatus(StatusAd.AD_RENDER_SUCCESS)
                adCallback?.onAdImpression()
            }

            override fun onAdClicked() {
                adCallback?.onAdClicked()
            }

            override fun onAdClicked(
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                adCallback?.onAdClicked(adUnitId, mediationAdapterClassName, adType)
            }

            override fun onAdLogRev(
                adValue: AdValue?,
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                adCallback?.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
            }

            override fun onAdClosed() {
                adCallback?.onAdClosed()
            }

            override fun onNextAction() {
                adCallback?.onNextAction()
            }
        })
        return apPipAd
    }

    @JvmOverloads
    fun showPipAd(
        activity: Activity,
        apPipAd: ApPipAd? = null,
        position: PictureInPictureAdPosition = PictureInPictureAdPosition.BOTTOM_RIGHT,
        scope: PictureInPictureAdPresentationScope = PictureInPictureAdPresentationScope.SCREEN,
        adCallback: AdCallback? = null
    ) {
        val pipAd = apPipAd?.pictureInPictureAd ?: Admob.getInstance().getCurrentPipAd()
        if (pipAd != null) {
            apPipAd?.setStatus(StatusAd.AD_RENDER_SUCCESS)
        }
        Admob.getInstance().showPipAd(activity, pipAd, position, scope, adCallback)
    }

    @JvmOverloads
    fun loadAndShowPipAd(
        activity: Activity,
        id: String,
        position: PictureInPictureAdPosition = PictureInPictureAdPosition.BOTTOM_RIGHT,
        scope: PictureInPictureAdPresentationScope = PictureInPictureAdPresentationScope.SCREEN,
        adCallback: AdCallback? = null
    ) {
        Admob.getInstance().loadAndShowPipAd(activity, id, position, scope, adCallback)
    }

    @JvmOverloads
    fun hidePipAd(apPipAd: ApPipAd? = null) {
        if (apPipAd?.pictureInPictureAd != null) {
            apPipAd.hide()
        } else {
            Admob.getInstance().hidePipAd()
        }
    }

    @JvmOverloads
    fun destroyPipAd(apPipAd: ApPipAd? = null) {
        if (apPipAd?.pictureInPictureAd != null) {
            apPipAd.destroy()
        } else {
            Admob.getInstance().destroyPipAd()
        }
    }

    // =========================================================================
    // Swipeable Ads (SwipeableInterstitialAd & Swipable NativeAd)
    // =========================================================================

    @JvmOverloads
    fun getSwipeableInterstitialAd(
        context: Context,
        id: String,
        maxScreenHoldDurationSeconds: Int = 0,
        swipeDirection: NativeAd.SwipeGestureDirection? = null,
        tapsAllowed: Boolean = true,
        adCallback: AdCallback? = null
    ): ApSwipeableAd {
        val apSwipeableAd = ApSwipeableAd(StatusAd.AD_LOADING)
        Admob.getInstance().loadSwipeableInterstitialAd(
            context,
            id,
            maxScreenHoldDurationSeconds,
            swipeDirection,
            tapsAllowed,
            object : AdCallback() {
                override fun onSwipeableAdLoaded(swipeableAd: SwipeableInterstitialAd?) {
                    apSwipeableAd.swipeableInterstitialAd = swipeableAd
                    if (swipeableAd == null) {
                        apSwipeableAd.setStatus(StatusAd.AD_LOAD_FAIL)
                    }
                    adCallback?.onSwipeableAdLoaded(swipeableAd)
                    adCallback?.onApSwipeableAdLoaded(apSwipeableAd)
                    adCallback?.onAdLoaded()
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    apSwipeableAd.setStatus(StatusAd.AD_LOAD_FAIL)
                    adCallback?.onAdFailedToLoad(i)
                }

                override fun onSwipeableScreenHoldTimerStarted() {
                    adCallback?.onSwipeableScreenHoldTimerStarted()
                }

                override fun onAdImpression() {
                    apSwipeableAd.setStatus(StatusAd.AD_RENDER_SUCCESS)
                    adCallback?.onAdImpression()
                }

                override fun onAdClicked() {
                    adCallback?.onAdClicked()
                }

                override fun onAdClicked(
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                    adCallback?.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                }

                override fun onAdLogRev(
                    adValue: AdValue?,
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                    adCallback?.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                }

                override fun onAdClosed() {
                    adCallback?.onAdClosed()
                }

                override fun onNextAction() {
                    adCallback?.onNextAction()
                }
            }
        )
        return apSwipeableAd
    }

    @JvmOverloads
    fun renderSwipeableInterstitialAd(
        activity: Activity,
        container: ViewGroup,
        apSwipeableAd: ApSwipeableAd? = null,
        adCallback: AdCallback? = null
    ): View? {
        val ad = apSwipeableAd?.swipeableInterstitialAd ?: Admob.getInstance().getCurrentSwipeableAd()
        val renderedView = Admob.getInstance().renderSwipeableInterstitialAd(activity, container, ad, adCallback)
        if (renderedView != null) {
            apSwipeableAd?.setStatus(StatusAd.AD_RENDER_SUCCESS)
        }
        return renderedView
    }

    @JvmOverloads
    fun destroySwipeableAd(activity: Activity, apSwipeableAd: ApSwipeableAd? = null) {
        if (apSwipeableAd?.swipeableInterstitialAd != null) {
            apSwipeableAd.destroy(activity)
        } else {
            Admob.getInstance().destroySwipeableAd(activity)
        }
    }

    @JvmOverloads
    fun loadAndShowSwipeableInterstitialAd(
        activity: Activity,
        id: String,
        container: ViewGroup,
        maxScreenHoldDurationSeconds: Int = 0,
        swipeDirection: NativeAd.SwipeGestureDirection? = null,
        tapsAllowed: Boolean = true,
        adCallback: AdCallback? = null
    ) {
        Admob.getInstance().loadAndShowSwipeableInterstitialAd(
            activity,
            id,
            container,
            maxScreenHoldDurationSeconds,
            swipeDirection,
            tapsAllowed,
            adCallback
        )
    }

    @JvmOverloads
    fun showSwipeableInterstitialFeedDialog(
        activity: Activity,
        id: String,
        initialMaxHoldSeconds: Int = 5,
        adCallback: AdCallback? = null
    ): SwipeableInterstitialFeedDialog {
        val dialog = SwipeableInterstitialFeedDialog(activity, id, initialMaxHoldSeconds, adCallback)
        dialog.show()
        return dialog
    }

    @JvmOverloads
    fun loadSwipeableNativeAd(
        activity: Activity,
        id: String,
        layoutCustomNative: Int,
        adPlaceHolder: FrameLayout,
        containerShimmer: ShimmerFrameLayout? = null,
        swipeDirection: NativeAd.SwipeGestureDirection = NativeAd.SwipeGestureDirection.UP,
        tapsAllowed: Boolean = true,
        callback: AdCallback? = null
    ) {
        Admob.getInstance().loadSwipeableNativeAd(
            activity,
            id,
            swipeDirection,
            tapsAllowed,
            object : AdCallback() {
                override fun onUnifiedNativeAdLoaded(unifiedNativeAd: NativeAd) {
                    val apNativeAd = ApNativeAd(layoutCustomNative, unifiedNativeAd)
                    callback?.onUnifiedNativeAdLoaded(unifiedNativeAd)
                    callback?.onNativeAdLoaded(apNativeAd)
                    populateNativeAdView(activity, apNativeAd, adPlaceHolder, containerShimmer)
                }

                override fun onAdImpression() {
                    callback?.onAdImpression()
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    containerShimmer?.stopShimmer()
                    containerShimmer?.visibility = View.GONE
                    adPlaceHolder.visibility = View.GONE
                    callback?.onAdFailedToLoad(i)
                }

                override fun onAdClicked() {
                    callback?.onAdClicked()
                }

                override fun onAdClicked(
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                    callback?.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                }

                override fun onAdLogRev(
                    adValue: AdValue?,
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                    callback?.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                }
            }
        )
    }

    @JvmOverloads
    fun showFullScreenSwipeableNativeDialog(
        activity: Activity,
        id: String,
        swipeDirection: NativeAd.SwipeGestureDirection = NativeAd.SwipeGestureDirection.UP,
        tapsAllowed: Boolean = true,
        adCallback: AdCallback? = null
    ): FullScreenNativeAdDialog {
        val dialog = FullScreenNativeAdDialog(activity, id, swipeDirection, tapsAllowed, adCallback)
        dialog.show()
        return dialog
    }

    companion object {
        const val TAG_ADJUST: String = "NkhAdjust"
        const val TAG: String = "NkhAd"

        @Volatile
        private var INSTANCE: NkhAd? = null

        @JvmStatic
        @Synchronized
        fun getInstance(): NkhAd {
            return INSTANCE ?: NkhAd().also { INSTANCE = it }
        }
    }
}
