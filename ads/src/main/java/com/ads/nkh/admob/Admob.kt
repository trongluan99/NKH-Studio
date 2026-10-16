package com.ads.nkh.admob

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.ads.nkh.R
import com.ads.nkh.ads.NkhAd
import com.ads.nkh.ads.native_ads.NativeAdConfig
import com.ads.nkh.ads.wrapper.ApPipAd
import com.ads.nkh.ads.wrapper.ApSwipeableAd
import com.ads.nkh.billing.AppPurchase
import com.ads.nkh.config.NkhAdConfig
import com.ads.nkh.dialog.PrepareLoadingAdsDialog
import com.ads.nkh.event.NkhLogEventManager
import com.ads.nkh.funtion.AdCallback
import com.ads.nkh.funtion.AdType
import com.ads.nkh.funtion.AdmobHelper
import com.ads.nkh.funtion.RewardCallback
import com.ads.nkh.util.AdDebugDialog
import com.ads.nkh.util.AppConstant
import com.ads.nkh.util.AppUtil
import com.ads.nkh.util.SharePreferenceUtils
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration
import com.google.android.libraries.ads.mobile.sdk.initialization.AdapterStatus
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAd
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAdOptions
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAdPosition
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAdPresentationScope
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAdRequest
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.swipeableinterstitial.SwipeableInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.swipeableinterstitial.SwipeableInterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.swipeableinterstitial.SwipeableInterstitialAdRequest
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Arrays
import java.util.Calendar
import java.util.Collections
import java.util.Locale
import java.util.Objects
import java.util.WeakHashMap
import java.util.concurrent.Executors

class Admob private constructor() {

    private var currentClicked = 0
    private var nativeId: String = ""
    private var numShowAds = 3
    private var maxClickAds = 100
    private var handlerTimeout: Handler? = null
    private var rdTimeout: Runnable? = null
    private var dialog: PrepareLoadingAdsDialog? = null
    private var isTimeout = false
    private var disableAdResumeWhenClickAds = false
    private var isShowLoadingSplash = false
    @JvmField
    var isTimeDelay = false
    private var openActivityAfterShowInterAds = false
    private var context: Context? = null

    @Volatile
    private var isSdkInitStarted = false
    @Volatile
    private var isSdkInitialized = false
    @Volatile
    var lastInitializationStatus: com.google.android.libraries.ads.mobile.sdk.initialization.InitializationStatus? = null
        private set
    @Volatile
    var lastInitLatencyMs: Long = 0L
        private set
    @Volatile
    private var biddingStatusListener: Runnable? = null

    data class BiddingNetworkInfo(
        val networkName: String,
        val adapterClassName: String,
        val isIntegrated: Boolean,
        val isRtbBiddingSupported: Boolean,
        val adapterVersion: String,
        val sdkVersion: String,
        val serverState: String,
        val serverLatencyMs: Int,
        val serverDescription: String
    )

    fun setOnBiddingStatusUpdatedListener(listener: Runnable?) {
        this.biddingStatusListener = listener
    }

    fun inspectBiddingNetworks(): List<BiddingNetworkInfo> {
        val statusMap = lastInitializationStatus?.adapterStatusMap ?: emptyMap()
        val results = ArrayList<BiddingNetworkInfo>()

        // 1. Google AdMob / GMA Next-Gen SDK Core
        val gmaClass = "com.google.android.libraries.ads.mobile.sdk.MobileAds"
        val gmaStatus = statusMap[gmaClass]
        val gmaVer = try {
            MobileAds.getVersion().toString()
        } catch (_: Throwable) {
            "1.5.0"
        }
        results.add(
            BiddingNetworkInfo(
                networkName = "Google AdMob (GMA Next-Gen)",
                adapterClassName = gmaClass,
                isIntegrated = true,
                isRtbBiddingSupported = true,
                adapterVersion = gmaVer,
                sdkVersion = gmaVer,
                serverState = gmaStatus?.initializationState?.name ?: if (isSdkInitialized) "READY" else "INITIALIZING",
                serverLatencyMs = gmaStatus?.latency ?: 0,
                serverDescription = gmaStatus?.description ?: "Core GMA Next-Gen SDK"
            )
        )

        // 2..8: All 7 Mediation & RTB Bidding Networks
        val networks = listOf(
            "Meta Audience Network (Facebook)" to listOf("com.google.ads.mediation.facebook.FacebookMediationAdapter"),
            "Mintegral (Bidding & Waterfall)" to listOf("com.google.ads.mediation.mintegral.MintegralMediationAdapter"),
            "Pangle / TikTok (Bidding & Waterfall)" to listOf("com.google.ads.mediation.pangle.PangleMediationAdapter"),
            "AppLovin (Bidding & Waterfall)" to listOf("com.google.ads.mediation.applovin.AppLovinMediationAdapter"),
            "Liftoff Monetize / Vungle (Bidding)" to listOf("com.google.ads.mediation.vungle.VungleMediationAdapter"),
            "Unity Ads (Bidding & Waterfall)" to listOf(
                "com.google.ads.mediation.unity.UnityMediationAdapter",
                "com.google.ads.mediation.unity.UnityAdapter"
            ),
            "ironSource / LevelPlay (Bidding)" to listOf("com.google.ads.mediation.ironsource.IronSourceMediationAdapter")
        )

        for ((displayName, candidateClasses) in networks) {
            var loadedClass: Class<*>? = null
            var resolvedClassName = candidateClasses.first()
            for (clsName in candidateClasses) {
                try {
                    loadedClass = Class.forName(clsName)
                    resolvedClassName = clsName
                    break
                } catch (_: Throwable) {
                }
            }

            var isIntegrated = false
            var isRtb = false
            var adapterVer = "N/A"
            var sdkVer = "N/A"

            if (loadedClass != null) {
                isIntegrated = true
                var curr: Class<*>? = loadedClass
                while (curr != null) {
                    if (curr.name.endsWith("RtbAdapter")) {
                        isRtb = true
                        break
                    }
                    curr = curr.superclass
                }
                if (!isRtb) {
                    isRtb = loadedClass.methods.any { it.name == "collectSignals" }
                }
                try {
                    val instance = loadedClass.getDeclaredConstructor().newInstance()
                    val vInfoObj = loadedClass.getMethod("getVersionInfo").invoke(instance)
                    if (vInfoObj != null) {
                        val vCls = vInfoObj.javaClass
                        val major = vCls.getMethod("getMajorVersion").invoke(vInfoObj)
                        val minor = vCls.getMethod("getMinorVersion").invoke(vInfoObj)
                        val micro = vCls.getMethod("getMicroVersion").invoke(vInfoObj)
                        adapterVer = "$major.$minor.$micro"
                    }
                    val sInfoObj = loadedClass.getMethod("getSDKVersionInfo").invoke(instance)
                    if (sInfoObj != null) {
                        val sCls = sInfoObj.javaClass
                        val major = sCls.getMethod("getMajorVersion").invoke(sInfoObj)
                        val minor = sCls.getMethod("getMinorVersion").invoke(sInfoObj)
                        val micro = sCls.getMethod("getMicroVersion").invoke(sInfoObj)
                        sdkVer = "$major.$minor.$micro"
                    }
                } catch (_: Throwable) {
                }
            }

            val srvStatus = statusMap[resolvedClassName]
            results.add(
                BiddingNetworkInfo(
                    networkName = displayName,
                    adapterClassName = resolvedClassName,
                    isIntegrated = isIntegrated,
                    isRtbBiddingSupported = isRtb,
                    adapterVersion = adapterVer,
                    sdkVersion = sdkVer,
                    serverState = srvStatus?.initializationState?.name ?: if (isIntegrated) "LINKED_READY" else "MISSING",
                    serverLatencyMs = srvStatus?.latency ?: 0,
                    serverDescription = srvStatus?.description ?: ""
                )
            )
        }

        return results
    }

