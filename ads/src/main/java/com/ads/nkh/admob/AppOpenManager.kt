package com.ads.nkh.admob

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.ads.nkh.R
import com.ads.nkh.billing.AppPurchase
import com.ads.nkh.config.NkhAdConfig
import com.ads.nkh.dialog.PrepareLoadingAdsDialog
import com.ads.nkh.dialog.ResumeLoadingDialog
import com.ads.nkh.event.NkhLogEventManager
import com.ads.nkh.funtion.AdCallback
import com.ads.nkh.funtion.AdType
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdActivity
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import java.util.Collections
import java.util.Date
import java.util.WeakHashMap

class AppOpenManager private constructor() : Application.ActivityLifecycleCallbacks, LifecycleObserver {
    private var appResumeAd: AppOpenAd? = null
    var splashAd: AppOpenAd? = null
    private var loadCallback: AdLoadCallback<AppOpenAd>? = null
    private val appOpenLoadCallbacks: MutableMap<AppOpenAd, AdCallback?> =
        Collections.synchronizedMap(WeakHashMap())

    private var loadCallbackHigh: AdLoadCallback<AppOpenAd>? = null
    private var loadCallbackMedium: AdLoadCallback<AppOpenAd>? = null
    private var loadCallbackAll: AdLoadCallback<AppOpenAd>? = null

    private var loadCallbackOpen: AdLoadCallback<AppOpenAd>? = null
    private var fullScreenContentCallback: AppOpenAdEventCallback? = null

    private var appResumeAdId: String? = null
    private var splashAdId: String? = null

    private var currentActivity: Activity? = null
    private var myApplication: Application? = null

    private var appResumeLoadTime: Long = 0
    private var splashLoadTime: Long = 0
    private var splashTimeout: Int = 0

    @get:JvmName("_isInitialized_prop")
    @set:JvmName("_setInitialized_prop")
    var isInitialized: Boolean = false // on - off ad resume on app
    private var isAppResumeEnabled: Boolean = true
    @get:JvmName("_isInterstitialShowing_prop")
    @set:JvmName("_setInterstitialShowing_prop")
    var isInterstitialShowing: Boolean = false
    private var enableScreenContentCallback: Boolean = false // default = true when use splash & false after show splash
    private var disableAdResumeByClickAction: Boolean = false
    var isDisableAdResumeByClickAction: Boolean
        @JvmName("_isDisableAdResumeByClickAction_prop") get() = disableAdResumeByClickAction
        @JvmName("_setDisableAdResumeByClickAction_prop") set(value) {
            disableAdResumeByClickAction = value
        }
    var isShowingAd: Boolean
        @JvmName("_isShowingAd_prop") get() = Companion.isShowingAd
        @JvmName("_setShowingAd_prop") private set(value) {
            Companion.isShowingAd = value
        }
    private val disabledAppOpenList: MutableList<Class<*>> = ArrayList()
    private var splashActivity: Class<*>? = null
    private var isTimeout: Boolean = false
    private var splashAdHigh: AppOpenAd? = null
    private var splashAdMedium: AppOpenAd? = null
    private var splashAdAll: AppOpenAd? = null

    private var splashAdOpen: AppOpenAd? = null
    private var splashAdInter: InterstitialAd? = null

    private var statusHigh: Int = -1
    private var statusMedium: Int = -1
    private var statusAll: Int = -1

    private var statusOpen: Int = -1
    private var statusInter: Int = -1

    private val Type_Loading = 0
    private val Type_Load_Success = 1
    private val Type_Load_Fail = 2
    private val Type_Show_Success = 3
    private val Type_Show_Fail = 4

    private var isAppOpenShowed: Boolean = false

    private var dialogSplash: Dialog? = null
    private var timerListenInter: CountDownTimer? = null
    private var currentTime: Long = 0
    private var timeRemaining: Long = 0

    private var timeoutHandler: Handler? = null

    /**
     * Init AppOpenManager
     *
     * @param application
     */
    fun init(application: Application, appOpenAdId: String?) {
        isInitialized = true
        disableAdResumeByClickAction = false
        this.myApplication = application
        this.myApplication?.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        this.appResumeAdId = appOpenAdId
    }

    fun isInitialized(): Boolean = isInitialized

    fun setInitialized(initialized: Boolean) {
        isInitialized = initialized
    }

    fun setEnableScreenContentCallback(enableScreenContentCallback: Boolean) {
        this.enableScreenContentCallback = enableScreenContentCallback
    }

    fun isInterstitialShowing(): Boolean = isInterstitialShowing

    fun setInterstitialShowing(interstitialShowing: Boolean) {
        isInterstitialShowing = interstitialShowing
    }

    fun disableAdResumeByClickAction() {
        disableAdResumeByClickAction = true
    }

    fun setDisableAdResumeByClickAction(disableAdResumeByClickAction: Boolean) {
        this.disableAdResumeByClickAction = disableAdResumeByClickAction
    }

    fun isDisableAdResumeByClickAction(): Boolean = disableAdResumeByClickAction

    fun isShowingAd(): Boolean = Companion.isShowingAd

    fun disableAppResumeWithActivity(activityClass: Class<*>) {
        Log.d(TAG, "disableAppResumeWithActivity: ${activityClass.name}")
        disabledAppOpenList.add(activityClass)
    }

    fun enableAppResumeWithActivity(activityClass: Class<*>) {
        Log.d(TAG, "enableAppResumeWithActivity: ${activityClass.name}")
        disabledAppOpenList.remove(activityClass)
    }

    fun disableAppResume() {
        isAppResumeEnabled = false
    }

    fun enableAppResume() {
        isAppResumeEnabled = true
    }

    fun setSplashActivity(splashActivity: Class<*>?, adId: String?, timeoutInMillis: Int) {
        this.splashActivity = splashActivity
        this.splashAdId = adId
        this.splashTimeout = timeoutInMillis
    }

    fun setAppResumeAdId(appResumeAdId: String?) {
        this.appResumeAdId = appResumeAdId
    }

    fun setFullScreenContentCallback(callback: AppOpenAdEventCallback?) {
        this.fullScreenContentCallback = callback
    }

    fun removeFullScreenContentCallback() {
        this.fullScreenContentCallback = null
    }

    private fun logAppOpenAdPaid(adValue: AdValue, ad: AppOpenAd, adCallback: AdCallback?) {
        val adapterName = ad.getResponseInfo().adapterClassName
        myApplication?.applicationContext?.let { context ->
            NkhLogEventManager.logPaidAdImpression(
                context,
                adValue,
                ad.adUnitId,
                adapterName
            )
        }
        NkhLogEventManager.logPaidAdjustWithToken(
            adValue,
            ad.adUnitId,
            NkhAdConfig.ADJUST_TOKEN_TIKTOK
        )
        adCallback?.onAdLogRev(
            adValue,
            ad.adUnitId,
            adapterName,
            AdType.APP_OPEN
        )
        val loadCb = appOpenLoadCallbacks[ad]
        if (loadCb != null && loadCb !== adCallback) {
            loadCb.onAdLogRev(
                adValue,
                ad.adUnitId,
                adapterName,
                AdType.APP_OPEN
            )
        }
    }

