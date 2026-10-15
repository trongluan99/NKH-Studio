package com.ads.nkh.event

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustEvent
import com.ads.nkh.config.NkhAdConfig
import com.ads.nkh.util.AppUtil
import com.ads.nkh.util.SharePreferenceUtils
import com.facebook.appevents.AppEventsLogger
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import java.math.BigDecimal
import java.util.Currency
import java.util.concurrent.Executors

object NkhLogEventManager {

    private const val TAG = "NkhLogEventManager"

    private val logExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "NkhAd-EventLogger").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY - 1
        }
    }

    @JvmStatic
    fun logPaidAdImpression(
        context: Context,
        adValue: AdValue,
        adUnitId: String?,
        mediationAdapterClassName: String?
    ) {
        val appContext = context.applicationContext ?: context
        val valueMicros = adValue.valueMicros
        val currencyCode = adValue.currencyCode
        val precisionOrdinal = adValue.precisionType.ordinal
        logExecutor.execute {
            try {
                Log.d(
                    TAG,
                    "logPaidAdImpression: valueMicros=$valueMicros, currency=$currencyCode, " +
                        "precision=$precisionOrdinal, adUnitId=$adUnitId, network=$mediationAdapterClassName"
                )
                logEventWithAds(
                    appContext,
                    valueMicros.toFloat(),
                    precisionOrdinal,
                    adUnitId,
                    mediationAdapterClassName
                )
                NkhAdjust.pushTrackEventAdmob(adValue)
                val value = valueMicros * 1.0f / 1000000 * 26000
                try {
                    AppEventsLogger.newLogger(appContext)
                        .logPurchase(BigDecimal.valueOf(value.toDouble()), Currency.getInstance("VND"))
                } catch (e: Exception) {
                    Log.w(TAG, "Facebook logPurchase skipped: ${e.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "logPaidAdImpression error: ${e.message}")
            }
        }
    }

    @JvmStatic
    fun logPaidAdjustWithToken(adValue: AdValue, adUnitId: String?, token: String?) {
        if (token.isNullOrEmpty() || token == NkhAdConfig.ADJUST_TOKEN_TIKTOK_DEFAULT) {
            return
        }
        val valueMicros = adValue.valueMicros
        logExecutor.execute {
            try {
                val adjustEvent = AdjustEvent(token)
                val value = valueMicros * 1.0f / 1000000
                adjustEvent.setRevenue(value.toDouble(), "USD")
                adjustEvent.setOrderId(adUnitId)
                Adjust.trackEvent(adjustEvent)
                Log.d(TAG, "logPaidAdjustWithToken: token=$token, revenueUSD=$value, adUnitId=$adUnitId")
            } catch (e: Exception) {
                Log.w(TAG, "logPaidAdjustWithToken error: ${e.message}")
            }
        }
    }

    private fun logEventWithAds(
        context: Context,
        revenue: Float,
        precision: Int,
        adUnitId: String?,
        network: String?
    ) {
        val params = Bundle() // Log ad value in micros.
        params.putDouble("valuemicros", revenue.toDouble())
        params.putString("currency", "USD")
        params.putInt("precision", precision)
        params.putString("adunitid", adUnitId)
        params.putString("network", network)

        logPaidAdImpressionValue(context, revenue / 1000000.0, precision, adUnitId, network)
        FirebaseAnalyticsUtil.logEventWithAds(context, params)
        FacebookEventUtils.logEventWithAds(context, params)
        SharePreferenceUtils.updateCurrentTotalRevenueAd(context, revenue)
        logCurrentTotalRevenueAd(context, "event_current_total_revenue_ad")

        AppUtil.currentTotalRevenue001Ad += revenue
        SharePreferenceUtils.updateCurrentTotalRevenue001Ad(context, AppUtil.currentTotalRevenue001Ad)
        logTotalRevenue001Ad(context)

        logTotalRevenueAdIn3DaysIfNeed(context)
        logTotalRevenueAdIn7DaysIfNeed(context)
    }

    private fun logPaidAdImpressionValue(
        context: Context,
        value: Double,
        precision: Int,
        adunitid: String?,
        network: String?
    ) {
        val params = Bundle()
        params.putDouble("value", value)
        params.putString("currency", "USD")
        params.putInt("precision", precision)
        params.putString("adunitid", adunitid)
        params.putString("network", network)

        NkhAdjust.logPaidAdImpressionValue(value)
        FirebaseAnalyticsUtil.logPaidAdImpressionValue(context, params)
        FacebookEventUtils.logPaidAdImpressionValue(context, params)
    }

    @JvmStatic
    fun logClickAdsEvent(context: Context, adUnitId: String?) {
        val appContext = context.applicationContext ?: context
        logExecutor.execute {
            try {
                Log.d(TAG, String.format("User click ad for ad unit %s.", adUnitId))
                val bundle = Bundle()
                bundle.putString("ad_unit_id", adUnitId)

                FirebaseAnalyticsUtil.logClickAdsEvent(appContext, bundle)
                FacebookEventUtils.logClickAdsEvent(appContext, bundle)
            } catch (e: Exception) {
                Log.w(TAG, "logClickAdsEvent error: ${e.message}")
            }
        }
    }

    @JvmStatic
    fun logCurrentTotalRevenueAd(context: Context, eventName: String) {
        val currentTotalRevenue = SharePreferenceUtils.getCurrentTotalRevenueAd(context)
        Log.d(TAG, "logCurrentTotalRevenueAd: event=$eventName, totalMicros=$currentTotalRevenue")
        val bundle = Bundle()
        bundle.putFloat("value", currentTotalRevenue)

        FirebaseAnalyticsUtil.logCurrentTotalRevenueAd(context, eventName, bundle)
        FacebookEventUtils.logCurrentTotalRevenueAd(context, eventName, bundle)
    }

    @JvmStatic
    fun logTotalRevenue001Ad(context: Context) {
        val revenue = AppUtil.currentTotalRevenue001Ad
        if (revenue / 1000000 >= 0.01) {
            Log.d(TAG, "logTotalRevenue001Ad: threshold reached ($${revenue / 1000000f})")
            AppUtil.currentTotalRevenue001Ad = 0f
            SharePreferenceUtils.updateCurrentTotalRevenue001Ad(context, 0f)
            val bundle = Bundle()
            bundle.putFloat("value", revenue / 1000000)
            FirebaseAnalyticsUtil.logTotalRevenue001Ad(context, bundle)
            FacebookEventUtils.logTotalRevenue001Ad(context, bundle)
        }
    }

    @JvmStatic
    fun logTotalRevenueAdIn3DaysIfNeed(context: Context) {
        val installTime = SharePreferenceUtils.getInstallTime(context)
        if (!SharePreferenceUtils.isPushRevenue3Day(context) &&
            System.currentTimeMillis() - installTime >= 3L * 24 * 60 * 60 * 1000
        ) {
            Log.d(TAG, "logTotalRevenueAdAt3DaysIfNeed: ")
            logCurrentTotalRevenueAd(context, "event_total_revenue_ad_in_3_days")
            SharePreferenceUtils.setPushedRevenue3Day(context)
        }
    }

    @JvmStatic
    fun logTotalRevenueAdIn7DaysIfNeed(context: Context) {
        val installTime = SharePreferenceUtils.getInstallTime(context)
        if (!SharePreferenceUtils.isPushRevenue7Day(context) &&
            System.currentTimeMillis() - installTime >= 7L * 24 * 60 * 60 * 1000
        ) {
            Log.d(TAG, "logTotalRevenueAdAt7DaysIfNeed: ")
            logCurrentTotalRevenueAd(context, "event_total_revenue_ad_in_7_days")
            SharePreferenceUtils.setPushedRevenue7Day(context)
        }
    }

    @JvmStatic
    fun setEventNamePurchaseAdjust(eventNamePurchase: String) {
        NkhAdjust.setEventNamePurchase(eventNamePurchase)
    }

    @JvmStatic
    fun trackAdRevenue(id: String) {
        NkhAdjust.trackAdRevenue(id)
    }

    @JvmStatic
    fun onTrackEvent(eventName: String) {
        NkhAdjust.onTrackEvent(eventName)
    }

    @JvmStatic
    fun onTrackEvent(eventName: String, id: String) {
        NkhAdjust.onTrackEvent(eventName, id)
    }

    @JvmStatic
    fun onTrackRevenue(eventName: String, revenue: Float, currency: String) {
        NkhAdjust.onTrackRevenue(eventName, revenue, currency)
    }

    @JvmStatic
    fun onTrackRevenuePurchase(revenue: Float, currency: String, idPurchase: String?, typeIAP: Int) {
        NkhAdjust.onTrackRevenuePurchase(revenue, currency)
    }

    @JvmStatic
    fun trackPlayStoreSubscription(
        priceMicros: Long,
        currency: String?,
        sku: String?,
        orderId: String?,
        signature: String?,
        purchaseToken: String?,
        purchaseTimeMillis: Long
    ) {
        NkhAdjust.trackPlayStoreSubscription(
            priceMicros,
            currency,
            sku,
            orderId,
            signature,
            purchaseToken,
            purchaseTimeMillis
        )
    }

    @JvmStatic
    fun pushTrackEventAdmob(adValue: AdValue) {
        NkhAdjust.pushTrackEventAdmob(adValue)
    }
}
