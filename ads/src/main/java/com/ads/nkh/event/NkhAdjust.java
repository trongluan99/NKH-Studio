package com.ads.nkh.event;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAdRevenue;
import com.adjust.sdk.AdjustEvent;
import com.ads.nkh.ads.NkhAd;
import com.google.android.gms.ads.AdValue;

public class NkhAdjust {
    public static boolean enableAdjust = false;
    private static String eventNamePurchase = "";

    public static void setEventNamePurchase(String eventNamePurchase) {
        NkhAdjust.eventNamePurchase = eventNamePurchase;
    }

    public static void trackAdRevenue(String id) {
        AdjustAdRevenue adjustAdRevenue = new AdjustAdRevenue(id);
        Adjust.trackAdRevenue(adjustAdRevenue);
    }

    public static void onTrackEvent(String eventName) {
        AdjustEvent event = new AdjustEvent(eventName);
        Adjust.trackEvent(event);
    }

    public static void onTrackEvent(String eventName, String id) {
        AdjustEvent event = new AdjustEvent(eventName);
        event.setCallbackId(id);
        Adjust.trackEvent(event);
    }

    public static void onTrackRevenue(String eventName, float revenue, String currency) {
        AdjustEvent event = new AdjustEvent(eventName);
        event.setRevenue(revenue / 1000000.0, currency);
        Adjust.trackEvent(event);
    }

    public static void onTrackRevenuePurchase(float revenue, String currency) {
        if (NkhAdjust.enableAdjust) {
            onTrackRevenue(eventNamePurchase, revenue, currency);
        }

    }

    public static void pushTrackEventAdmob(AdValue adValue) {
        if (NkhAdjust.enableAdjust) {
            AdjustAdRevenue adRevenue = new AdjustAdRevenue("admob_sdk");
            adRevenue.setRevenue(adValue.getValueMicros() / 1000000.0, adValue.getCurrencyCode());

            Adjust.trackAdRevenue(adRevenue);
        }
    }

    static void logPaidAdImpressionValue(double revenue) {
        if (NkhAd.getInstance().getAdConfig().getAdjustConfig() != null && NkhAd.getInstance().getAdConfig().getAdjustConfig().isEnableAdjust()) {
            AdjustEvent event = new AdjustEvent(NkhAd.getInstance().getAdConfig().getAdjustConfig().getEventAdImpression());
            event.setRevenue(revenue, "USD");
            Adjust.trackEvent(event);
        }
    }

}