    fun fetchAd(isSplash: Boolean) {
        Log.d(TAG, "fetchAd: isSplash = $isSplash")
        if (isAdAvailable(isSplash)) {
            return
        }

        val callback = object : AdLoadCallback<AppOpenAd> {
            override fun onAdLoaded(ad: AppOpenAd) {
                runOnUiThread {
                    Log.d(TAG, "onAppOpenAdLoaded: isSplash = $isSplash")
                    if (!isSplash) {
                        this@AppOpenManager.appResumeAd = ad
                        ad.adEventCallback = object : AppOpenAdEventCallback {
                            override fun onAdPaid(adValue: AdValue) {
                                runOnUiThread {
                                    logAppOpenAdPaid(adValue, ad, null)
                                }
                            }
                        }
                        this@AppOpenManager.appResumeLoadTime = Date().time
                    } else {
                        this@AppOpenManager.splashAd = ad
                        ad.adEventCallback = object : AppOpenAdEventCallback {
                            override fun onAdPaid(adValue: AdValue) {
                                runOnUiThread {
                                    logAppOpenAdPaid(adValue, ad, null)
                                }
                            }
                        }
                        this@AppOpenManager.splashLoadTime = Date().time
                    }
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                runOnUiThread {
                    Log.d(TAG, "onAppOpenAdFailedToLoad: isSplash$isSplash message ${loadAdError.message}")
                }
            }
        }
        loadCallback = callback

        val activity = currentActivity
        val targetAdId = if (isSplash) splashAdId else appResumeAdId
        if (activity != null) {
            if (AppPurchase.getInstance().isPurchased(activity)) {
                return
            }
            if (listOf(*activity.resources.getStringArray(R.array.list_id_test)).contains(targetAdId)) {
                showTestIdAlert(activity, isSplash, targetAdId)
            }
        }
        if (!targetAdId.isNullOrEmpty()) {
            val request = getAdRequest(targetAdId)
            loadAppOpenAd(request, callback)
        }
    }

    private fun loadAppOpenAd(request: AdRequest, callback: AdLoadCallback<AppOpenAd>) {
        Admob.getInstance().runWhenSdkInitialized(Runnable {
            AppOpenAd.load(request, callback)
        })
    }

    @SuppressLint("MissingPermission")
    private fun showTestIdAlert(context: Context, isSplash: Boolean, id: String?) {
        val notification = NotificationCompat.Builder(context, "warning_ads")
            .setContentTitle("Found test ad id")
            .setContentText(if (isSplash) "Splash Ads: " else "AppResume Ads: $id")
            .setSmallIcon(R.drawable.ic_warning)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "warning_ads",
                "Warning Ads",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(if (isSplash) Admob.SPLASH_ADS else Admob.RESUME_ADS, notification)
    }

    private fun getAdRequest(adUnitId: String): AdRequest {
        return AdRequest.Builder(adUnitId).build()
    }

    private fun wasLoadTimeLessThanNHoursAgo(loadTime: Long, numHours: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    fun isAdAvailable(isSplash: Boolean): Boolean {
        val loadTime = if (isSplash) splashLoadTime else appResumeLoadTime
        val wasLoadTimeLessThanNHoursAgo = wasLoadTimeLessThanNHoursAgo(loadTime, 4)
        Log.d(TAG, "isAdAvailable: $wasLoadTimeLessThanNHoursAgo")
        return (if (isSplash) splashAd != null else appResumeAd != null) &&
            wasLoadTimeLessThanNHoursAgo
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (activity.javaClass.name != AdActivity::class.java.name) {
            currentActivity = activity
            Log.d(TAG, "onActivityStarted: $currentActivity")
        }
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity.javaClass.name != AdActivity::class.java.name) {
            currentActivity = activity
            Log.d(TAG, "onActivityResumed: $currentActivity")
            val splashCls = splashActivity
            if (splashCls == null) {
                Log.d(TAG, "onActivityResumed 1: with ${activity.javaClass.name}")
                fetchAd(false)
            } else if (activity.javaClass.name != splashCls.name) {
                Log.d(TAG, "onActivityResumed 2: with ${activity.javaClass.name}")
                fetchAd(false)
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
            Log.d(TAG, "onActivityDestroyed: null")
        }
    }

    fun showAdIfAvailable(isSplash: Boolean) {
        val activity = currentActivity
        if (activity == null || AppPurchase.getInstance().isPurchased(activity)) {
            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                fullScreenContentCallback?.onAdDismissedFullScreenContent()
            }
            return
        }

        Log.d(TAG, "showAdIfAvailable: ${ProcessLifecycleOwner.get().lifecycle.currentState}")
        Log.d(TAG, "showAd isSplash: $isSplash")
        if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            Log.d(TAG, "showAdIfAvailable: return")
            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                fullScreenContentCallback?.onAdDismissedFullScreenContent()
            }
            return
        }

