package com.ads.nkh.dialog

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.ads.nkh.admob.Admob
import com.ads.nkh.funtion.AdCallback
import com.ads.nkh.funtion.AdType
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView

class FullScreenNativeAdDialog(
    private val hostActivity: Activity,
    private val adUnitId: String,
    private val swipeDirection: NativeAd.SwipeGestureDirection = NativeAd.SwipeGestureDirection.UP,
    private val tapsAllowed: Boolean = true,
    private val externalCallback: AdCallback? = null
) : Dialog(hostActivity, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    private var loadedNativeAd: NativeAd? = null
    private lateinit var nativeAdView: NativeAdView
    private lateinit var mediaView: MediaView
    private lateinit var iconView: ImageView
    private lateinit var headlineView: TextView
    private lateinit var advertiserView: TextView
    private lateinit var bodyView: TextView
    private lateinit var ctaButton: Button
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val root = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#0B0F19"))
        }

        nativeAdView = NativeAdView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val innerContainer = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#0B0F19"))
        }

        mediaView = MediaView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        innerContainer.addView(mediaView)

        val bottomCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dpToPx(22).toFloat()
            }
            elevation = dpToPx(8).toFloat()
            setPadding(dpToPx(20), dpToPx(18), dpToPx(20), dpToPx(18))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM
                marginStart = dpToPx(20)
                marginEnd = dpToPx(20)
                bottomMargin = dpToPx(28)
            }
        }

        val swipeHint = TextView(context).apply {
            text = "⌃ Vuốt hướng ${swipeDirection.name} hoặc chạm nút bên dưới"
            setTextColor(Color.parseColor("#2563EB"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EFF6FF"))
                cornerRadius = dpToPx(10).toFloat()
            }
            setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(12)
            }
        }
        bottomCard.addView(swipeHint)

        val topInfoRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10)
            }
        }

        iconView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(52), dpToPx(52)).apply {
                marginEnd = dpToPx(12)
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F1F5F9"))
                cornerRadius = dpToPx(12).toFloat()
            }
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
        }
        topInfoRow.addView(iconView)

        val titleCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val badgeRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(3)
            }
        }

        val adBadge = TextView(context).apply {
            text = "Ad"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F59E0B"))
                cornerRadius = dpToPx(4).toFloat()
            }
            setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(6)
            }
        }
        badgeRow.addView(adBadge)

        advertiserView = TextView(context).apply {
            text = "Được tài trợ • Fullscreen Native"
            setTextColor(Color.parseColor("#64748B"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
        }
        badgeRow.addView(advertiserView)
        titleCol.addView(badgeRow)

        headlineView = TextView(context).apply {
            text = "Đang tải Fullscreen Swipeable Native Ad..."
            setTextColor(Color.parseColor("#0F172A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 2
        }
        titleCol.addView(headlineView)
        topInfoRow.addView(titleCol)
        bottomCard.addView(topInfoRow)

        bodyView = TextView(context).apply {
            text = "Quảng cáo Native toàn màn hình có hỗ trợ cử chỉ vuốt tùy chỉnh (Custom Click Swipe Gesture)."
            setTextColor(Color.parseColor("#475569"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(14)
            }
        }
        bottomCard.addView(bodyView)

        ctaButton = Button(context).apply {
            text = "CÀI ĐẶT NGAY"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            includeFontPadding = false
            minHeight = dpToPx(48)
            minimumHeight = dpToPx(48)
            setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2563EB"))
                cornerRadius = dpToPx(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        bottomCard.addView(ctaButton)

        innerContainer.addView(bottomCard)
        nativeAdView.addView(innerContainer)
        root.addView(nativeAdView)

        statusView = TextView(context).apply {
            text = "⏳ Đang tải Native..."
            setTextColor(Color.parseColor("#FBBF24"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC0F172A"))
                cornerRadius = dpToPx(18).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#44FFFFFF"))
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
        root.addView(statusView)

        val backButton = Button(context).apply {
            text = "← Đóng"
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
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                topMargin = dpToPx(40)
                marginStart = dpToPx(16)
            }
            setOnClickListener { dismiss() }
        }
        root.addView(backButton)

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
            (statusView.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
                lp.topMargin = topInset + dpToPx(14)
                statusView.layoutParams = lp
            }
            (bottomCard.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
                lp.bottomMargin = maxOf(bars.bottom, dpToPx(16)) + dpToPx(16)
                bottomCard.layoutParams = lp
            }
            insets
        }
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        loadFullscreenNative()
    }

    private fun loadFullscreenNative() {
        Admob.getInstance().loadSwipeableNativeAd(
            hostActivity,
            adUnitId,
            swipeDirection,
            tapsAllowed,
            object : AdCallback() {
                override fun onUnifiedNativeAdLoaded(unifiedNativeAd: NativeAd) {
                    loadedNativeAd = unifiedNativeAd
                    headlineView.text = unifiedNativeAd.headline ?: "Test Ad"
                    bodyView.text = unifiedNativeAd.body ?: ""
                    ctaButton.text = unifiedNativeAd.callToAction ?: "MỞ NGAY"
                    val adv = unifiedNativeAd.advertiser
                    if (!adv.isNullOrEmpty()) {
                        advertiserView.text = adv
                    }
                    val iconDrawable = unifiedNativeAd.icon?.drawable
                    if (iconDrawable != null) {
                        iconView.setImageDrawable(iconDrawable)
                        iconView.visibility = View.VISIBLE
                    } else {
                        iconView.visibility = View.GONE
                    }
                    nativeAdView.headlineView = headlineView
                    nativeAdView.bodyView = bodyView
                    nativeAdView.callToActionView = ctaButton
                    nativeAdView.iconView = iconView
                    nativeAdView.advertiserView = advertiserView
                    nativeAdView.registerNativeAd(unifiedNativeAd, mediaView)
                    statusView.text = "🟢 Sẵn sàng (Swipe=${unifiedNativeAd.isCustomClickGestureEnabled()})"
                    statusView.setTextColor(Color.parseColor("#34D399"))
                    externalCallback?.onUnifiedNativeAdLoaded(unifiedNativeAd)
                    externalCallback?.onAdLoaded()
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    statusView.text = "❌ Lỗi: ${i?.message}"
                    statusView.setTextColor(Color.parseColor("#F87171"))
                    externalCallback?.onAdFailedToLoad(i)
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
            }
        )
    }

    override fun dismiss() {
        loadedNativeAd?.destroy()
        loadedNativeAd = null
        super.dismiss()
        externalCallback?.onNextAction()
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            hostActivity.resources.displayMetrics
        ).toInt()
    }
}
