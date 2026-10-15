package com.ads.nkh.event

import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustEvent
import com.adjust.sdk.AdjustPlayStoreSubscription
import com.ads.nkh.ads.NkhAd
import com.google.android.libraries.ads.mobile.sdk.common.AdValue

object NkhAdjust {
    @JvmField
    var enableAdjust: Boolean = false

    private var eventNamePurchase: String = ""

    @JvmStatic
    fun setEventNamePurchase(eventNamePurchase: String) {
        NkhAdjust.eventNamePurchase = eventNamePurchase
    }

    @JvmStatic
    fun trackAdRevenue(id: String) {
        if (id.isEmpty()) return
        val adjustAdRevenue = AdjustAdRevenue(id)
        Adjust.trackAdRevenue(adjustAdRevenue)
    }

    @JvmStatic
    fun onTrackEvent(eventName: String) {
        if (eventName.isEmpty()) return
        val event = AdjustEvent(eventName)
        Adjust.trackEvent(event)
    }

    @JvmStatic
    fun onTrackEvent(eventName: String, id: String) {
        if (eventName.isEmpty()) return
        val event = AdjustEvent(eventName)
        event.setCallbackId(id)
        Adjust.trackEvent(event)
    }

    @JvmStatic
    fun onTrackRevenue(eventName: String, revenue: Float, currency: String) {
        if (eventName.isEmpty()) return
        val event = AdjustEvent(eventName)
        event.setRevenue(revenue / 1000000.0, currency)
        Adjust.trackEvent(event)
    }

    @JvmStatic
    fun onTrackRevenuePurchase(revenue: Float, currency: String) {
        if (enableAdjust && eventNamePurchase.isNotEmpty()) {
            onTrackRevenue(eventNamePurchase, revenue, currency)
        }
    }

    /**
     * Track a Play Store subscription purchase for server-side verification.
     * Adjust re-validates the token with Google (Developer API) and tracks
     * renewals/cancellations/refunds automatically via RTDN, without further
     * client-side calls on renew.
     *
     * @param priceMicros      subscription price in micros (ProductDetails.PricingPhase#getPriceAmountMicros)
     * @param currency         ISO 4217 currency code
     * @param sku              subscription product ID
     * @param orderId          Purchase#getOrderId()
     * @param signature        Purchase#getSignature()
     * @param purchaseToken    Purchase#getPurchaseToken()
     * @param purchaseTimeMillis Purchase#getPurchaseTime()
     */
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
        if (!enableAdjust) {
            return
        }
        val subscription = AdjustPlayStoreSubscription(
            priceMicros,
            currency,
            sku,
            orderId,
            signature,
            purchaseToken
        )
        subscription.setPurchaseTime(purchaseTimeMillis)
        Adjust.trackPlayStoreSubscription(subscription)
    }

    @JvmStatic
    fun pushTrackEventAdmob(adValue: AdValue) {
        if (enableAdjust) {
            val adRevenue = AdjustAdRevenue("admob_sdk")
            adRevenue.setRevenue(adValue.valueMicros / 1000000.0, adValue.currencyCode)
            Adjust.trackAdRevenue(adRevenue)
        }
    }

    @JvmStatic
    fun logPaidAdImpressionValue(revenue: Double) {
        val adjustConfig = NkhAd.getInstance().adConfig?.adjustConfig
        if (adjustConfig != null && adjustConfig.isEnableAdjust && !adjustConfig.eventAdImpression.isNullOrEmpty()) {
            val event = AdjustEvent(adjustConfig.eventAdImpression)
            event.setRevenue(revenue, "USD")
            Adjust.trackEvent(event)
        }
    }
}
