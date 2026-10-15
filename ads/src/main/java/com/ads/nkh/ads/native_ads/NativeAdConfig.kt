package com.ads.nkh.ads.native_ads

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import org.json.JSONException
import org.json.JSONObject

/**
 * Configuration class for customizing Native Ad appearance
 */
class NativeAdConfig {

    // Text sizes (in SP)
    private var headlineTextSize: Float = 16f
    private var bodyTextSize: Float = 14f
    private var callToActionTextSize: Float = 14f
    private var priceTextSize: Float = 12f
    private var advertiserTextSize: Float = 12f

    // Text colors
    private var headlineTextColor: Int = 0xFF000000.toInt() // Black
    private var bodyTextColor: Int = 0xFF666666.toInt() // Gray
    private var callToActionTextColor: Int = 0xFFFFFFFF.toInt() // White
    private var priceTextColor: Int = 0xFF333333.toInt() // Dark gray
    private var advertiserTextColor: Int = 0xFF999999.toInt() // Light gray

    // Background colors
    private var callToActionBackgroundColor: Int = 0xFF4285F4.toInt() // Blue
    private var adViewBackgroundColor: Int = 0xFFFFFFFF.toInt() // White

    // Icon size (in DP)
    private var iconSize: Int = 48

    // Star rating size (in DP)
    private var starRatingSize: Int = 16

    // Text styles
    private var headlineTypeface: Typeface? = null
    private var bodyTypeface: Typeface? = null
    private var callToActionTypeface: Typeface? = null
    private var headlineFontAsset: String? = null
    private var bodyFontAsset: String? = null
    private var callToActionFontAsset: String? = null

    // Corner radius for call to action button (in DP)
    private var callToActionCornerRadius: Float = 8f

    // Call to action button height (in DP)
    private var callToActionHeight: Int = 0 // 0 means use default height

    // Padding values (in DP)
    private var contentPadding: Int = 16

    // Media view size (in DP)
    private var mediaViewSize: Float = 120f

    // Getters
    fun getHeadlineTextSize(): Float = headlineTextSize

    fun getBodyTextSize(): Float = bodyTextSize

    fun getCallToActionTextSize(): Float = callToActionTextSize

    fun getPriceTextSize(): Float = priceTextSize

    fun getAdvertiserTextSize(): Float = advertiserTextSize

    fun getHeadlineTextColor(): Int = headlineTextColor

    fun getBodyTextColor(): Int = bodyTextColor

    fun getCallToActionTextColor(): Int = callToActionTextColor

    fun getPriceTextColor(): Int = priceTextColor

    fun getAdvertiserTextColor(): Int = advertiserTextColor

    fun getCallToActionBackgroundColor(): Int = callToActionBackgroundColor

    fun getAdViewBackgroundColor(): Int = adViewBackgroundColor

    fun getIconSize(): Int = iconSize

    fun getStarRatingSize(): Int = starRatingSize

    fun getHeadlineTypeface(): Typeface? = headlineTypeface

    fun getBodyTypeface(): Typeface? = bodyTypeface

    fun getCallToActionTypeface(): Typeface? = callToActionTypeface

    fun getHeadlineFontAsset(): String? = headlineFontAsset

    fun getBodyFontAsset(): String? = bodyFontAsset

    fun getCallToActionFontAsset(): String? = callToActionFontAsset

    fun getCallToActionCornerRadius(): Float = callToActionCornerRadius

    fun getCallToActionHeight(): Int = callToActionHeight

    fun getContentPadding(): Int = contentPadding

    fun getMediaViewSize(): Float = mediaViewSize

    // Setters with builder pattern
    fun setHeadlineTextSize(headlineTextSize: Float): NativeAdConfig {
        this.headlineTextSize = headlineTextSize
        return this
    }

    fun setBodyTextSize(bodyTextSize: Float): NativeAdConfig {
        this.bodyTextSize = bodyTextSize
        return this
    }

    fun setCallToActionTextSize(callToActionTextSize: Float): NativeAdConfig {
        this.callToActionTextSize = callToActionTextSize
        return this
    }

    fun setPriceTextSize(priceTextSize: Float): NativeAdConfig {
        this.priceTextSize = priceTextSize
        return this
    }

