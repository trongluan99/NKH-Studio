package com.nkh.module

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ads.nkh.admob.Admob
import com.ads.nkh.admob.AppOpenManager
import com.ads.nkh.ads.NkhAd
import com.ads.nkh.ads.wrapper.ApInterstitialAd
import com.ads.nkh.ads.wrapper.ApInterstitialPriority2Ad
import com.ads.nkh.ads.wrapper.ApInterstitialPriorityAd
import com.ads.nkh.ads.wrapper.ApNativeAd
import com.ads.nkh.billing.AppPurchase
import com.ads.nkh.funtion.AdCallback
import com.ads.nkh.funtion.AdType
import com.ads.nkh.funtion.PurchaseListener
import com.ads.nkh.funtion.RewardCallback
import com.ads.nkh.util.AppConstant
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAd
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAdPosition
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAdPresentationScope
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.swipeableinterstitial.SwipeableInterstitialAd
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var mInterstitialPriority2Ad: ApInterstitialPriority2Ad
    private lateinit var mInterstitialPriority4Ad: ApInterstitialPriorityAd

    private lateinit var tvTestLog: TextView
    private lateinit var tvStatLoaded: TextView
    private lateinit var tvStatImpression: TextView
    private lateinit var tvStatPaid: TextView
    private lateinit var tvInterBadge: TextView
    private lateinit var tvRewardBadge: TextView
    private lateinit var frAds: FrameLayout
    private lateinit var frBanner: FrameLayout
    private lateinit var shimmerAds: ShimmerFrameLayout

    private var mApNativeAd: ApNativeAd? = null
    private var rewardedAds: RewardedAd? = null
    private var rewardedInterstitialAds: RewardedInterstitialAd? = null
    private var isEarn: Boolean = false
    private var selectedPipPosition: PictureInPictureAdPosition = PictureInPictureAdPosition.BOTTOM_RIGHT
    private var isLogExpanded: Boolean = false
    private var isNativePreviewVisible: Boolean = true

    private var countLoaded: Int = 0
    private var countImpression: Int = 0
    private var countPaid: Int = 0

    private val logLines = ArrayDeque<String>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTestLog = findViewById(R.id.tvTestLog)
        tvTestLog.movementMethod = ScrollingMovementMethod()
        tvStatLoaded = findViewById(R.id.tvStatLoaded)
        tvStatImpression = findViewById(R.id.tvStatImpression)
        tvStatPaid = findViewById(R.id.tvStatPaid)
        tvInterBadge = findViewById(R.id.tvInterBadge)
        tvRewardBadge = findViewById(R.id.tvRewardBadge)
        frAds = findViewById(R.id.fr_ads)
        frBanner = findViewById(R.id.frBanner)
        shimmerAds = findViewById(R.id.shimmer_native)

        mInterstitialPriority2Ad = ApInterstitialPriority2Ad(
            BuildConfig.ad_interstitial_splash,
            BuildConfig.ad_interstitial_splash
        )
        mInterstitialPriority4Ad = ApInterstitialPriorityAd(
            BuildConfig.ad_interstitial_splash,
            BuildConfig.ad_interstitial_splash,
            BuildConfig.ad_interstitial_splash,
            BuildConfig.ad_interstitial_splash
        )

        setupDashboardControls()
        setupTestButtons()
        setupBillingListener()

        // Auto-load Native Ad & Adaptive Banner Ad immediately on startup so ads are visible right away
        appendTestLog("Auto-loading TC1 (Native Large) & TC4 (Adaptive Banner)...")
        runTestCase1NativeLarge()
        runTestCase4BannerAdaptive()
    }

    private fun appendTestLog(message: String) {
        Log.d(TAG, message)
        runOnUiThread {
            if (message.contains("✅ PASS")) {
                countLoaded++
                tvStatLoaded.text = "✅ Loaded: $countLoaded"
            }
            if (message.contains("👁")) {
                countImpression++
                tvStatImpression.text = "👁 Impr: $countImpression"
            }
            if (message.contains("💰")) {
                countPaid++
                tvStatPaid.text = "💰 Paid: $countPaid"
            }
            if (message.contains("TC8 ✅ PASS")) {
                tvInterBadge.text = "✅ P2 Sẵn sàng"
                tvInterBadge.setTextColor(Color.parseColor("#059669"))
                tvInterBadge.setBackgroundColor(Color.parseColor("#D1FAE5"))
            }
            if (message.contains("TC14 ✅ PASS")) {
                tvRewardBadge.text = "✅ Reward Sẵn sàng"
                tvRewardBadge.setTextColor(Color.parseColor("#059669"))
                tvRewardBadge.setBackgroundColor(Color.parseColor("#D1FAE5"))
            }
            val line = "[${timeFormat.format(Date())}] $message"
            if (logLines.size >= 40) {
                logLines.removeFirst()
            }
            logLines.addLast(line)
            tvTestLog.text = logLines.joinToString("\n")
        }
    }

    private fun setupDashboardControls() {
        val btnToggleLogSize = findViewById<TextView>(R.id.btnToggleLogSize)
        btnToggleLogSize.setOnClickListener {
            isLogExpanded = !isLogExpanded
            val targetDp = if (isLogExpanded) 160 else 68
            val px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                targetDp.toFloat(),
                resources.displayMetrics
            ).toInt()
            tvTestLog.layoutParams = tvTestLog.layoutParams.apply { height = px }
            tvTestLog.requestLayout()
            btnToggleLogSize.text = if (isLogExpanded) "↕ Thu gọn" else "↕ Mở rộng"
        }

        findViewById<TextView>(R.id.btnClearLog).setOnClickListener {
            logLines.clear()
            countLoaded = 0
            countImpression = 0
            countPaid = 0
            tvStatLoaded.text = "✅ Loaded: 0"
            tvStatImpression.text = "👁 Impr: 0"
            tvStatPaid.text = "💰 Paid: 0"
            tvTestLog.text = "Đã xóa log. Sẵn sàng kiểm thử..."
        }

        val btnTogglePreview = findViewById<TextView>(R.id.btnTogglePreview)
        btnTogglePreview.setOnClickListener {
            isNativePreviewVisible = !isNativePreviewVisible
            frAds.visibility = if (isNativePreviewVisible) View.VISIBLE else View.GONE
            btnTogglePreview.text = if (isNativePreviewVisible) "👁 Ẩn khung Native" else "👁 Hiện khung Native"
        }

        // Category Filter Tabs
        val tabAll = findViewById<MaterialButton>(R.id.tabAll)
        val tabNextGen = findViewById<MaterialButton>(R.id.tabNextGen)
        val tabNative = findViewById<MaterialButton>(R.id.tabNative)
        val tabBanner = findViewById<MaterialButton>(R.id.tabBanner)
        val tabInter = findViewById<MaterialButton>(R.id.tabInter)
        val tabReward = findViewById<MaterialButton>(R.id.tabReward)
        val allTabs = listOf(tabAll, tabNextGen, tabNative, tabBanner, tabInter, tabReward)

        val cardNextGen = findViewById<View>(R.id.cardSectionNextGen)
        val cardNative = findViewById<View>(R.id.cardSectionNative)
        val cardBanner = findViewById<View>(R.id.cardSectionBanner)
        val cardInter = findViewById<View>(R.id.cardSectionInter)
        val cardReward = findViewById<View>(R.id.cardSectionReward)
        val allCards = listOf(cardNextGen, cardNative, cardBanner, cardInter, cardReward)

        fun selectFilter(activeTab: MaterialButton, visibleCards: List<View>) {
            for (t in allTabs) {
                val active = (t === activeTab)
                t.backgroundTintList = ColorStateList.valueOf(
                    Color.parseColor(if (active) "#2563EB" else "#E2E8F0")
                )
                t.setTextColor(Color.parseColor(if (active) "#FFFFFF" else "#1E293B"))
            }
            for (c in allCards) {
                c.visibility = if (visibleCards.contains(c)) View.VISIBLE else View.GONE
            }
        }

        tabAll.setOnClickListener { selectFilter(tabAll, allCards) }
        tabNextGen.setOnClickListener { selectFilter(tabNextGen, listOf(cardNextGen)) }
        tabNative.setOnClickListener { selectFilter(tabNative, listOf(cardNative)) }
        tabBanner.setOnClickListener { selectFilter(tabBanner, listOf(cardBanner)) }
        tabInter.setOnClickListener { selectFilter(tabInter, listOf(cardInter)) }
        tabReward.setOnClickListener { selectFilter(tabReward, listOf(cardReward)) }

        // PiP Corner Position Selector
        val posBottomRight = findViewById<TextView>(R.id.pipPosBottomRight)
        val posBottomLeft = findViewById<TextView>(R.id.pipPosBottomLeft)
        val posTopRight = findViewById<TextView>(R.id.pipPosTopRight)
        val posTopLeft = findViewById<TextView>(R.id.pipPosTopLeft)
        val posViews = listOf(
            posBottomRight to PictureInPictureAdPosition.BOTTOM_RIGHT,
            posBottomLeft to PictureInPictureAdPosition.BOTTOM_LEFT,
            posTopRight to PictureInPictureAdPosition.TOP_RIGHT,
            posTopLeft to PictureInPictureAdPosition.TOP_LEFT
        )
        for ((view, pos) in posViews) {
            view.setOnClickListener {
                selectedPipPosition = pos
                for ((v, _) in posViews) {
                    val active = (v === view)
                    v.setBackgroundColor(Color.parseColor(if (active) "#2563EB" else "#F1F5F9"))
                    v.setTextColor(Color.parseColor(if (active) "#FFFFFF" else "#334155"))
                }
                appendTestLog("TC18 [PiP]: Đã chọn góc hiển thị ${pos.name}")
            }
        }
    }

    private fun ensureNativePreviewExpanded() {
        if (!isNativePreviewVisible) {
            isNativePreviewVisible = true
            frAds.visibility = View.VISIBLE
            findViewById<TextView>(R.id.btnTogglePreview)?.text = "👁 Ẩn khung Native"
        }
    }

    private fun setupTestButtons() {
        findViewById<Button>(R.id.btnRunAllLoadTests).setOnClickListener {
            runAllNonBlockingLoadTests()
        }

        // 1. NATIVE ADS TEST CASES
        findViewById<Button>(R.id.btnNativeLarge).setOnClickListener {
            ensureNativePreviewExpanded()
            runTestCase1NativeLarge()
        }
        findViewById<Button>(R.id.btnNativeCustomCta).setOnClickListener {
            ensureNativePreviewExpanded()
            runTestCase2NativePreloadAndCustomCta()
        }
        findViewById<Button>(R.id.btnNativePriority4).setOnClickListener {
            ensureNativePreviewExpanded()
            runTestCase3NativePriority4()
        }

        // 2. BANNER ADS TEST CASES
        findViewById<Button>(R.id.btnBannerAdaptive).setOnClickListener {
            runTestCase4BannerAdaptive()
        }
        findViewById<Button>(R.id.btnBannerInline).setOnClickListener {
            runTestCase5BannerInline()
        }
        findViewById<Button>(R.id.btnBannerCollapsibleBottom).setOnClickListener {
            runTestCase6CollapsibleBanner(AppConstant.CollapsibleGravity.BOTTOM)
        }
        findViewById<Button>(R.id.btnBannerCollapsibleTop).setOnClickListener {
            runTestCase6CollapsibleBanner(AppConstant.CollapsibleGravity.TOP)
        }

        // 3. INTERSTITIAL ADS TEST CASES
        findViewById<Button>(R.id.btnLoad).setOnClickListener {
            runTestCase8LoadInterPriority2()
        }
        findViewById<Button>(R.id.btnShowPriority2).setOnClickListener {
            runTestCase9ShowInterPriority2()
        }
        findViewById<Button>(R.id.btnShow).setOnClickListener {
            runTestCase10LoadAndShowInter()
        }
        findViewById<Button>(R.id.btnInterPriority4).setOnClickListener {
            runTestCase11InterPriority4LoadAndShow()
        }

        // 4. APP OPEN, REWARDED & TOOLS TEST CASES
        findViewById<Button>(R.id.btnAppOpenSplash).setOnClickListener {
            runTestCase12AppOpenSplash()
        }
        findViewById<Button>(R.id.btnAppOpenResume).setOnClickListener {
            runTestCase13AppOpenResume()
        }
        findViewById<Button>(R.id.btnLoadReward).setOnClickListener {
            runTestCase14LoadReward()
        }
        findViewById<Button>(R.id.btnShowReward).setOnClickListener {
            runTestCase14ShowReward()
        }
        findViewById<Button>(R.id.btnRewardInter).setOnClickListener {
            runTestCase15RewardInterstitial()
        }
        findViewById<Button>(R.id.btnIap).setOnClickListener {
            appendTestLog("TC16 [IAP]: Opening PurchaseDevBottomSheet / Play Billing...")
            AppPurchase.getInstance().purchase(this@MainActivity, "android.test.purchased")
        }
        findViewById<Button>(R.id.btnAdInspector).setOnClickListener {
            appendTestLog("TC17 [AdInspector]: Opening GMA Next-Gen Ad Inspector...")
            MobileAds.openAdInspector { error ->
                appendTestLog("TC17 [AdInspector] Closed: error=${error?.message ?: "none"}")
            }
        }

        // 5. NEXT-GEN PIP & SWIPEABLE ADS TEST CASES
        findViewById<Button>(R.id.btnPipAd).setOnClickListener {
            runTestCase18PipAd()
        }
        findViewById<Button>(R.id.btnHidePipAd).setOnClickListener {
            appendTestLog("TC18 [PiP Ad]: Hiding PictureInPictureAd...")
            NkhAd.getInstance().hidePipAd()
        }
        findViewById<Button>(R.id.btnDestroyPipAd).setOnClickListener {
            appendTestLog("TC18 [PiP Ad]: Destroying PictureInPictureAd...")
            NkhAd.getInstance().destroyPipAd()
        }
        findViewById<Button>(R.id.btnSwipeableInter).setOnClickListener {
            runTestCase19SwipeableInterstitial()
        }
        findViewById<Button>(R.id.btnSwipeableNative).setOnClickListener {
            runTestCase20SwipeableNative()
        }
    }

    // ==================== TC1: Native Large (Load & Show) ====================
    fun runTestCase1NativeLarge() {
        appendTestLog("TC1 [Native Large]: Loading ${BuildConfig.ad_native}...")
        NkhAd.getInstance().loadNativeAd(
            this,
            BuildConfig.ad_native,
            R.layout.native_large,
            frAds,
            shimmerAds,
            object : AdCallback() {
                override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                    super.onNativeAdLoaded(nativeAd)
                    val headline = nativeAd.admobNativeAd?.headline ?: "N/A"
                    val adapter = nativeAd.admobNativeAd?.getResponseInfo()?.adapterClassName ?: "unknown"
                    appendTestLog("TC1 ✅ PASS [Native Large]: headline='$headline' ($adapter)")
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    appendTestLog("TC1 👁 [Native Large]: onAdImpression")
                }

                override fun onAdLogRev(
                    adValue: AdValue?,
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                    super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                    appendTestLog("TC1 💰 [Native Paid]: ${adValue?.valueMicros} ${adValue?.currencyCode}")
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    appendTestLog("TC1 ❌ FAIL [Native Large]: ${i?.message}")
                }
            }
        )
    }

    // ==================== TC2: Native Preload + Custom CTA Populate ====================
    fun runTestCase2NativePreloadAndCustomCta() {
        appendTestLog("TC2 [Native Preload+CTA]: Preloading...")
        NkhAd.getInstance().loadNativeAdResultCallback(
            this,
            BuildConfig.ad_native,
            R.layout.native_large,
            object : AdCallback() {
                override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                    super.onNativeAdLoaded(nativeAd)
                    mApNativeAd = nativeAd
                    appendTestLog("TC2 ✅ PASS [Native Preload]: Populating with #2563EB CTA...")
                    NkhAd.getInstance().populateNativeAdView(
                        this@MainActivity,
                        nativeAd,
                        frAds,
                        shimmerAds,
                        "#2563EB",
                        44,
                        16
                    )
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    appendTestLog("TC2 ❌ FAIL [Native Preload]: ${i?.message}")
                }
            }
        )
    }

    // ==================== TC3: Native Priority 4 Same Time ====================
    fun runTestCase3NativePriority4() {
        appendTestLog("TC3 [Native Priority 4]: Loading 4 slots...")
        NkhAd.getInstance().loadNative4SameTime(
            this,
            BuildConfig.ad_native,
            BuildConfig.ad_native,
            BuildConfig.ad_native,
            BuildConfig.ad_native,
            R.layout.native_large,
            object : AdCallback() {
                override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                    super.onNativeAdLoaded(nativeAd)
                    appendTestLog("TC3 ✅ PASS [Native Priority 4]: Populating with Close button...")
                    NkhAd.getInstance().populateNativeAdView(
                        this@MainActivity,
                        nativeAd,
                        frAds,
                        shimmerAds,
                        true
                    )
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    appendTestLog("TC3 ❌ FAIL [Native Priority 4]: ${i?.message}")
                }
            }
        )
    }

    // ==================== TC4: Adaptive Banner ====================
    fun runTestCase4BannerAdaptive() {
        appendTestLog("TC4 [Adaptive Banner]: Loading ${BuildConfig.ad_banner}...")
        NkhAd.getInstance().loadBanner(
            this,
            BuildConfig.ad_banner,
            object : AdCallback() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    appendTestLog("TC4 ✅ PASS [Adaptive Banner]: onAdLoaded")
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    appendTestLog("TC4 👁 [Adaptive Banner]: onAdImpression")
                }

                override fun onAdLogRev(
                    adValue: AdValue?,
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                    super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                    appendTestLog("TC4 💰 [Banner Paid]: ${adValue?.valueMicros} ${adValue?.currencyCode}")
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    appendTestLog("TC4 ❌ FAIL [Adaptive Banner]: ${i?.message}")
                }
            }
        )
    }

    // ==================== TC5: Inline Adaptive Banner ====================
    fun runTestCase5BannerInline() {
        appendTestLog("TC5 [Inline Banner]: Loading SMALL inline style...")
        NkhAd.getInstance().loadInlineBanner(
            this,
            BuildConfig.ad_banner,
            Admob.BANNER_INLINE_SMALL_STYLE,
            object : AdCallback() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    appendTestLog("TC5 ✅ PASS [Inline Banner]: onAdLoaded")
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    appendTestLog("TC5 ❌ FAIL [Inline Banner]: ${i?.message}")
                }
            }
        )
    }

    // ==================== TC6 & TC7: Collapsible Banner ====================
    fun runTestCase6CollapsibleBanner(gravity: String) {
        val label = if (gravity == AppConstant.CollapsibleGravity.TOP) "TC7 [Collapsible TOP]" else "TC6 [Collapsible BOTTOM]"
        appendTestLog("$label: Loading...")
        NkhAd.getInstance().loadCollapsibleBanner(
            this,
            BuildConfig.ad_banner,
            gravity,
            object : AdCallback() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    appendTestLog("$label ✅ PASS: onAdLoaded (gravity=$gravity)")
                }

                override fun onAdLogRev(
                    adValue: AdValue?,
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                    super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                    appendTestLog("$label 💰 Paid: ${adValue?.valueMicros} ($adType)")
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    appendTestLog("$label ❌ FAIL: ${i?.message}")
                }
            }
        )
    }

    // ==================== TC8 & TC9: Interstitial Priority 2 ====================
    fun runTestCase8LoadInterPriority2() {
        appendTestLog("TC8 [Inter Priority 2]: Loading High + Normal...")
        NkhAd.getInstance().loadPriority2InterstitialAds(
            this@MainActivity,
            mInterstitialPriority2Ad,
            object : AdCallback() {
                override fun onApInterstitialLoad(apInterstitialAd: ApInterstitialAd?) {
                    super.onApInterstitialLoad(apInterstitialAd)
                    val adapter = apInterstitialAd?.interstitialAd?.getResponseInfo()?.adapterClassName
                    appendTestLog("TC8 ✅ PASS [Inter Priority 2]: Ready ($adapter)")
                    Toast.makeText(this@MainActivity, "Inter Priority 2 Ready", Toast.LENGTH_SHORT).show()
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    appendTestLog("TC8 ❌ FAIL [Inter Priority 2]: ${i?.message}")
                }
            }
        )
    }

    fun runTestCase9ShowInterPriority2() {
        appendTestLog("TC9 [Show Inter Priority 2]: Showing...")
        NkhAd.getInstance().forceShowInterstitialPriority2(
            this@MainActivity,
            mInterstitialPriority2Ad,
            object : AdCallback() {
                override fun onInterstitialShow() {
                    super.onInterstitialShow()
                    appendTestLog("TC9 ✅ PASS [Inter Priority 2]: onInterstitialShow")
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    appendTestLog("TC9 👁 [Inter Priority 2]: onAdImpression")
                }

                override fun onAdLogRev(
                    adValue: AdValue?,
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                    super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                    appendTestLog("TC9 💰 [Inter P2 Paid]: ${adValue?.valueMicros} ${adValue?.currencyCode}")
                }

                override fun onAdClosed() {
                    super.onAdClosed()
                    appendTestLog("TC9 ✅ [Inter Priority 2]: onAdClosed")
                }

                override fun onNextAction() {
                    super.onNextAction()
                    appendTestLog("TC9 -> onNextAction")
                }
            },
            true
        )
    }

    // ==================== TC10: Load & Show Interstitial Immediate ====================
    fun runTestCase10LoadAndShowInter() {
        appendTestLog("TC10 [Load&Show Inter]: Loading & showing immediately...")
        NkhAd.getInstance().loadAndShowInterstitial(
            this@MainActivity,
            BuildConfig.ad_interstitial_splash,
            15000,
            false,
            object : AdCallback() {
                override fun onAdImpression() {
                    super.onAdImpression()
                    appendTestLog("TC10 ✅ PASS [Load&Show Inter]: onAdImpression")
                }

                override fun onAdLogRev(
                    adValue: AdValue?,
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                    super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                    appendTestLog("TC10 💰 [Inter Paid]: ${adValue?.valueMicros} ${adValue?.currencyCode}")
                }

                override fun onAdClosed() {
                    super.onAdClosed()
                    appendTestLog("TC10 ✅ [Load&Show Inter]: onAdClosed")
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    appendTestLog("TC10 ❌ FAIL [Load&Show Inter]: ${i?.message}")
                }

                override fun onNextAction() {
                    super.onNextAction()
                    appendTestLog("TC10 -> onNextAction")
                }
            }
        )
    }

    // ==================== TC11: Interstitial Priority 4 ====================
    fun runTestCase11InterPriority4LoadAndShow() {
        appendTestLog("TC11 [Inter Priority 4]: Loading 4 priority slots...")
        var shown = false
        NkhAd.getInstance().loadPriorityInterstitialAds(
            this@MainActivity,
            mInterstitialPriority4Ad,
            object : AdCallback() {
                override fun onApInterstitialLoad(apInterstitialAd: ApInterstitialAd?) {
                    super.onApInterstitialLoad(apInterstitialAd)
                    appendTestLog("TC11 ✅ PASS [Inter Priority 4]: Slot loaded")
                    if (!shown) {
                        shown = true
                        NkhAd.getInstance().forceShowInterstitialPriority(
                            this@MainActivity,
                            mInterstitialPriority4Ad,
                            object : AdCallback() {
                                override fun onAdImpression() {
                                    super.onAdImpression()
                                    appendTestLog("TC11 👁 [Inter Priority 4]: onAdImpression")
                                }

                                override fun onAdClosed() {
                                    super.onAdClosed()
                                    appendTestLog("TC11 ✅ [Inter Priority 4]: onAdClosed")
                                }
                            },
                            false
                        )
                    }
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    appendTestLog("TC11 ❌ FAIL [Inter Priority 4]: ${i?.message}")
                }
            }
        )
    }

    // ==================== TC12: App Open Splash Ad ====================
    fun runTestCase12AppOpenSplash() {
        appendTestLog("TC12 [AppOpen Splash]: Loading & showing ${BuildConfig.ads_open_app}...")
        AppOpenManager.getInstance().loadOpenAppAdSplash(
            this@MainActivity,
            BuildConfig.ads_open_app,
            500,
            15000,
            true,
            object : AdCallback() {
                override fun onAdImpression() {
                    super.onAdImpression()
                    appendTestLog("TC12 ✅ PASS [AppOpen Splash]: onAdImpression")
                }

                override fun onAdLogRev(
                    adValue: AdValue?,
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                    super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                    appendTestLog("TC12 💰 [AppOpen Paid]: ${adValue?.valueMicros} ${adValue?.currencyCode}")
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    appendTestLog("TC12 ❌ FAIL [AppOpen Splash]: ${i?.message}")
                }

                override fun onNextAction() {
                    super.onNextAction()
                    appendTestLog("TC12 ✅ [AppOpen Splash]: onNextAction (Closed)")
                }
            }
        )
    }

    // ==================== TC13: App Open Resume Ad ====================
    fun runTestCase13AppOpenResume() {
        val available = AppOpenManager.getInstance().isAdAvailable(false)
        appendTestLog("TC13 [AppOpen Resume]: isAdAvailable=$available")
        if (available) {
            AppOpenManager.getInstance().showAdIfAvailable(false)
            appendTestLog("TC13 ✅ PASS [AppOpen Resume]: Showing AppOpen Resume Ad")
        } else {
            appendTestLog("TC13 ⏳ [AppOpen Resume]: Fetching Resume Ad... Tap again in 3s")
            AppOpenManager.getInstance().fetchAd(false)
        }
    }

    // ==================== TC14: Rewarded Ad ====================
    fun runTestCase14LoadReward() {
        appendTestLog("TC14 [Rewarded Ad]: Loading ${BuildConfig.ad_reward}...")
        NkhAd.getInstance().initRewardAds(
            this,
            BuildConfig.ad_reward,
            object : AdCallback() {
                override fun onRewardAdLoaded(rewardedAd: RewardedAd?) {
                    super.onRewardAdLoaded(rewardedAd)
                    rewardedAds = rewardedAd
                    val adapter = rewardedAd?.getResponseInfo()?.adapterClassName
                    appendTestLog("TC14 ✅ PASS [Rewarded Ad]: Loaded ($adapter)")
                    Toast.makeText(this@MainActivity, "Reward Ad Ready", Toast.LENGTH_SHORT).show()
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    appendTestLog("TC14 ❌ FAIL [Rewarded Ad]: ${i?.message}")
                }
            }
        )
    }

    fun runTestCase14ShowReward() {
        if (rewardedAds == null) {
            appendTestLog("TC14 ⚠️ [Show Reward]: Not loaded yet, loading now...")
            runTestCase14LoadReward()
            return
        }
        isEarn = false
        appendTestLog("TC14 [Show Reward]: Showing RewardedAd...")
        NkhAd.getInstance().showRewardAds(
            this@MainActivity,
            rewardedAds,
            object : RewardCallback {
                override fun onUserEarnedReward(var1: RewardItem?) {
                    isEarn = true
                    appendTestLog("TC14 🎁 [Reward Earned]: ${var1?.amount} ${var1?.type}")
                }

                override fun onRewardedAdClosed() {
                    rewardedAds = null
                    appendTestLog("TC14 ✅ [Reward Closed]: isEarn=$isEarn")
                }

                override fun onRewardedAdFailedToShow(codeError: Int) {
                    appendTestLog("TC14 ❌ [Reward FailedToShow]: code=$codeError")
                }

                override fun onAdClicked() {
                    appendTestLog("TC14 👆 [Reward Clicked]")
                }

                override fun onAdClicked(
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                }

                override fun onAdImpression() {
                    appendTestLog("TC14 👁 [Reward Impression]")
                }

                override fun onAdLogRev(
                    adValue: AdValue?,
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                    appendTestLog("TC14 💰 [Reward Paid]: ${adValue?.valueMicros} ${adValue?.currencyCode}")
                }
            }
        )
    }

    // ==================== TC15: Rewarded Interstitial Ad ====================
    fun runTestCase15RewardInterstitial() {
        appendTestLog("TC15 [Reward Inter]: Loading ${BuildConfig.ad_reward_inter}...")
        NkhAd.getInstance().getRewardInterstitial(
            this,
            BuildConfig.ad_reward_inter,
            object : AdCallback() {
                override fun onRewardAdLoaded(rewardedAd: RewardedInterstitialAd?) {
                    super.onRewardAdLoaded(rewardedAd)
                    rewardedInterstitialAds = rewardedAd
                    appendTestLog("TC15 ✅ PASS [Reward Inter]: Loaded -> Showing...")
                    NkhAd.getInstance().showRewardInterstitial(
                        this@MainActivity,
                        rewardedAd,
                        object : RewardCallback {
                            override fun onUserEarnedReward(var1: RewardItem?) {
                                appendTestLog("TC15 🎁 [RewardInter Earned]: ${var1?.amount} ${var1?.type}")
                            }

                            override fun onRewardedAdClosed() {
                                appendTestLog("TC15 ✅ [RewardInter Closed]")
                            }

                            override fun onRewardedAdFailedToShow(codeError: Int) {
                                appendTestLog("TC15 ❌ [RewardInter FailShow]: $codeError")
                            }

                            override fun onAdClicked() {}

                            override fun onAdClicked(
                                adUnitId: String?,
                                mediationAdapterClassName: String?,
                                adType: AdType?
                            ) {
                            }

                            override fun onAdImpression() {
                                appendTestLog("TC15 👁 [RewardInter Impression]")
                            }

                            override fun onAdLogRev(
                                adValue: AdValue?,
                                adUnitId: String?,
                                mediationAdapterClassName: String?,
                                adType: AdType?
                            ) {
                                appendTestLog("TC15 💰 [RewardInter Paid]: ${adValue?.valueMicros}")
                            }
                        }
                    )
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    appendTestLog("TC15 ❌ FAIL [Reward Inter]: ${i?.message}")
                }
            }
        )
    }

    // ==================== TC18: Picture-in-Picture (PiP) Ad ====================
    fun runTestCase18PipAd() {
        appendTestLog("TC18 [PiP Ad]: Loading & Showing PiP at ${selectedPipPosition.name} (${BuildConfig.ad_pip})...")
        NkhAd.getInstance().loadAndShowPipAd(
            this,
            BuildConfig.ad_pip,
            selectedPipPosition,
            PictureInPictureAdPresentationScope.SCREEN,
            object : AdCallback() {
                override fun onPipAdLoaded(pipAd: PictureInPictureAd?) {
                    appendTestLog("TC18 ✅ PASS [PiP Ad Loaded]: pos=${selectedPipPosition.name}")
                    Toast.makeText(this@MainActivity, "PiP Ad Showing at ${selectedPipPosition.name}!", Toast.LENGTH_SHORT).show()
                }

                override fun onPipAdShown() {
                    appendTestLog("TC18 👁 [PiP Ad Shown]")
                }

                override fun onPipAdHidden() {
                    appendTestLog("TC18 🙈 [PiP Ad Hidden]")
                }

                override fun onAdImpression() {
                    appendTestLog("TC18 👁 [PiP Ad Impression]")
                }

                override fun onAdLogRev(
                    adValue: AdValue?,
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                    appendTestLog("TC18 💰 [PiP Paid]: ${adValue?.valueMicros} ($adType)")
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    appendTestLog("TC18 ❌ FAIL [PiP Load]: code=${i?.code} (${i?.message})")
                    Toast.makeText(this@MainActivity, "PiP Load Failed: ${i?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // ==================== TC19: Swipeable Interstitial Ad ====================
    fun runTestCase19SwipeableInterstitial() {
        appendTestLog("TC19 [Swipeable Inter]: Opening Fullscreen Vertical Feed (${BuildConfig.ad_swipeable_inter})...")
        NkhAd.getInstance().showSwipeableInterstitialFeedDialog(
            this,
            BuildConfig.ad_swipeable_inter,
            initialMaxHoldSeconds = 5,
            adCallback = object : AdCallback() {
                override fun onSwipeableAdLoaded(swipeableAd: SwipeableInterstitialAd?) {
                    appendTestLog("TC19 ✅ PASS [Swipeable Inter Loaded]: hold=${swipeableAd?.getScreenHoldDuration()}s")
                }

                override fun onSwipeableScreenHoldTimerStarted() {
                    appendTestLog("TC19 ⏱ [Swipeable Hold Timer Started]")
                }

                override fun onAdImpression() {
                    appendTestLog("TC19 👁 [Swipeable Inter Impression]")
                }

                override fun onAdLogRev(
                    adValue: AdValue?,
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                    appendTestLog("TC19 💰 [Swipeable Inter Paid]: ${adValue?.valueMicros} ($adType)")
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    appendTestLog("TC19 ❌ FAIL [Swipeable Inter Load]: code=${i?.code} (${i?.message})")
                }
            }
        )
    }

    // ==================== TC20: Swipeable / FullScreen Native Ad ====================
    fun runTestCase20SwipeableNative() {
        appendTestLog("TC20 [Swipeable Native]: Opening FullScreen Swipeable NativeAd (${BuildConfig.ad_native})...")
        NkhAd.getInstance().showFullScreenSwipeableNativeDialog(
            this,
            BuildConfig.ad_native,
            NativeAd.SwipeGestureDirection.UP,
            true,
            object : AdCallback() {
                override fun onUnifiedNativeAdLoaded(unifiedNativeAd: NativeAd) {
                    val headline = unifiedNativeAd.headline ?: "N/A"
                    val customSwipe = unifiedNativeAd.isCustomClickGestureEnabled()
                    appendTestLog("TC20 ✅ PASS [Swipeable Native]: headline='$headline' (customSwipe=$customSwipe)")
                }

                override fun onAdImpression() {
                    appendTestLog("TC20 👁 [Swipeable Native Impression]")
                }

                override fun onAdLogRev(
                    adValue: AdValue?,
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                    appendTestLog("TC20 💰 [Swipeable Native Paid]: ${adValue?.valueMicros} ($adType)")
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    appendTestLog("TC20 ❌ FAIL [Swipeable Native]: ${i?.message}")
                }
            }
        )
    }

    // ==================== Run All Non-Blocking Load Test Cases ====================
    private fun runAllNonBlockingLoadTests() {
        appendTestLog("=== RUNNING ALL LOAD TEST CASES ===")
        runTestCase1NativeLarge()
        runTestCase4BannerAdaptive()
        runTestCase8LoadInterPriority2()
        runTestCase14LoadReward()
        AppOpenManager.getInstance().fetchAd(false)
    }

    private fun setupBillingListener() {
        AppPurchase.getInstance().setPurchaseListener(object : PurchaseListener {
            override fun onProductPurchased(productId: String?, transactionDetails: String?) {
                appendTestLog("TC16 ✅ [IAP Purchased]: productId=$productId")
                Toast.makeText(this@MainActivity, "onProductPurchased: $productId", Toast.LENGTH_SHORT).show()
            }

            override fun displayErrorMessage(errorMsg: String?) {
                appendTestLog("TC16 ⚠️ [IAP Error]: $errorMsg")
            }

            override fun onUserCancelBilling() {
                appendTestLog("TC16 ℹ️ [IAP Cancelled]")
            }
        })
    }

    companion object {
        private const val TAG = "NkhAdsTestSuite"
    }
}
