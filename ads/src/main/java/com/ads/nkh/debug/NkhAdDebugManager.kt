package com.ads.nkh.debug

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.ads.nkh.admob.Admob
import com.ads.nkh.ads.NkhAd
import com.ads.nkh.billing.AppPurchase
import com.ads.nkh.util.SharePreferenceUtils
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

object NkhAdDebugManager {

    enum class AdEventState {
        LOADING,
        LOADED,
        SHOWN,
        PAID,
        CLICKED,
        FAILED
    }

    data class AdTraceRecord(
        val traceId: Long,
        val format: String,
        val adUnitId: String,
        val isTestAdUnit: Boolean,
        val startElapsedMs: Long,
        val timestampStr: String,
        @Volatile var state: AdEventState = AdEventState.LOADING,
        @Volatile var latencyMs: Long = -1L,
        @Volatile var adapterClassName: String = "",
        @Volatile var detailMessage: String = ""
    )

    private const val MAX_RECORDS = 80
    private const val FLOATING_TAG = "NKH_DEBUG_FLOATING_BADGE"
    private val mainHandler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val records = Collections.synchronizedList(ArrayList<AdTraceRecord>())
    private val activeRequestsByUnit = ConcurrentHashMap<String, AdTraceRecord>()
    private val attachedActivities = Collections.synchronizedMap(WeakHashMap<Activity, View>())
    @Volatile
    private var lifecycleRegistered = false
    @Volatile
    private var traceCounter = 0L

    @JvmStatic
    fun isDebugEnabled(context: Context?): Boolean {
        val cfg = NkhAd.getInstance().adConfig ?: return false
        if (!cfg.isEnableDebugMode) return false
        val ctx = context ?: cfg.application
        return 0 != (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)
    }