    fun setAdvertiserTextSize(advertiserTextSize: Float): NativeAdConfig {
        this.advertiserTextSize = advertiserTextSize
        return this
    }

    fun setHeadlineTextColor(headlineTextColor: Int): NativeAdConfig {
        this.headlineTextColor = headlineTextColor
        return this
    }

    fun setBodyTextColor(bodyTextColor: Int): NativeAdConfig {
        this.bodyTextColor = bodyTextColor
        return this
    }

    fun setCallToActionTextColor(callToActionTextColor: Int): NativeAdConfig {
        this.callToActionTextColor = callToActionTextColor
        return this
    }

    fun setPriceTextColor(priceTextColor: Int): NativeAdConfig {
        this.priceTextColor = priceTextColor
        return this
    }

    fun setAdvertiserTextColor(advertiserTextColor: Int): NativeAdConfig {
        this.advertiserTextColor = advertiserTextColor
        return this
    }

    fun setCallToActionBackgroundColor(callToActionBackgroundColor: Int): NativeAdConfig {
        this.callToActionBackgroundColor = callToActionBackgroundColor
        return this
    }

    fun setAdViewBackgroundColor(adViewBackgroundColor: Int): NativeAdConfig {
        this.adViewBackgroundColor = adViewBackgroundColor
        return this
    }

    fun setIconSize(iconSize: Int): NativeAdConfig {
        this.iconSize = iconSize
        return this
    }

    fun setStarRatingSize(starRatingSize: Int): NativeAdConfig {
        this.starRatingSize = starRatingSize
        return this
    }

    fun setHeadlineTypeface(headlineTypeface: Typeface?): NativeAdConfig {
        this.headlineTypeface = headlineTypeface
        return this
    }

    fun setBodyTypeface(bodyTypeface: Typeface?): NativeAdConfig {
        this.bodyTypeface = bodyTypeface
        return this
    }

    fun setCallToActionTypeface(callToActionTypeface: Typeface?): NativeAdConfig {
        this.callToActionTypeface = callToActionTypeface
        return this
    }

    fun setHeadlineFontAsset(headlineFontAsset: String?): NativeAdConfig {
        this.headlineFontAsset = headlineFontAsset
        return this
    }

    fun setBodyFontAsset(bodyFontAsset: String?): NativeAdConfig {
        this.bodyFontAsset = bodyFontAsset
        return this
    }

    fun setCallToActionFontAsset(callToActionFontAsset: String?): NativeAdConfig {
        this.callToActionFontAsset = callToActionFontAsset
        return this
    }

    fun setCallToActionCornerRadius(callToActionCornerRadius: Float): NativeAdConfig {
        this.callToActionCornerRadius = callToActionCornerRadius
        return this
    }

    fun setCallToActionHeight(callToActionHeight: Int): NativeAdConfig {
        this.callToActionHeight = callToActionHeight
        return this
    }

    fun setContentPadding(contentPadding: Int): NativeAdConfig {
        this.contentPadding = contentPadding
        return this
    }

    fun setMediaViewSize(mediaViewSize: Float): NativeAdConfig {
        this.mediaViewSize = mediaViewSize
        return this
    }

