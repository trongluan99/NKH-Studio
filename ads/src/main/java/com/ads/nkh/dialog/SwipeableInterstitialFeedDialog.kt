package com.ads.nkh.dialog

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.ads.nkh.admob.Admob
import com.ads.nkh.funtion.AdCallback
import com.ads.nkh.funtion.AdType
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.VideoController
import com.google.android.libraries.ads.mobile.sdk.swipeableinterstitial.SwipeableInterstitialAd

class SwipeableInterstitialFeedDialog(
    private val hostActivity: Activity,
    private val adUnitId: String,
    initialMaxHoldSeconds: Int = 5,
    private val externalCallback: AdCallback? = null
) : Dialog(hostActivity, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    private lateinit var viewPager: ViewPager2
    private lateinit var backButton: Button
    private lateinit var holdCountdownBadge: TextView
    private var swipeableAd: SwipeableInterstitialAd? = null
    private var requestedMaxScreenHoldSeconds: Int = initialMaxHoldSeconds
    private var lastSettledPosition: Int = 0
    private var holdStartTimeMs: Long = 0L
    private var holdDurationMs: Long = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var unlockRunnable: Runnable? = null
    private var tickRunnable: Runnable? = null

    private enum class AdLoadState(val statusText: String, val statusColor: Int, val bgColor: Int) {
        IDLE("", Color.TRANSPARENT, Color.TRANSPARENT),
        LOADING("⏳ Đang tải Swipeable Ad...", Color.parseColor("#FBBF24"), Color.parseColor("#33FBBF24")),
        LOADED("🟢 Quảng cáo đã sẵn sàng — Vuốt lên để xem!", Color.parseColor("#34D399"), Color.parseColor("#3310B981")),
        FAILED("❌ Tải quảng cáo thất bại", Color.parseColor("#F87171"), Color.parseColor("#33EF4444"))
    }

    private var currentLoadState: AdLoadState = AdLoadState.IDLE
    private var lastErrorText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val root = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#0F172A"))
        }

        viewPager = ViewPager2(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = ViewPager2.ORIENTATION_VERTICAL
        }
        preventOverscrollArtifacts(viewPager)
        viewPager.adapter = SwipeablePagerAdapter()
        viewPager.registerOnPageChangeCallback(pageChangeCallback)
        root.addView(viewPager)

        backButton = Button(context).apply {
            text = "← Đóng Feed"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            includeFontPadding = false
            minHeight = dpToPx(40)
            minimumHeight = dpToPx(40)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#990F172A"))
                cornerRadius = dpToPx(20).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#44FFFFFF"))
            }
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                topMargin = dpToPx(40)
                marginStart = dpToPx(16)
            }
            layoutParams = params
            setOnClickListener { dismiss() }
        }
        root.addView(backButton)

        holdCountdownBadge = TextView(context).apply {
            visibility = View.GONE
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC0F172A"))
                cornerRadius = dpToPx(18).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#6638BDF8"))
            }
            setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = dpToPx(42)
                marginEnd = dpToPx(16)
            }
        }
        root.addView(holdCountdownBadge)

        setContentView(root)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars() or
                    androidx.core.view.WindowInsetsCompat.Type.displayCutout()
            )
            val topInset = maxOf(bars.top, dpToPx(28))
            (backButton.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
                lp.topMargin = topInset + dpToPx(12)
                backButton.layoutParams = lp
            }
            (holdCountdownBadge.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
                lp.topMargin = topInset + dpToPx(14)
                holdCountdownBadge.layoutParams = lp
            }
            insets
        }
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        reloadSwipeableInterstitialAd()
    }

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrollStateChanged(state: Int) {
            super.onPageScrollStateChanged(state)
            if (state == ViewPager2.SCROLL_STATE_IDLE) {
                val position = viewPager.currentItem
                val previousPosition = lastSettledPosition
                if (position == previousPosition) return
                lastSettledPosition = position
                backButton.visibility = View.VISIBLE
                if (position % 2 == 1) {
                    currentLoadState = AdLoadState.IDLE
                    showSwipeableInterstitialAd()
                    if (previousPosition % 2 == 0) {
                        startHoldScreen()
                    }
                } else {
                    stopHoldCountdownUi()
                    reloadSwipeableInterstitialAd()
                }
            }
        }
    }

    private fun reloadSwipeableInterstitialAd() {
        if (currentLoadState == AdLoadState.LOADING) return
        discardAd()
        viewPager.isUserInputEnabled = false
        currentLoadState = AdLoadState.LOADING
        lastErrorText = ""
        notifyContentChanged()

        Admob.getInstance().loadSwipeableInterstitialAd(
            hostActivity,
            adUnitId,
            maxScreenHoldDurationSeconds = requestedMaxScreenHoldSeconds,
            swipeDirection = null,
            tapsAllowed = true,
            adCallback = object : AdCallback() {
                override fun onSwipeableAdLoaded(ad: SwipeableInterstitialAd?) {
                    swipeableAd = ad
                    if (ad != null) {
                        registerVideoLifecycleCallbacks(ad)
                    }
                    currentLoadState = AdLoadState.LOADED
                    if (viewPager.currentItem % 2 == 0) {
                        viewPager.isUserInputEnabled = true
                    }
                    notifyContentChanged()
                    showSwipeableInterstitialAd()
                    externalCallback?.onSwipeableAdLoaded(ad)
                    externalCallback?.onAdLoaded()
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    swipeableAd = null
                    currentLoadState = AdLoadState.FAILED
                    lastErrorText = i?.message ?: ""
                    if (viewPager.currentItem % 2 == 0) {
                        viewPager.isUserInputEnabled = true
                    }
                    notifyContentChanged()
                    externalCallback?.onAdFailedToLoad(i)
                }

                override fun onSwipeableScreenHoldTimerStarted() {
                    externalCallback?.onSwipeableScreenHoldTimerStarted()
                }

                override fun onAdImpression() {
                    externalCallback?.onAdImpression()
                }

                override fun onAdClicked() {
                    externalCallback?.onAdClicked()
                }

                override fun onAdClicked(
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                    externalCallback?.onAdClicked(adUnitId, mediationAdapterClassName, adType)
                }

                override fun onAdLogRev(
                    adValue: AdValue?,
                    adUnitId: String?,
                    mediationAdapterClassName: String?,
                    adType: AdType?
                ) {
                    externalCallback?.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType)
                }

                override fun onAdClosed() {
                    checkScreenHoldOnReturn()
                    externalCallback?.onAdClosed()
                }

                override fun onAdFailedToShow(adError: FullScreenContentError?) {
                    externalCallback?.onAdFailedToShow(adError)
                }
            }
        )
    }

    private fun registerVideoLifecycleCallbacks(ad: SwipeableInterstitialAd) {
        ad.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks {
            override fun onVideoStart() {}
            override fun onVideoPlay() {}
            override fun onVideoPause() {}
            override fun onVideoEnd() {}
            override fun onVideoMute(isMuted: Boolean) {}
        }
    }

    private fun notifyContentChanged() {
        viewPager.post {
            val contentPosition = if (viewPager.currentItem % 2 == 0) {
                viewPager.currentItem
            } else {
                viewPager.currentItem - 1
            }
            viewPager.adapter?.notifyItemChanged(contentPosition)
        }
    }

    private fun showSwipeableInterstitialAd() {
        if (swipeableAd == null || viewPager.adapter == null) return
        viewPager.post {
            val adPosition = if (viewPager.currentItem % 2 == 1) {
                viewPager.currentItem
            } else {
                viewPager.currentItem + 1
            }
            viewPager.adapter?.notifyItemChanged(adPosition)
        }
    }

    private fun startHoldScreen() {
        val holdTime = swipeableAd?.getScreenHoldDuration() ?: 0
        if (holdTime <= 0) {
            viewPager.isUserInputEnabled = true
            holdCountdownBadge.visibility = View.VISIBLE
            holdCountdownBadge.text = "✅ Vuốt lên/xuống tự do (Hold 0s)"
            return
        }
        viewPager.isUserInputEnabled = false
        holdStartTimeMs = System.currentTimeMillis()
        holdDurationMs = holdTime * 1000L

        unlockRunnable?.let { handler.removeCallbacks(it) }
        tickRunnable?.let { handler.removeCallbacks(it) }

        val ticker = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - holdStartTimeMs
                val remainingSec = ((holdDurationMs - elapsed + 999) / 1000).coerceAtLeast(0)
                if (remainingSec > 0) {
                    holdCountdownBadge.visibility = View.VISIBLE
                    holdCountdownBadge.text = "🔒 Khóa vuốt: còn ${remainingSec}s"
                    handler.postDelayed(this, 250)
                } else {
                    holdCountdownBadge.visibility = View.VISIBLE
                    holdCountdownBadge.text = "✅ Đã mở khóa — Vuốt tiếp!"
                }
            }
        }
        tickRunnable = ticker
        handler.post(ticker)

        val r = Runnable {
            viewPager.isUserInputEnabled = true
            holdStartTimeMs = 0L
            holdCountdownBadge.visibility = View.VISIBLE
            holdCountdownBadge.text = "✅ Đã mở khóa — Vuốt tiếp!"
        }
        unlockRunnable = r
        handler.postDelayed(r, holdDurationMs)
    }

    private fun stopHoldCountdownUi() {
        tickRunnable?.let { handler.removeCallbacks(it) }
        holdCountdownBadge.visibility = View.GONE
    }

    private fun checkScreenHoldOnReturn() {
        if (viewPager.isUserInputEnabled || holdStartTimeMs == 0L) return
        val elapsed = System.currentTimeMillis() - holdStartTimeMs
        if (elapsed >= holdDurationMs) {
            unlockRunnable?.let { handler.removeCallbacks(it) }
            tickRunnable?.let { handler.removeCallbacks(it) }
            viewPager.isUserInputEnabled = true
            holdStartTimeMs = 0L
            holdCountdownBadge.text = "✅ Đã mở khóa — Vuốt tiếp!"
        }
    }

    private fun preventOverscrollArtifacts(vp: ViewPager2) {
        for (i in 0 until vp.childCount) {
            val child = vp.getChildAt(i)
            if (child is RecyclerView) {
                child.overScrollMode = View.OVER_SCROLL_NEVER
                child.itemAnimator = null
                break
            }
        }
    }

    private fun discardAd() {
        swipeableAd?.let { ad ->
            val adView = ad.getView(hostActivity)
            (adView.parent as? ViewGroup)?.removeView(adView)
            ad.destroy()
        }
        swipeableAd = null
    }

    override fun dismiss() {
        unlockRunnable?.let { handler.removeCallbacks(it) }
        tickRunnable?.let { handler.removeCallbacks(it) }
        viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        discardAd()
        super.dismiss()
        externalCallback?.onNextAction()
    }

    private inner class SwipeablePagerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val TYPE_CONTENT = 0
        private val TYPE_AD = 1

        override fun getItemViewType(position: Int): Int =
            if (position % 2 == 0) TYPE_CONTENT else TYPE_AD

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == TYPE_CONTENT) {
                createContentViewHolder(parent)
            } else {
                val adContainer = FrameLayout(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(Color.BLACK)
                }
                AdViewHolder(adContainer)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is ContentViewHolder) {
                val slideNumber = (position / 2) + 1
                holder.slideBadge.text = "🎬 SHORT VIDEO FEED • SLIDE #$slideNumber"
                holder.adLoadStateLabel.text = if (lastErrorText.isNotEmpty()) {
                    "${currentLoadState.statusText}: $lastErrorText"
                } else {
                    currentLoadState.statusText
                }
                holder.adLoadStateLabel.setTextColor(currentLoadState.statusColor)
                holder.adLoadStateBadgeBg.setColor(currentLoadState.bgColor)

                val ad = swipeableAd
                holder.loadedHoldTimeLabel.text = if (ad != null) {
                    "⏱ Thời gian giữ màn hình từ Ad Server: ${ad.getScreenHoldDuration()}s"
                } else {
                    "⏱ Thời gian giữ màn hình tối đa yêu cầu: ${requestedMaxScreenHoldSeconds}s"
                }

                holder.screenHoldRadioGroup.setOnCheckedChangeListener(null)
                if (requestedMaxScreenHoldSeconds == 5) {
                    holder.radio5s.isChecked = true
                } else {
                    holder.radio0s.isChecked = true
                }
                holder.screenHoldRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                    val newHoldSeconds = if (checkedId == holder.radio5s.id) 5 else 0
                    if (newHoldSeconds != requestedMaxScreenHoldSeconds) {
                        requestedMaxScreenHoldSeconds = newHoldSeconds
                        reloadSwipeableInterstitialAd()
                    }
                }
            } else if (holder is AdViewHolder) {
                val current = viewPager.currentItem
                if (position == current || (current % 2 == 0 && position == current + 1)) {
                    swipeableAd?.let { ad ->
                        val adView = ad.getView(hostActivity)
                        if (adView.parent == null) {
                            holder.adContainer.addView(
                                adView,
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        } else if (adView.parent != holder.adContainer) {
                            (adView.parent as? ViewGroup)?.removeView(adView)
                            holder.adContainer.addView(
                                adView,
                                ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                    }
                }
            }
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            super.onViewRecycled(holder)
            if (holder is AdViewHolder) {
                holder.adContainer.removeAllViews()
            }
        }

        override fun getItemCount(): Int = Int.MAX_VALUE

        private fun createContentViewHolder(parent: ViewGroup): ContentViewHolder {
            val container = LinearLayout(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(
                        Color.parseColor("#0F172A"),
                        Color.parseColor("#1E1B4B"),
                        Color.parseColor("#0F172A")
                    )
                )
                setPadding(dpToPx(24), dpToPx(32), dpToPx(24), dpToPx(32))
            }

            val card = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1E293B"))
                    cornerRadius = dpToPx(24).toFloat()
                    setStroke(dpToPx(1), Color.parseColor("#334155"))
                }
                setPadding(dpToPx(24), dpToPx(28), dpToPx(24), dpToPx(28))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            val slideBadge = TextView(parent.context).apply {
                text = "🎬 SHORT VIDEO FEED • SLIDE #1"
                setTextColor(Color.parseColor("#38BDF8"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1E3A8A"))
                    cornerRadius = dpToPx(12).toFloat()
                }
                setPadding(dpToPx(12), dpToPx(5), dpToPx(12), dpToPx(5))
            }
            card.addView(slideBadge)

            val instructionText = TextView(parent.context).apply {
                text = "⌃ Vuốt lên để xem\nSwipeable Interstitial Ad"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(18), 0, dpToPx(8))
            }
            card.addView(instructionText)

            val subDesc = TextView(parent.context).apply {
                text = "Trải nghiệm quảng cáo xen kẽ luồng dọc (Reels / TikTok / Shorts) của GMA Next-Gen SDK."
                setTextColor(Color.parseColor("#94A3B8"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dpToPx(20))
            }
            card.addView(subDesc)

            val holdTitle = TextView(parent.context).apply {
                text = "Chế độ giữ màn hình tối đa (Max Screen Hold)"
                setTextColor(Color.parseColor("#E2E8F0"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dpToPx(8))
            }
            card.addView(holdTitle)

            val radioGroup = RadioGroup(parent.context).apply {
                orientation = RadioGroup.HORIZONTAL
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#0F172A"))
                    cornerRadius = dpToPx(12).toFloat()
                }
                setPadding(dpToPx(16), dpToPx(6), dpToPx(16), dpToPx(6))
            }
            val radio0s = RadioButton(parent.context).apply {
                id = View.generateViewId()
                text = "0 giây (Vuốt ngay)"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(dpToPx(4), 0, dpToPx(16), 0)
            }
            val radio5s = RadioButton(parent.context).apply {
                id = View.generateViewId()
                text = "5 giây (Giữ tối đa)"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            }
            radioGroup.addView(radio0s)
            radioGroup.addView(radio5s)
            card.addView(radioGroup)

            val loadedHoldLabel = TextView(parent.context).apply {
                text = " "
                setTextColor(Color.parseColor("#CBD5E1"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(14), 0, dpToPx(12))
            }
            card.addView(loadedHoldLabel)

            val stateBg = GradientDrawable().apply {
                setColor(Color.parseColor("#33FBBF24"))
                cornerRadius = dpToPx(12).toFloat()
            }
            val loadStateLabel = TextView(parent.context).apply {
                text = "⏳ Đang tải Swipeable Ad..."
                setTextColor(Color.parseColor("#FBBF24"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                background = stateBg
                setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
            }
            card.addView(loadStateLabel)

            container.addView(card)

            return ContentViewHolder(
                container,
                slideBadge,
                radioGroup,
                radio0s,
                radio5s,
                loadedHoldLabel,
                loadStateLabel,
                stateBg
            )
        }

        inner class ContentViewHolder(
            root: View,
            val slideBadge: TextView,
            val screenHoldRadioGroup: RadioGroup,
            val radio0s: RadioButton,
            val radio5s: RadioButton,
            val loadedHoldTimeLabel: TextView,
            val adLoadStateLabel: TextView,
            val adLoadStateBadgeBg: GradientDrawable
        ) : RecyclerView.ViewHolder(root)

        inner class AdViewHolder(val adContainer: FrameLayout) :
            RecyclerView.ViewHolder(adContainer)
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            hostActivity.resources.displayMetrics
        ).toInt()
    }
}