    @JvmStatic
    fun registerLifecycleIfNeeded(application: Application) {
        if (lifecycleRegistered) return
        synchronized(this) {
            if (lifecycleRegistered) return
            lifecycleRegistered = true
            application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityResumed(activity: Activity) {
                    val cfg = NkhAd.getInstance().adConfig ?: return
                    if (cfg.isEnableDebugMode && cfg.isShowFloatingDebugButton && isDebugEnabled(activity)) {
                        attachFloatingBadge(activity)
                    } else {
                        detachFloatingBadge(activity)
                    }
                }

                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {
                    attachedActivities.remove(activity)
                }
            })
        }
    }

    @JvmStatic
    fun onAdRequestStarted(format: String, adUnitId: String, isTestId: Boolean): Long {
        if (!isDebugEnabled(null)) return -1L
        val id = ++traceCounter
        val nowStr = synchronized(timeFormat) { timeFormat.format(Date()) }
        val record = AdTraceRecord(
            traceId = id,
            format = format,
            adUnitId = adUnitId,
            isTestAdUnit = isTestId,
            startElapsedMs = SystemClock.elapsedRealtime(),
            timestampStr = nowStr,
            state = AdEventState.LOADING
        )
        synchronized(records) {
            records.add(0, record)
            while (records.size > MAX_RECORDS) {
                records.removeAt(records.size - 1)
            }
        }
        activeRequestsByUnit["$format:$adUnitId"] = record
        return id
    }

    @JvmStatic
    fun onAdLoaded(format: String, adUnitId: String, adapterClassName: String? = null) {
        if (!isDebugEnabled(null)) return
        val record = activeRequestsByUnit["$format:$adUnitId"] ?: findLatestRecord(format, adUnitId) ?: return
        val elapsed = (SystemClock.elapsedRealtime() - record.startElapsedMs).coerceAtLeast(1L)
        record.state = AdEventState.LOADED
        record.latencyMs = elapsed
        if (!adapterClassName.isNullOrBlank()) {
            record.adapterClassName = adapterClassName
        }
    }

    @JvmStatic
    fun onAdFailed(format: String, adUnitId: String, errorCode: Int, errorMessage: String?) {
        if (!isDebugEnabled(null)) return
        val record = activeRequestsByUnit.remove("$format:$adUnitId") ?: findLatestRecord(format, adUnitId) ?: return
        val elapsed = (SystemClock.elapsedRealtime() - record.startElapsedMs).coerceAtLeast(1L)
        record.state = AdEventState.FAILED
        record.latencyMs = elapsed
        record.detailMessage = "Code $errorCode: ${errorMessage ?: "Unknown error"}"
    }

    @JvmStatic
    fun onAdShown(format: String, adUnitId: String, adapterClassName: String? = null) {
        if (!isDebugEnabled(null)) return
        val record = findLatestRecord(format, adUnitId) ?: return
        record.state = AdEventState.SHOWN
        if (!adapterClassName.isNullOrBlank()) {
            record.adapterClassName = adapterClassName
        }
    }

    @JvmStatic
    fun onAdPaid(
        format: String,
        adUnitId: String,
        valueMicros: Long,
        currencyCode: String,
        adapterClassName: String?
    ) {
        if (!isDebugEnabled(null)) return
        val record = findLatestRecord(format, adUnitId)
        val revUsd = valueMicros / 1_000_000.0
        val revText = String.format(Locale.US, "%.6f %s (%d µs)", revUsd, currencyCode, valueMicros)
        if (record != null) {
            record.state = AdEventState.PAID
            if (!adapterClassName.isNullOrBlank()) {
                record.adapterClassName = adapterClassName
            }
            record.detailMessage = "Paid: $revText"
        }
    }

    @JvmStatic
    fun onAdClicked(format: String, adUnitId: String, adapterClassName: String? = null) {
        if (!isDebugEnabled(null)) return
        val record = findLatestRecord(format, adUnitId) ?: return
        record.state = AdEventState.CLICKED
        if (!adapterClassName.isNullOrBlank()) {
            record.adapterClassName = adapterClassName
        }
    }

    private fun findLatestRecord(format: String, adUnitId: String): AdTraceRecord? {
        synchronized(records) {
            return records.firstOrNull { it.format == format && it.adUnitId == adUnitId }
                ?: records.firstOrNull { it.adUnitId == adUnitId }
        }
    }

    @JvmStatic
    fun getRecordsSnapshot(): List<AdTraceRecord> {
        synchronized(records) {
            return ArrayList(records)
        }
    }

    @JvmStatic
    fun clearRecords() {
        synchronized(records) {
            records.clear()
        }
        activeRequestsByUnit.clear()
    }

    private fun dp(context: Context, value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    @SuppressLint("ClickableViewAccessibility")
    @JvmStatic
    fun attachFloatingBadge(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        val clsName = activity.javaClass.name
        if (clsName.contains("AdActivity") || clsName.startsWith("com.google.android.gms.ads")) return
        val decorView = activity.window?.decorView as? ViewGroup ?: return
        if (decorView.findViewWithTag<View>(FLOATING_TAG) != null) return

        val badge = TextView(activity).apply {
            tag = FLOATING_TAG
            text = "🐞 Ads"
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            textSize = 12f
            setPadding(dp(activity, 12), dp(activity, 7), dp(activity, 12), dp(activity, 7))
            elevation = dp(activity, 8).toFloat()
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#059669"), Color.parseColor("#0284C7"))
            ).apply {
                cornerRadius = dp(activity, 20).toFloat()
                setStroke(dp(activity, 1), Color.parseColor("#6EE7B7"))
            }
        }

        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = dp(activity, 96)
            rightMargin = dp(activity, 12)
        }

        var downRawX = 0f
        var downRawY = 0f
        var startX = 0f
        var startY = 0f
        badge.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = v.x
                    startY = v.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    v.x = (startX + dx).coerceIn(0f, (decorView.width - v.width).toFloat().coerceAtLeast(0f))
                    v.y = (startY + dy).coerceIn(dp(activity, 36).toFloat(), (decorView.height - v.height).toFloat().coerceAtLeast(0f))
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val moved = abs(event.rawX - downRawX) + abs(event.rawY - downRawY)
                    if (moved < dp(activity, 10)) {
                        showDebugInspectorDialog(activity)
                    }
                    true
                }
                else -> false
            }
        }

        try {
            decorView.addView(badge, params)
            attachedActivities[activity] = badge
        } catch (_: Throwable) {
        }
    }

    @JvmStatic
    fun detachFloatingBadge(activity: Activity) {
        val decorView = activity.window?.decorView as? ViewGroup ?: return
        val existing = decorView.findViewWithTag<View>(FLOATING_TAG)
        if (existing != null) {
            try {
                decorView.removeView(existing)
            } catch (_: Throwable) {
            }
        }
        attachedActivities.remove(activity)
    }

    @SuppressLint("SetTextI18n")
    @JvmStatic
    fun showDebugInspectorDialog(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 16), dp(activity, 16), dp(activity, 16), dp(activity, 16))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0F172A"))
                cornerRadius = dp(activity, 18).toFloat()
                setStroke(dp(activity, 1), Color.parseColor("#334155"))
            }
        }

        // 1. Header Row
        val headerRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleCol = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val titleTv = TextView(activity).apply {
            text = "🐞 NKH-Studio Ads Debug Console"
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
        }
        val initLatency = Admob.getInstance().lastInitLatencyMs
        val subtitleTv = TextView(activity).apply {
            text = "GMA Next-Gen 1.5.0 • SDK Init: ${if (initLatency > 0) "${initLatency}ms" else "Ready"}"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 12f
        }
        titleCol.addView(titleTv)
        titleCol.addView(subtitleTv)

        val closeBtn = TextView(activity).apply {
            text = "✕"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(activity, 10), dp(activity, 4), dp(activity, 10), dp(activity, 4))
            setOnClickListener { dialog.dismiss() }
        }
        headerRow.addView(titleCol)
        headerRow.addView(closeBtn)
        root.addView(headerRow)

        // 2. Quick Toggles & Actions Row
        val actionScroll = HorizontalScrollView(activity).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, dp(activity, 10), 0, dp(activity, 10))
        }
        val actionRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        fun makeActionChip(label: String, bgColor: String, onClick: (TextView) -> Unit): TextView {
            return TextView(activity).apply {
                text = label
                setTextColor(Color.WHITE)
                textSize = 11.5f
                setTypeface(null, Typeface.BOLD)
                setPadding(dp(activity, 11), dp(activity, 7), dp(activity, 11), dp(activity, 7))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(bgColor))
                    cornerRadius = dp(activity, 10).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    rightMargin = dp(activity, 8)
                }
                setOnClickListener { onClick(this) }
            }
        }

        // Chip 1: Google Ad Inspector
        actionRow.addView(
            makeActionChip("🔍 Google Ad Inspector", "#0284C7") {
                dialog.dismiss()
                try {
                    MobileAds.openAdInspector { error ->
                        if (error != null) {
                            mainHandler.post {
                                Toast.makeText(
                                    activity,
                                    "Ad Inspector: ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } catch (e: Throwable) {
                    Toast.makeText(activity, "Lỗi mở Ad Inspector: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // Chip 2: Simulate VIP (isPurchased)
        val isVipNow = AppPurchase.getInstance().isPurchased(activity)
        actionRow.addView(
            makeActionChip(
                if (isVipNow) "👑 VIP (No-Ads): BẬT" else "👑 VIP (No-Ads): TẮT",
                if (isVipNow) "#059669" else "#475569"
            ) { chip ->
                val nextVip = !AppPurchase.getInstance().isPurchased(activity)
                AppPurchase.getInstance().setPurchase(nextVip)
                chip.text = if (nextVip) "👑 VIP (No-Ads): BẬT" else "👑 VIP (No-Ads): TẮT"
                chip.background = GradientDrawable().apply {
                    setColor(Color.parseColor(if (nextVip) "#059669" else "#475569"))
                    cornerRadius = dp(activity, 10).toFloat()
                }
                Toast.makeText(
                    activity,
                    if (nextVip) "Đã bật giả lập VIP (Tắt toàn bộ Ads)" else "Đã tắt giả lập VIP (Hiện lại Ads)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        // Chip 3: Toggle Organic / Non-Organic (UA)
        val isOrganicNow = SharePreferenceUtils.getIsOrganic(activity)
        actionRow.addView(
            makeActionChip(
                if (isOrganicNow) "🌱 User: Organic" else "🚀 User: Non-Organic (UA)",
                if (isOrganicNow) "#475569" else "#7C3AED"
            ) { chip ->
                val nextOrganic = !SharePreferenceUtils.getIsOrganic(activity)
                SharePreferenceUtils.setIsOrganic(activity, nextOrganic)
                chip.text = if (nextOrganic) "🌱 User: Organic" else "🚀 User: Non-Organic (UA)"
                chip.background = GradientDrawable().apply {
                    setColor(Color.parseColor(if (nextOrganic) "#475569" else "#7C3AED"))
                    cornerRadius = dp(activity, 10).toFloat()
                }
            }
        )

        // Chip 4: Toggle Native Validator Popup
        val adConfig = NkhAd.getInstance().adConfig
        val validatorNow = adConfig?.isShowAdDebugDialog == true
        actionRow.addView(
            makeActionChip(
                if (validatorNow) "📐 Native Validator: BẬT" else "📐 Native Validator: TẮT",
                if (validatorNow) "#059669" else "#475569"
            ) { chip ->
                val nextVal = !(adConfig?.isShowAdDebugDialog ?: false)
                adConfig?.isShowAdDebugDialog = nextVal
                chip.text = if (nextVal) "📐 Native Validator: BẬT" else "📐 Native Validator: TẮT"
                chip.background = GradientDrawable().apply {
                    setColor(Color.parseColor(if (nextVal) "#059669" else "#475569"))
                    cornerRadius = dp(activity, 10).toFloat()
                }
            }
        )

        actionScroll.addView(actionRow)
        root.addView(actionScroll)

        // 3. Tabs Row (Tab 1: Live Ad Requests | Tab 2: 8 Bidding Networks)
        val tabRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(activity, 8))
        }
        val contentScroll = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (activity.resources.displayMetrics.heightPixels * 0.56f).toInt()
            )
        }
        val contentContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        contentScroll.addView(contentContainer)

        fun renderHistoryTab() {
            contentContainer.removeAllViews()
            val snapshot = getRecordsSnapshot()
            if (snapshot.isEmpty()) {
                val emptyTv = TextView(activity).apply {
                    text = "Chưa có request quảng cáo nào được ghi nhận trong phiên này."
                    setTextColor(Color.parseColor("#94A3B8"))
                    textSize = 13f
                    setPadding(dp(activity, 8), dp(activity, 24), dp(activity, 8), dp(activity, 24))
                }
                contentContainer.addView(emptyTv)
                return
            }

            // Summary stats bar
            val loadedCount = snapshot.count {
                it.state == AdEventState.LOADED || it.state == AdEventState.SHOWN ||
                    it.state == AdEventState.PAID || it.state == AdEventState.CLICKED
            }
            val failedCount = snapshot.count { it.state == AdEventState.FAILED }
            val avgLatency = snapshot.filter { it.latencyMs > 0 }.map { it.latencyMs }.average()
            val avgText = if (avgLatency.isNaN()) "N/A" else "${avgLatency.toLong()} ms"

            val statsBar = TextView(activity).apply {
                text = "Tổng: ${snapshot.size} request  •  Thành công: $loadedCount  •  Lỗi: $failedCount  •  TB Tải: $avgText"
                setTextColor(Color.parseColor("#A7F3D0"))
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setPadding(dp(activity, 10), dp(activity, 8), dp(activity, 10), dp(activity, 8))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1E293B"))
                    cornerRadius = dp(activity, 8).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(activity, 8)
                }
            }
            contentContainer.addView(statsBar)

            for (item in snapshot) {
                val itemCard = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10))
                    val borderColor = when (item.state) {
                        AdEventState.FAILED -> "#F43F5E"
                        AdEventState.PAID -> "#F59E0B"
                        AdEventState.SHOWN, AdEventState.LOADED, AdEventState.CLICKED -> "#10B981"
                        AdEventState.LOADING -> "#38BDF8"
                    }
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#1E293B"))
                        cornerRadius = dp(activity, 10).toFloat()
                        setStroke(dp(activity, 1), Color.parseColor(borderColor))
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(activity, 8)
                    }
                    setOnClickListener {
                        val cb = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cb?.setPrimaryClip(ClipData.newPlainText("AdUnitId", item.adUnitId))
                        Toast.makeText(activity, "Đã copy Ad Unit ID: ${item.adUnitId}", Toast.LENGTH_SHORT).show()
                    }
                }

                val topLine = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                val stateLabel = when (item.state) {
                    AdEventState.LOADING -> "⏳ LOADING"
                    AdEventState.LOADED -> "✅ LOADED"
                    AdEventState.SHOWN -> "📺 SHOWN"
                    AdEventState.PAID -> "💰 PAID"
                    AdEventState.CLICKED -> "👆 CLICKED"
                    AdEventState.FAILED -> "❌ FAILED"
                }
                val leftTitle = TextView(activity).apply {
                    text = "$stateLabel • ${item.format} (${if (item.isTestAdUnit) "TEST ID" else "PROD ID"})"
                    setTextColor(Color.WHITE)
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                val latencyTv = TextView(activity).apply {
                    text = if (item.latencyMs >= 0) "${item.latencyMs} ms" else "..."
                    val color = when {
                        item.latencyMs < 0 -> "#38BDF8"
                        item.latencyMs <= 1200 -> "#34D399"
                        item.latencyMs <= 2500 -> "#FBBF24"
                        else -> "#FB7185"
                    }
                    setTextColor(Color.parseColor(color))
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                }
                topLine.addView(leftTitle)
                topLine.addView(latencyTv)
                itemCard.addView(topLine)

                val unitTv = TextView(activity).apply {
                    text = "ID: ${item.adUnitId}  •  Lúc: ${item.timestampStr}"
                    setTextColor(Color.parseColor("#94A3B8"))
                    textSize = 11.5f
                    setPadding(0, dp(activity, 3), 0, 0)
                }
                itemCard.addView(unitTv)

                if (item.adapterClassName.isNotBlank()) {
                    val shortAdapter = item.adapterClassName.substringAfterLast('.')
                    val adapterTv = TextView(activity).apply {
                        text = "🤝 Network: $shortAdapter (${item.adapterClassName})"
                        setTextColor(Color.parseColor("#38BDF8"))
                        textSize = 11.5f
                        setPadding(0, dp(activity, 2), 0, 0)
                    }
                    itemCard.addView(adapterTv)
                }

                if (item.detailMessage.isNotBlank()) {
                    val detailTv = TextView(activity).apply {
                        text = item.detailMessage
                        setTextColor(
                            if (item.state == AdEventState.FAILED) Color.parseColor("#FDA4AF")
                            else Color.parseColor("#FDE68A")
                        )
                        textSize = 11.5f
                        setPadding(0, dp(activity, 2), 0, 0)
                    }
                    itemCard.addView(detailTv)
                }

                contentContainer.addView(itemCard)
            }
        }

        fun renderBiddingTab() {
            contentContainer.removeAllViews()
            val networks = Admob.getInstance().inspectBiddingNetworks()
            for (net in networks) {
                val card = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10))
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#1E293B"))
                        cornerRadius = dp(activity, 10).toFloat()
                        setStroke(
                            dp(activity, 1),
                            Color.parseColor(if (net.isIntegrated) "#10B981" else "#F43F5E")
                        )
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(activity, 8)
                    }
                }
                val title = TextView(activity).apply {
                    text = "${if (net.isIntegrated) "✅" else "❌"} ${net.networkName} • [${net.serverState}]"
                    setTextColor(Color.WHITE)
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                }
                val sub = TextView(activity).apply {
                    text = "Adapter: ${net.adapterVersion} | SDK: ${net.sdkVersion} | RTB: ${if (net.isRtbBiddingSupported) "YES" else "NO"} | Latency: ${net.serverLatencyMs}ms"
                    setTextColor(Color.parseColor("#94A3B8"))
                    textSize = 11.5f
                    setPadding(0, dp(activity, 3), 0, 0)
                }
                card.addView(title)
                card.addView(sub)
                if (net.serverDescription.isNotBlank()) {
                    val desc = TextView(activity).apply {
                        text = net.serverDescription
                        setTextColor(Color.parseColor("#FBBF24"))
                        textSize = 11f
                        setPadding(0, dp(activity, 2), 0, 0)
                    }
                    card.addView(desc)
                }
                contentContainer.addView(card)
            }
        }

        val tabHistoryBtn = makeActionChip("📊 Lịch sử Request Ads", "#059669") {
            renderHistoryTab()
        }.apply {
            gravity = Gravity.CENTER
            setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(activity, 6)
            }
        }
        val tabBiddingBtn = makeActionChip("🤝 8 Mạng Bidding & SDK", "#334155") {
            renderBiddingTab()
        }.apply {
            gravity = Gravity.CENTER
            setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(activity, 6)
            }
        }
        val clearBtn = makeActionChip("🗑️ Xóa Log", "#475569") {
            clearRecords()
            renderHistoryTab()
        }
        actionRow.addView(clearBtn)

        tabHistoryBtn.setOnClickListener {
            tabHistoryBtn.background = GradientDrawable().apply {
                setColor(Color.parseColor("#059669"))
                cornerRadius = dp(activity, 10).toFloat()
            }
            tabBiddingBtn.background = GradientDrawable().apply {
                setColor(Color.parseColor("#334155"))
                cornerRadius = dp(activity, 10).toFloat()
            }
            renderHistoryTab()
        }
        tabBiddingBtn.setOnClickListener {
            tabBiddingBtn.background = GradientDrawable().apply {
                setColor(Color.parseColor("#059669"))
                cornerRadius = dp(activity, 10).toFloat()
            }
            tabHistoryBtn.background = GradientDrawable().apply {
                setColor(Color.parseColor("#334155"))
                cornerRadius = dp(activity, 10).toFloat()
            }
            renderBiddingTab()
        }

        tabRow.addView(tabHistoryBtn)
        tabRow.addView(tabBiddingBtn)
        root.addView(tabRow)
        root.addView(contentScroll)

        renderHistoryTab()

        dialog.setContentView(root)
        dialog.window?.let { win ->
            win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val lp = win.attributes
            lp.width = (activity.resources.displayMetrics.widthPixels * 0.94f).toInt()
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
            win.attributes = lp
        }

        try {
            dialog.show()
        } catch (_: Throwable) {
        }
    }
}