    private val sdkInitLock = Any()
    private val pendingAdActions = ArrayList<Runnable>()
    private val sdkInitExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "NkhAd-SdkInit").apply {
            isDaemon = true
            priority = Thread.MAX_PRIORITY
        }
    }
    private val bgExecutor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "NkhAd-BgWorker").apply {
            isDaemon = true
        }
    }

    @JvmField
    var mInterstitialSplash: InterstitialAd? = null
    @JvmField
    var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var currentPipAd: PictureInPictureAd? = null
    private var currentSwipeableAd: SwipeableInterstitialAd? = null

    private val interstitialLoadCallbacks: MutableMap<InterstitialAd, AdCallback?> =
        Collections.synchronizedMap(WeakHashMap())
    private val rewardLoadCallbacks: MutableMap<Any, Any?> =
        Collections.synchronizedMap(WeakHashMap())
    private val pipLoadCallbacks: MutableMap<PictureInPictureAd, AdCallback?> =
        Collections.synchronizedMap(WeakHashMap())
    private val swipeableLoadCallbacks: MutableMap<SwipeableInterstitialAd, AdCallback?> =
        Collections.synchronizedMap(WeakHashMap())

    fun setMaxClickAdsPerDay(maxClickAds: Int) {
        this.maxClickAds = maxClickAds
    }

    fun setNumToShowAds(numShowAds: Int) {
        this.numShowAds = numShowAds
    }

    fun setNumToShowAds(numShowAds: Int, currentClicked: Int) {
        this.numShowAds = numShowAds
        this.currentClicked = currentClicked
    }

    fun setDisableAdResumeWhenClickAds(disableAdResumeWhenClickAds: Boolean) {
        this.disableAdResumeWhenClickAds = disableAdResumeWhenClickAds
    }

    fun runOnBackgroundThread(action: Runnable) {
        bgExecutor.execute(action)
    }

    fun runWhenSdkInitialized(action: Runnable) {
        val runImmediate: Boolean
        synchronized(sdkInitLock) {
            if (isSdkInitialized || !isSdkInitStarted) {
                runImmediate = true
            } else {
                pendingAdActions.add(action)
                runImmediate = false
            }
        }
        if (runImmediate) {
            action.run()
        }
    }

    private fun completeSdkInitAndDrainPendingAds() {
        val actionsToRun: List<Runnable>
        synchronized(sdkInitLock) {
            if (isSdkInitialized) {
                return
            }
            isSdkInitialized = true
            actionsToRun = ArrayList(pendingAdActions)
            pendingAdActions.clear()
        }
        for (pending in actionsToRun) {
            try {
                pending.run()
            } catch (t: Throwable) {
                Log.e(TAG, "Error running pending ad action: ${t.message}", t)
            }
        }
    }

    fun init(context: Context, testDeviceList: List<String>? = null, tokenAdjust: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = Application.getProcessName()
            val packageName = context.packageName
            if (packageName != processName) {
                try {
                    WebView.setDataDirectorySuffix(processName)
                } catch (ignored: Exception) {
                }
            }
        }
        NkhAdConfig.ADJUST_TOKEN_TIKTOK = tokenAdjust ?: ""
        val appContext = context.applicationContext ?: context
        this.context = appContext
        val deviceListCopy = testDeviceList?.toList()

        synchronized(sdkInitLock) {
            isSdkInitStarted = true
        }

        // Google GMA Next-Gen SDK initializes its internal DI container and multi-threaded
        // background coroutines (GMA(BG)) directly inside MobileAds.initialize().
        // Running MobileAds.initialize() immediately on a dedicated high-priority background
        // thread without a sequential WebSettings.getDefaultUserAgent() pre-call avoids
        // blocking SDK init for ~1.4s on cold start, while waiting for OnAdapterInitializationCompleteListener
        // ensures all Mediation & RTB Bidding adapters (Meta, Mintegral, Pangle, AppLovin, Liftoff,
        // Unity, ironSource) are READY before dispatching the first Splash/Onboarding ad request.
        val initStartMs = System.currentTimeMillis()
        sdkInitExecutor.execute {
            try {
                val appId = readManifestApplicationId(appContext)
                val initConfigBuilder = InitializationConfig.Builder(appId)
                if (!deviceListCopy.isNullOrEmpty()) {
                    initConfigBuilder.setRequestConfiguration(
                        RequestConfiguration.Builder().setTestDeviceIds(deviceListCopy).build()
                    )
                }
                MobileAds.initialize(appContext, initConfigBuilder.build()) { initializationStatus ->
                    val totalLatency = System.currentTimeMillis() - initStartMs
                    lastInitializationStatus = initializationStatus
                    val statusMap = initializationStatus.adapterStatusMap
                    val gmaCoreLatency = statusMap["com.google.android.libraries.ads.mobile.sdk.MobileAds"]?.latency
                    if (gmaCoreLatency != null && gmaCoreLatency > 0) {
                        lastInitLatencyMs = gmaCoreLatency.toLong()
                    } else if (lastInitLatencyMs <= 0L) {
                        lastInitLatencyMs = totalLatency
                    }
                    Log.d(TAG, "GMA Next-Gen SDK & Bidding adapters initialized in ${totalLatency}ms (GMA Core: ${lastInitLatencyMs}ms)")
                    for ((adapterClass, status) in statusMap) {
                        Log.d(
                            TAG,
                            String.format(
                                "Adapter name: %s, Description: %s, Latency: %d",
                                adapterClass,
                                status.description,
                                status.latency
                            )
                        )
                    }
                    biddingStatusListener?.let { listener ->
                        runOnUiThread(listener)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "MobileAds.initialize error: ${e.message}", e)
            } finally {
                if (lastInitLatencyMs <= 0L) {
                    lastInitLatencyMs = (System.currentTimeMillis() - initStartMs).coerceAtLeast(1L)
                }
                completeSdkInitAndDrainPendingAds()
            }
        }
    }

    fun init(context: Context, tokenAdjust: String?) {
        init(context, null, tokenAdjust)
    }

    fun isShowLoadingSplash(): Boolean = isShowLoadingSplash

    private fun getProcessName(context: Context?): String? {
        if (context == null) return null
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        manager?.runningAppProcesses?.forEach { processInfo ->
            if (processInfo.pid == android.os.Process.myPid()) {
                return processInfo.processName
            }
        }
        return null
    }

    fun setOpenActivityAfterShowInterAds(openActivityAfterShowInterAds: Boolean) {
        this.openActivityAfterShowInterAds = openActivityAfterShowInterAds
    }

    fun getAdRequest(adUnitId: String): AdRequest {
        return AdRequest.Builder(adUnitId).build()
    }

    private fun handleInterstitialPaid(
        logCtx: Context?,
        ad: InterstitialAd,
        adValue: AdValue,
        showCallback: AdCallback?
    ) {
        val adapterName = ad.getResponseInfo().adapterClassName
        if (logCtx != null) {
            NkhLogEventManager.logPaidAdImpression(
                logCtx,
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
        showCallback?.onAdLogRev(
            adValue,
            ad.adUnitId,
            adapterName,
            AdType.INTERSTITIAL
        )
        val loadCb = interstitialLoadCallbacks[ad]
        if (loadCb != null && loadCb !== showCallback) {
            loadCb.onAdLogRev(
                adValue,
                ad.adUnitId,
                adapterName,
                AdType.INTERSTITIAL
            )
        }
    }

    private fun handleRewardPaid(
        logCtx: Context,
        ad: Any,
        adUnitId: String,
        adapterName: String?,
        adValue: AdValue,
        showCallback: Any?
    ) {
        NkhLogEventManager.logPaidAdImpression(
            logCtx,
            adValue,
            adUnitId,
            adapterName
        )
        NkhLogEventManager.logPaidAdjustWithToken(
            adValue,
            adUnitId,
            NkhAdConfig.ADJUST_TOKEN_TIKTOK
        )
        when (showCallback) {
            is RewardCallback -> showCallback.onAdLogRev(adValue, adUnitId, adapterName, AdType.REWARDED)
            is AdCallback -> showCallback.onAdLogRev(adValue, adUnitId, adapterName, AdType.REWARDED)
        }
        val loadCb = rewardLoadCallbacks[ad]
        if (loadCb != null && loadCb !== showCallback) {
            when (loadCb) {
                is RewardCallback -> loadCb.onAdLogRev(adValue, adUnitId, adapterName, AdType.REWARDED)
                is AdCallback -> loadCb.onAdLogRev(adValue, adUnitId, adapterName, AdType.REWARDED)
            }
        }
    }

    private fun registerInterstitialPaidListener(
        ad: InterstitialAd,
        ctx: Context?,
        loadCallback: AdCallback? = null
    ) {
        if (loadCallback != null) {
            interstitialLoadCallbacks[ad] = loadCallback
        }
        val logCtx = ctx ?: this.context
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdPaid(adValue: AdValue) {
                runOnUiThread {
                    handleInterstitialPaid(logCtx, ad, adValue, loadCallback)
                }
            }
        }
    }

    fun interstitialSplashLoaded(): Boolean = mInterstitialSplash != null

    fun getInterstitialSplash(): InterstitialAd? = mInterstitialSplash

    fun loadSplashInterstitialAds(
        context: Context,
        id: String,
        timeOut: Long,
        timeDelay: Long,
        adListener: AdCallback?
    ) {
        isTimeDelay = false
        isTimeout = false
        Log.i(
            TAG,
            "loadSplashInterstitialAds  start time loading:" + Calendar.getInstance().timeInMillis + "    ShowLoadingSplash:" + isShowLoadingSplash
        )

        if (AppPurchase.getInstance().isPurchased(context)) {
            adListener?.onNextAction()
            return
        }
        Handler().postDelayed({
            if (mInterstitialSplash != null) {
                Log.i(TAG, "loadSplashInterstitialAds:show ad on delay ")
                onShowSplash(context as AppCompatActivity, adListener)
                return@postDelayed
            }
            Log.i(TAG, "loadSplashInterstitialAds: delay validate")
            isTimeDelay = true
        }, timeDelay)

        if (timeOut > 0) {
            handlerTimeout = Handler()
            rdTimeout = Runnable {
                Log.e(TAG, "loadSplashInterstitialAds: on timeout")
                isTimeout = true
                if (mInterstitialSplash != null) {
                    Log.i(TAG, "loadSplashInterstitialAds:show ad on timeout ")
                    onShowSplash(context as AppCompatActivity, adListener)
                    return@Runnable
                }
                if (adListener != null) {
                    adListener.onNextAction()
                    isShowLoadingSplash = false
                }
            }
            handlerTimeout?.postDelayed(rdTimeout!!, timeOut)
        }

        isShowLoadingSplash = true
        getInterstitialAds(context, id, object : AdCallback() {
            override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                super.onInterstitialLoad(interstitialAd)
                Log.e(
                    TAG,
                    "loadSplashInterstitialAds  end time loading success:" + Calendar.getInstance().timeInMillis + "     time limit:" + isTimeout
                )
                if (isTimeout) return
                if (interstitialAd != null) {
                    mInterstitialSplash = interstitialAd
                    if (isTimeDelay) {
                        onShowSplash(context as AppCompatActivity, adListener)
                        Log.i(TAG, "loadSplashInterstitialAds:show ad on loaded ")
                    }
                }
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                isShowLoadingSplash = false
                Log.e(
                    TAG,
                    "loadSplashInterstitialAds  end time loading error:" + Calendar.getInstance().timeInMillis + "     time limit:" + isTimeout
                )
                if (isTimeout) return
                if (adListener != null) {
                    if (handlerTimeout != null && rdTimeout != null) {
                        handlerTimeout?.removeCallbacks(rdTimeout!!)
                    }
                    if (i != null) {
                        Log.e(TAG, "loadSplashInterstitialAds: load fail " + i.message)
                    }
                    adListener.onAdFailedToLoad(i)
                    adListener.onNextAction()
                }
            }
        })
    }

    fun loadSplashInterstitialAds(
        context: Context,
        id: String,
        timeOut: Long,
        timeDelay: Long,
        showSplashIfReady: Boolean,
        adListener: AdCallback?
    ) {
        isTimeDelay = false
        isTimeout = false
        Log.i(
            TAG,
            "loadSplashInterstitialAds  start time loading:" + Calendar.getInstance().timeInMillis + "    ShowLoadingSplash:" + isShowLoadingSplash
        )

        if (AppPurchase.getInstance().isPurchased(context)) {
            adListener?.onNextAction()
            return
        }
        Handler().postDelayed({
            if (mInterstitialSplash != null) {
                Log.i(TAG, "loadSplashInterstitialAds:show ad on delay ")
                if (showSplashIfReady) {
                    onShowSplash(context as AppCompatActivity, adListener)
                } else {
                    adListener?.onAdSplashReady()
                }
                return@postDelayed
            }
            Log.i(TAG, "loadSplashInterstitialAds: delay validate")
            isTimeDelay = true
        }, timeDelay)

        if (timeOut > 0) {
            handlerTimeout = Handler()
            rdTimeout = Runnable {
                Log.e(TAG, "loadSplashInterstitialAds: on timeout")
                isTimeout = true
                if (mInterstitialSplash != null) {
                    Log.i(TAG, "loadSplashInterstitialAds:show ad on timeout ")
                    if (showSplashIfReady) {
                        onShowSplash(context as AppCompatActivity, adListener)
                    } else {
                        adListener?.onAdSplashReady()
                    }
                    return@Runnable
                }
                if (adListener != null) {
                    adListener.onNextAction()
                    isShowLoadingSplash = false
                }
            }
            handlerTimeout?.postDelayed(rdTimeout!!, timeOut)
        }

        isShowLoadingSplash = true
        getInterstitialAds(context, id, object : AdCallback() {
            override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                super.onInterstitialLoad(interstitialAd)
                Log.e(
                    TAG,
                    "loadSplashInterstitialAds  end time loading success:" + Calendar.getInstance().timeInMillis + "     time limit:" + isTimeout
                )
                if (isTimeout) return
                if (interstitialAd != null) {
                    mInterstitialSplash = interstitialAd
                    if (isTimeDelay) {
                        if (showSplashIfReady) {
                            onShowSplash(context as AppCompatActivity, adListener)
                        } else {
                            adListener?.onAdSplashReady()
                        }
                        Log.i(TAG, "loadSplashInterstitialAds:show ad on loaded ")
                    }
                }
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                Log.e(
                    TAG,
                    "loadSplashInterstitialAds  end time loading error:" + Calendar.getInstance().timeInMillis + "     time limit:" + isTimeout
                )
                if (isTimeout) return
                if (adListener != null) {
                    adListener.onNextAction()
                    if (handlerTimeout != null && rdTimeout != null) {
                        handlerTimeout?.removeCallbacks(rdTimeout!!)
                    }
                    if (i != null) {
                        Log.e(TAG, "loadSplashInterstitialAds: load fail " + i.message)
                    }
                    adListener.onAdFailedToLoad(i)
                }
            }

            override fun onAdFailedToShow(adError: FullScreenContentError?) {
                super.onAdFailedToShow(adError)
                if (adListener != null) {
                    adListener.onAdFailedToShow(adError)
                    adListener.onNextAction()
                }
            }
        })
    }

    fun onShowSplash(activity: AppCompatActivity, adListener: AdCallback?) {
        Log.e("onShowSplash", "onShowSplash: 1")
        isShowLoadingSplash = true

        if (mInterstitialSplash == null) {
            adListener?.onNextAction()
            return
        }

        if (handlerTimeout != null && rdTimeout != null) {
            handlerTimeout?.removeCallbacks(rdTimeout!!)
        }

        adListener?.onInterstitialShow()

        val ad = mInterstitialSplash!!
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdPaid(adValue: AdValue) {
                runOnUiThread {
                    val logCtx = this@Admob.context ?: activity.applicationContext
                    handleInterstitialPaid(logCtx, ad, adValue, adListener)
                }
            }

            override fun onAdShowedFullScreenContent() {
                runOnUiThread {
                    Log.d(TAG, "onAdShowedFullScreenContent ")
                    AppOpenManager.getInstance().setInterstitialShowing(true)
                    isShowLoadingSplash = false
                    if (!openActivityAfterShowInterAds) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                dialog?.dismiss()
                            } catch (_: Exception) {
                            }
                        }, 200L)
                    }
                }
            }

            override fun onAdDismissedFullScreenContent() {
                runOnUiThread {
                    Log.d(TAG, "onAdDismissedFullScreenContent ")
                    AppOpenManager.getInstance().setInterstitialShowing(false)
                    mInterstitialSplash = null
                    try {
                        dialog?.dismiss()
                    } catch (_: Exception) {
                    }
                    if (adListener != null) {
                        if (!openActivityAfterShowInterAds) {
                            adListener.onNextAction()
                        }
                        adListener.onAdClosed()
                    }
                    isShowLoadingSplash = false
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                runOnUiThread {
                    Log.e(TAG, "onAdFailedToShowFullScreenContent : " + adError.message)
                    mInterstitialSplash = null
                    isShowLoadingSplash = false
                    try {
                        dialog?.dismiss()
                    } catch (_: Exception) {
                    }
                    if (adListener != null) {
                        adListener.onAdFailedToShow(adError)
                        if (!openActivityAfterShowInterAds) {
                            adListener.onNextAction()
                        }
                    }
                }
            }

            override fun onAdClicked() {
                runOnUiThread {
                    if (disableAdResumeWhenClickAds) {
                        AppOpenManager.getInstance().disableAdResumeByClickAction()
                    }
                    adListener?.onAdClicked()
                    adListener?.onAdClicked(
                        ad.adUnitId,
                        ad.getResponseInfo().adapterClassName,
                        AdType.INTERSTITIAL
                    )
                    NkhLogEventManager.logClickAdsEvent(context ?: activity, ad.adUnitId)
                }
            }

            override fun onAdImpression() {
                runOnUiThread {
                    adListener?.onAdImpression()
                }
            }
        }

        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            try {
                if (dialog != null && dialog!!.isShowing) {
                    dialog?.dismiss()
                }
                dialog = PrepareLoadingAdsDialog(activity)
                try {
                    dialog?.show()
                } catch (e: Exception) {
                    adListener?.onNextAction()
                    return
                }
            } catch (e: Exception) {
                dialog = null
                e.printStackTrace()
            }
            Handler().postDelayed({
                if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                    openActivityAfterShowInterAds &&
                    adListener != null
                ) {
                    adListener.onNextAction()
                    Handler().postDelayed({
                        if (dialog != null && dialog!!.isShowing && !activity.isDestroyed) {
                            dialog?.dismiss()
                        }
                    }, 1500)
                }
                if (mInterstitialSplash != null &&
                    dialog != null &&
                    !activity.isDestroyed &&
                    !activity.isFinishing
                ) {
                    Log.i(
                        TAG,
                        "start show InterstitialAd " + activity.lifecycle.currentState.name + "/" + ProcessLifecycleOwner.get().lifecycle.currentState.name
                    )
                    mInterstitialSplash?.show(activity)
                    isShowLoadingSplash = false
                } else if (mInterstitialSplash == null) {
                    if (dialog != null) {
                        dialog?.dismiss()
                    }
                    adListener?.onNextAction()
                    isShowLoadingSplash = false
                }
            }, 800)
        } else {
            Log.e(TAG, "onShowSplash:   show fail in background after show loading ad")
            isShowLoadingSplash = false
        }
    }

    fun onShowSplash(activity: AppCompatActivity, adListener: AdCallback?, mInter: InterstitialAd?) {
        mInterstitialSplash = mInter
        onShowSplash(activity, adListener)
    }

    fun onCheckShowSplashWhenFail(activity: AppCompatActivity, callback: AdCallback?, timeDelay: Int) {
        Handler(activity.mainLooper).postDelayed({
            if (mInterstitialSplash != null && !isShowLoadingSplash()) {
                Log.i(TAG, "show ad splash when show fail in background")
                getInstance().onShowSplash(activity, callback)
            }
        }, timeDelay.toLong())
    }

    fun getInterstitialAds(context: Context, id: String, adCallback: AdCallback?) {
        if (Arrays.asList(*context.resources.getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, INTERS_ADS, id)
        }
        if (AppPurchase.getInstance().isPurchased(context) ||
            AdmobHelper.getNumClickAdsPerDay(context, id) >= maxClickAds
        ) {
            adCallback?.onInterstitialLoad(null)
            return
        }

        runWhenSdkInitialized(Runnable {
            InterstitialAd.load(
                getAdRequest(id),
                object : AdLoadCallback<InterstitialAd> {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        runOnUiThread {
                            registerInterstitialPaidListener(interstitialAd, context, adCallback)
                            adCallback?.onInterstitialLoad(interstitialAd)
                            Log.i(TAG, "InterstitialAds onAdLoaded")
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        runOnUiThread {
                            Log.i(TAG, loadAdError.message)
                            adCallback?.onAdFailedToLoad(loadAdError)
                        }
                    }
                }
            )
        })
    }

    fun showInterstitialAdByTimes(
        context: Context,
        mInterstitialAd: InterstitialAd?,
        callback: AdCallback?,
        timeDelay: Long
    ) {
        if (timeDelay > 0) {
            handlerTimeout = Handler()
            rdTimeout = Runnable {
                forceShowInterstitial(context, mInterstitialAd, callback)
            }
            handlerTimeout?.postDelayed(rdTimeout!!, timeDelay)
        } else {
            forceShowInterstitial(context, mInterstitialAd, callback)
        }
    }

    fun showInterstitialAdByTimes(
        context: Context,
        mInterstitialAd: InterstitialAd?,
        callback: AdCallback?
    ) {
        AdmobHelper.setupAdmobData(context)
        if (AppPurchase.getInstance().isPurchased(context)) {
            callback?.onNextAction()
            return
        }
        if (mInterstitialAd == null) {
            callback?.onNextAction()
            return
        }

        mInterstitialAd.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdPaid(adValue: AdValue) {
                runOnUiThread {
                    val logCtx = this@Admob.context ?: context.applicationContext
                    handleInterstitialPaid(logCtx, mInterstitialAd, adValue, callback)
                }
            }

            override fun onAdDismissedFullScreenContent() {
                runOnUiThread {
                    AppOpenManager.getInstance().setInterstitialShowing(false)
                    SharePreferenceUtils.setLastImpressionInterstitialTime(context)
                    try {
                        dialog?.dismiss()
                    } catch (_: Exception) {
                    }
                    if (callback != null) {
                        if (!openActivityAfterShowInterAds) {
                            callback.onNextAction()
                        }
                        callback.onAdClosed()
                    }
                    Log.e(TAG, "onAdDismissedFullScreenContent")
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                runOnUiThread {
                    Log.e(TAG, "onAdFailedToShowFullScreenContent: " + adError.message)
                    AppOpenManager.getInstance().setInterstitialShowing(false)
                    try {
                        dialog?.dismiss()
                    } catch (_: Exception) {
                    }
                    if (callback != null) {
                        callback.onAdFailedToShow(adError)
                        if (!openActivityAfterShowInterAds) {
                            callback.onNextAction()
                        }
                    }
                }
            }

            override fun onAdShowedFullScreenContent() {
                runOnUiThread {
                    Log.e(TAG, "onAdShowedFullScreenContent ")
                    AppOpenManager.getInstance().setInterstitialShowing(true)
                    if (!openActivityAfterShowInterAds) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                dialog?.dismiss()
                            } catch (_: Exception) {
                            }
                        }, 200L)
                    }
                }
            }

            override fun onAdClicked() {
                runOnUiThread {
                    if (disableAdResumeWhenClickAds) {
                        AppOpenManager.getInstance().disableAdResumeByClickAction()
                    }
                    if (callback != null) {
                        callback.onAdClicked()
                        callback.onAdClicked(
                            mInterstitialAd.adUnitId,
                            mInterstitialAd.getResponseInfo().adapterClassName,
                            AdType.INTERSTITIAL
                        )
                    }
                    NkhLogEventManager.logClickAdsEvent(context, mInterstitialAd.adUnitId)
                }
            }

            override fun onAdImpression() {
                runOnUiThread {
                    callback?.onAdImpression()
                }
            }
        }

        if (AdmobHelper.getNumClickAdsPerDay(context, mInterstitialAd.adUnitId) < maxClickAds) {
            showInterstitialAd(context, mInterstitialAd, callback)
            return
        }
        callback?.onNextAction()
    }

    fun forceShowInterstitial(
        context: Context,
        mInterstitialAd: InterstitialAd?,
        callback: AdCallback?
    ) {
        currentClicked = numShowAds
        showInterstitialAdByTimes(context, mInterstitialAd, callback)
    }

    private fun showInterstitialAd(
        context: Context,
        mInterstitialAd: InterstitialAd?,
        callback: AdCallback?
    ) {
        currentClicked++
        if (currentClicked >= numShowAds && mInterstitialAd != null) {
            if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                try {
                    if (dialog != null && dialog!!.isShowing) {
                        dialog?.dismiss()
                    }
                    dialog = PrepareLoadingAdsDialog(context)
                    dialog?.setCancelable(false)
                    try {
                        callback?.onInterstitialShow()
                        dialog?.show()
                        AppOpenManager.getInstance().setInterstitialShowing(true)
                    } catch (e: Exception) {
                        callback?.onNextAction()
                        return
                    }
                } catch (e: Exception) {
                    dialog = null
                    e.printStackTrace()
                }
                Handler().postDelayed({
                    if ((context as AppCompatActivity).lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                        openActivityAfterShowInterAds &&
                        callback != null
                    ) {
                        callback.onNextAction()
                        Handler().postDelayed({
                            if (dialog != null && dialog!!.isShowing && !(context as Activity).isDestroyed) {
                                dialog?.dismiss()
                            }
                        }, 1500)
                    }
                    Log.i(
                        TAG,
                        "start show InterstitialAd " + context.lifecycle.currentState.name + "/" + ProcessLifecycleOwner.get().lifecycle.currentState.name
                    )
                    mInterstitialAd.show(context as Activity)
                }, 800)
            }
            currentClicked = 0
        } else if (callback != null) {
            if (dialog != null) {
                dialog?.dismiss()
            }
            callback.onNextAction()
        }
    }

    fun loadBanner(mActivity: Activity, id: String) {
        loadBanner(mActivity, id, null)
    }

    fun loadBanner(mActivity: Activity, id: String, callback: AdCallback?) {
        loadBanner(mActivity, id, callback, false, BANNER_INLINE_LARGE_STYLE)
    }

    fun loadBanner(mActivity: Activity, id: String, useInlineAdaptive: Boolean) {
        loadBanner(mActivity, id, null, useInlineAdaptive, BANNER_INLINE_LARGE_STYLE)
    }

    fun loadInlineBanner(activity: Activity, id: String, inlineStyle: String) {
        loadBanner(activity, id, null, true, inlineStyle)
    }

    fun loadBanner(
        mActivity: Activity,
        id: String,
        callback: AdCallback?,
        useInlineAdaptive: Boolean
    ) {
        loadBanner(mActivity, id, callback, useInlineAdaptive, BANNER_INLINE_LARGE_STYLE)
    }

    fun loadInlineBanner(
        activity: Activity,
        id: String,
        inlineStyle: String,
        callback: AdCallback?
    ) {
        loadBanner(activity, id, callback, true, inlineStyle)
    }

    fun loadCollapsibleBanner(
        mActivity: Activity,
        id: String,
        gravity: String?,
        callback: AdCallback?
    ) {
        loadBanner(mActivity, id, callback, false, BANNER_INLINE_LARGE_STYLE, gravity, true)
    }

    fun loadCollapsibleBannerSizeMedium(
        mActivity: Activity,
        id: String,
        gravity: String?,
        sizeBanner: AdSize,
        callback: AdCallback?
    ) {
        loadBannerSizeMedium(
            mActivity,
            id,
            callback,
            false,
            BANNER_INLINE_LARGE_STYLE,
            sizeBanner,
            gravity,
            true
        )
    }

    fun loadBannerFragment(mActivity: Activity, id: String, rootView: View) {
        loadBannerFragment(mActivity, id, rootView, null, false, BANNER_INLINE_LARGE_STYLE)
    }

    fun loadBannerFragment(
        mActivity: Activity,
        id: String,
        rootView: View,
        callback: AdCallback?
    ) {
        loadBannerFragment(mActivity, id, rootView, callback, false, BANNER_INLINE_LARGE_STYLE)
    }

    fun loadBannerFragment(
        mActivity: Activity,
        id: String,
        rootView: View,
        useInlineAdaptive: Boolean
    ) {
        loadBannerFragment(mActivity, id, rootView, null, useInlineAdaptive, BANNER_INLINE_LARGE_STYLE)
    }

    fun loadInlineBannerFragment(
        activity: Activity,
        id: String,
        rootView: View,
        inlineStyle: String
    ) {
        loadBannerFragment(activity, id, rootView, null, true, inlineStyle)
    }

    fun loadBannerFragment(
        mActivity: Activity,
        id: String,
        rootView: View,
        callback: AdCallback?,
        useInlineAdaptive: Boolean
    ) {
        loadBannerFragment(
            mActivity,
            id,
            rootView,
            callback,
            useInlineAdaptive,
            BANNER_INLINE_LARGE_STYLE
        )
    }

    fun loadInlineBannerFragment(
        activity: Activity,
        id: String,
        rootView: View,
        inlineStyle: String,
        callback: AdCallback?
    ) {
        loadBannerFragment(activity, id, rootView, callback, true, inlineStyle)
    }

    fun loadCollapsibleBannerFragment(
        mActivity: Activity,
        id: String,
        rootView: View,
        gravity: String?,
        callback: AdCallback?
    ) {
        loadBannerFragment(
            mActivity,
            id,
            rootView,
            callback,
            false,
            BANNER_INLINE_LARGE_STYLE,
            gravity,
            true
        )
    }

    private fun loadBannerImpl(
        mActivity: Activity,
        id: String,
        adContainer: FrameLayout?,
        containerShimmer: ShimmerFrameLayout?,
        callback: AdCallback?,
        useInlineAdaptive: Boolean,
        inlineStyle: String,
        explicitAdSize: AdSize? = null,
        gravity: String? = null,
        useCollapsible: Boolean = false
    ) {
        if (Arrays.asList(*mActivity.resources.getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(mActivity, BANNER_ADS, id)
        }
        if (AppPurchase.getInstance().isPurchased(mActivity)) {
            containerShimmer?.visibility = View.GONE
            return
        }

        containerShimmer?.visibility = View.VISIBLE
        containerShimmer?.startShimmer()
        runWhenSdkInitialized(Runnable {
            runOnUiThread {
                try {
                    val adSize = explicitAdSize ?: getAdSize(mActivity, useInlineAdaptive, inlineStyle)
                    val adView = AdView(mActivity)
                    val layerHeight = adSize.getHeightInPixels(mActivity)
                    val layoutHeight = if (useInlineAdaptive || layerHeight <= 0) {
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    } else {
                        layerHeight
                    }
                    adContainer?.removeAllViews()
                    adContainer?.addView(
                        adView,
                        FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, layoutHeight)
                    )
                    val requestBuilder = BannerAdRequest.Builder(id, adSize)
                    if (useCollapsible && gravity != null) {
                        val extras = Bundle()
                        extras.putString("collapsible", gravity)
                        requestBuilder.setGoogleExtrasBundle(extras)
                    }
                    adView.loadAd(
                        requestBuilder.build(),
                        object : AdLoadCallback<BannerAd> {
                            override fun onAdLoaded(bannerAd: BannerAd) {
                                runOnUiThread {
                                    containerShimmer?.stopShimmer()
                                    containerShimmer?.visibility = View.GONE
                                    adContainer?.visibility = View.VISIBLE
                                    bannerAd.adEventCallback = object : BannerAdEventCallback {
                                        override fun onAdPaid(adValue: AdValue) {
                                            runOnUiThread {
                                                NkhLogEventManager.logPaidAdImpression(
                                                    mActivity,
                                                    adValue,
                                                    bannerAd.adUnitId,
                                                    bannerAd.getResponseInfo().adapterClassName
                                                )
                                                NkhLogEventManager.logPaidAdjustWithToken(
                                                    adValue,
                                                    bannerAd.adUnitId,
                                                    NkhAdConfig.ADJUST_TOKEN_TIKTOK
                                                )
                                                callback?.onAdLogRev(
                                                    adValue,
                                                    bannerAd.adUnitId,
                                                    bannerAd.getResponseInfo().adapterClassName,
                                                    if (useCollapsible) AdType.COLLAPSIBLE_BANNER else AdType.BANNER
                                                )
                                            }
                                        }

                                        override fun onAdClicked() {
                                            runOnUiThread {
                                                if (disableAdResumeWhenClickAds) {
                                                    AppOpenManager.getInstance().disableAdResumeByClickAction()
                                                }
                                                if (callback != null) {
                                                    callback.onAdClicked()
                                                    callback.onAdClicked(
                                                        bannerAd.adUnitId,
                                                        bannerAd.getResponseInfo().adapterClassName,
                                                        if (useCollapsible) AdType.COLLAPSIBLE_BANNER else AdType.BANNER
                                                    )
                                                    Log.d(
                                                        TAG,
                                                        "onAdClicked() called with bannerAdLoaded" + bannerAd.adUnitId
                                                    )
                                                }
                                                NkhLogEventManager.logClickAdsEvent(mActivity, id)
                                            }
                                        }

                                        override fun onAdImpression() {
                                            runOnUiThread {
                                                callback?.onAdImpression()
                                            }
                                        }
                                    }
                                    callback?.onAdLoaded()
                                }
                            }

                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                runOnUiThread {
                                    containerShimmer?.stopShimmer()
                                    adContainer?.visibility = View.GONE
                                    containerShimmer?.visibility = View.GONE
                                    callback?.onAdFailedToLoad(adError)
                                }
                            }
                        }
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    private fun loadBanner(
        mActivity: Activity,
        id: String,
        callback: AdCallback?,
        useInlineAdaptive: Boolean,
        inlineStyle: String
    ) {
        val adContainer = mActivity.findViewById<FrameLayout>(R.id.banner_container)
        val containerShimmer =
            mActivity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        loadBannerImpl(
            mActivity,
            id,
            adContainer,
            containerShimmer,
            callback,
            useInlineAdaptive,
            inlineStyle
        )
    }

    private fun loadBanner(
        mActivity: Activity,
        id: String,
        callback: AdCallback?,
        useInlineAdaptive: Boolean,
        inlineStyle: String,
        gravity: String?,
        useCollapsible: Boolean
    ) {
        val adContainer = mActivity.findViewById<FrameLayout>(R.id.banner_container)
        val containerShimmer =
            mActivity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        loadBannerImpl(
            mActivity,
            id,
            adContainer,
            containerShimmer,
            callback,
            useInlineAdaptive,
            inlineStyle,
            null,
            gravity,
            useCollapsible
        )
    }

    private fun loadBannerSizeMedium(
        mActivity: Activity,
        id: String,
        callback: AdCallback?,
        useInlineAdaptive: Boolean,
        inlineStyle: String,
        sizeBanner: AdSize,
        gravity: String?,
        useCollapsible: Boolean
    ) {
        val adContainer = mActivity.findViewById<FrameLayout>(R.id.banner_container)
        val containerShimmer =
            mActivity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        loadBannerImpl(
            mActivity,
            id,
            adContainer,
            containerShimmer,
            callback,
            useInlineAdaptive,
            inlineStyle,
            sizeBanner,
            gravity,
            useCollapsible
        )
    }

    private fun loadBannerFragment(
        mActivity: Activity,
        id: String,
        rootView: View,
        callback: AdCallback?,
        useInlineAdaptive: Boolean,
        inlineStyle: String
    ) {
        val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)
        val containerShimmer =
            rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        loadBannerImpl(
            mActivity,
            id,
            adContainer,
            containerShimmer,
            callback,
            useInlineAdaptive,
            inlineStyle
        )
    }

    private fun loadBannerFragment(
        mActivity: Activity,
        id: String,
        rootView: View,
        callback: AdCallback?,
        useInlineAdaptive: Boolean,
        inlineStyle: String,
        gravity: String?,
        useCollapsible: Boolean
    ) {
        val adContainer = rootView.findViewById<FrameLayout>(R.id.banner_container)
        val containerShimmer =
            rootView.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
        loadBannerImpl(
            mActivity,
            id,
            adContainer,
            containerShimmer,
            callback,
            useInlineAdaptive,
            inlineStyle,
            null,
            gravity,
            useCollapsible
        )
    }

    private fun getAdSize(
        mActivity: Activity,
        useInlineAdaptive: Boolean,
        inlineStyle: String
    ): AdSize {
        val display = mActivity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()

        if (useInlineAdaptive) {
            return if (inlineStyle.equals(BANNER_INLINE_LARGE_STYLE, ignoreCase = true)) {
                AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(mActivity, adWidth)
            } else {
                AdSize.getInlineAdaptiveBannerAdSize(adWidth, MAX_SMALL_INLINE_BANNER_HEIGHT)
            }
        }
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(mActivity, adWidth)
    }

    fun loadNative(mActivity: Activity, id: String, adCallback: AdCallback?) {
        val frameLayout = mActivity.findViewById<FrameLayout>(R.id.fl_adplaceholder)
        val containerShimmer =
            mActivity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)
        loadNative(mActivity, containerShimmer, frameLayout, id, R.layout.custom_native_admob_free_size, adCallback)
    }

    fun loadNativeFragment(mActivity: Activity, id: String, parent: View, adCallback: AdCallback?) {
        val frameLayout = parent.findViewById<FrameLayout>(R.id.fl_adplaceholder)
        val containerShimmer =
            parent.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)
        loadNative(mActivity, containerShimmer, frameLayout, id, R.layout.custom_native_admob_free_size, adCallback)
    }

    fun loadSmallNative(mActivity: Activity, adUnitId: String, adCallback: AdCallback?) {
        val frameLayout = mActivity.findViewById<FrameLayout>(R.id.fl_adplaceholder)
        val containerShimmer =
            mActivity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)
        loadNative(mActivity, containerShimmer, frameLayout, adUnitId, R.layout.custom_native_admob_medium, adCallback)
    }

    fun loadSmallNativeFragment(
        mActivity: Activity,
        adUnitId: String,
        parent: View,
        adCallback: AdCallback?
    ) {
        val frameLayout = parent.findViewById<FrameLayout>(R.id.fl_adplaceholder)
        val containerShimmer =
            parent.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)
        loadNative(mActivity, containerShimmer, frameLayout, adUnitId, R.layout.custom_native_admob_medium, adCallback)
    }

    fun loadNativeWithConfig(
        mActivity: Activity,
        id: String,
        layout: Int,
        config: NativeAdConfig,
        callback: AdCallback?
    ) {
        val frameLayout = mActivity.findViewById<FrameLayout>(R.id.fl_adplaceholder)
        val containerShimmer =
            mActivity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)
        loadNativeWithConfig(mActivity, containerShimmer, frameLayout, id, layout, config, callback)
    }

    fun loadNativeWithConfig(
        mActivity: Activity,
        id: String,
        layout: Int,
        colorCTA: String,
        heightCTA: Int,
        radiusCTA: Int,
        callback: AdCallback?
    ) {
        val frameLayout = mActivity.findViewById<FrameLayout>(R.id.fl_adplaceholder)
        val containerShimmer =
            mActivity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_native)
        loadNativeWithConfig(
            mActivity,
            containerShimmer,
            frameLayout,
            id,
            layout,
            colorCTA,
            heightCTA,
            radiusCTA,
            callback
        )
    }

    private fun loadNativeWithConfig(
        context: Context,
        containerShimmer: ShimmerFrameLayout?,
        frameLayout: FrameLayout,
        id: String,
        layout: Int,
        colorCTA: String,
        heightCTA: Int,
        radiusCTA: Int,
        callback: AdCallback?
    ) {
        if (Arrays.asList(*context.resources.getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, NATIVE_ADS, id)
        }
        if (AppPurchase.getInstance().isPurchased(context)) {
            containerShimmer?.visibility = View.GONE
            return
        }
        frameLayout.removeAllViews()
        frameLayout.visibility = View.GONE
        containerShimmer?.visibility = View.VISIBLE
        containerShimmer?.startShimmer()

        runWhenSdkInitialized(Runnable {
            val request = NativeAdRequest.Builder(id, listOf(NativeAd.NativeAdType.NATIVE)).build()
            NativeAdLoader.load(
                request,
                object : NativeAdLoaderCallback {
                    override fun onNativeAdLoaded(nativeAd: NativeAd) {
                        runOnUiThread {
                            containerShimmer?.stopShimmer()
                            containerShimmer?.visibility = View.GONE
                            nativeAd.adEventCallback = createNativeAdEventCallback(context, nativeAd, id, callback)
                            frameLayout.visibility = View.VISIBLE
                            @SuppressLint("InflateParams")
                            val adView = (context as Activity).layoutInflater.inflate(layout, null) as NativeAdView
                            populateUnifiedNativeAdView(nativeAd, adView, colorCTA, heightCTA, radiusCTA)
                            frameLayout.removeAllViews()
                            frameLayout.addView(adView)
                            callback?.onUnifiedNativeAdLoaded(nativeAd)
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        runOnUiThread {
                            Log.e(TAG, "onAdFailedToLoad: " + adError.message)
                            containerShimmer?.stopShimmer()
                            containerShimmer?.visibility = View.GONE
                            frameLayout.visibility = View.GONE
                            callback?.onAdFailedToLoad(adError)
                        }
                    }
                }
            )
        })
    }

    fun loadNativeAd(context: Context, id: String, callback: AdCallback?) {
        if (Arrays.asList(*context.resources.getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, NATIVE_ADS, id)
        }
        if (AppPurchase.getInstance().isPurchased(context)) {
            return
        }
        runWhenSdkInitialized(Runnable {
            val request = NativeAdRequest.Builder(id, listOf(NativeAd.NativeAdType.NATIVE)).build()
            NativeAdLoader.load(
                request,
                object : NativeAdLoaderCallback {
                    override fun onNativeAdLoaded(nativeAd: NativeAd) {
                        runOnUiThread {
                            nativeAd.adEventCallback = createNativeAdEventCallback(context, nativeAd, id, callback)
                            callback?.onUnifiedNativeAdLoaded(nativeAd)
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        runOnUiThread {
                            Log.e(TAG, "onAdFailedToLoad: " + adError.message)
                            callback?.onAdFailedToLoad(adError)
                        }
                    }
                }
            )
        })
    }

    fun loadNativeAds(context: Context, id: String, callback: AdCallback?, countAd: Int) {
        if (Arrays.asList(*context.resources.getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, NATIVE_ADS, id)
        }
        if (AppPurchase.getInstance().isPurchased(context)) {
            callback?.onAdClosed()
            return
        }
        runWhenSdkInitialized(Runnable {
            val request = NativeAdRequest.Builder(id, listOf(NativeAd.NativeAdType.NATIVE)).build()
            NativeAdLoader.load(
                request,
                object : NativeAdLoaderCallback {
                    override fun onNativeAdLoaded(nativeAd: NativeAd) {
                        runOnUiThread {
                            nativeAd.adEventCallback = createNativeAdEventCallback(context, nativeAd, id, callback)
                            callback?.onUnifiedNativeAdLoaded(nativeAd)
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        runOnUiThread {
                            Log.e(TAG, "onAdFailedToLoad: " + adError.message)
                            callback?.onAdFailedToLoad(adError)
                        }
                    }
                }
            )
        })
    }

    private fun loadNative(
        context: Context,
        containerShimmer: ShimmerFrameLayout?,
        frameLayout: FrameLayout,
        id: String,
        layout: Int,
        callback: AdCallback?
    ) {
        if (Arrays.asList(*context.resources.getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, NATIVE_ADS, id)
        }
        if (AppPurchase.getInstance().isPurchased(context)) {
            containerShimmer?.visibility = View.GONE
            return
        }
        frameLayout.removeAllViews()
        frameLayout.visibility = View.GONE
        containerShimmer?.visibility = View.VISIBLE
        containerShimmer?.startShimmer()

        runWhenSdkInitialized(Runnable {
            val request = NativeAdRequest.Builder(id, listOf(NativeAd.NativeAdType.NATIVE)).build()
            NativeAdLoader.load(
                request,
                object : NativeAdLoaderCallback {
                    override fun onNativeAdLoaded(nativeAd: NativeAd) {
                        runOnUiThread {
                            containerShimmer?.stopShimmer()
                            containerShimmer?.visibility = View.GONE
                            nativeAd.adEventCallback = createNativeAdEventCallback(context, nativeAd, id, callback)
                            frameLayout.visibility = View.VISIBLE
                            @SuppressLint("InflateParams")
                            val adView = (context as Activity).layoutInflater.inflate(layout, null) as NativeAdView
                            populateUnifiedNativeAdView(nativeAd, adView)
                            frameLayout.removeAllViews()
                            frameLayout.addView(adView)
                            callback?.onUnifiedNativeAdLoaded(nativeAd)
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        runOnUiThread {
                            Log.e(TAG, "onAdFailedToLoad: " + adError.message)
                            containerShimmer?.stopShimmer()
                            containerShimmer?.visibility = View.GONE
                            frameLayout.visibility = View.GONE
                            callback?.onAdFailedToLoad(adError)
                        }
                    }
                }
            )
        })
    }

    private fun loadNativeWithConfig(
        context: Context,
        containerShimmer: ShimmerFrameLayout?,
        frameLayout: FrameLayout,
        id: String,
        layout: Int,
        config: NativeAdConfig,
        callback: AdCallback?
    ) {
        if (Arrays.asList(*context.resources.getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, NATIVE_ADS, id)
        }
        if (AppPurchase.getInstance().isPurchased(context)) {
            containerShimmer?.visibility = View.GONE
            return
        }
        frameLayout.removeAllViews()
        frameLayout.visibility = View.GONE
        containerShimmer?.visibility = View.VISIBLE
        containerShimmer?.startShimmer()

        runWhenSdkInitialized(Runnable {
            val request = NativeAdRequest.Builder(id, listOf(NativeAd.NativeAdType.NATIVE)).build()
            NativeAdLoader.load(
                request,
                object : NativeAdLoaderCallback {
                    override fun onNativeAdLoaded(nativeAd: NativeAd) {
                        runOnUiThread {
                            containerShimmer?.stopShimmer()
                            containerShimmer?.visibility = View.GONE
                            nativeAd.adEventCallback = createNativeAdEventCallback(context, nativeAd, id, callback)
                            frameLayout.visibility = View.VISIBLE
                            @SuppressLint("InflateParams")
                            val adView = (context as Activity).layoutInflater.inflate(layout, null) as NativeAdView
                            populateUnifiedNativeAdView(nativeAd, adView, config)
                            frameLayout.removeAllViews()
                            frameLayout.addView(adView)
                            callback?.onUnifiedNativeAdLoaded(nativeAd)
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        runOnUiThread {
                            Log.e(TAG, "onAdFailedToLoad: " + adError.message)
                            containerShimmer?.stopShimmer()
                            containerShimmer?.visibility = View.GONE
                            frameLayout.visibility = View.GONE
                            callback?.onAdFailedToLoad(adError)
                        }
                    }
                }
            )
        })
    }

    private fun createNativeAdEventCallback(
        context: Context,
        nativeAd: NativeAd,
        id: String,
        callback: AdCallback?
    ): NativeAdEventCallback {
        return object : NativeAdEventCallback {
            override fun onAdPaid(adValue: AdValue) {
                runOnUiThread {
                    NkhLogEventManager.logPaidAdImpression(
                        context,
                        adValue,
                        id,
                        nativeAd.getResponseInfo().adapterClassName
                    )
                    NkhLogEventManager.logPaidAdjustWithToken(
                        adValue,
                        id,
                        NkhAdConfig.ADJUST_TOKEN_TIKTOK
                    )
                    callback?.onAdLogRev(
                        adValue,
                        id,
                        nativeAd.getResponseInfo().adapterClassName,
                        AdType.NATIVE
                    )
                }
            }

            override fun onAdClicked() {
                runOnUiThread {
                    if (disableAdResumeWhenClickAds) {
                        AppOpenManager.getInstance().disableAdResumeByClickAction()
                    }
                    if (callback != null) {
                        callback.onAdClicked()
                        callback.onAdClicked(id, nativeAd.getResponseInfo().adapterClassName, AdType.NATIVE)
                    }
                    NkhLogEventManager.logClickAdsEvent(context, id)
                }
            }

            override fun onAdImpression() {
                runOnUiThread {
                    callback?.onAdImpression()
                }
            }
        }
    }

    private fun parseColor(value: String?): Int {
        if (value.isNullOrEmpty() || value == "default") {
            return Color.parseColor("#FF5722")
        }
        return Color.parseColor(value)
    }

    fun populateUnifiedNativeAdView(nativeAd: NativeAd?, adView: NativeAdView) {
        if (nativeAd == null) return
        val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

        bindStandardNativeAssets(nativeAd, adView)
        adView.registerNativeAd(nativeAd, mediaView)
        showNativeDebugDialog(nativeAd, adView)
    }

    fun populateUnifiedNativeAdView(
        nativeAd: NativeAd?,
        adView: NativeAdView,
        colorCTA: String?,
        heightCTA: Int,
        radiusCTA: Int
    ) {
        if (nativeAd == null) return
        val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

        bindStandardNativeAssets(nativeAd, adView)

        try {
            val callToActionView = adView.callToActionView as? TextView
            if (callToActionView != null && heightCTA > 0) {
                val ctaHeightPx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    heightCTA.toFloat(),
                    adView.resources.displayMetrics
                ).toInt()
                val lp = callToActionView.layoutParams
                if (lp != null) {
                    lp.height = ctaHeightPx
                    callToActionView.layoutParams = lp
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        adView.registerNativeAd(nativeAd, mediaView)

        try {
            val cta = adView.callToActionView
            if (cta != null) {
                val parsedColor = parseColor(colorCTA)
                val drawable = GradientDrawable()
                drawable.setColor(parsedColor)
                drawable.cornerRadius = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    radiusCTA.toFloat(),
                    adView.resources.displayMetrics
                )
                cta.background = drawable
                if (cta is Button) {
                    cta.backgroundTintList = ColorStateList.valueOf(parsedColor)
                }
            }
        } catch (ignored: Exception) {
        }

        showNativeDebugDialog(nativeAd, adView)
    }

    fun populateUnifiedNativeAdView(
        nativeAd: NativeAd?,
        adView: NativeAdView,
        config: NativeAdConfig?
    ) {
        if (nativeAd == null) return
        val finalConfig = config ?: NativeAdConfig()
        val displayMetrics = (context ?: adView.context).resources.displayMetrics

        val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
        if (mediaView != null) {
            try {
                val mediaViewSize = finalConfig.getMediaViewSize()
                if (mediaViewSize <= 0f) {
                    mediaView.visibility = View.GONE
                } else {
                    mediaView.visibility = View.VISIBLE
                    val layoutParams = mediaView.layoutParams
                    if (layoutParams != null) {
                        val mediaSizePx = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            mediaViewSize,
                            displayMetrics
                        ).toInt()
                        layoutParams.height = mediaSizePx
                        mediaView.layoutParams = layoutParams
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

        try {
            val adUnitContent = adView.findViewById<View>(R.id.ad_unit_content)
            val bgColor = finalConfig.getAdViewBackgroundColor()
            if (adUnitContent != null) {
                adUnitContent.backgroundTintList = ColorStateList.valueOf(bgColor)
            } else {
                adView.setBackgroundColor(bgColor)
            }
        } catch (ignored: Exception) {
        }

        try {
            val padPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                finalConfig.getContentPadding().toFloat(),
                displayMetrics
            ).toInt()
            adView.setPadding(padPx, padPx, padPx, padPx)
        } catch (ignored: Exception) {
        }

        try {
            val headlineView = adView.headlineView as? TextView
            if (headlineView != null) {
                headlineView.text = nativeAd.headline
                headlineView.setTextSize(TypedValue.COMPLEX_UNIT_SP, finalConfig.getHeadlineTextSize())
                headlineView.setTextColor(finalConfig.getHeadlineTextColor())
                if (finalConfig.getHeadlineTypeface() != null) {
                    headlineView.typeface = finalConfig.getHeadlineTypeface()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (nativeAd.body == null) {
                adView.bodyView?.visibility = View.INVISIBLE
            } else {
                adView.bodyView?.visibility = View.VISIBLE
                val bodyView = adView.bodyView as? TextView
                if (bodyView != null) {
                    bodyView.text = nativeAd.body
                    bodyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, finalConfig.getBodyTextSize())
                    bodyView.setTextColor(finalConfig.getBodyTextColor())
                    if (finalConfig.getBodyTypeface() != null) {
                        bodyView.typeface = finalConfig.getBodyTypeface()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (nativeAd.callToAction == null) {
                adView.callToActionView?.visibility = View.INVISIBLE
            } else {
                adView.callToActionView?.visibility = View.VISIBLE
                val callToActionView = adView.callToActionView as? TextView
                if (callToActionView != null) {
                    callToActionView.text = nativeAd.callToAction
                    callToActionView.setTextSize(
                        TypedValue.COMPLEX_UNIT_SP,
                        finalConfig.getCallToActionTextSize()
                    )
                    callToActionView.setTextColor(finalConfig.getCallToActionTextColor())
                    callToActionView.setBackgroundColor(finalConfig.getCallToActionBackgroundColor())
                    if (finalConfig.getCallToActionTypeface() != null) {
                        callToActionView.typeface = finalConfig.getCallToActionTypeface()
                    }

                    val drawable = GradientDrawable()
                    drawable.setColor(finalConfig.getCallToActionBackgroundColor())
                    drawable.cornerRadius = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        finalConfig.getCallToActionCornerRadius(),
                        displayMetrics
                    )
                    callToActionView.background = drawable

                    val ctaHeightDp = getCtaButtonHeight(finalConfig)
                    if (ctaHeightDp > 0) {
                        val ctaHeightPx = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            ctaHeightDp.toFloat(),
                            displayMetrics
                        ).toInt()
                        val lp = callToActionView.layoutParams
                        if (lp != null) {
                            lp.height = ctaHeightPx
                            callToActionView.layoutParams = lp
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (nativeAd.icon == null) {
                adView.iconView?.visibility = View.GONE
            } else {
                val iconView = adView.iconView as? ImageView
                if (iconView != null) {
                    iconView.setImageDrawable(nativeAd.icon?.drawable)
                    iconView.visibility = View.VISIBLE

                    val layoutParams = iconView.layoutParams
                    if (layoutParams != null) {
                        val iconSizePx = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            finalConfig.getIconSize().toFloat(),
                            displayMetrics
                        ).toInt()
                        layoutParams.width = iconSizePx
                        layoutParams.height = iconSizePx
                        iconView.layoutParams = layoutParams
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (nativeAd.price == null) {
                adView.priceView?.visibility = View.INVISIBLE
            } else {
                adView.priceView?.visibility = View.VISIBLE
                val priceView = adView.priceView as? TextView
                if (priceView != null) {
                    priceView.text = nativeAd.price
                    priceView.setTextSize(TypedValue.COMPLEX_UNIT_SP, finalConfig.getPriceTextSize())
                    priceView.setTextColor(finalConfig.getPriceTextColor())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (nativeAd.starRating == null) {
                adView.starRatingView?.visibility = View.INVISIBLE
            } else {
                val starRatingView = adView.starRatingView as? RatingBar
                if (starRatingView != null) {
                    starRatingView.rating = nativeAd.starRating!!.toFloat()
                    starRatingView.visibility = View.VISIBLE

                    val layoutParams = starRatingView.layoutParams
                    if (layoutParams != null) {
                        val starSizePx = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            finalConfig.getStarRatingSize().toFloat(),
                            displayMetrics
                        ).toInt()
                        layoutParams.height = starSizePx
                        starRatingView.layoutParams = layoutParams
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (nativeAd.advertiser == null) {
                adView.advertiserView?.visibility = View.INVISIBLE
            } else {
                val advertiserView = adView.advertiserView as? TextView
                if (advertiserView != null) {
                    advertiserView.text = nativeAd.advertiser
                    advertiserView.visibility = View.VISIBLE
                    advertiserView.setTextSize(
                        TypedValue.COMPLEX_UNIT_SP,
                        finalConfig.getAdvertiserTextSize()
                    )
                    advertiserView.setTextColor(finalConfig.getAdvertiserTextColor())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        adView.registerNativeAd(nativeAd, mediaView)

        try {
            val cta = adView.callToActionView
            if (cta != null) {
                val bgColor = finalConfig.getCallToActionBackgroundColor()
                val drawable = GradientDrawable()
                drawable.setColor(bgColor)
                drawable.cornerRadius = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    finalConfig.getCallToActionCornerRadius(),
                    displayMetrics
                )
                cta.background = drawable
                if (cta is Button) {
                    cta.backgroundTintList = ColorStateList.valueOf(bgColor)
                }
            }
        } catch (ignored: Exception) {
        }

        showNativeDebugDialog(nativeAd, adView)
    }

    private fun bindStandardNativeAssets(nativeAd: NativeAd, adView: NativeAdView) {
        try {
            (adView.headlineView as? TextView)?.text = nativeAd.headline
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (nativeAd.body == null) {
                adView.bodyView?.visibility = View.INVISIBLE
            } else {
                adView.bodyView?.visibility = View.VISIBLE
                (adView.bodyView as? TextView)?.text = nativeAd.body
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (nativeAd.callToAction == null) {
                adView.callToActionView?.visibility = View.INVISIBLE
            } else {
                adView.callToActionView?.visibility = View.VISIBLE
                (adView.callToActionView as? TextView)?.text = nativeAd.callToAction
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (nativeAd.icon == null) {
                adView.iconView?.visibility = View.GONE
            } else {
                (adView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
                adView.iconView?.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (nativeAd.price == null) {
                adView.priceView?.visibility = View.INVISIBLE
            } else {
                adView.priceView?.visibility = View.VISIBLE
                (adView.priceView as? TextView)?.text = nativeAd.price
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (nativeAd.starRating == null) {
                adView.starRatingView?.visibility = View.INVISIBLE
            } else {
                (adView.starRatingView as? RatingBar)?.rating = nativeAd.starRating!!.toFloat()
                adView.starRatingView?.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (nativeAd.advertiser == null) {
                adView.advertiserView?.visibility = View.INVISIBLE
            } else {
                (adView.advertiserView as? TextView)?.text = nativeAd.advertiser
                adView.advertiserView?.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showNativeDebugDialog(nativeAd: NativeAd, adView: NativeAdView) {
        adView.post {
            val extra = StringBuilder()
            val density = adView.context.resources.displayMetrics.density
            var isMediaValid = true
            var isCtaValid = true

            val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
            if (mediaView != null) {
                val pxWidth = mediaView.width.toFloat()
                val pxHeight = mediaView.height.toFloat()
                val dpWidth = pxWidth / density
                val dpHeight = pxHeight / density

                if (dpWidth < 120f || dpHeight < 120f) {
                    isMediaValid = false
                    extra.append(
                        String.format(
                            "MediaView Size: %.1f x %.1f dp (%.0f x %.0f px)",
                            dpWidth,
                            dpHeight,
                            pxWidth,
                            pxHeight
                        )
                    )
                    extra.append("\nWarning: MediaView < 120x120 dp (Video ads disabled)\n")
                }
            }
            val ctaView = adView.callToActionView
            if (ctaView != null) {
                val pxHeight = ctaView.height.toFloat()
                val dpHeight = pxHeight / density

                if (dpHeight < 36f) {
                    isCtaValid = false
                    if (extra.isNotEmpty() && extra[extra.length - 1] != '\n') {
                        extra.append("\n")
                    }
                    extra.append(
                        String.format(
                            "CTA Height: %.1f dp (%.0f px)\nWarning: CTA target is too small (< 36 dp)",
                            dpHeight,
                            pxHeight
                        )
                    )
                }
            }

            val overallSuccess = isMediaValid && isCtaValid
            val issues = (if (isMediaValid) 0 else 1) + (if (isCtaValid) 0 else 1)

            AdDebugDialog.show(
                adView.context,
                adView,
                "NATIVE",
                "Native Ad",
                extra.toString().trim(),
                overallSuccess,
                issues
            )
        }
    }

    fun getCtaButtonHeight(nativeAdConfig: NativeAdConfig): Int {
        return try {
            var value = nativeAdConfig.getCallToActionHeight()
            value = if (NkhAd.getInstance().organic) {
                Math.min(Math.max(value, 36), 46)
            } else {
                Math.min(Math.max(value, 36), 52)
            }
            value
        } catch (ex: Exception) {
            40
        }
    }

    fun initRewardAds(context: Context, id: String, adCallback: RewardCallback?) {
        if (Arrays.asList(*context.resources.getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, REWARD_ADS, id)
        }
        if (AppPurchase.getInstance().isPurchased(context)) {
            return
        }
        this.nativeId = id
        this.rewardedAd = null
        runWhenSdkInitialized(Runnable {
            RewardedAd.load(
                getAdRequest(id),
                object : AdLoadCallback<RewardedAd> {
                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        runOnUiThread {
                            this@Admob.rewardedAd = rewardedAd
                            if (adCallback != null) {
                                rewardLoadCallbacks[rewardedAd] = adCallback
                            }
                            rewardedAd.adEventCallback = object : RewardedAdEventCallback {
                                override fun onAdPaid(adValue: AdValue) {
                                    runOnUiThread {
                                        handleRewardPaid(
                                            context,
                                            rewardedAd,
                                            rewardedAd.adUnitId,
                                            rewardedAd.getResponseInfo().adapterClassName,
                                            adValue,
                                            adCallback
                                        )
                                    }
                                }
                            }
                            Log.i(TAG, "RewardedAd onAdLoaded")
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        runOnUiThread {
                            Log.e(TAG, "RewardedAd onAdFailedToLoad: " + loadAdError.message)
                        }
                    }
                }
            )
        })
    }

    fun initRewardAds(context: Context, id: String, callback: AdCallback?) {
        if (Arrays.asList(*context.resources.getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, REWARD_ADS, id)
        }
        if (AppPurchase.getInstance().isPurchased(context)) {
            return
        }
        this.nativeId = id
        this.rewardedAd = null
        runWhenSdkInitialized(Runnable {
            RewardedAd.load(
                getAdRequest(id),
                object : AdLoadCallback<RewardedAd> {
                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        runOnUiThread {
                            this@Admob.rewardedAd = rewardedAd
                            if (callback != null) {
                                rewardLoadCallbacks[rewardedAd] = callback
                            }
                            rewardedAd.adEventCallback = object : RewardedAdEventCallback {
                                override fun onAdPaid(adValue: AdValue) {
                                    runOnUiThread {
                                        handleRewardPaid(
                                            context,
                                            rewardedAd,
                                            rewardedAd.adUnitId,
                                            rewardedAd.getResponseInfo().adapterClassName,
                                            adValue,
                                            callback
                                        )
                                    }
                                }
                            }
                            callback?.onRewardAdLoaded(rewardedAd)
                            Log.i(TAG, "RewardedAd onAdLoaded")
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        runOnUiThread {
                            callback?.onAdFailedToLoad(loadAdError)
                            this@Admob.rewardedAd = null
                            Log.e(TAG, "RewardedAd onAdFailedToLoad: " + loadAdError.message)
                        }
                    }
                }
            )
        })
    }

    fun getRewardInterstitial(context: Context, id: String, callback: AdCallback?) {
        if (Arrays.asList(*context.resources.getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, REWARD_ADS, id)
        }
        if (AppPurchase.getInstance().isPurchased(context)) {
            return
        }
        this.nativeId = id
        runWhenSdkInitialized(Runnable {
            RewardedInterstitialAd.load(
                getAdRequest(id),
                object : AdLoadCallback<RewardedInterstitialAd> {
                    override fun onAdLoaded(rewardedAd: RewardedInterstitialAd) {
                        runOnUiThread {
                            if (callback != null) {
                                rewardLoadCallbacks[rewardedAd] = callback
                            }
                            rewardedAd.adEventCallback = object : RewardedInterstitialAdEventCallback {
                                override fun onAdPaid(adValue: AdValue) {
                                    runOnUiThread {
                                        handleRewardPaid(
                                            context,
                                            rewardedAd,
                                            rewardedAd.adUnitId,
                                            rewardedAd.getResponseInfo().adapterClassName,
                                            adValue,
                                            callback
                                        )
                                    }
                                }
                            }
                            callback?.onRewardAdLoaded(rewardedAd)
                            Log.i(TAG, "RewardInterstitial onAdLoaded ")
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        runOnUiThread {
                            callback?.onAdFailedToLoad(loadAdError)
                            Log.e(TAG, "RewardInterstitial onAdFailedToLoad: " + loadAdError.message)
                        }
                    }
                }
            )
        })
    }

    fun getRewardedAd(): RewardedAd? = this.rewardedAd

    fun showRewardAds(context: Activity, adCallback: RewardCallback?) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            adCallback?.onUserEarnedReward(null)
            return
        }
        val ad = rewardedAd
        if (ad == null) {
            initRewardAds(context, nativeId, adCallback)
            adCallback?.onRewardedAdFailedToShow(0)
            return
        }
        ad.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdPaid(adValue: AdValue) {
                runOnUiThread {
                    handleRewardPaid(
                        context,
                        ad,
                        ad.adUnitId,
                        ad.getResponseInfo().adapterClassName,
                        adValue,
                        adCallback
                    )
                }
            }

            override fun onAdDismissedFullScreenContent() {
                runOnUiThread {
                    AppOpenManager.getInstance().setInterstitialShowing(false)
                    adCallback?.onRewardedAdClosed()
                    Log.d(TAG, "onAdDismissedFullScreenContent")
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                runOnUiThread {
                    Log.e(TAG, "onAdFailedToShowFullScreenContent: " + adError.message)
                    AppOpenManager.getInstance().setInterstitialShowing(false)
                    adCallback?.onRewardedAdFailedToShow(adError.code.ordinal)
                }
            }

            override fun onAdShowedFullScreenContent() {
                runOnUiThread {
                    Log.d(TAG, "onAdShowedFullScreenContent")
                    AppOpenManager.getInstance().setInterstitialShowing(true)
                    rewardedAd = null
                }
            }

            override fun onAdClicked() {
                runOnUiThread {
                    if (disableAdResumeWhenClickAds) {
                        AppOpenManager.getInstance().disableAdResumeByClickAction()
                    }
                    adCallback?.onAdClicked()
                    adCallback?.onAdClicked(
                        ad.adUnitId,
                        ad.getResponseInfo().adapterClassName,
                        AdType.REWARDED
                    )
                    NkhLogEventManager.logClickAdsEvent(context, ad.adUnitId)
                }
            }

            override fun onAdImpression() {
                runOnUiThread {
                    adCallback?.onAdImpression()
                }
            }
        }
        ad.show(context) { rewardItem: RewardItem ->
            runOnUiThread {
                adCallback?.onUserEarnedReward(rewardItem)
            }
        }
    }

    fun showRewardInterstitial(
        activity: Activity,
        rewardedInterstitialAd: RewardedInterstitialAd?,
        adCallback: RewardCallback?
    ) {
        if (AppPurchase.getInstance().isPurchased(activity)) {
            adCallback?.onUserEarnedReward(null)
            return
        }
        if (rewardedInterstitialAd == null) {
            initRewardAds(activity, nativeId, adCallback)
            adCallback?.onRewardedAdFailedToShow(0)
            return
        }
        val ad = rewardedInterstitialAd
        ad.adEventCallback = object : RewardedInterstitialAdEventCallback {
            override fun onAdPaid(adValue: AdValue) {
                runOnUiThread {
                    handleRewardPaid(
                        activity,
                        ad,
                        ad.adUnitId,
                        ad.getResponseInfo().adapterClassName,
                        adValue,
                        adCallback
                    )
                }
            }

            override fun onAdDismissedFullScreenContent() {
                runOnUiThread {
                    AppOpenManager.getInstance().setInterstitialShowing(false)
                    adCallback?.onRewardedAdClosed()
                    Log.d(TAG, "onAdDismissedFullScreenContent")
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                runOnUiThread {
                    Log.e(TAG, "onAdFailedToShowFullScreenContent: " + adError.message)
                    AppOpenManager.getInstance().setInterstitialShowing(false)
                    adCallback?.onRewardedAdFailedToShow(adError.code.ordinal)
                }
            }

            override fun onAdShowedFullScreenContent() {
                runOnUiThread {
                    Log.d(TAG, "onAdShowedFullScreenContent")
                    AppOpenManager.getInstance().setInterstitialShowing(true)
                }
            }

            override fun onAdClicked() {
                runOnUiThread {
                    if (disableAdResumeWhenClickAds) {
                        AppOpenManager.getInstance().disableAdResumeByClickAction()
                    }
                    NkhLogEventManager.logClickAdsEvent(activity, ad.adUnitId)
                    adCallback?.onAdClicked()
                    adCallback?.onAdClicked(
                        ad.adUnitId,
                        ad.getResponseInfo().adapterClassName,
                        AdType.REWARDED
                    )
                }
            }

            override fun onAdImpression() {
                runOnUiThread {
                    adCallback?.onAdImpression()
                }
            }
        }
        ad.show(activity) { rewardItem: RewardItem ->
            runOnUiThread {
                adCallback?.onUserEarnedReward(rewardItem)
            }
        }
    }

    fun showRewardAds(context: Activity, rewardedAd: RewardedAd?, adCallback: RewardCallback?) {
        if (AppPurchase.getInstance().isPurchased(context)) {
            adCallback?.onUserEarnedReward(null)
            return
        }
        if (rewardedAd == null) {
            initRewardAds(context, nativeId, adCallback)
            adCallback?.onRewardedAdFailedToShow(0)
            return
        }
        val ad = rewardedAd
        ad.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdPaid(adValue: AdValue) {
                runOnUiThread {
                    handleRewardPaid(
                        context,
                        ad,
                        ad.adUnitId,
                        ad.getResponseInfo().adapterClassName,
                        adValue,
                        adCallback
                    )
                }
            }

            override fun onAdDismissedFullScreenContent() {
                runOnUiThread {
                    AppOpenManager.getInstance().setInterstitialShowing(false)
                    adCallback?.onRewardedAdClosed()
                    Log.d(TAG, "onAdDismissedFullScreenContent")
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                runOnUiThread {
                    Log.e(TAG, "onAdFailedToShowFullScreenContent: " + adError.message)
                    AppOpenManager.getInstance().setInterstitialShowing(false)
                    adCallback?.onRewardedAdFailedToShow(adError.code.ordinal)
                }
            }

            override fun onAdShowedFullScreenContent() {
                runOnUiThread {
                    Log.d(TAG, "onAdShowedFullScreenContent")
                    AppOpenManager.getInstance().setInterstitialShowing(true)
                    initRewardAds(context, nativeId, adCallback)
                }
            }

            override fun onAdClicked() {
                runOnUiThread {
                    if (disableAdResumeWhenClickAds) {
                        AppOpenManager.getInstance().disableAdResumeByClickAction()
                    }
                    adCallback?.onAdClicked()
                    adCallback?.onAdClicked(
                        ad.adUnitId,
                        ad.getResponseInfo().adapterClassName,
                        AdType.REWARDED
                    )
                    NkhLogEventManager.logClickAdsEvent(context, ad.adUnitId)
                }
            }

            override fun onAdImpression() {
                runOnUiThread {
                    adCallback?.onAdImpression()
                }
            }
        }
        ad.show(context) { rewardItem: RewardItem ->
            runOnUiThread {
                adCallback?.onUserEarnedReward(rewardItem)
            }
        }
    }

    @SuppressLint("HardwareIds")
    fun getDeviceId(activity: Activity): String {
        val androidId = Settings.Secure.getString(
            activity.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        return md5(androidId).uppercase(Locale.getDefault())
    }

    private fun md5(s: String): String {
        try {
            val digest = MessageDigest.getInstance("MD5")
            digest.update(s.toByteArray())
            val messageDigest = digest.digest()
            val hexString = StringBuilder()
            for (b in messageDigest) {
                var h = Integer.toHexString(0xFF and b.toInt())
                while (h.length < 2) h = "0$h"
                hexString.append(h)
            }
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            // ignore
        }
        return ""
    }

    @SuppressLint("MissingPermission")
    private fun showTestIdAlert(context: Context, typeAds: Int, id: String?) {
        val content = when (typeAds) {
            BANNER_ADS -> "Banner Ads: "
            INTERS_ADS -> "Interstitial Ads: "
            REWARD_ADS -> "Rewarded Ads: "
            NATIVE_ADS -> "Native Ads: "
            PIP_ADS -> "PiP Ads: "
            SWIPEABLE_ADS -> "Swipeable Ads: "
            else -> "App Open Ads: "
        } + id
        val notification = NotificationCompat.Builder(context, "warning_ads")
            .setContentTitle("Found test ad id")
            .setContentText(content)
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
        notificationManager.notify(typeAds, notification)
    }

    // Load Priority Inter Splash (4 same time & 2 same time)
    private var isInterHigh1Failed = false
    private var isInterHigh2Loaded = false
    private var isInterHigh3Loaded = false
    private var isInterNormalLoaded = false

    fun loadInterSplashPriority4SameTime(
        context: Context,
        idAdsHigh1: String,
        idAdsHigh2: String,
        idAdsHigh3: String,
        idAdsNormal: String,
        timeOut: Long,
        timeDelay: Long,
        adListener: AdCallback?
    ) {
        isInterHigh1Failed = false
        isInterHigh2Loaded = false
        isInterHigh3Loaded = false
        isInterNormalLoaded = false

        loadInterSplashHigh1(context, idAdsHigh1, timeOut, timeDelay, object : AdCallback() {
            override fun onAdSplashReady() {
                super.onAdSplashReady()
                adListener?.onAdSplashHigh1Ready()
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                adListener?.onAdPriorityFailedToLoad(i)
            }

            override fun onNextAction() {
                super.onNextAction()
                if (!isInterHigh1Failed) {
                    if (isInterHigh2Loaded && mInterSplashHigh2 != null) {
                        adListener?.onAdSplashHigh2Ready()
                        isInterHigh1Failed = true
                    } else if (isInterHigh3Loaded && mInterSplashHigh3 != null) {
                        adListener?.onAdSplashHigh3Ready()
                        isInterHigh1Failed = true
                    } else if (isInterHigh3Loaded && isInterNormalLoaded) {
                        adListener?.onAdSplashNormalReady()
                        isInterHigh1Failed = true
                    } else {
                        isInterHigh1Failed = true
                    }
                }
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
        })

        loadInterSplashHigh2(context, idAdsHigh2, timeOut, timeDelay, object : AdCallback() {
            override fun onAdSplashReady() {
                super.onAdSplashReady()
                if (isInterHigh1Failed) {
                    adListener?.onAdSplashHigh2Ready()
                } else {
                    isInterHigh2Loaded = true
                }
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                adListener?.onAdPriorityFailedToLoad(i)
            }

            override fun onNextAction() {
                super.onNextAction()
                if (isInterHigh1Failed && !isInterHigh2Loaded) {
                    if (isInterHigh3Loaded && mInterSplashHigh3 != null) {
                        adListener?.onAdSplashHigh3Ready()
                        isInterHigh2Loaded = true
                    } else if (isInterNormalLoaded && mInterSplashNormal != null) {
                        adListener?.onAdSplashNormalReady()
                        isInterHigh2Loaded = true
                    } else {
                        isInterHigh2Loaded = true
                    }
                } else {
                    isInterHigh2Loaded = true
                }
            }

            override fun onAdLogRev(
                adValue: AdValue?,
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
                super.onAdLogRev(adValue, adValue?.let { adUnitId }, mediationAdapterClassName, adType)
                adListener?.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
            }
        })

        loadInterSplashHigh3(context, idAdsHigh3, timeOut, timeDelay, object : AdCallback() {
            override fun onAdSplashReady() {
                super.onAdSplashReady()
                if (isInterHigh1Failed && isInterHigh2Loaded) {
                    adListener?.onAdSplashHigh3Ready()
                } else {
                    isInterHigh3Loaded = true
                }
            }

            override fun onNextAction() {
                super.onNextAction()
                if (isInterHigh1Failed && isInterHigh2Loaded && !isInterHigh3Loaded) {
                    if (isInterNormalLoaded && mInterSplashNormal != null) {
                        adListener?.onAdSplashNormalReady()
                        isInterHigh3Loaded = true
                    } else {
                        isInterHigh3Loaded = true
                    }
                } else {
                    isInterHigh3Loaded = true
                }
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                adListener?.onAdPriorityFailedToLoad(i)
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
        })

        loadInterSplashNormal(context, idAdsNormal, timeOut, timeDelay, object : AdCallback() {
            override fun onAdSplashReady() {
                super.onAdSplashReady()
                if (isInterHigh1Failed && isInterHigh2Loaded && isInterHigh3Loaded) {
                    adListener?.onAdSplashNormalReady()
                } else {
                    isInterNormalLoaded = true
                }
            }

            override fun onNextAction() {
                super.onNextAction()
                if (isInterHigh1Failed && isInterHigh2Loaded && isInterHigh3Loaded) {
                    adListener?.onAdSplashNormalReady()
                    isInterHigh1Failed = true
                    isInterHigh2Loaded = true
                    isInterHigh3Loaded = true
                    isInterNormalLoaded = true
                } else {
                    isInterNormalLoaded = true
                }
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                adListener?.onAdFailedToLoad(i)
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
        })
    }

    private var isFailedPriority = false

    private fun createForwardingSplashCallback(
        adListener: AdCallback?,
        includeInterstitialShow: Boolean = false,
        onFailFallback: (FullScreenContentError?) -> Unit,
        onNext: () -> Unit
    ): AdCallback {
        return object : AdCallback() {
            override fun onAdClosed() {
                super.onAdClosed()
                adListener?.onAdClosed()
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

            override fun onAdImpression() {
                super.onAdImpression()
                adListener?.onAdImpression()
            }

            override fun onInterstitialShow() {
                super.onInterstitialShow()
                if (includeInterstitialShow) {
                    adListener?.onInterstitialShow()
                }
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

            override fun onAdFailedToShow(adError: FullScreenContentError?) {
                super.onAdFailedToShow(adError)
                onFailFallback(adError)
            }

            override fun onNextAction() {
                super.onNextAction()
                onNext()
            }
        }
    }

    fun onShowSplashPriority4(activity: AppCompatActivity, adListener: AdCallback?) {
        isFailedPriority = false
        when {
            mInterSplashHigh1 != null -> {
                onShowSplashHigh1(
                    activity,
                    createForwardingSplashCallback(
                        adListener = adListener,
                        includeInterstitialShow = true,
                        onFailFallback = { adError ->
                            isFailedPriority = true
                            adListener?.onAdPriorityFailedToShow(adError)
                            onShowSplashHigh2(
                                activity,
                                createForwardingSplashCallback(
                                    adListener = adListener,
                                    onFailFallback = { err2 ->
                                        isFailedPriority = true
                                        adListener?.onAdPriorityFailedToShow(err2)
                                        onShowSplashHigh3(
                                            activity,
                                            createForwardingSplashCallback(
                                                adListener = adListener,
                                                onFailFallback = { err3 ->
                                                    isFailedPriority = true
                                                    adListener?.onAdPriorityFailedToShow(err3)
                                                    onShowSplashNormal(
                                                        activity,
                                                        createForwardingSplashCallback(
                                                            adListener = adListener,
                                                            onFailFallback = { err4 ->
                                                                adListener?.onAdFailedToShow(err4)
                                                            },
                                                            onNext = {
                                                                adListener?.onNextAction()
                                                            }
                                                        )
                                                    )
                                                },
                                                onNext = {
                                                    if (!isFailedPriority) {
                                                        adListener?.onNextAction()
                                                    }
                                                }
                                            )
                                        )
                                    },
                                    onNext = {
                                        if (!isFailedPriority) {
                                            adListener?.onNextAction()
                                        }
                                    }
                                )
                            )
                        },
                        onNext = {
                            if (!isFailedPriority) {
                                adListener?.onNextAction()
                            }
                        }
                    )
                )
            }
            mInterSplashHigh2 != null -> {
                onShowSplashHigh2(
                    activity,
                    createForwardingSplashCallback(
                        adListener = adListener,
                        onFailFallback = { err2 ->
                            isFailedPriority = true
                            adListener?.onAdPriorityFailedToShow(err2)
                            onShowSplashHigh3(
                                activity,
                                createForwardingSplashCallback(
                                    adListener = adListener,
                                    onFailFallback = { err3 ->
                                        isFailedPriority = true
                                        adListener?.onAdPriorityFailedToShow(err3)
                                        onShowSplashNormal(
                                            activity,
                                            createForwardingSplashCallback(
                                                adListener = adListener,
                                                onFailFallback = { err4 ->
                                                    adListener?.onAdFailedToShow(err4)
                                                },
                                                onNext = {
                                                    adListener?.onNextAction()
                                                }
                                            )
                                        )
                                    },
                                    onNext = {
                                        if (!isFailedPriority) {
                                            adListener?.onNextAction()
                                        }
                                    }
                                )
                            )
                        },
                        onNext = {
                            if (!isFailedPriority) {
                                adListener?.onNextAction()
                            }
                        }
                    )
                )
            }
            mInterSplashHigh3 != null -> {
                onShowSplashHigh3(
                    activity,
                    createForwardingSplashCallback(
                        adListener = adListener,
                        onFailFallback = { err3 ->
                            isFailedPriority = true
                            adListener?.onAdPriorityFailedToShow(err3)
                            onShowSplashNormal(
                                activity,
                                createForwardingSplashCallback(
                                    adListener = adListener,
                                    onFailFallback = { err4 ->
                                        adListener?.onAdFailedToShow(err4)
                                    },
                                    onNext = {
                                        adListener?.onNextAction()
                                    }
                                )
                            )
                        },
                        onNext = {
                            if (!isFailedPriority) {
                                adListener?.onNextAction()
                            }
                        }
                    )
                )
            }
            mInterSplashNormal != null -> {
                onShowSplashNormal(
                    activity,
                    createForwardingSplashCallback(
                        adListener = adListener,
                        onFailFallback = { err4 ->
                            adListener?.onAdFailedToShow(err4)
                        },
                        onNext = {
                            adListener?.onNextAction()
                        }
                    )
                )
            }
            else -> {
                adListener?.onNextAction()
            }
        }
    }

    private var mInterSplashHigh1: InterstitialAd? = null
    private var handlerTimeoutHigh1: Handler? = null
    private var rdTimeoutHigh1: Runnable? = null
    private var isTimeDelayHigh1 = false
    private var isTimeoutHigh1 = false
    private var isShowLoadingSplashHigh1 = false
    private var dialogHigh1: PrepareLoadingAdsDialog? = null

    private fun loadInterSplashHigh1(
        context: Context,
        id: String,
        timeOut: Long,
        timeDelay: Long,
        adListener: AdCallback?
    ) {
        isTimeDelayHigh1 = false
        isTimeoutHigh1 = false
        if (AppPurchase.getInstance().isPurchased(context)) {
            adListener?.onNextAction()
            return
        }
        Handler().postDelayed({
            if (mInterSplashHigh1 != null) {
                Log.d(TAG, "loadSplashInterstitialAds:show ad on delay ")
                adListener?.onAdSplashReady()
                return@postDelayed
            }
            Log.d(TAG, "loadSplashInterstitialAds: delay validate")
            isTimeDelayHigh1 = true
        }, timeDelay)

        if (timeOut > 0) {
            handlerTimeoutHigh1 = Handler()
            rdTimeoutHigh1 = Runnable {
                Log.e(TAG, "loadSplashInterstitialAds: on timeout")
                isTimeoutHigh1 = true
                if (mInterSplashHigh1 != null) {
                    Log.i(TAG, "loadSplashInterstitialAds:show ad on timeout ")
                    adListener?.onAdSplashReady()
                    return@Runnable
                }
                if (adListener != null) {
                    adListener.onNextAction()
                    isShowLoadingSplashHigh1 = false
                }
            }
            handlerTimeoutHigh1?.postDelayed(rdTimeoutHigh1!!, timeOut)
        }

        isShowLoadingSplashHigh1 = true
        getInterstitialAds(context, id, object : AdCallback() {
            override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                super.onInterstitialLoad(interstitialAd)
                if (isTimeoutHigh1) return
                if (interstitialAd != null) {
                    mInterSplashHigh1 = interstitialAd
                    if (isTimeDelayHigh1) {
                        adListener?.onAdSplashReady()
                    }
                }
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                isShowLoadingSplashHigh1 = false
                if (isTimeoutHigh1) return
                if (adListener != null) {
                    if (handlerTimeoutHigh1 != null && rdTimeoutHigh1 != null) {
                        handlerTimeoutHigh1?.removeCallbacks(rdTimeoutHigh1!!)
                    }
                    if (i != null) {
                        Log.e(TAG, "loadSplashInterstitialAds: load fail " + i.message)
                    }
                    adListener.onAdFailedToLoad(i)
                    adListener.onNextAction()
                }
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
        })
    }

    private fun cancelAllSplashPriorityTimeouts() {
        if (handlerTimeoutHigh1 != null && rdTimeoutHigh1 != null) {
            handlerTimeoutHigh1?.removeCallbacks(rdTimeoutHigh1!!)
        }
        if (handlerTimeoutHigh2 != null && rdTimeoutHigh2 != null) {
            handlerTimeoutHigh2?.removeCallbacks(rdTimeoutHigh2!!)
        }
        if (handlerTimeoutHigh3 != null && rdTimeoutHigh3 != null) {
            handlerTimeoutHigh3?.removeCallbacks(rdTimeoutHigh3!!)
        }
        if (handlerTimeoutNormal != null && rdTimeoutNormal != null) {
            handlerTimeoutNormal?.removeCallbacks(rdTimeoutNormal!!)
        }
    }

    fun onShowSplashHigh1(activity: AppCompatActivity, adListener: AdCallback?) {
        isShowLoadingSplashHigh1 = true
        if (mInterSplashHigh1 == null) {
            adListener?.onNextAction()
            return
        }
        cancelAllSplashPriorityTimeouts()
        adListener?.onInterstitialShow()
        val ad = mInterSplashHigh1!!
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdPaid(adValue: AdValue) {
                runOnUiThread {
                    val logCtx = this@Admob.context ?: activity.applicationContext
                    handleInterstitialPaid(logCtx, ad, adValue, adListener)
                }
            }

            override fun onAdShowedFullScreenContent() {
                runOnUiThread {
                    AppOpenManager.getInstance().setInterstitialShowing(true)
                    isShowLoadingSplashHigh1 = false
                    if (!openActivityAfterShowInterAds) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                dialogHigh1?.dismiss()
                            } catch (_: Exception) {
                            }
                        }, 200L)
                    }
                }
            }

            override fun onAdDismissedFullScreenContent() {
                runOnUiThread {
                    AppOpenManager.getInstance().setInterstitialShowing(false)
                    mInterSplashHigh1 = null
                    try {
                        dialogHigh1?.dismiss()
                    } catch (_: Exception) {
                    }
                    if (adListener != null) {
                        if (!openActivityAfterShowInterAds) {
                            adListener.onNextAction()
                        }
                        adListener.onAdClosed()
                    }
                    isShowLoadingSplashHigh1 = false
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                runOnUiThread {
                    mInterSplashHigh1 = null
                    isShowLoadingSplashHigh1 = false
                    try {
                        dialogHigh1?.dismiss()
                    } catch (_: Exception) {
                    }
                    if (adListener != null) {
                        adListener.onAdFailedToShow(adError)
                        if (!openActivityAfterShowInterAds) {
                            adListener.onNextAction()
                        }
                    }
                }
            }

            override fun onAdClicked() {
                runOnUiThread {
                    if (disableAdResumeWhenClickAds) {
                        AppOpenManager.getInstance().disableAdResumeByClickAction()
                    }
                    if (adListener != null) {
                        adListener.onAdClicked()
                        adListener.onAdClicked(
                            ad.adUnitId,
                            ad.getResponseInfo().adapterClassName,
                            AdType.INTERSTITIAL
                        )
                    }
                    NkhLogEventManager.logClickAdsEvent(context ?: activity, ad.adUnitId)
                }
            }

            override fun onAdImpression() {
                runOnUiThread {
                    adListener?.onAdImpression()
                }
            }
        }

        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            try {
                if (dialogHigh1 != null && dialogHigh1!!.isShowing) {
                    dialogHigh1?.dismiss()
                }
                dialogHigh1 = PrepareLoadingAdsDialog(activity)
                try {
                    dialogHigh1?.show()
                } catch (e: Exception) {
                    adListener?.onNextAction()
                    return
                }
            } catch (e: Exception) {
                dialogHigh1 = null
                e.printStackTrace()
            }
            Handler().postDelayed({
                if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                    openActivityAfterShowInterAds &&
                    adListener != null
                ) {
                    adListener.onNextAction()
                    Handler().postDelayed({
                        if (dialogHigh1 != null && dialogHigh1!!.isShowing && !activity.isDestroyed) {
                            dialogHigh1?.dismiss()
                        }
                    }, 1500)
                }
                if (mInterSplashHigh1 != null &&
                    dialogHigh1 != null &&
                    !activity.isDestroyed &&
                    !activity.isFinishing
                ) {
                    mInterSplashHigh1?.show(activity)
                    isShowLoadingSplashHigh1 = false
                } else if (mInterSplashHigh1 == null) {
                    dialogHigh1?.dismiss()
                    adListener?.onNextAction()
                    isShowLoadingSplashHigh1 = false
                }
            }, 800)
        } else {
            isShowLoadingSplashHigh1 = false
        }
    }

    private var mInterSplashHigh2: InterstitialAd? = null
    private var handlerTimeoutHigh2: Handler? = null
    private var rdTimeoutHigh2: Runnable? = null
    private var isTimeDelayHigh2 = false
    private var isTimeoutHigh2 = false
    private var isShowLoadingSplashHigh2 = false
    private var dialogHigh2: PrepareLoadingAdsDialog? = null

    private fun loadInterSplashHigh2(
        context: Context,
        id: String,
        timeOut: Long,
        timeDelay: Long,
        adListener: AdCallback?
    ) {
        isTimeDelayHigh2 = false
        isTimeoutHigh2 = false
        if (AppPurchase.getInstance().isPurchased(context)) {
            adListener?.onNextAction()
            return
        }
        Handler().postDelayed({
            if (mInterSplashHigh2 != null) {
                adListener?.onAdSplashReady()
                return@postDelayed
            }
            isTimeDelayHigh2 = true
        }, timeDelay)

        if (timeOut > 0) {
            handlerTimeoutHigh2 = Handler()
            rdTimeoutHigh2 = Runnable {
                isTimeoutHigh2 = true
                if (mInterSplashHigh2 != null) {
                    adListener?.onAdSplashReady()
                    return@Runnable
                }
                if (adListener != null) {
                    adListener.onNextAction()
                    isShowLoadingSplashHigh2 = false
                }
            }
            handlerTimeoutHigh2?.postDelayed(rdTimeoutHigh2!!, timeOut)
        }

        isShowLoadingSplashHigh2 = true
        getInterstitialAds(context, id, object : AdCallback() {
            override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                super.onInterstitialLoad(interstitialAd)
                if (isTimeoutHigh2) return
                if (interstitialAd != null) {
                    mInterSplashHigh2 = interstitialAd
                    if (isTimeDelayHigh2) {
                        adListener?.onAdSplashReady()
                    }
                }
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                isShowLoadingSplashHigh2 = false
                if (isTimeoutHigh2) return
                if (adListener != null) {
                    if (handlerTimeoutHigh2 != null && rdTimeoutHigh2 != null) {
                        handlerTimeoutHigh2?.removeCallbacks(rdTimeoutHigh2!!)
                    }
                    adListener.onAdFailedToLoad(i)
                    adListener.onNextAction()
                }
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
        })
    }

    fun onShowSplashHigh2(activity: AppCompatActivity, adListener: AdCallback?) {
        isShowLoadingSplashHigh2 = true
        if (mInterSplashHigh2 == null) {
            adListener?.onNextAction()
            return
        }
        cancelAllSplashPriorityTimeouts()
        adListener?.onInterstitialShow()
        val ad = mInterSplashHigh2!!
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdPaid(adValue: AdValue) {
                runOnUiThread {
                    val logCtx = this@Admob.context ?: activity.applicationContext
                    handleInterstitialPaid(logCtx, ad, adValue, adListener)
                }
            }

            override fun onAdShowedFullScreenContent() {
                runOnUiThread {
                    AppOpenManager.getInstance().setInterstitialShowing(true)
                    isShowLoadingSplashHigh2 = false
                    if (!openActivityAfterShowInterAds) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                dialogHigh2?.dismiss()
                            } catch (_: Exception) {
                            }
                        }, 200L)
                    }
                }
            }

            override fun onAdDismissedFullScreenContent() {
                runOnUiThread {
                    AppOpenManager.getInstance().setInterstitialShowing(false)
                    mInterSplashHigh2 = null
                    try {
                        dialogHigh2?.dismiss()
                    } catch (_: Exception) {
                    }
                    if (adListener != null) {
                        if (!openActivityAfterShowInterAds) {
                            adListener.onNextAction()
                        }
                        adListener.onAdClosed()
                    }
                    isShowLoadingSplashHigh2 = false
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                runOnUiThread {
                    mInterSplashHigh2 = null
                    isShowLoadingSplashHigh2 = false
                    try {
                        dialogHigh2?.dismiss()
                    } catch (_: Exception) {
                    }
                    if (adListener != null) {
                        adListener.onAdFailedToShow(adError)
                        if (!openActivityAfterShowInterAds) {
                            adListener.onNextAction()
                        }
                    }
                }
            }

            override fun onAdClicked() {
                runOnUiThread {
                    if (disableAdResumeWhenClickAds) {
                        AppOpenManager.getInstance().disableAdResumeByClickAction()
                    }
                    if (adListener != null) {
                        adListener.onAdClicked()
                        adListener.onAdClicked(
                            ad.adUnitId,
                            ad.getResponseInfo().adapterClassName,
                            AdType.INTERSTITIAL
                        )
                    }
                    NkhLogEventManager.logClickAdsEvent(context ?: activity, ad.adUnitId)
                }
            }

            override fun onAdImpression() {
                runOnUiThread {
                    adListener?.onAdImpression()
                }
            }
        }

        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            try {
                if (dialogHigh2 != null && dialogHigh2!!.isShowing) {
                    dialogHigh2?.dismiss()
                }
                dialogHigh2 = PrepareLoadingAdsDialog(activity)
                try {
                    dialogHigh2?.show()
                } catch (e: Exception) {
                    adListener?.onNextAction()
                    return
                }
            } catch (e: Exception) {
                dialogHigh2 = null
                e.printStackTrace()
            }
            Handler().postDelayed({
                if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                    openActivityAfterShowInterAds &&
                    adListener != null
                ) {
                    adListener.onNextAction()
                    Handler().postDelayed({
                        if (dialogHigh2 != null && dialogHigh2!!.isShowing && !activity.isDestroyed) {
                            dialogHigh2?.dismiss()
                        }
                    }, 1500)
                }
                if (mInterSplashHigh2 != null &&
                    dialogHigh2 != null &&
                    !activity.isDestroyed &&
                    !activity.isFinishing
                ) {
                    mInterSplashHigh2?.show(activity)
                    isShowLoadingSplashHigh2 = false
                } else if (mInterSplashHigh2 == null) {
                    dialogHigh2?.dismiss()
                    adListener?.onNextAction()
                    isShowLoadingSplashHigh2 = false
                }
            }, 800)
        } else {
            isShowLoadingSplashHigh2 = false
        }
    }

    private var mInterSplashHigh3: InterstitialAd? = null
    private var handlerTimeoutHigh3: Handler? = null
    private var rdTimeoutHigh3: Runnable? = null
    private var isTimeDelayHigh3 = false
    private var isTimeoutHigh3 = false
    private var isShowLoadingSplashHigh3 = false
    private var dialogHigh3: PrepareLoadingAdsDialog? = null

    private fun loadInterSplashHigh3(
        context: Context,
        id: String,
        timeOut: Long,
        timeDelay: Long,
        adListener: AdCallback?
    ) {
        isTimeDelayHigh3 = false
        isTimeoutHigh3 = false
        if (AppPurchase.getInstance().isPurchased(context)) {
            adListener?.onNextAction()
            return
        }
        Handler().postDelayed({
            if (mInterSplashHigh3 != null) {
                adListener?.onAdSplashReady()
                return@postDelayed
            }
            isTimeDelayHigh3 = true
        }, timeDelay)

        if (timeOut > 0) {
            handlerTimeoutHigh3 = Handler()
            rdTimeoutHigh3 = Runnable {
                isTimeoutHigh3 = true
                if (mInterSplashHigh3 != null) {
                    adListener?.onAdSplashReady()
                    return@Runnable
                }
                if (adListener != null) {
                    adListener.onNextAction()
                    isShowLoadingSplashHigh3 = false
                }
            }
            handlerTimeoutHigh3?.postDelayed(rdTimeoutHigh3!!, timeOut)
        }

        isShowLoadingSplashHigh3 = true
        getInterstitialAds(context, id, object : AdCallback() {
            override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                super.onInterstitialLoad(interstitialAd)
                if (isTimeoutHigh3) return
                if (interstitialAd != null) {
                    mInterSplashHigh3 = interstitialAd
                    if (isTimeDelayHigh3) {
                        adListener?.onAdSplashReady()
                    }
                }
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                isShowLoadingSplashHigh3 = false
                if (isTimeoutHigh3) return
                if (adListener != null) {
                    if (handlerTimeoutHigh3 != null && rdTimeoutHigh3 != null) {
                        handlerTimeoutHigh3?.removeCallbacks(rdTimeoutHigh3!!)
                    }
                    adListener.onAdFailedToLoad(i)
                    adListener.onNextAction()
                }
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
        })
    }

    fun onShowSplashHigh3(activity: AppCompatActivity, adListener: AdCallback?) {
        isShowLoadingSplashHigh3 = true
        if (mInterSplashHigh3 == null) {
            adListener?.onNextAction()
            return
        }
        cancelAllSplashPriorityTimeouts()
        adListener?.onInterstitialShow()
        val ad = mInterSplashHigh3!!
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdPaid(adValue: AdValue) {
                runOnUiThread {
                    val logCtx = this@Admob.context ?: activity.applicationContext
                    handleInterstitialPaid(logCtx, ad, adValue, adListener)
                }
            }

            override fun onAdShowedFullScreenContent() {
                runOnUiThread {
                    AppOpenManager.getInstance().setInterstitialShowing(true)
                    isShowLoadingSplashHigh3 = false
                    if (!openActivityAfterShowInterAds) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                dialogHigh3?.dismiss()
                            } catch (_: Exception) {
                            }
                        }, 200L)
                    }
                }
            }

            override fun onAdDismissedFullScreenContent() {
                runOnUiThread {
                    AppOpenManager.getInstance().setInterstitialShowing(false)
                    mInterSplashHigh3 = null
                    try {
                        dialogHigh3?.dismiss()
                    } catch (_: Exception) {
                    }
                    if (adListener != null) {
                        if (!openActivityAfterShowInterAds) {
                            adListener.onNextAction()
                        }
                        adListener.onAdClosed()
                    }
                    isShowLoadingSplashHigh3 = false
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                runOnUiThread {
                    mInterSplashHigh3 = null
                    isShowLoadingSplashHigh3 = false
                    try {
                        dialogHigh3?.dismiss()
                    } catch (_: Exception) {
                    }
                    if (adListener != null) {
                        adListener.onAdFailedToShow(adError)
                        if (!openActivityAfterShowInterAds) {
                            adListener.onNextAction()
                        }
                    }
                }
            }

            override fun onAdClicked() {
                runOnUiThread {
                    if (disableAdResumeWhenClickAds) {
                        AppOpenManager.getInstance().disableAdResumeByClickAction()
                    }
                    if (adListener != null) {
                        adListener.onAdClicked()
                        adListener.onAdClicked(
                            ad.adUnitId,
                            ad.getResponseInfo().adapterClassName,
                            AdType.INTERSTITIAL
                        )
                    }
                    NkhLogEventManager.logClickAdsEvent(context ?: activity, ad.adUnitId)
                }
            }

            override fun onAdImpression() {
                runOnUiThread {
                    adListener?.onAdImpression()
                }
            }
        }

        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            try {
                if (dialogHigh3 != null && dialogHigh3!!.isShowing) {
                    dialogHigh3?.dismiss()
                }
                dialogHigh3 = PrepareLoadingAdsDialog(activity)
                try {
                    dialogHigh3?.show()
                } catch (e: Exception) {
                    adListener?.onNextAction()
                    return
                }
            } catch (e: Exception) {
                dialogHigh3 = null
                e.printStackTrace()
            }
            Handler().postDelayed({
                if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                    openActivityAfterShowInterAds &&
                    adListener != null
                ) {
                    adListener.onNextAction()
                    Handler().postDelayed({
                        if (dialogHigh3 != null && dialogHigh3!!.isShowing && !activity.isDestroyed) {
                            dialogHigh3?.dismiss()
                        }
                    }, 1500)
                }
                if (mInterSplashHigh3 != null &&
                    dialogHigh3 != null &&
                    !activity.isDestroyed &&
                    !activity.isFinishing
                ) {
                    mInterSplashHigh3?.show(activity)
                    isShowLoadingSplashHigh3 = false
                } else if (mInterSplashHigh3 == null) {
                    dialogHigh3?.dismiss()
                    adListener?.onNextAction()
                    isShowLoadingSplashHigh3 = false
                }
            }, 800)
        } else {
            isShowLoadingSplashHigh3 = false
        }
    }

    private var mInterSplashNormal: InterstitialAd? = null
    private var handlerTimeoutNormal: Handler? = null
    private var rdTimeoutNormal: Runnable? = null
    private var isTimeDelayNormal = false
    private var isTimeoutNormal = false
    private var isShowLoadingSplashNormal = false
    private var dialogNormal: PrepareLoadingAdsDialog? = null

    private fun loadInterSplashNormal(
        context: Context,
        id: String,
        timeOut: Long,
        timeDelay: Long,
        adListener: AdCallback?
    ) {
        isTimeDelayNormal = false
        isTimeoutNormal = false
        if (AppPurchase.getInstance().isPurchased(context)) {
            adListener?.onNextAction()
            return
        }
        Handler().postDelayed({
            if (mInterSplashNormal != null) {
                adListener?.onAdSplashReady()
                return@postDelayed
            }
            isTimeDelayNormal = true
        }, timeDelay)

        if (timeOut > 0) {
            handlerTimeoutNormal = Handler()
            rdTimeoutNormal = Runnable {
                isTimeoutNormal = true
                if (mInterSplashNormal != null) {
                    adListener?.onAdSplashReady()
                    return@Runnable
                }
                if (adListener != null) {
                    adListener.onNextAction()
                    isShowLoadingSplashNormal = false
                }
            }
            handlerTimeoutNormal?.postDelayed(rdTimeoutNormal!!, timeOut)
        }

        isShowLoadingSplashNormal = true
        getInterstitialAds(context, id, object : AdCallback() {
            override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                super.onInterstitialLoad(interstitialAd)
                if (isTimeoutNormal) return
                if (interstitialAd != null) {
                    mInterSplashNormal = interstitialAd
                    if (isTimeDelayNormal) {
                        adListener?.onAdSplashReady()
                    }
                }
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                isShowLoadingSplashNormal = false
                if (isTimeoutNormal) return
                if (adListener != null) {
                    if (handlerTimeoutNormal != null && rdTimeoutNormal != null) {
                        handlerTimeoutNormal?.removeCallbacks(rdTimeoutNormal!!)
                    }
                    adListener.onAdFailedToLoad(i)
                    adListener.onNextAction()
                }
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
        })
    }

    fun onShowSplashNormal(activity: AppCompatActivity, adListener: AdCallback?) {
        isShowLoadingSplashNormal = true
        if (mInterSplashNormal == null) {
            adListener?.onNextAction()
            return
        }
        cancelAllSplashPriorityTimeouts()
        adListener?.onInterstitialShow()
        val ad = mInterSplashNormal!!
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdPaid(adValue: AdValue) {
                runOnUiThread {
                    val logCtx = this@Admob.context ?: activity.applicationContext
                    handleInterstitialPaid(logCtx, ad, adValue, adListener)
                }
            }

            override fun onAdShowedFullScreenContent() {
                runOnUiThread {
                    AppOpenManager.getInstance().setInterstitialShowing(true)
                    isShowLoadingSplashNormal = false
                    if (!openActivityAfterShowInterAds) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                dialogNormal?.dismiss()
                            } catch (_: Exception) {
                            }
                        }, 200L)
                    }
                }
            }

            override fun onAdDismissedFullScreenContent() {
                runOnUiThread {
                    AppOpenManager.getInstance().setInterstitialShowing(false)
                    mInterSplashNormal = null
                    try {
                        dialogNormal?.dismiss()
                    } catch (_: Exception) {
                    }
                    if (adListener != null) {
                        if (!openActivityAfterShowInterAds) {
                            adListener.onNextAction()
                        }
                        adListener.onAdClosed()
                    }
                    isShowLoadingSplashNormal = false
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                runOnUiThread {
                    mInterSplashNormal = null
                    isShowLoadingSplashNormal = false
                    try {
                        dialogNormal?.dismiss()
                    } catch (_: Exception) {
                    }
                    if (adListener != null) {
                        adListener.onAdFailedToShow(adError)
                        if (!openActivityAfterShowInterAds) {
                            adListener.onNextAction()
                        }
                    }
                }
            }

            override fun onAdClicked() {
                runOnUiThread {
                    if (disableAdResumeWhenClickAds) {
                        AppOpenManager.getInstance().disableAdResumeByClickAction()
                    }
                    if (adListener != null) {
                        adListener.onAdClicked()
                        adListener.onAdClicked(
                            ad.adUnitId,
                            ad.getResponseInfo().adapterClassName,
                            AdType.INTERSTITIAL
                        )
                    }
                    NkhLogEventManager.logClickAdsEvent(context ?: activity, ad.adUnitId)
                }
            }

            override fun onAdImpression() {
                runOnUiThread {
                    adListener?.onAdImpression()
                }
            }
        }

        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            try {
                if (dialogNormal != null && dialogNormal!!.isShowing) {
                    dialogNormal?.dismiss()
                }
                dialogNormal = PrepareLoadingAdsDialog(activity)
                try {
                    dialogNormal?.show()
                } catch (e: Exception) {
                    adListener?.onNextAction()
                    return
                }
            } catch (e: Exception) {
                dialogNormal = null
                e.printStackTrace()
            }
            Handler().postDelayed({
                if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                    openActivityAfterShowInterAds &&
                    adListener != null
                ) {
                    adListener.onNextAction()
                    Handler().postDelayed({
                        if (dialogNormal != null && dialogNormal!!.isShowing && !activity.isDestroyed) {
                            dialogNormal?.dismiss()
                        }
                    }, 1500)
                }
                if (mInterSplashNormal != null &&
                    dialogNormal != null &&
                    !activity.isDestroyed &&
                    !activity.isFinishing
                ) {
                    mInterSplashNormal?.show(activity)
                    isShowLoadingSplashNormal = false
                } else if (mInterSplashNormal == null) {
                    dialogNormal?.dismiss()
                    adListener?.onNextAction()
                    isShowLoadingSplashNormal = false
                }
            }, 800)
        } else {
            isShowLoadingSplashNormal = false
        }
    }

    fun onCheckShowSplashPriority4WhenFail(
        activity: AppCompatActivity,
        callback: AdCallback?,
        timeDelay: Int
    ) {
        Handler(activity.mainLooper).postDelayed({
            if ((mInterSplashHigh1 != null || mInterSplashHigh2 != null || mInterSplashHigh3 != null || mInterSplashNormal != null) &&
                !isShowLoadingSplashHigh1 && !isShowLoadingSplashHigh2 && !isShowLoadingSplashHigh3 && !isShowLoadingSplashNormal
            ) {
                onShowSplashPriority4(activity, object : AdCallback() {
                    override fun onAdClosed() {
                        super.onAdClosed()
                        callback?.onAdClosed()
                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                        callback?.onAdImpression()
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        callback?.onAdClicked()
                    }

                    override fun onAdClicked(
                        adUnitId: String?,
                        mediationAdapterClassName: String?,
                        adType: AdType?
                    ) {
                        super.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                        callback?.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                    }

                    override fun onAdLogRev(
                        adValue: AdValue?,
                        adUnitId: String?,
                        mediationAdapterClassName: String?,
                        adType: AdType?
                    ) {
                        super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                        callback?.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                    }

                    override fun onAdPriorityFailedToShow(adError: FullScreenContentError?) {
                        super.onAdPriorityFailedToShow(adError)
                        callback?.onAdPriorityFailedToShow(adError)
                    }

                    override fun onAdFailedToShow(adError: FullScreenContentError?) {
                        super.onAdFailedToShow(adError)
                        callback?.onAdFailedToShow(adError)
                    }

                    override fun onNextAction() {
                        super.onNextAction()
                        callback?.onNextAction()
                    }
                })
            }
        }, timeDelay.toLong())
    }

    // Load 2 Priority Ads
    private var isInterHighFailed = false

    fun loadInterSplashPriority2SameTime(
        context: Context,
        idAdsHigh: String,
        idAdsNormal: String,
        timeOut: Long,
        timeDelay: Long,
        adListener: AdCallback?
    ) {
        isInterHighFailed = false
        isInterNormalLoaded = false

        loadInterSplashHigh1(context, idAdsHigh, timeOut, timeDelay, object : AdCallback() {
            override fun onAdSplashReady() {
                super.onAdSplashReady()
                adListener?.onAdSplashHigh1Ready()
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                adListener?.onAdPriorityFailedToLoad(i)
            }

            override fun onNextAction() {
                super.onNextAction()
                if (!isInterHighFailed) {
                    if (isInterNormalLoaded) {
                        adListener?.onAdSplashNormalReady()
                        isInterHighFailed = true
                    } else {
                        isInterHighFailed = true
                    }
                }
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
        })

        loadInterSplashNormal(context, idAdsNormal, timeOut, timeDelay, object : AdCallback() {
            override fun onAdSplashReady() {
                super.onAdSplashReady()
                if (isInterHighFailed) {
                    adListener?.onAdSplashNormalReady()
                } else {
                    isInterNormalLoaded = true
                }
            }

            override fun onNextAction() {
                super.onNextAction()
                if (isInterHighFailed) {
                    adListener?.onAdSplashNormalReady()
                    isInterHighFailed = true
                    isInterNormalLoaded = true
                } else {
                    isInterNormalLoaded = true
                }
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                adListener?.onAdFailedToLoad(i)
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
        })
    }

    fun onShowSplashPriority2(activity: AppCompatActivity, adListener: AdCallback?) {
        isFailedPriority = false
        if (mInterSplashHigh1 != null) {
            onShowSplashHigh1(
                activity,
                createForwardingSplashCallback(
                    adListener = adListener,
                    includeInterstitialShow = true,
                    onFailFallback = { adError ->
                        isFailedPriority = true
                        adListener?.onAdPriorityFailedToShow(adError)
                        onShowSplashNormal(
                            activity,
                            createForwardingSplashCallback(
                                adListener = adListener,
                                onFailFallback = { errNormal ->
                                    adListener?.onAdFailedToShow(errNormal)
                                },
                                onNext = {
                                    adListener?.onNextAction()
                                }
                            )
                        )
                    },
                    onNext = {
                        if (!isFailedPriority) {
                            adListener?.onNextAction()
                        }
                    }
                )
            )
        } else if (mInterSplashNormal != null) {
            onShowSplashNormal(
                activity,
                createForwardingSplashCallback(
                    adListener = adListener,
                    onFailFallback = { adError ->
                        adListener?.onAdFailedToShow(adError)
                    },
                    onNext = {
                        adListener?.onNextAction()
                    }
                )
            )
        } else {
            adListener?.onNextAction()
        }
    }

    fun onCheckShowSplashPriority2WhenFail(
        activity: AppCompatActivity,
        callback: AdCallback?,
        timeDelay: Int
    ) {
        Handler(activity.mainLooper).postDelayed({
            if ((mInterSplashHigh1 != null || mInterSplashNormal != null) &&
                !isShowLoadingSplashHigh1 && !isShowLoadingSplashNormal
            ) {
                onShowSplashPriority2(activity, object : AdCallback() {
                    override fun onAdClosed() {
                        super.onAdClosed()
                        callback?.onAdClosed()
                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                        callback?.onAdImpression()
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        callback?.onAdClicked()
                    }

                    override fun onAdClicked(
                        adUnitId: String?,
                        mediationAdapterClassName: String?,
                        adType: AdType?
                    ) {
                        super.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                        callback?.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                    }

                    override fun onAdLogRev(
                        adValue: AdValue?,
                        adUnitId: String?,
                        mediationAdapterClassName: String?,
                        adType: AdType?
                    ) {
                        super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                        callback?.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                    }

                    override fun onAdPriorityFailedToShow(adError: FullScreenContentError?) {
                        super.onAdPriorityFailedToShow(adError)
                        callback?.onAdPriorityFailedToShow(adError)
                    }

                    override fun onAdFailedToShow(adError: FullScreenContentError?) {
                        super.onAdFailedToShow(adError)
                        callback?.onAdFailedToShow(adError)
                    }

                    override fun onNextAction() {
                        super.onNextAction()
                        callback?.onNextAction()
                    }
                })
            }
        }, timeDelay.toLong())
    }

    private fun isNetworkConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
    }

    fun loadAndShowInterstitial(
        context: Context,
        id: String,
        timeOut: Long,
        callback: AdCallback?
    ) {
        if (!isNetworkConnected(context)) {
            callback?.onNextAction()
            return
        }

        if (AppPurchase.getInstance().isPurchased(context) && callback != null) {
            callback.onNextAction()
            return
        }

        isTimeout = false

        if (Arrays.asList(*context.resources.getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, INTERS_ADS, id)
        }

        if (AppPurchase.getInstance().isPurchased(context) ||
            AdmobHelper.getNumClickAdsPerDay(context, id) >= maxClickAds
        ) {
            callback?.onNextAction()
            return
        }

        if (dialog != null && dialog!!.isShowing) {
            dialog?.dismiss()
        }
        dialog = PrepareLoadingAdsDialog(context)
        dialog?.setCancelable(false)
        try {
            dialog?.show()
        } catch (e: Exception) {
            callback?.onNextAction()
            return
        }

        if (timeOut > 0) {
            handlerTimeout = Handler()
            rdTimeout = Runnable {
                Log.e(TAG, "loadSplashInterstitialAds: on timeout")
                isTimeout = true
                if (interstitialAd != null) {
                    forceShowInterstitial(context, interstitialAd, callback)
                    return@Runnable
                }
                if (callback != null) {
                    if (dialog != null && !(context as Activity).isDestroyed) {
                        dialog?.dismiss()
                    }
                    callback.onNextAction()
                }
            }
            handlerTimeout?.postDelayed(rdTimeout!!, timeOut)
        }

        runWhenSdkInitialized(Runnable {
            InterstitialAd.load(
                getAdRequest(id),
                object : AdLoadCallback<InterstitialAd> {
                    override fun onAdLoaded(loadedAd: InterstitialAd) {
                        runOnUiThread {
                            if (isTimeout) return@runOnUiThread

                            if (handlerTimeout != null && rdTimeout != null) {
                                handlerTimeout?.removeCallbacks(rdTimeout!!)
                            }

                            if (loadedAd != null) {
                                loadedAd.adEventCallback = object : InterstitialAdEventCallback {
                                    override fun onAdPaid(adValue: AdValue) {
                                        runOnUiThread {
                                            val logCtx = this@Admob.context ?: context.applicationContext
                                            handleInterstitialPaid(logCtx, loadedAd, adValue, callback)
                                        }
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        runOnUiThread {
                                            Log.d(TAG, "onAdShowedFullScreenContent ")
                                            callback?.onInterstitialShow()
                                            AppOpenManager.getInstance().setInterstitialShowing(true)
                                            isShowLoadingSplash = false
                                            if (!openActivityAfterShowInterAds) {
                                                Handler(Looper.getMainLooper()).postDelayed({
                                                    try {
                                                        dialog?.dismiss()
                                                    } catch (_: Exception) {
                                                    }
                                                }, 200L)
                                            }
                                        }
                                    }

                                    override fun onAdDismissedFullScreenContent() {
                                        runOnUiThread {
                                            Log.d(TAG, "onAdDismissedFullScreenContent ")
                                            AppOpenManager.getInstance().setInterstitialShowing(false)
                                            SharePreferenceUtils.setLastImpressionInterstitialTime(context)
                                            this@Admob.interstitialAd = null
                                            try {
                                                dialog?.dismiss()
                                            } catch (_: Exception) {
                                            }
                                            if (callback != null) {
                                                if (!openActivityAfterShowInterAds) {
                                                    callback.onNextAction()
                                                }
                                                callback.onAdClosed()
                                            }
                                            isShowLoadingSplash = false
                                        }
                                    }

                                    override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                                        runOnUiThread {
                                            Log.e(TAG, "onAdFailedToShowFullScreenContent : " + adError.message)
                                            AppOpenManager.getInstance().setInterstitialShowing(false)
                                            mInterstitialSplash = null
                                            isShowLoadingSplash = false
                                            try {
                                                dialog?.dismiss()
                                            } catch (_: Exception) {
                                            }
                                            if (callback != null) {
                                                callback.onAdFailedToShow(adError)
                                                if (!openActivityAfterShowInterAds) {
                                                    callback.onNextAction()
                                                }
                                            }
                                        }
                                    }

                                    override fun onAdClicked() {
                                        runOnUiThread {
                                            if (disableAdResumeWhenClickAds) {
                                                AppOpenManager.getInstance().disableAdResumeByClickAction()
                                            }
                                            callback?.onAdClicked()
                                            callback?.onAdClicked(
                                                loadedAd.adUnitId,
                                                loadedAd.getResponseInfo().adapterClassName,
                                                AdType.INTERSTITIAL
                                            )
                                            NkhLogEventManager.logClickAdsEvent(
                                                this@Admob.context ?: context,
                                                loadedAd.adUnitId
                                            )
                                        }
                                    }

                                    override fun onAdImpression() {
                                        runOnUiThread {
                                            callback?.onAdImpression()
                                        }
                                    }
                                }

                                this@Admob.interstitialAd = loadedAd
                                Handler(Looper.getMainLooper()).postDelayed({
                                    if ((context as AppCompatActivity).lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                                        openActivityAfterShowInterAds &&
                                        callback != null
                                    ) {
                                        callback.onNextAction()
                                        Handler().postDelayed({
                                            if (dialog != null && dialog!!.isShowing && !(context as Activity).isDestroyed) {
                                                dialog?.dismiss()
                                            }
                                        }, 1500)
                                    }
                                    if (this@Admob.interstitialAd != null &&
                                        dialog != null &&
                                        !(context as Activity).isDestroyed &&
                                        !context.isFinishing
                                    ) {
                                        this@Admob.interstitialAd?.show(context)
                                    } else if (this@Admob.interstitialAd == null) {
                                        dialog?.dismiss()
                                        callback?.onNextAction()
                                    }
                                }, 800)
                            }
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        runOnUiThread {
                            if (isTimeout) return@runOnUiThread
                            Log.i(TAG, loadAdError.message)

                            if (handlerTimeout != null && rdTimeout != null) {
                                handlerTimeout?.removeCallbacks(rdTimeout!!)
                            }

                            if (dialog != null && !(context as Activity).isDestroyed && !context.isFinishing) {
                                dialog?.dismiss()
                            }

                            if (callback != null) {
                                callback.onAdFailedToLoad(loadAdError)
                                callback.onNextAction()
                            }
                        }
                    }
                }
            )
        })
    }

    // =========================================================================
    // Picture-in-Picture (PiP) Ads (GMA Next-Gen SDK)
    // =========================================================================

    private fun handlePipPaid(
        logCtx: Context?,
        ad: PictureInPictureAd,
        adValue: AdValue,
        showCallback: AdCallback?
    ) {
        val adapterName = ad.getResponseInfo().adapterClassName
        if (logCtx != null) {
            NkhLogEventManager.logPaidAdImpression(
                logCtx,
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
        showCallback?.onAdLogRev(
            adValue,
            ad.adUnitId,
            adapterName,
            AdType.PIP
        )
        val loadCb = pipLoadCallbacks[ad]
        if (loadCb != null && loadCb !== showCallback) {
            loadCb.onAdLogRev(
                adValue,
                ad.adUnitId,
                adapterName,
                AdType.PIP
            )
        }
    }

    fun getCurrentPipAd(): PictureInPictureAd? = currentPipAd

    @JvmOverloads
    fun loadPipAd(
        context: Context,
        id: String,
        adCallback: AdCallback? = null
    ) {
        if (Arrays.asList(*context.resources.getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, PIP_ADS, id)
        }
        if (AppPurchase.getInstance().isPurchased(context)) {
            adCallback?.onNextAction()
            return
        }
        runWhenSdkInitialized(Runnable {
            val request = PictureInPictureAdRequest.Builder(id).build()
            PictureInPictureAd.load(
                request,
                object : AdLoadCallback<PictureInPictureAd> {
                    override fun onAdLoaded(pipAd: PictureInPictureAd) {
                        runOnUiThread {
                            currentPipAd?.destroy()
                            currentPipAd = pipAd
                            if (adCallback != null) {
                                pipLoadCallbacks[pipAd] = adCallback
                            }
                            pipAd.adEventCallback = createPipEventCallback(context, pipAd, adCallback)
                            Log.i(TAG, "PictureInPictureAd onAdLoaded: ${pipAd.adUnitId}")
                            adCallback?.onPipAdLoaded(pipAd)
                            adCallback?.onApPipAdLoaded(ApPipAd(pipAd))
                            adCallback?.onAdLoaded()
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        runOnUiThread {
                            Log.e(TAG, "PictureInPictureAd onAdFailedToLoad: ${loadAdError.message}")
                            adCallback?.onAdFailedToLoad(loadAdError)
                        }
                    }
                }
            )
        })
    }

    private fun createPipEventCallback(
        logCtx: Context?,
        pipAd: PictureInPictureAd,
        adCallback: AdCallback?
    ): PictureInPictureAdEventCallback {
        return object : PictureInPictureAdEventCallback {
            override fun onAdPaid(adValue: AdValue) {
                runOnUiThread {
                    handlePipPaid(logCtx ?: this@Admob.context, pipAd, adValue, adCallback)
                }
            }

            override fun onAdShown() {
                runOnUiThread {
                    Log.d(TAG, "PictureInPictureAd onAdShown")
                    adCallback?.onPipAdShown()
                }
            }

            override fun onAdHidden() {
                runOnUiThread {
                    Log.d(TAG, "PictureInPictureAd onAdHidden")
                    adCallback?.onPipAdHidden()
                }
            }

            override fun onAdImpression() {
                runOnUiThread {
                    Log.d(TAG, "PictureInPictureAd onAdImpression")
                    adCallback?.onAdImpression()
                }
            }

            override fun onAdClicked() {
                runOnUiThread {
                    if (disableAdResumeWhenClickAds) {
                        AppOpenManager.getInstance().disableAdResumeByClickAction()
                    }
                    if (logCtx != null) {
                        NkhLogEventManager.logClickAdsEvent(logCtx, pipAd.adUnitId)
                    }
                    adCallback?.onAdClicked()
                    adCallback?.onAdClicked(
                        pipAd.adUnitId,
                        pipAd.getResponseInfo().adapterClassName,
                        AdType.PIP
                    )
                }
            }

            override fun onAdDismissedFullScreenContent() {
                runOnUiThread {
                    Log.d(TAG, "PictureInPictureAd onAdDismissedFullScreenContent")
                    if (currentPipAd === pipAd) {
                        currentPipAd = null
                    }
                    adCallback?.onAdClosed()
                    adCallback?.onNextAction()
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                runOnUiThread {
                    Log.e(TAG, "PictureInPictureAd onAdFailedToShow: ${adError.message}")
                    adCallback?.onAdFailedToShow(adError)
                }
            }
        }
    }

    @JvmOverloads
    fun showPipAd(
        activity: Activity,
        pipAd: PictureInPictureAd? = currentPipAd,
        position: PictureInPictureAdPosition = PictureInPictureAdPosition.BOTTOM_RIGHT,
        scope: PictureInPictureAdPresentationScope = PictureInPictureAdPresentationScope.SCREEN,
        adCallback: AdCallback? = null
    ) {
        if (AppPurchase.getInstance().isPurchased(activity)) {
            adCallback?.onNextAction()
            return
        }
        if (pipAd == null) {
            Log.e(TAG, "showPipAd: PictureInPictureAd is null")
            adCallback?.onAdFailedToShow(null)
            adCallback?.onNextAction()
            return
        }
        val effectiveCallback = adCallback ?: pipLoadCallbacks[pipAd]
        pipAd.adEventCallback = createPipEventCallback(activity, pipAd, effectiveCallback)
        val options = PictureInPictureAdOptions.Builder()
            .setPosition(position)
            .setPresentationScope(scope)
            .build()
        pipAd.show(activity, options)
    }

    @JvmOverloads
    fun loadAndShowPipAd(
        activity: Activity,
        id: String,
        position: PictureInPictureAdPosition = PictureInPictureAdPosition.BOTTOM_RIGHT,
        scope: PictureInPictureAdPresentationScope = PictureInPictureAdPresentationScope.SCREEN,
        adCallback: AdCallback? = null
    ) {
        loadPipAd(activity, id, object : AdCallback() {
            override fun onPipAdLoaded(pipAd: PictureInPictureAd?) {
                adCallback?.onPipAdLoaded(pipAd)
                adCallback?.onAdLoaded()
                if (pipAd != null && !activity.isFinishing && !activity.isDestroyed) {
                    showPipAd(activity, pipAd, position, scope, adCallback)
                }
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                adCallback?.onAdFailedToLoad(i)
                adCallback?.onNextAction()
            }
        })
    }

    @JvmOverloads
    fun hidePipAd(pipAd: PictureInPictureAd? = currentPipAd) {
        pipAd?.hide()
    }

    @JvmOverloads
    fun destroyPipAd(pipAd: PictureInPictureAd? = currentPipAd) {
        pipAd?.destroy()
        if (pipAd === currentPipAd) {
            currentPipAd = null
        }
    }

    // =========================================================================
    // Swipeable Ads (SwipeableInterstitialAd & Swipable NativeAd - GMA Next-Gen SDK)
    // =========================================================================

    private fun handleSwipeablePaid(
        logCtx: Context?,
        ad: SwipeableInterstitialAd,
        adValue: AdValue,
        showCallback: AdCallback?
    ) {
        val adapterName = ad.getResponseInfo().adapterClassName
        if (logCtx != null) {
            NkhLogEventManager.logPaidAdImpression(
                logCtx,
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
        showCallback?.onAdLogRev(
            adValue,
            ad.adUnitId,
            adapterName,
            AdType.SWIPEABLE
        )
        val loadCb = swipeableLoadCallbacks[ad]
        if (loadCb != null && loadCb !== showCallback) {
            loadCb.onAdLogRev(
                adValue,
                ad.adUnitId,
                adapterName,
                AdType.SWIPEABLE
            )
        }
    }

    fun getCurrentSwipeableAd(): SwipeableInterstitialAd? = currentSwipeableAd

    @JvmOverloads
    fun destroySwipeableAd(activity: Activity? = null, swipeableAd: SwipeableInterstitialAd? = currentSwipeableAd) {
        swipeableAd?.let { ad ->
            if (activity != null) {
                val adView = ad.getView(activity)
                (adView.parent as? ViewGroup)?.removeView(adView)
            }
            ad.destroy()
        }
        if (swipeableAd === currentSwipeableAd) {
            currentSwipeableAd = null
        }
    }

    @JvmOverloads
    fun loadSwipeableInterstitialAd(
        context: Context,
        id: String,
        maxScreenHoldDurationSeconds: Int = 0,
        swipeDirection: NativeAd.SwipeGestureDirection? = null,
        tapsAllowed: Boolean = true,
        adCallback: AdCallback? = null
    ) {
        if (Arrays.asList(*context.resources.getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, SWIPEABLE_ADS, id)
        }
        if (AppPurchase.getInstance().isPurchased(context)) {
            adCallback?.onNextAction()
            return
        }
        runWhenSdkInitialized(Runnable {
            val builder = SwipeableInterstitialAdRequest.Builder(id)
            if (maxScreenHoldDurationSeconds > 0) {
                builder.setMaxScreenHoldDurationSeconds(maxScreenHoldDurationSeconds)
            }
            if (swipeDirection != null) {
                builder.enableCustomClickSwipeGesture(swipeDirection, tapsAllowed)
            }
            SwipeableInterstitialAd.load(
                builder.build(),
                object : AdLoadCallback<SwipeableInterstitialAd> {
                    override fun onAdLoaded(swipeableAd: SwipeableInterstitialAd) {
                        runOnUiThread {
                            if (currentSwipeableAd != null && currentSwipeableAd !== swipeableAd) {
                                currentSwipeableAd?.destroy()
                            }
                            currentSwipeableAd = swipeableAd
                            if (adCallback != null) {
                                swipeableLoadCallbacks[swipeableAd] = adCallback
                            }
                            swipeableAd.adEventCallback = createSwipeableEventCallback(context, swipeableAd, adCallback)
                            Log.i(TAG, "SwipeableInterstitialAd onAdLoaded: ${swipeableAd.adUnitId}")
                            adCallback?.onSwipeableAdLoaded(swipeableAd)
                            adCallback?.onApSwipeableAdLoaded(ApSwipeableAd(swipeableAd))
                            adCallback?.onAdLoaded()
                        }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        runOnUiThread {
                            Log.e(TAG, "SwipeableInterstitialAd onAdFailedToLoad: ${loadAdError.message}")
                            adCallback?.onAdFailedToLoad(loadAdError)
                        }
                    }
                }
            )
        })
    }

    private fun createSwipeableEventCallback(
        logCtx: Context?,
        swipeableAd: SwipeableInterstitialAd,
        adCallback: AdCallback?
    ): SwipeableInterstitialAdEventCallback {
        return object : SwipeableInterstitialAdEventCallback {
            override fun onAdPaid(adValue: AdValue) {
                runOnUiThread {
                    handleSwipeablePaid(logCtx ?: this@Admob.context, swipeableAd, adValue, adCallback)
                }
            }

            override fun onAdScreenHoldTimerStarted() {
                runOnUiThread {
                    Log.d(TAG, "SwipeableInterstitialAd onAdScreenHoldTimerStarted")
                    adCallback?.onSwipeableScreenHoldTimerStarted()
                }
            }

            override fun onAdImpression() {
                runOnUiThread {
                    Log.d(TAG, "SwipeableInterstitialAd onAdImpression")
                    adCallback?.onAdImpression()
                }
            }

            override fun onAdClicked() {
                runOnUiThread {
                    if (disableAdResumeWhenClickAds) {
                        AppOpenManager.getInstance().disableAdResumeByClickAction()
                    }
                    if (logCtx != null) {
                        NkhLogEventManager.logClickAdsEvent(logCtx, swipeableAd.adUnitId)
                    }
                    adCallback?.onAdClicked()
                    adCallback?.onAdClicked(
                        swipeableAd.adUnitId,
                        swipeableAd.getResponseInfo().adapterClassName,
                        AdType.SWIPEABLE
                    )
                }
            }

            override fun onAdShowedFullScreenContent() {
                runOnUiThread {
                    Log.d(TAG, "SwipeableInterstitialAd onAdShowedFullScreenContent")
                    adCallback?.onInterstitialShow()
                }
            }

            override fun onAdDismissedFullScreenContent() {
                runOnUiThread {
                    Log.d(TAG, "SwipeableInterstitialAd onAdDismissedFullScreenContent")
                    if (currentSwipeableAd === swipeableAd) {
                        currentSwipeableAd = null
                    }
                    adCallback?.onAdClosed()
                    adCallback?.onNextAction()
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                runOnUiThread {
                    Log.e(TAG, "SwipeableInterstitialAd onAdFailedToShow: ${adError.message}")
                    adCallback?.onAdFailedToShow(adError)
                }
            }
        }
    }

    @JvmOverloads
    fun renderSwipeableInterstitialAd(
        activity: Activity,
        container: ViewGroup,
        swipeableAd: SwipeableInterstitialAd? = currentSwipeableAd,
        adCallback: AdCallback? = null
    ): View? {
        if (AppPurchase.getInstance().isPurchased(activity)) {
            container.visibility = View.GONE
            adCallback?.onNextAction()
            return null
        }
        if (swipeableAd == null) {
            Log.e(TAG, "renderSwipeableInterstitialAd: SwipeableInterstitialAd is null")
            adCallback?.onAdFailedToShow(null)
            return null
        }
        val effectiveCallback = adCallback ?: swipeableLoadCallbacks[swipeableAd]
        swipeableAd.adEventCallback = createSwipeableEventCallback(activity, swipeableAd, effectiveCallback)
        val adView = swipeableAd.getView(activity)
        container.removeAllViews()
        container.visibility = View.VISIBLE
        container.addView(
            adView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        return adView
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
        loadSwipeableInterstitialAd(
            activity,
            id,
            maxScreenHoldDurationSeconds,
            swipeDirection,
            tapsAllowed,
            object : AdCallback() {
                override fun onSwipeableAdLoaded(swipeableAd: SwipeableInterstitialAd?) {
                    adCallback?.onSwipeableAdLoaded(swipeableAd)
                    adCallback?.onAdLoaded()
                    if (swipeableAd != null && !activity.isFinishing && !activity.isDestroyed) {
                        renderSwipeableInterstitialAd(activity, container, swipeableAd, adCallback)
                    }
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    adCallback?.onAdFailedToLoad(i)
                }
            }
        )
    }

    @JvmOverloads
    fun loadSwipeableNativeAd(
        context: Context,
        id: String,
        swipeDirection: NativeAd.SwipeGestureDirection = NativeAd.SwipeGestureDirection.UP,
        tapsAllowed: Boolean = true,
        callback: AdCallback? = null
    ) {
        if (Arrays.asList(*context.resources.getStringArray(R.array.list_id_test)).contains(id)) {
            showTestIdAlert(context, SWIPEABLE_ADS, id)
        }
        if (AppPurchase.getInstance().isPurchased(context)) {
            callback?.onAdClosed()
            return
        }
        runWhenSdkInitialized(Runnable {
            val adRequest = NativeAdRequest.Builder(id, listOf(NativeAd.NativeAdType.NATIVE))
                .enableCustomClickGestureDirection(swipeDirection, tapsAllowed)
                .build()
            NativeAdLoader.load(
                adRequest,
                object : NativeAdLoaderCallback {
                    override fun onNativeAdLoaded(nativeAd: NativeAd) {
                        runOnUiThread {
                            if (nativeAd.isCustomClickGestureEnabled()) {
                                nativeAd.enableCustomClickGesture()
                            }
                            nativeAd.adEventCallback = object : NativeAdEventCallback {
                                override fun onAdPaid(adValue: AdValue) {
                                    runOnUiThread {
                                        NkhLogEventManager.logPaidAdImpression(
                                            context,
                                            adValue,
                                            id,
                                            nativeAd.getResponseInfo().adapterClassName
                                        )
                                        NkhLogEventManager.logPaidAdjustWithToken(
                                            adValue,
                                            id,
                                            NkhAdConfig.ADJUST_TOKEN_TIKTOK
                                        )
                                        callback?.onAdLogRev(
                                            adValue,
                                            id,
                                            nativeAd.getResponseInfo().adapterClassName,
                                            AdType.SWIPEABLE
                                        )
                                    }
                                }

                                override fun onAdImpression() {
                                    runOnUiThread {
                                        callback?.onAdImpression()
                                    }
                                }

                                override fun onAdClicked() {
                                    runOnUiThread {
                                        if (disableAdResumeWhenClickAds) {
                                            AppOpenManager.getInstance().disableAdResumeByClickAction()
                                        }
                                        NkhLogEventManager.logClickAdsEvent(context, id)
                                        callback?.onAdClicked()
                                        callback?.onAdClicked(
                                            id,
                                            nativeAd.getResponseInfo().adapterClassName,
                                            AdType.SWIPEABLE
                                        )
                                    }
                                }
                            }
                            callback?.onUnifiedNativeAdLoaded(nativeAd)
                            callback?.onAdLoaded()
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        runOnUiThread {
                            Log.e(TAG, "SwipeableNativeAd onAdFailedToLoad: ${adError.message}")
                            callback?.onAdFailedToLoad(adError)
                        }
                    }
                }
            )
        })
    }

    companion object {
        private const val TAG = "NkhModuleAds"
        private var instance: Admob? = null

        const val BANNER_INLINE_SMALL_STYLE = "BANNER_INLINE_SMALL_STYLE"
        const val BANNER_INLINE_LARGE_STYLE = "BANNER_INLINE_LARGE_STYLE"
        private const val MAX_SMALL_INLINE_BANNER_HEIGHT = 50

        const val SPLASH_ADS = 0
        const val RESUME_ADS = 1
        private const val BANNER_ADS = 2
        private const val INTERS_ADS = 3
        private const val REWARD_ADS = 4
        private const val NATIVE_ADS = 5
        private const val PIP_ADS = 6
        private const val SWIPEABLE_ADS = 7

        @JvmStatic
        fun getInstance(): Admob {
            if (instance == null) {
                instance = Admob()
            }
            return instance!!
        }

        private fun runOnUiThread(runnable: Runnable) {
            Handler(Looper.getMainLooper()).post(runnable)
        }

        private fun readManifestApplicationId(context: Context): String {
            try {
                val ai = context.packageManager.getApplicationInfo(
                    context.packageName,
                    PackageManager.GET_META_DATA
                )
                if (ai.metaData != null) {
                    val v = ai.metaData.getString("com.google.android.gms.ads.APPLICATION_ID")
                    if (!v.isNullOrEmpty()) return v
                }
            } catch (ignored: Exception) {
            }
            return ""
        }
    }
}