    companion object {
        @JvmStatic
        @Throws(JSONException::class)
        fun fromJson(context: Context, jsonString: String): NativeAdConfig {
            val obj = JSONObject(jsonString)
            return fromJson(context, obj)
        }

        @JvmStatic
        fun fromJson(context: Context, obj: JSONObject): NativeAdConfig {
            val cfg = NativeAdConfig()
            // Sizes
            if (obj.has("headlineTextSize")) cfg.setHeadlineTextSize(obj.optDouble("headlineTextSize", cfg.getHeadlineTextSize().toDouble()).toFloat())
            if (obj.has("bodyTextSize")) cfg.setBodyTextSize(obj.optDouble("bodyTextSize", cfg.getBodyTextSize().toDouble()).toFloat())
            if (obj.has("callToActionTextSize")) cfg.setCallToActionTextSize(obj.optDouble("callToActionTextSize", cfg.getCallToActionTextSize().toDouble()).toFloat())
            if (obj.has("priceTextSize")) cfg.setPriceTextSize(obj.optDouble("priceTextSize", cfg.getPriceTextSize().toDouble()).toFloat())
            if (obj.has("advertiserTextSize")) cfg.setAdvertiserTextSize(obj.optDouble("advertiserTextSize", cfg.getAdvertiserTextSize().toDouble()).toFloat())
            if (obj.has("iconSize")) cfg.setIconSize(obj.optInt("iconSize", cfg.getIconSize()))
            if (obj.has("starRatingSize")) cfg.setStarRatingSize(obj.optInt("starRatingSize", cfg.getStarRatingSize()))
            if (obj.has("contentPadding")) cfg.setContentPadding(obj.optInt("contentPadding", cfg.getContentPadding()))
            if (obj.has("mediaViewSize")) cfg.setMediaViewSize(obj.optDouble("mediaViewSize", cfg.getMediaViewSize().toDouble()).toFloat())

            // Colors (support hex strings like #RRGGBB/#AARRGGBB)
            if (obj.has("headlineTextColor")) cfg.setHeadlineTextColor(parseColor(obj.optString("headlineTextColor"), cfg.getHeadlineTextColor()))
            if (obj.has("bodyTextColor")) cfg.setBodyTextColor(parseColor(obj.optString("bodyTextColor"), cfg.getBodyTextColor()))
            if (obj.has("callToActionTextColor")) cfg.setCallToActionTextColor(parseColor(obj.optString("callToActionTextColor"), cfg.getCallToActionTextColor()))
            if (obj.has("priceTextColor")) cfg.setPriceTextColor(parseColor(obj.optString("priceTextColor"), cfg.getPriceTextColor()))
            if (obj.has("advertiserTextColor")) cfg.setAdvertiserTextColor(parseColor(obj.optString("advertiserTextColor"), cfg.getAdvertiserTextColor()))
            if (obj.has("callToActionBackgroundColor")) cfg.setCallToActionBackgroundColor(parseColor(obj.optString("callToActionBackgroundColor"), cfg.getCallToActionBackgroundColor()))
            if (obj.has("adViewBackgroundColor")) cfg.setAdViewBackgroundColor(parseColor(obj.optString("adViewBackgroundColor"), cfg.getAdViewBackgroundColor()))

            // Corner radius and height
            if (obj.has("callToActionCornerRadius")) cfg.setCallToActionCornerRadius(obj.optDouble("callToActionCornerRadius", cfg.getCallToActionCornerRadius().toDouble()).toFloat())
            if (obj.has("callToActionHeight")) cfg.setCallToActionHeight(obj.optInt("callToActionHeight", cfg.getCallToActionHeight()))

            // Fonts via assets
            val headlineAsset = if (obj.has("headlineFontAsset") && !obj.isNull("headlineFontAsset")) obj.optString("headlineFontAsset") else null
            val bodyAsset = if (obj.has("bodyFontAsset") && !obj.isNull("bodyFontAsset")) obj.optString("bodyFontAsset") else null
            val ctaAsset = if (obj.has("callToActionFontAsset") && !obj.isNull("callToActionFontAsset")) obj.optString("callToActionFontAsset") else null
            if (!headlineAsset.isNullOrEmpty()) {
                cfg.setHeadlineFontAsset(headlineAsset)
                try {
                    cfg.setHeadlineTypeface(Typeface.createFromAsset(context.assets, headlineAsset))
                } catch (ignored: Exception) {
                }
            }
            if (!bodyAsset.isNullOrEmpty()) {
                cfg.setBodyFontAsset(bodyAsset)
                try {
                    cfg.setBodyTypeface(Typeface.createFromAsset(context.assets, bodyAsset))
                } catch (ignored: Exception) {
                }
            }
            if (!ctaAsset.isNullOrEmpty()) {
                cfg.setCallToActionFontAsset(ctaAsset)
                try {
                    cfg.setCallToActionTypeface(Typeface.createFromAsset(context.assets, ctaAsset))
                } catch (ignored: Exception) {
                }
            }

            return cfg
        }

        private fun parseColor(value: String?, fallback: Int): Int {
            if (value.isNullOrEmpty()) return fallback
            return try {
                Color.parseColor(value)
            } catch (e: Exception) {
                fallback
            }
        }
    }
}