        if (!isShowingAd && isAdAvailable(isSplash)) {
            Log.d(TAG, "Will show ad isSplash:$isSplash")
            if (isSplash) {
                showAdsWithLoading()
            } else {
                showResumeAds()
            }
        } else {
            Log.d(TAG, "Ad is not ready")
            if (!isSplash) {
                fetchAd(false)
            }
            if (isSplash && isShowingAd && isAdAvailable(true)) {
                showAdsWithLoading()
            }
        }
    }

    private fun showAdsWithLoading() {
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            dialogSplash = null
            try {
                val activity = currentActivity ?: return
                dialogSplash = PrepareLoadingAdsDialog(activity)
                try {
                    dialogSplash?.show()
                } catch (e: Exception) {
                    if (fullScreenContentCallback != null && enableScreenContentCallback) {
                        fullScreenContentCallback?.onAdDismissedFullScreenContent()
                    }
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Handler(Looper.getMainLooper()).postDelayed({
                val ad = splashAd
                if (ad != null) {
                    ad.adEventCallback = object : AppOpenAdEventCallback {
                        override fun onAdDismissedFullScreenContent() {
                            runOnUiThread {
                                try {
                                    dialogSplash?.dismiss()
                                } catch (ignored: Exception) {
                                }
                                appResumeAd = null
                                if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                    fullScreenContentCallback?.onAdDismissedFullScreenContent()
                                    enableScreenContentCallback = false
                                }
                                isShowingAd = false
                                fetchAd(true)
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                            runOnUiThread {
                                try {
                                    dialogSplash?.dismiss()
                                } catch (ignored: Exception) {
                                }
                                if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                    fullScreenContentCallback?.onAdFailedToShowFullScreenContent(adError)
                                }
                            }
                        }

                        override fun onAdShowedFullScreenContent() {
                            runOnUiThread {
                                if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                    fullScreenContentCallback?.onAdShowedFullScreenContent()
                                }
                                isShowingAd = true
                                splashAd = null
                                Handler(Looper.getMainLooper()).postDelayed({
                                    try {
                                        dialogSplash?.dismiss()
                                    } catch (ignored: Exception) {
                                    }
                                }, 200L)
                            }
                        }

                        override fun onAdClicked() {
                            runOnUiThread {
                                val act = currentActivity
                                if (act != null) {
                                    NkhLogEventManager.logClickAdsEvent(act, splashAdId ?: ad.adUnitId)
                                    fullScreenContentCallback?.onAdClicked()
                                }
                            }
                        }

                        override fun onAdPaid(adValue: AdValue) {
                            runOnUiThread {
                                logAppOpenAdPaid(adValue, ad, null)
                                fullScreenContentCallback?.onAdPaid(adValue)
                            }
                        }
                    }
                    currentActivity?.let { ad.show(it) }
                } else {
                    try {
                        dialogSplash?.dismiss()
                    } catch (ignored: Exception) {
                    }
                }
            }, 800)
        }
    }

    var dialog: Dialog? = null

    private fun showResumeAds() {
        val activity = currentActivity
        if (appResumeAd == null || activity == null || AppPurchase.getInstance().isPurchased(activity)) {
            return
        }
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            try {
                dismissDialogLoading()
                dialog = ResumeLoadingDialog(activity)
                try {
                    dialog?.show()
                } catch (e: Exception) {
                    if (fullScreenContentCallback != null && enableScreenContentCallback) {
                        fullScreenContentCallback?.onAdDismissedFullScreenContent()
                    }
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val ad = appResumeAd
            if (ad != null) {
                ad.adEventCallback = object : AppOpenAdEventCallback {
                    override fun onAdDismissedFullScreenContent() {
                        runOnUiThread {
                            appResumeAd = null
                            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                fullScreenContentCallback?.onAdDismissedFullScreenContent()
                            }
                            isShowingAd = false
                            fetchAd(false)
                            dismissDialogLoading()
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                        runOnUiThread {
                            Log.e(TAG, "onAdFailedToShowFullScreenContent: ${adError.message}")
                            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                fullScreenContentCallback?.onAdFailedToShowFullScreenContent(adError)
                            }

                            val act = currentActivity
                            val dlg = dialog
                            if (act != null && !act.isDestroyed && dlg != null && dlg.isShowing) {
                                Log.d(TAG, "dismiss dialog loading ad open: ")
                                try {
                                    dlg.dismiss()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            appResumeAd = null
                            isShowingAd = false
                            fetchAd(false)
                        }
                    }

                    override fun onAdShowedFullScreenContent() {
                        runOnUiThread {
                            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                                fullScreenContentCallback?.onAdShowedFullScreenContent()
                            }
                            isShowingAd = true
                            appResumeAd = null
                            Handler(Looper.getMainLooper()).postDelayed({
                                dismissDialogLoading()
                            }, 200L)
                        }
                    }

                    override fun onAdClicked() {
                        runOnUiThread {
                            val act = currentActivity
                            if (act != null) {
                                NkhLogEventManager.logClickAdsEvent(act, appResumeAdId ?: ad.adUnitId)
                                fullScreenContentCallback?.onAdClicked()
                            }
                        }
                    }

                    override fun onAdImpression() {
                        runOnUiThread {
                            if (currentActivity != null) {
                                fullScreenContentCallback?.onAdImpression()
                            }
                        }
                    }

                    override fun onAdPaid(adValue: AdValue) {
                        runOnUiThread {
                            logAppOpenAdPaid(adValue, ad, null)
                            fullScreenContentCallback?.onAdPaid(adValue)
                        }
                    }
                }
                currentActivity?.let { ad.show(it) }
            } else {
                dismissDialogLoading()
            }
        }
    }

    fun loadSplashOpenHighFloor(
        splashActivity: Class<*>?,
        activity: Activity,
        idOpenHigh: String,
        idOpenMedium: String,
        idOpenAll: String,
        timeOutOpen: Int,
        adListener: AdCallback?
    ) {
        isAppOpenShowed = false

        statusHigh = Type_Loading
        statusMedium = Type_Loading
        statusAll = Type_Loading

        if (AppPurchase.getInstance().isPurchased(activity)) {
            adListener?.onNextAction()
            return
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (adListener != null && !isAppOpenShowed) {
                isAppOpenShowed = true
                adListener.onNextAction()
            }
        }, timeOutOpen.toLong())

        getInstance().setSplashActivity(splashActivity, idOpenHigh, timeOutOpen)

        // load Open Splash High
        val cbHigh = object : AdLoadCallback<AppOpenAd> {
            override fun onAdLoaded(appOpenAd: AppOpenAd) {
                runOnUiThread {
                    Log.d(TAG, "loadCallbackHigh: onAdLoaded")
                    adListener?.onAdLoadedHigh()

                    appOpenAd.adEventCallback = object : AppOpenAdEventCallback {
                        override fun onAdClicked() {
                            runOnUiThread {
                                disableAdResumeByClickAction = true
                                NkhLogEventManager.logClickAdsEvent(activity, appOpenAd.adUnitId)
                                adListener?.onAdClickedHigh()
                                adListener?.onAdClicked(
                                    appOpenAd.adUnitId,
                                    appOpenAd.getResponseInfo().adapterClassName,
                                    AdType.APP_OPEN
                                )
                            }
                        }

                        override fun onAdDismissedFullScreenContent() {
                            runOnUiThread {
                                adListener?.onNextAction()
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                            runOnUiThread {
                                Log.e(TAG, "onAdFailedToShowFullScreenContent: High")
                                statusHigh = Type_Load_Fail
                                if (splashAdHigh != null && statusMedium == Type_Load_Success && !isAppOpenShowed) {
                                    getInstance().setSplashActivity(splashActivity, idOpenMedium, timeOutOpen)
                                    splashAdMedium?.show(activity)
                                }
                                splashAdHigh = null
                                adListener?.onAdFailedToShowHigh(adError)
                            }
                        }

                        override fun onAdImpression() {
                            runOnUiThread {
                                isAppOpenShowed = true
                                adListener?.onAdImpression()
                            }
                        }

                        override fun onAdShowedFullScreenContent() {}

                        override fun onAdPaid(adValue: AdValue) {
                            runOnUiThread {
                                logAppOpenAdPaid(adValue, appOpenAd, adListener)
                            }
                        }
                    }

                    splashAdHigh = appOpenAd
                    splashLoadTime = Date().time

                    if (!isAppOpenShowed) {
                        (currentActivity ?: activity).let { appOpenAd.show(it) }
                    }
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                runOnUiThread {
                    Log.d(TAG, "loadCallbackHigh: onAdFailedToLoad")
                    statusHigh = Type_Load_Fail
                    if (splashAdHigh == null) {
                        if (statusMedium == Type_Load_Success && !isAppOpenShowed) {
                            getInstance().setSplashActivity(splashActivity, idOpenMedium, timeOutOpen)
                            splashAdMedium?.show(activity)
                        }
                    }
                    if (splashAdMedium == null &&
                        splashAdAll == null &&
                        statusMedium == Type_Load_Fail &&
                        statusAll == Type_Load_Fail
                    ) {
                        if (adListener != null && !isAppOpenShowed) {
                            isAppOpenShowed = true
                            adListener.onNextAction()
                        }
                    }
                }
            }
        }
        loadCallbackHigh = cbHigh

        // load Open Splash Medium
        val cbMedium = object : AdLoadCallback<AppOpenAd> {
            override fun onAdLoaded(appOpenAd: AppOpenAd) {
                runOnUiThread {
                    Log.d(TAG, "loadCallbackMedium: onAdLoaded")
                    adListener?.onAdLoaded()
                    statusMedium = Type_Load_Success
                    splashAdMedium = appOpenAd

                    appOpenAd.adEventCallback = object : AppOpenAdEventCallback {
                        override fun onAdClicked() {
                            runOnUiThread {
                                disableAdResumeByClickAction = true
                                NkhLogEventManager.logClickAdsEvent(activity, appOpenAd.adUnitId)
                                adListener?.onAdClickedMedium()
                                adListener?.onAdClicked(
                                    appOpenAd.adUnitId,
                                    appOpenAd.getResponseInfo().adapterClassName,
                                    AdType.APP_OPEN
                                )
                            }
                        }

                        override fun onAdDismissedFullScreenContent() {
                            runOnUiThread {
                                adListener?.onNextAction()
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                            runOnUiThread {
                                Log.e(TAG, "onAdFailedToShowFullScreenContent: Medium")
                                splashAdMedium = null
                                statusMedium = Type_Load_Fail

                                if (statusAll == Type_Load_Success && !isAppOpenShowed) {
                                    getInstance().setSplashActivity(splashActivity, idOpenAll, timeOutOpen)
                                    if (splashAdAll != null && !isAppOpenShowed) {
                                        splashAdAll?.show(activity)
                                    }
                                }

                                adListener?.onAdFailedToShowMedium(adError)
                            }
                        }

                        override fun onAdImpression() {
                            runOnUiThread {
                                isAppOpenShowed = true
                                adListener?.onAdImpression()
                            }
                        }

                        override fun onAdShowedFullScreenContent() {}

                        override fun onAdPaid(adValue: AdValue) {
                            runOnUiThread {
                                logAppOpenAdPaid(adValue, appOpenAd, adListener)
                            }
                        }
                    }
                    splashLoadTime = Date().time
                    if ((statusHigh == Type_Load_Fail || statusHigh == Type_Load_Success) &&
                        (statusAll == Type_Load_Fail || statusAll == Type_Load_Success || statusAll == Type_Loading) &&
                        !isAppOpenShowed
                    ) {
                        getInstance().setSplashActivity(splashActivity, idOpenMedium, timeOutOpen)
                        appOpenAd.show(activity)
                    }
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                runOnUiThread {
                    Log.d(TAG, "loadCallbackMedium: onAdFailedToLoad")
                    splashAdMedium = null
                    statusMedium = Type_Load_Fail

                    if (splashAdHigh == null &&
                        splashAdAll == null &&
                        statusHigh == Type_Load_Fail &&
                        statusAll == Type_Load_Fail
                    ) {
                        if (adListener != null && !isAppOpenShowed) {
                            isAppOpenShowed = true
                            adListener.onNextAction()
                        }
                    }
                }
            }
        }
        loadCallbackMedium = cbMedium

        // load Open Splash All
        val cbAll = object : AdLoadCallback<AppOpenAd> {
            override fun onAdLoaded(appOpenAd: AppOpenAd) {
                runOnUiThread {
                    Log.d(TAG, "loadCallbackAll: onAdLoaded")
                    adListener?.onAdLoadedAll()
                    splashAdAll = appOpenAd
                    statusAll = Type_Load_Success

                    appOpenAd.adEventCallback = object : AppOpenAdEventCallback {
                        override fun onAdClicked() {
                            runOnUiThread {
                                disableAdResumeByClickAction = true
                                NkhLogEventManager.logClickAdsEvent(activity, appOpenAd.adUnitId)
                                adListener?.onAdClickedAll()
                                adListener?.onAdClicked(
                                    appOpenAd.adUnitId,
                                    appOpenAd.getResponseInfo().adapterClassName,
                                    AdType.APP_OPEN
                                )
                            }
                        }

                        override fun onAdDismissedFullScreenContent() {
                            runOnUiThread {
                                adListener?.onNextAction()
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                            runOnUiThread {
                                Log.e(TAG, "onAdFailedToShowFullScreenContent: All")
                                splashAdAll = null
                                statusAll = Type_Load_Fail

                                if (statusHigh == Type_Load_Fail && statusMedium == Type_Load_Fail) {
                                    if (adListener != null && !isAppOpenShowed) {
                                        adListener.onNextAction()
                                    }
                                }

                                adListener?.onAdFailedToShowAll(adError)
                            }
                        }

                        override fun onAdImpression() {
                            runOnUiThread {
                                isAppOpenShowed = true
                                adListener?.onAdImpression()
                            }
                        }

                        override fun onAdShowedFullScreenContent() {}

                        override fun onAdPaid(adValue: AdValue) {
                            runOnUiThread {
                                logAppOpenAdPaid(adValue, appOpenAd, adListener)
                            }
                        }
                    }

                    splashLoadTime = Date().time
                    if ((statusHigh == Type_Load_Fail || statusHigh == Type_Load_Success) &&
                        (statusMedium == Type_Load_Fail || statusMedium == Type_Load_Success) &&
                        !isAppOpenShowed
                    ) {
                        getInstance().setSplashActivity(splashActivity, idOpenAll, timeOutOpen)
                        appOpenAd.show(activity)
                    }
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                runOnUiThread {
                    Log.d(TAG, "loadCallbackAll: onAdFailedToLoad")
                    splashAdAll = null
                    statusAll = Type_Load_Fail

                    if (splashAdHigh == null &&
                        splashAdMedium == null &&
                        statusHigh == Type_Load_Fail &&
                        statusMedium == Type_Load_Fail
                    ) {
                        if (adListener != null && !isAppOpenShowed) {
                            isAppOpenShowed = true
                            adListener.onNextAction()
                        }
                    }
                }
            }
        }
        loadCallbackAll = cbAll

        loadAppOpenAd(getAdRequest(idOpenHigh), cbHigh)
        loadAppOpenAd(getAdRequest(idOpenMedium), cbMedium)
        loadAppOpenAd(getAdRequest(idOpenAll), cbAll)
    }

    fun loadSplashOpenAndInter(
        splashActivity: Class<*>?,
        activity: AppCompatActivity,
        idOpen: String,
        idInter: String,
        timeOutOpen: Int,
        adListener: AdCallback?
    ) {
        isAppOpenShowed = false
        statusOpen = Type_Loading
        statusInter = Type_Loading

        if (AppPurchase.getInstance().isPurchased(activity)) {
            adListener?.onNextAction()
            return
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (adListener != null && !isAppOpenShowed && splashAdOpen == null && splashAdInter == null) {
                isAppOpenShowed = true
                adListener.onNextAction()
            }
        }, timeOutOpen.toLong())

        getInstance().setSplashActivity(splashActivity, idOpen, timeOutOpen)

        val cbOpen = object : AdLoadCallback<AppOpenAd> {
            override fun onAdLoaded(appOpenAd: AppOpenAd) {
                runOnUiThread {
                    Log.d(TAG, "loadCallbackOpen: onAdLoaded")
                    adListener?.onAdLoadedHigh()

                    splashAdOpen = appOpenAd
                    appOpenAd.adEventCallback = object : AppOpenAdEventCallback {
                        override fun onAdClicked() {
                            runOnUiThread {
                                disableAdResumeByClickAction = true
                                NkhLogEventManager.logClickAdsEvent(activity, appOpenAd.adUnitId)
                                adListener?.onAdClickedHigh()
                                adListener?.onAdClicked(
                                    appOpenAd.adUnitId,
                                    appOpenAd.getResponseInfo().adapterClassName,
                                    AdType.APP_OPEN
                                )
                            }
                        }

                        override fun onAdDismissedFullScreenContent() {
                            runOnUiThread {
                                adListener?.onNextAction()
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                            runOnUiThread {
                                Log.e(TAG, "onAdFailedToShowFullScreenContent: Open")
                                statusOpen = Type_Load_Fail
                                splashAdOpen = null

                                val time = timeOutOpen - (System.currentTimeMillis() - currentTime)

                                if (timerListenInter == null) {
                                    timerListenInter = object : CountDownTimer(time, 1000) {
                                        override fun onTick(l: Long) {
                                            if (statusInter == Type_Load_Success && !isAppOpenShowed) {
                                                isAppOpenShowed = true
                                                Admob.getInstance().onShowSplash(activity, adListener, splashAdInter)
                                            } else if (statusInter == Type_Load_Fail && !isAppOpenShowed) {
                                                if (adListener != null) {
                                                    isAppOpenShowed = true
                                                    adListener.onNextAction()
                                                }
                                            }
                                        }

                                        override fun onFinish() {
                                            if (!isAppOpenShowed && adListener != null) {
                                                isAppOpenShowed = true
                                                adListener.onNextAction()
                                            }
                                        }
                                    }.start()
                                }
                            }
                        }

                        override fun onAdImpression() {
                            runOnUiThread {
                                isAppOpenShowed = true
                                adListener?.onAdImpression()
                            }
                        }

                        override fun onAdShowedFullScreenContent() {}

                        override fun onAdPaid(adValue: AdValue) {
                            runOnUiThread {
                                logAppOpenAdPaid(adValue, appOpenAd, adListener)
                            }
                        }
                    }
                    splashLoadTime = Date().time
                    if (!isAppOpenShowed) {
                        (currentActivity ?: activity).let { appOpenAd.show(it) }
                    }
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                runOnUiThread {
                    Log.d(TAG, "loadCallbackOpen: onAdFailedToLoad")
                    statusOpen = Type_Load_Fail
                    splashAdOpen = null

                    val time = timeOutOpen - (System.currentTimeMillis() - currentTime)

                    if (statusInter != Type_Loading) {
                        if (adListener != null && !isAppOpenShowed) {
                            isAppOpenShowed = true
                            adListener.onNextAction()
                        }
                    } else {
                        timerListenInter = object : CountDownTimer(time, 1000) {
                            override fun onTick(l: Long) {
                                if (statusInter == Type_Load_Success && !isAppOpenShowed) {
                                    isAppOpenShowed = true
                                    Admob.getInstance().onShowSplash(activity, adListener, splashAdInter)
                                } else if (statusInter == Type_Load_Fail && !isAppOpenShowed) {
                                    if (adListener != null) {
                                        isAppOpenShowed = true
                                        adListener.onNextAction()
                                    }
                                }
                            }

                            override fun onFinish() {
                                if (!isAppOpenShowed && adListener != null) {
                                    isAppOpenShowed = true
                                    adListener.onNextAction()
                                }
                            }
                        }.start()
                    }
                }
            }
        }
        loadCallbackOpen = cbOpen

        Admob.getInstance().runWhenSdkInitialized(Runnable {
            InterstitialAd.load(
                getAdRequest(idInter),
                object : AdLoadCallback<InterstitialAd> {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        runOnUiThread {
                            statusInter = Type_Load_Success
                            splashAdInter = interstitialAd

                            // Log paid Ads Interstitial
                            interstitialAd.adEventCallback = object : InterstitialAdEventCallback {
                                override fun onAdPaid(adValue: AdValue) {
                                    runOnUiThread {
                                        NkhLogEventManager.logPaidAdImpression(
                                            activity,
                                            adValue,
                                            interstitialAd.adUnitId,
                                            interstitialAd.getResponseInfo().adapterClassName
                                        )
                                        NkhLogEventManager.logPaidAdjustWithToken(
                                            adValue,
                                            interstitialAd.adUnitId,
                                            NkhAdConfig.ADJUST_TOKEN_TIKTOK
                                        )
                                        adListener?.onAdLogRev(
                                            adValue,
                                            interstitialAd.adUnitId,
                                            interstitialAd.getResponseInfo().adapterClassName,
                                            AdType.INTERSTITIAL
                                        )
                                    }
                                }
                            }

                            adListener?.onInterstitialLoad(interstitialAd)
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        runOnUiThread {
                            Log.i(TAG, loadAdError.message)
                            statusInter = Type_Load_Fail
                            splashAdInter = null

                            if (statusOpen == Type_Load_Fail) {
                                if (adListener != null && !isAppOpenShowed) {
                                    isAppOpenShowed = true
                                    adListener.onNextAction()
                                }
                            }
                        }
                    }
                }
            )
        })

        loadAppOpenAd(getAdRequest(idOpen), cbOpen)
        currentTime = System.currentTimeMillis()
    }

    fun loadAndShowSplashAds(aId: String?) {
        loadAndShowSplashAds(aId, 0)
    }

    fun loadAndShowSplashAds(adId: String?, delay: Long) {
        isTimeout = false
        enableScreenContentCallback = true
        val activity = currentActivity
        if (activity != null && AppPurchase.getInstance().isPurchased(activity)) {
            if (fullScreenContentCallback != null && enableScreenContentCallback) {
                Handler(Looper.getMainLooper()).postDelayed({
                    fullScreenContentCallback?.onAdDismissedFullScreenContent()
                }, delay)
            }
            return
        }
        val callback = object : AdLoadCallback<AppOpenAd> {
            override fun onAdLoaded(appOpenAd: AppOpenAd) {
                runOnUiThread {
                    Log.d(TAG, "onAppOpenAdLoaded: splash")
                    timeoutHandler?.removeCallbacks(runnableTimeout)

                    if (isTimeout) {
                        Log.e(TAG, "onAppOpenAdLoaded: splash timeout")
                    } else {
                        this@AppOpenManager.splashAd = appOpenAd
                        splashLoadTime = Date().time
                        appOpenAd.adEventCallback = object : AppOpenAdEventCallback {
                            override fun onAdPaid(adValue: AdValue) {
                                runOnUiThread {
                                    logAppOpenAdPaid(adValue, appOpenAd, null)
                                }
                            }
                        }

                        Handler(Looper.getMainLooper()).postDelayed({
                            showAdIfAvailable(true)
                        }, delay)
                    }
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                runOnUiThread {
                    Log.e(TAG, "onAppOpenAdFailedToLoad: splash ${loadAdError.message}")
                    if (isTimeout) {
                        Log.e(TAG, "onAdFailedToLoad: splash timeout")
                        return@runOnUiThread
                    }
                    if (fullScreenContentCallback != null && enableScreenContentCallback) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            fullScreenContentCallback?.onAdDismissedFullScreenContent()
                        }, delay)
                        enableScreenContentCallback = false
                    }
                }
            }
        }
        loadCallback = callback

        val targetAdId = splashAdId ?: adId
        if (targetAdId != null) {
            val request = getAdRequest(targetAdId)
            loadAppOpenAd(request, callback)
        }

        if (splashTimeout > 0) {
            val handler = Handler(Looper.getMainLooper())
            timeoutHandler = handler
            handler.postDelayed(runnableTimeout, splashTimeout.toLong())
        }
    }

    var runnableTimeout: Runnable = Runnable {
        Log.e(TAG, "timeout load ad ")
        isTimeout = true
        enableScreenContentCallback = false
        fullScreenContentCallback?.onAdDismissedFullScreenContent()
    }

    fun loadAdOpenSplash2id(
        splashActivity: Class<*>?,
        activity: Activity,
        idOpenHigh: String,
        idOpenAll: String,
        timeOutOpen: Int,
        adListener: AdCallback?
    ) {
        if (AppPurchase.getInstance().isPurchased(activity)) {
            adListener?.onNextAction()
            return
        }

        statusHigh = Type_Loading
        statusAll = Type_Loading
        isAppOpenShowed = false
        currentTime = System.currentTimeMillis()

        val actionTimeOut = Runnable {
            Log.d("AppOpenSplash", "getAdSplash time out")
            adListener?.onNextAction()
            isShowingAd = false
        }
        val handleTimeOut = Handler(Looper.getMainLooper())
        handleTimeOut.postDelayed(actionTimeOut, timeOutOpen.toLong())
        getInstance().setSplashActivity(splashActivity, idOpenHigh, timeOutOpen)

        loadAppOpenAd(getAdRequest(idOpenHigh), object : AdLoadCallback<AppOpenAd> {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                runOnUiThread {
                    statusHigh = Type_Load_Fail
                    if (statusAll == Type_Load_Success && !isAppOpenShowed && splashAdAll != null) {
                        Log.d("AppOpenSplash", "onAdFailedToLoad: High")
                        getInstance().setSplashActivity(splashActivity, idOpenAll, timeOutOpen)
                        splashAdAll?.show(activity)
                    }

                    if (statusAll == Type_Load_Fail || statusAll == Type_Show_Fail) {
                        Log.d("AppOpenSplash", "onAdFailedToHigh: High")
                        if (adListener != null && !isAppOpenShowed) {
                            adListener.onNextAction()
                        }
                        handleTimeOut.removeCallbacks(actionTimeOut)
                    }
                }
            }

            override fun onAdLoaded(appOpenAd: AppOpenAd) {
                runOnUiThread {
                    handleTimeOut.removeCallbacks(actionTimeOut)
                    adListener?.onAdLoadedHigh()

                    splashAdHigh = appOpenAd
                    statusHigh = Type_Load_Success

                    appOpenAd.adEventCallback = object : AppOpenAdEventCallback {
                        override fun onAdClicked() {
                            runOnUiThread {
                                disableAdResumeByClickAction = true
                                NkhLogEventManager.logClickAdsEvent(activity, appOpenAd.adUnitId)
                                adListener?.onAdClickedHigh()
                                adListener?.onAdClicked(
                                    appOpenAd.adUnitId,
                                    appOpenAd.getResponseInfo().adapterClassName,
                                    AdType.APP_OPEN
                                )
                            }
                        }

                        override fun onAdDismissedFullScreenContent() {
                            runOnUiThread {
                                if (adListener != null) {
                                    adListener.onNextAction()
                                    Log.d("AppOpenSplash", "onAdDismissedFullScreenContent: vao 1")
                                }
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                            runOnUiThread {
                                if (statusAll == Type_Load_Success && splashAdAll != null && statusHigh != Type_Load_Success) {
                                    getInstance().setSplashActivity(splashActivity, idOpenAll, timeOutOpen)
                                    splashAdAll?.show(activity)
                                    Log.d("AppOpenSplash", "onAdFailedToShowFullScreenContent show All")
                                }
                                timeRemaining = timeOutOpen - (System.currentTimeMillis() - currentTime)
                                statusHigh = Type_Show_Fail
                            }
                        }

                        override fun onAdImpression() {
                            runOnUiThread {
                                isAppOpenShowed = true
                                statusHigh = Type_Show_Success
                                adListener?.onAdImpression()
                            }
                        }

                        override fun onAdShowedFullScreenContent() {}

                        override fun onAdPaid(adValue: AdValue) {
                            runOnUiThread {
                                logAppOpenAdPaid(adValue, appOpenAd, adListener)
                            }
                        }
                    }

                    if (!isAppOpenShowed) {
                        appOpenAd.show(activity)
                        Log.d("AppOpenSplash", "show High")
                    }
                }
            }
        })

        loadAppOpenAd(getAdRequest(idOpenAll), object : AdLoadCallback<AppOpenAd> {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                runOnUiThread {
                    statusAll = Type_Load_Fail
                    if (statusHigh == Type_Load_Fail || statusHigh == Type_Show_Fail) {
                        Log.d("AppOpenSplash", "onAdFailedToLoad: All")
                        if (adListener != null && !isAppOpenShowed) {
                            adListener.onNextAction()
                        }
                        handleTimeOut.removeCallbacks(actionTimeOut)
                    }
                }
            }

            override fun onAdLoaded(appOpenAd: AppOpenAd) {
                runOnUiThread {
                    handleTimeOut.removeCallbacks(actionTimeOut)
                    adListener?.onAdLoadedAll()

                    splashAdAll = appOpenAd
                    statusAll = Type_Load_Success

                    appOpenAd.adEventCallback = object : AppOpenAdEventCallback {
                        override fun onAdClicked() {
                            runOnUiThread {
                                disableAdResumeByClickAction = true
                                NkhLogEventManager.logClickAdsEvent(activity, appOpenAd.adUnitId)
                                adListener?.onAdClickedAll()
                                adListener?.onAdClicked(
                                    appOpenAd.adUnitId,
                                    appOpenAd.getResponseInfo().adapterClassName,
                                    AdType.APP_OPEN
                                )
                            }
                        }

                        override fun onAdDismissedFullScreenContent() {
                            runOnUiThread {
                                if (adListener != null) {
                                    adListener.onNextAction()
                                    Log.d("AppOpenSplash", "onAdDismissedFullScreenContent: vao 2")
                                }
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                            runOnUiThread {
                                if (statusHigh == Type_Load_Fail) {
                                    if (timerListenInter == null) {
                                        timerListenInter = object : CountDownTimer(timeRemaining, 1000) {
                                            override fun onTick(l: Long) {
                                                if (isAppOpenShowed) {
                                                    cancel()
                                                }
                                            }

                                            override fun onFinish() {
                                                if (adListener != null && !isAppOpenShowed) {
                                                    if (statusAll != Type_Load_Success &&
                                                        (statusHigh == Type_Load_Fail || statusHigh == Type_Show_Fail)
                                                    ) {
                                                        adListener.onNextAction()
                                                        Log.d("AppOpenSplash", "onAdFailedToShowFullScreenContentAll: vao 2")
                                                    }
                                                }
                                            }
                                        }.start()
                                    }
                                }
                                statusAll = Type_Show_Fail
                            }
                        }

                        override fun onAdImpression() {
                            runOnUiThread {
                                isAppOpenShowed = true
                                statusAll = Type_Load_Success
                                adListener?.onAdImpression()
                            }
                        }

                        override fun onAdShowedFullScreenContent() {}

                        override fun onAdPaid(adValue: AdValue) {
                            runOnUiThread {
                                logAppOpenAdPaid(adValue, appOpenAd, adListener)
                            }
                        }
                    }

                    if (!isAppOpenShowed && (statusHigh == Type_Load_Fail || statusHigh == Type_Show_Fail)) {
                        getInstance().setSplashActivity(splashActivity, idOpenAll, timeOutOpen)
                        appOpenAd.show(activity)
                        Log.d("AppOpenSplash", "show All")
                    }
                }
            }
        })
    }

    fun onCheckShowAppOpenSplashWhenFail(
        activity: AppCompatActivity,
        callback: AdCallback?,
        timeDelay: Int
    ) {
        Handler(activity.mainLooper).postDelayed({
            if (!isAppOpenShowed) {
                if (splashAdHigh != null && (statusHigh == Type_Load_Fail || statusHigh == Type_Show_Fail)) {
                    splashAd = splashAdHigh
                    showAppOpenSplash(activity, callback)
                    Log.d("AppOpenSplash", "onCheckShowAppOpenSplashWhenFail: vao 1")
                } else if (splashAdAll != null && (statusAll == Type_Load_Fail || statusAll == Type_Show_Fail)) {
                    splashAd = splashAdAll
                    showAppOpenSplash(activity, callback)
                    Log.d("AppOpenSplash", "onCheckShowAppOpenSplashWhenFail: vao 2")
                }
            }
        }, timeDelay.toLong())
    }

    fun showAppOpenSplash(context: Context?, adCallback: AdCallback?) {
        if (splashAd == null) {
            adCallback?.onNextAction()
            Log.d("AppOpenSplash Failed", "splashAd null: vao 2")
            return
        }
        Handler(Looper.getMainLooper()).postDelayed({
            val ad = splashAd ?: run {
                adCallback?.onNextAction()
                return@postDelayed
            }
            ad.adEventCallback = object : AppOpenAdEventCallback {
                override fun onAdDismissedFullScreenContent() {
                    runOnUiThread {
                        adCallback?.onNextAction()
                        isAppOpenShowed = false
                        Log.d("AppOpenSplash Failed", "onAdDismissedFullScreenContent: vao 1")
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                    runOnUiThread {
                        adCallback?.onAdFailedToShow(adError)
                        isAppOpenShowed = false
                    }
                }

                override fun onAdShowedFullScreenContent() {
                    runOnUiThread {
                        adCallback?.onAdImpression()
                        isAppOpenShowed = true
                    }
                }

                override fun onAdClicked() {
                    runOnUiThread {
                        if (context != null) {
                            NkhLogEventManager.logClickAdsEvent(context, splashAdId ?: ad.adUnitId)
                        }
                        adCallback?.onAdClicked()
                        adCallback?.onAdClicked(
                            ad.adUnitId,
                            ad.getResponseInfo().adapterClassName,
                            AdType.APP_OPEN
                        )
                    }
                }

                override fun onAdImpression() {
                    runOnUiThread {
                        adCallback?.onAdImpression()
                    }
                }

                override fun onAdPaid(adValue: AdValue) {
                    runOnUiThread {
                        logAppOpenAdPaid(adValue, ad, adCallback)
                    }
                }
            }
            currentActivity?.let { ad.show(it) }
        }, 800)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onResume() {
        if (!isAppResumeEnabled) {
            Log.d(TAG, "onResume: app resume is disabled")
            return
        }

        if (isInterstitialShowing) {
            Log.d(TAG, "onResume: interstitial is showing")
            return
        }

        if (disableAdResumeByClickAction) {
            Log.d(TAG, "onResume:ad resume disable ad by action")
            disableAdResumeByClickAction = false
            return
        }

        val act = currentActivity ?: return
        for (activity in disabledAppOpenList) {
            if (activity.name == act.javaClass.name) {
                Log.d(TAG, "onStart: activity is disabled")
                return
            }
        }

        val splashCls = splashActivity
        if (splashCls != null && splashCls.name == act.javaClass.name) {
            val adId = splashAdId
            if (adId == null) {
                Log.e(TAG, "splash ad id must not be null")
            }
            Log.d(TAG, "onStart: load and show splash ads")
            loadAndShowSplashAds(adId)
            return
        }

        Log.d(TAG, "onStart: show resume ads :${act.javaClass.name}")
        showAdIfAvailable(false)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        Log.d(TAG, "onStop: app stop")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        Log.d(TAG, "onPause")
    }

    private fun dismissDialogLoading() {
        val dlg = dialog
        if (dlg != null && dlg.isShowing) {
            try {
                dlg.dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadOpenAppAdSplash(
        context: Context,
        idResumeSplash: String,
        timeDelay: Long,
        timeOut: Long,
        isShowAdIfReady: Boolean,
        adCallback: AdCallback
    ) {
        this.splashAdId = idResumeSplash
        if (!isNetworkConnected(context)) {
            Handler(Looper.getMainLooper()).postDelayed({
                adCallback.onAdFailedToLoad(null)
                adCallback.onNextAction()
            }, timeDelay)
        } else {
            val currentTimeMillis = System.currentTimeMillis()
            val timeOutRunnable = Runnable {
                Log.d("AppOpenManager", "getAdSplash time out")
                adCallback.onNextAction()
                isShowingAd = false
            }
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed(timeOutRunnable, timeOut)
            val adRequest = getAdRequest(idResumeSplash)
            val appOpenAdLoadCallback = object : AdLoadCallback<AppOpenAd> {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    runOnUiThread {
                        handler.removeCallbacks(timeOutRunnable)
                        adCallback.onAdFailedToLoad(loadAdError)
                        adCallback.onNextAction()
                    }
                }

                override fun onAdLoaded(appOpenAd: AppOpenAd) {
                    runOnUiThread {
                        handler.removeCallbacks(timeOutRunnable)
                        this@AppOpenManager.splashAd = appOpenAd
                        appOpenLoadCallbacks[appOpenAd] = adCallback
                        appOpenAd.adEventCallback = object : AppOpenAdEventCallback {
                            override fun onAdPaid(adValue: AdValue) {
                                runOnUiThread {
                                    logAppOpenAdPaid(adValue, appOpenAd, adCallback)
                                }
                            }
                        }
                        if (isShowAdIfReady) {
                            var elapsedTime = System.currentTimeMillis() - currentTimeMillis
                            if (elapsedTime >= timeDelay) {
                                elapsedTime = 0L
                            }
                            val handler1 = Handler(Looper.getMainLooper())
                            val showAppOpenSplashRunnable = Runnable {
                                this@AppOpenManager.showAppOpenSplash(context, adCallback)
                            }
                            handler1.postDelayed(showAppOpenSplashRunnable, elapsedTime)
                        } else {
                            adCallback.onAdSplashReady()
                        }
                    }
                }
            }
            loadAppOpenAd(adRequest, appOpenAdLoadCallback)
        }
    }

    fun loadOpenAppAdSplashFloor(
        context: Context,
        listIDResume: MutableList<String>?,
        isShowAdIfReady: Boolean,
        adCallback: AdCallback
    ) {
        if (!isNetworkConnected(context)) {
            Handler(Looper.getMainLooper()).postDelayed({
                adCallback.onAdFailedToLoad(null)
                adCallback.onNextAction()
            }, 3000L)
        } else {
            if (listIDResume == null) {
                adCallback.onAdFailedToLoad(null)
                adCallback.onNextAction()
                return
            }

            if (listIDResume.isNotEmpty()) {
                Log.e("AppOpenManager", "load ID :${listIDResume[0]}")
            }

            if (listIDResume.isEmpty()) {
                adCallback.onAdFailedToLoad(null)
                adCallback.onNextAction()
                return
            }

            val adRequest = getAdRequest(listIDResume[0])
            val appOpenAdLoadCallback = object : AdLoadCallback<AppOpenAd> {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    runOnUiThread {
                        listIDResume.removeAt(0)
                        if (listIDResume.isEmpty()) {
                            adCallback.onAdFailedToLoad(null)
                            adCallback.onNextAction()
                        } else {
                            this@AppOpenManager.loadOpenAppAdSplashFloor(
                                context,
                                listIDResume,
                                isShowAdIfReady,
                                adCallback
                            )
                        }
                    }
                }

                override fun onAdLoaded(appOpenAd: AppOpenAd) {
                    runOnUiThread {
                        this@AppOpenManager.splashAd = appOpenAd
                        appOpenLoadCallbacks[appOpenAd] = adCallback
                        appOpenAd.adEventCallback = object : AppOpenAdEventCallback {
                            override fun onAdPaid(adValue: AdValue) {
                                runOnUiThread {
                                    logAppOpenAdPaid(adValue, appOpenAd, adCallback)
                                }
                            }
                        }
                        if (isShowAdIfReady) {
                            this@AppOpenManager.showAppOpenSplash(context, adCallback)
                        } else {
                            adCallback.onAdSplashReady()
                        }
                    }
                }
            }
            loadAppOpenAd(adRequest, appOpenAdLoadCallback)
        }
    }

    fun onCheckShowSplashWhenFail(
        activity: AppCompatActivity,
        callback: AdCallback?,
        timeDelay: Int
    ) {
        Handler(activity.mainLooper).postDelayed({
            if (this@AppOpenManager.splashAd != null && !isShowingAd) {
                Log.e("AppOpenManager", "show ad splash when show fail in background")
                getInstance().showAppOpenSplash(activity, callback)
            }
        }, timeDelay.toLong())
    }

    private fun isNetworkConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    private var isOpenHigh1Failed = false
    private var isOpenHigh2Loaded = false
    private var isOpenHigh3Loaded = false
    private var isOpenNormalLoaded = false

    fun loadAndShowOpenSplash4SameTime(
        context: Context,
        idOpenHigh1: String,
        idOpenHigh2: String,
        idOpenHigh3: String,
        idOpenNormal: String,
        timeOut: Long,
        adCallback: AdCallback
    ) {
        isOpenHigh1Failed = false
        isOpenHigh2Loaded = false
        isOpenHigh3Loaded = false
        isOpenNormalLoaded = false

        if (!isNetworkConnected(context)) {
            Handler(Looper.getMainLooper()).postDelayed({
                adCallback.onAdFailedToLoad(null)
                adCallback.onNextAction()
            }, 3000L)
        } else {
            // Load Open High 1
            loadOpenHigh1(context, idOpenHigh1, timeOut, object : AdCallback() {
                override fun onAdSplashReady() {
                    super.onAdSplashReady()
                    Log.d("LuanDev", "onAdSplashReady: 1")
                    adCallback.onAdSplashHigh1Ready()
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    adCallback.onAdPriorityFailedToLoad(i)
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

                override fun onNextAction() {
                    super.onNextAction()
                    if (!isOpenHigh1Failed) {
                        if (isOpenHigh2Loaded && mOpenSplashHigh2 != null) {
                            adCallback.onAdSplashHigh2Ready()
                            isOpenHigh1Failed = true
                            Log.d("LuanDev", "onAdSplashReady: 2")
                        } else if (isOpenHigh3Loaded && mOpenSplashHigh3 != null) {
                            adCallback.onAdSplashHigh3Ready()
                            isOpenHigh1Failed = true
                            Log.d("LuanDev", "onAdSplashReady: 4")
                        } else if (isOpenHigh3Loaded && isOpenNormalLoaded) {
                            adCallback.onAdSplashNormalReady()
                            Log.d("LuanDev", "onAdSplashReady: 7")
                            isOpenHigh1Failed = true
                        } else {
                            // waiting for ads loaded
                            isOpenHigh1Failed = true
                        }
                    }
                }
            })

            loadOpenHigh2(context, idOpenHigh2, timeOut, object : AdCallback() {
                override fun onAdSplashReady() {
                    super.onAdSplashReady()
                    if (isOpenHigh1Failed) {
                        adCallback.onAdSplashHigh2Ready()
                        Log.d("LuanDev", "onAdSplashReady: 3")
                    } else {
                        isOpenHigh2Loaded = true
                    }
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

                override fun onNextAction() {
                    super.onNextAction()
                    if (isOpenHigh1Failed && !isOpenHigh2Loaded) {
                        if (isOpenHigh3Loaded && mOpenSplashHigh3 != null) {
                            adCallback.onAdSplashHigh3Ready()
                            isOpenHigh2Loaded = true
                            Log.d("LuanDev", "onAdSplashReady: 5")
                        } else if (isOpenNormalLoaded && mOpenSplashNormal != null) {
                            adCallback.onAdSplashNormalReady()
                            isOpenHigh2Loaded = true
                            Log.d("LuanDev", "onAdSplashReady: 8")
                        } else {
                            isOpenHigh2Loaded = true
                        }
                    } else {
                        isOpenHigh2Loaded = true
                    }
                }
            })

            loadOpenHigh3(context, idOpenHigh3, timeOut, object : AdCallback() {
                override fun onAdSplashReady() {
                    super.onAdSplashReady()
                    if (isOpenHigh1Failed && isOpenHigh2Loaded) {
                        adCallback.onAdSplashHigh3Ready()
                        Log.d("LuanDev", "onAdSplashReady: 6")
                    } else {
                        isOpenHigh3Loaded = true
                    }
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

                override fun onNextAction() {
                    super.onNextAction()
                    if (isOpenHigh1Failed && isOpenHigh2Loaded && !isOpenHigh3Loaded) {
                        if (isOpenNormalLoaded && mOpenSplashNormal != null) {
                            adCallback.onAdSplashNormalReady()
                            isOpenHigh3Loaded = true
                            Log.d("LuanDev", "onAdSplashReady: 9")
                        } else {
                            isOpenHigh3Loaded = true
                        }
                    } else {
                        isOpenHigh3Loaded = true
                    }
                }
            })

            loadOpenNormal(context, idOpenNormal, timeOut, object : AdCallback() {
                override fun onAdSplashReady() {
                    super.onAdSplashReady()
                    if (isOpenHigh1Failed && isOpenHigh2Loaded && isOpenHigh3Loaded) {
                        adCallback.onAdSplashNormalReady()
                        Log.d("LuanDev", "onAdSplashReady: 10")
                    } else {
                        isOpenNormalLoaded = true
                    }
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

                override fun onNextAction() {
                    super.onNextAction()
                    if (isOpenHigh1Failed && isOpenHigh2Loaded && isOpenHigh3Loaded) {
                        adCallback.onAdSplashNormalReady()
                        isOpenHigh1Failed = true
                        isOpenHigh2Loaded = true
                        isOpenHigh3Loaded = true
                        isOpenNormalLoaded = true
                        Log.d("LuanDev", "onAdSplashReady: 11")
                    } else {
                        isOpenNormalLoaded = true
                    }
                }
            })
        }
    }

    private var isFailedPriority = false

    private fun createSplashPriorityCallback(
        adListener: AdCallback?,
        onFailed: (FullScreenContentError?) -> Unit,
        onNext: () -> Unit
    ): AdCallback {
        return object : AdCallback() {
            override fun onAdFailedToShow(adError: FullScreenContentError?) {
                super.onAdFailedToShow(adError)
                onFailed(adError)
            }

            override fun onNextAction() {
                super.onNextAction()
                onNext()
            }

            override fun onAdImpression() {
                super.onAdImpression()
                adListener?.onAdImpression()
            }

            override fun onAdClicked() {
                super.onAdClicked()
                adListener?.onAdClicked()
            }

            override fun onAdClicked(
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                adListener?.onAdClicked(adUnitId, mediationAdapterClassName, adType)
            }

            override fun onAdLogRev(
                adValue: AdValue?,
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                adListener?.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
            }
        }
    }

    fun showOpenSplash4SameTime(activity: AppCompatActivity, adListener: AdCallback?) {
        isFailedPriority = false
        if (mOpenSplashHigh1 != null) {
            onShowSplashHigh1(
                activity,
                createSplashPriorityCallback(
                    adListener,
                    onFailed = {
                        isFailedPriority = true
                        onShowSplashHigh2(
                            activity,
                            createSplashPriorityCallback(
                                adListener,
                                onFailed = {
                                    isFailedPriority = true
                                    onShowSplashHigh3(
                                        activity,
                                        createSplashPriorityCallback(
                                            adListener,
                                            onFailed = {
                                                isFailedPriority = true
                                                onShowSplashNormal(
                                                    activity,
                                                    createSplashPriorityCallback(
                                                        adListener,
                                                        onFailed = { err -> adListener?.onAdFailedToShow(err) },
                                                        onNext = {
                                                            adListener?.onNextAction()
                                                            Log.d("LuanDev", "onAdFailedToShow: 41")
                                                        }
                                                    )
                                                )
                                            },
                                            onNext = {
                                                if (!isFailedPriority) {
                                                    Log.d("LuanDev", "onAdFailedToShow: 31")
                                                    adListener?.onNextAction()
                                                }
                                            }
                                        )
                                    )
                                },
                                onNext = {
                                    if (!isFailedPriority) {
                                        Log.d("LuanDev", "onAdFailedToShow: 21")
                                        adListener?.onNextAction()
                                    }
                                }
                            )
                        )
                    },
                    onNext = {
                        if (!isFailedPriority) {
                            Log.d("LuanDev", "onAdFailedToShow: 11")
                            adListener?.onNextAction()
                        }
                    }
                )
            )
        } else if (mOpenSplashHigh2 != null) {
            onShowSplashHigh2(
                activity,
                createSplashPriorityCallback(
                    adListener,
                    onFailed = {
                        isFailedPriority = true
                        onShowSplashHigh3(
                            activity,
                            createSplashPriorityCallback(
                                adListener,
                                onFailed = {
                                    isFailedPriority = true
                                    onShowSplashNormal(
                                        activity,
                                        createSplashPriorityCallback(
                                            adListener,
                                            onFailed = { err -> adListener?.onAdFailedToShow(err) },
                                            onNext = {
                                                adListener?.onNextAction()
                                                Log.d("LuanDev", "onAdFailedToShow: 42")
                                            }
                                        )
                                    )
                                },
                                onNext = {
                                    if (!isFailedPriority) {
                                        adListener?.onNextAction()
                                        Log.d("LuanDev", "onAdFailedToShow: 32")
                                    }
                                }
                            )
                        )
                    },
                    onNext = {
                        if (!isFailedPriority) {
                            adListener?.onNextAction()
                            Log.d("LuanDev", "onAdFailedToShow: 22")
                        }
                    }
                )
            )
        } else if (mOpenSplashHigh3 != null) {
            onShowSplashHigh3(
                activity,
                createSplashPriorityCallback(
                    adListener,
                    onFailed = {
                        isFailedPriority = true
                        onShowSplashNormal(
                            activity,
                            createSplashPriorityCallback(
                                adListener,
                                onFailed = { err -> adListener?.onAdFailedToShow(err) },
                                onNext = {
                                    adListener?.onNextAction()
                                    Log.d("LuanDev", "onAdFailedToShow: 43")
                                }
                            )
                        )
                    },
                    onNext = {
                        if (!isFailedPriority) {
                            adListener?.onNextAction()
                            Log.d("LuanDev", "onAdFailedToShow: 33")
                        }
                    }
                )
            )
        } else if (mOpenSplashNormal != null) {
            onShowSplashNormal(
                activity,
                createSplashPriorityCallback(
                    adListener,
                    onFailed = { err -> adListener?.onAdFailedToShow(err) },
                    onNext = {
                        adListener?.onNextAction()
                        Log.d("LuanDev", "onAdFailedToShow: 44")
                    }
                )
            )
        } else {
            adListener?.onNextAction()
            Log.d("LuanDev", "onAdFailedToShow: 55")
        }
    }

    private var mOpenSplashHigh1: AppOpenAd? = null
    private var handlerTimeoutHigh1: Handler? = null
    private var rdTimeoutHigh1: Runnable? = null

    private var mOpenSplashHigh2: AppOpenAd? = null
    private var handlerTimeoutHigh2: Handler? = null
    private var rdTimeoutHigh2: Runnable? = null

    private var mOpenSplashHigh3: AppOpenAd? = null
    private var handlerTimeoutHigh3: Handler? = null
    private var rdTimeoutHigh3: Runnable? = null

    private var mOpenSplashNormal: AppOpenAd? = null
    private var handlerTimeoutNormal: Handler? = null
    private var rdTimeoutNormal: Runnable? = null

    private fun loadOpenPrioritySlot(
        context: Context,
        adUnitId: String,
        timeOut: Long,
        adCallback: AdCallback?,
        onSaveTimeout: (Handler, Runnable) -> Unit,
        onMarkFailedAndCheckAllDone: () -> Boolean,
        onSaveLoadedAd: (AppOpenAd) -> Unit
    ) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            adCallback?.onNextAction()
            return
        }

        val timeoutRunnable = Runnable {
            Log.d("AppOpenManager", "getAdSplash time out")
            adCallback?.onNextAction()
        }
        val handler = Handler(Looper.getMainLooper())
        onSaveTimeout(handler, timeoutRunnable)
        handler.postDelayed(timeoutRunnable, timeOut)

        val adRequest = getAdRequest(adUnitId)
        val appOpenAdLoadCallback = object : AdLoadCallback<AppOpenAd> {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                runOnUiThread {
                    if (onMarkFailedAndCheckAllDone()) {
                        handler.removeCallbacks(timeoutRunnable)
                        adCallback?.onNextAction()
                    }
                }
            }

            override fun onAdLoaded(appOpenAd: AppOpenAd) {
                runOnUiThread {
                    onSaveLoadedAd(appOpenAd)
                    if (adCallback != null) {
                        appOpenLoadCallbacks[appOpenAd] = adCallback
                    }
                    handler.removeCallbacks(timeoutRunnable)
                    appOpenAd.adEventCallback = object : AppOpenAdEventCallback {
                        override fun onAdPaid(adValue: AdValue) {
                            runOnUiThread {
                                logAppOpenAdPaid(adValue, appOpenAd, adCallback)
                            }
                        }
                    }
                    adCallback?.onAdSplashReady()
                }
            }
        }
        loadAppOpenAd(adRequest, appOpenAdLoadCallback)
    }

    private fun showSplashPrioritySlot(
        activity: AppCompatActivity,
        adCallback: AdCallback?,
        getAd: () -> AppOpenAd?,
        clearAd: () -> Unit,
        timeoutHandler: Handler?,
        timeoutRunnable: Runnable?,
        clearOnShowed: Boolean = false
    ) {
        if (getAd() == null) {
            adCallback?.onNextAction()
            return
        }

        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val ad = getAd() ?: run {
                adCallback?.onNextAction()
                return@postDelayed
            }
            ad.adEventCallback = object : AppOpenAdEventCallback {
                override fun onAdDismissedFullScreenContent() {
                    runOnUiThread {
                        clearAd()
                        adCallback?.onNextAction()
                        Log.d("AppOpenSplash Failed", "onAdDismissedFullScreenContent: vao 1")
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                    runOnUiThread {
                        adCallback?.onAdFailedToShow(adError)
                        clearAd()
                    }
                }

                override fun onAdShowedFullScreenContent() {
                    runOnUiThread {
                        adCallback?.onAdImpression()
                        if (clearOnShowed) {
                            clearAd()
                        }
                    }
                }

                override fun onAdClicked() {
                    runOnUiThread {
                        NkhLogEventManager.logClickAdsEvent(activity, ad.adUnitId)
                        adCallback?.onAdClicked()
                        adCallback?.onAdClicked(
                            ad.adUnitId,
                            ad.getResponseInfo().adapterClassName,
                            AdType.APP_OPEN
                        )
                    }
                }

                override fun onAdImpression() {
                    runOnUiThread {
                        adCallback?.onAdImpression()
                    }
                }

                override fun onAdPaid(adValue: AdValue) {
                    runOnUiThread {
                        logAppOpenAdPaid(adValue, ad, adCallback)
                    }
                }
            }
            (currentActivity ?: activity).let { ad.show(it) }
        }, 800)
    }

    private fun loadOpenHigh1(context: Context, isOpenHigh1: String, timeOut: Long, adCallback: AdCallback?) {
        loadOpenPrioritySlot(
            context = context,
            adUnitId = isOpenHigh1,
            timeOut = timeOut,
            adCallback = adCallback,
            onSaveTimeout = { h, r ->
                handlerTimeoutHigh1 = h
                rdTimeoutHigh1 = r
            },
            onMarkFailedAndCheckAllDone = {
                isOpenHigh1Failed = true
                isOpenHigh2Loaded && isOpenHigh3Loaded && isOpenNormalLoaded
            },
            onSaveLoadedAd = { mOpenSplashHigh1 = it }
        )
    }

    private fun onShowSplashHigh1(activity: AppCompatActivity, adCallback: AdCallback?) {
        showSplashPrioritySlot(
            activity = activity,
            adCallback = adCallback,
            getAd = { mOpenSplashHigh1 },
            clearAd = { mOpenSplashHigh1 = null },
            timeoutHandler = handlerTimeoutHigh1,
            timeoutRunnable = rdTimeoutHigh1
        )
    }

    private fun loadOpenHigh2(context: Context, isOpenHigh2: String, timeOut: Long, adCallback: AdCallback?) {
        loadOpenPrioritySlot(
            context = context,
            adUnitId = isOpenHigh2,
            timeOut = timeOut,
            adCallback = adCallback,
            onSaveTimeout = { h, r ->
                handlerTimeoutHigh2 = h
                rdTimeoutHigh2 = r
            },
            onMarkFailedAndCheckAllDone = {
                isOpenHigh2Loaded = true
                isOpenHigh1Failed && isOpenHigh3Loaded && isOpenNormalLoaded
            },
            onSaveLoadedAd = { mOpenSplashHigh2 = it }
        )
    }

    private fun onShowSplashHigh2(activity: AppCompatActivity, adCallback: AdCallback?) {
        showSplashPrioritySlot(
            activity = activity,
            adCallback = adCallback,
            getAd = { mOpenSplashHigh2 },
            clearAd = { mOpenSplashHigh2 = null },
            timeoutHandler = handlerTimeoutHigh2,
            timeoutRunnable = rdTimeoutHigh2
        )
    }

    private fun loadOpenHigh3(context: Context, isOpenHigh3: String, timeOut: Long, adCallback: AdCallback?) {
        loadOpenPrioritySlot(
            context = context,
            adUnitId = isOpenHigh3,
            timeOut = timeOut,
            adCallback = adCallback,
            onSaveTimeout = { h, r ->
                handlerTimeoutHigh3 = h
                rdTimeoutHigh3 = r
            },
            onMarkFailedAndCheckAllDone = {
                isOpenHigh3Loaded = true
                isOpenHigh1Failed && isOpenHigh2Loaded && isOpenNormalLoaded
            },
            onSaveLoadedAd = { mOpenSplashHigh3 = it }
        )
    }

    private fun onShowSplashHigh3(activity: AppCompatActivity, adCallback: AdCallback?) {
        showSplashPrioritySlot(
            activity = activity,
            adCallback = adCallback,
            getAd = { mOpenSplashHigh3 },
            clearAd = { mOpenSplashHigh3 = null },
            timeoutHandler = handlerTimeoutHigh3,
            timeoutRunnable = rdTimeoutHigh3
        )
    }

    private fun loadOpenNormal(context: Context, isOpenNormal: String, timeOut: Long, adCallback: AdCallback?) {
        loadOpenPrioritySlot(
            context = context,
            adUnitId = isOpenNormal,
            timeOut = timeOut,
            adCallback = adCallback,
            onSaveTimeout = { h, r ->
                handlerTimeoutNormal = h
                rdTimeoutNormal = r
            },
            onMarkFailedAndCheckAllDone = {
                isOpenNormalLoaded = true
                isOpenHigh1Failed && isOpenHigh2Loaded && isOpenHigh3Loaded
            },
            onSaveLoadedAd = { mOpenSplashNormal = it }
        )
    }

    private fun onShowSplashNormal(activity: AppCompatActivity, adCallback: AdCallback?) {
        showSplashPrioritySlot(
            activity = activity,
            adCallback = adCallback,
            getAd = { mOpenSplashNormal },
            clearAd = { mOpenSplashNormal = null },
            timeoutHandler = handlerTimeoutNormal,
            timeoutRunnable = rdTimeoutNormal,
            clearOnShowed = true
        )
    }

    companion object {
        private const val TAG = "AppOpenManager"
        const val AD_UNIT_ID_TEST: String = "ca-app-pub-3940256099942544/9257395921"

        @Volatile
        private var INSTANCE: AppOpenManager? = null

        private var isShowingAd: Boolean = false

        @JvmStatic
        @Synchronized
        fun getInstance(): AppOpenManager {
            return INSTANCE ?: AppOpenManager().also { INSTANCE = it }
        }

        private fun runOnUiThread(runnable: Runnable) {
            Handler(Looper.getMainLooper()).post(runnable)
        }
    }
}
