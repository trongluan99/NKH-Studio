package com.ads.nkh.ads.native_ads;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Configuration class for customizing Native Ad appearance
 */
public class NativeAdConfig {
    
    // Text sizes (in SP)
    private float headlineTextSize = 16f;
    private float bodyTextSize = 14f;
    private float callToActionTextSize = 14f;
    private float priceTextSize = 12f;
    private float advertiserTextSize = 12f;
    
    // Text colors
    private int headlineTextColor = 0xFF000000; // Black
    private int bodyTextColor = 0xFF666666; // Gray
    private int callToActionTextColor = 0xFFFFFFFF; // White
    private int priceTextColor = 0xFF333333; // Dark gray
    private int advertiserTextColor = 0xFF999999; // Light gray
    
    // Background colors
    private int callToActionBackgroundColor = 0xFF4285F4; // Blue
    private int adViewBackgroundColor = 0xFFFFFFFF; // White
    
    // Icon size (in DP)
    private int iconSize = 48;
    
    // Star rating size (in DP)
    private int starRatingSize = 16;
    
    // Text styles
    private Typeface headlineTypeface = null;
    private Typeface bodyTypeface = null;
    private Typeface callToActionTypeface = null;
    private String headlineFontAsset = null;
    private String bodyFontAsset = null;
    private String callToActionFontAsset = null;
    
    // Corner radius for call to action button (in DP)
    private float callToActionCornerRadius = 8f;
    
    // Call to action button height (in DP)
    private int callToActionHeight = 0; // 0 means use default height
    
    // Padding values (in DP)
    private int contentPadding = 16;
    
    // Media view size (in DP)
    private float mediaViewSize = 120f;
    
    public NativeAdConfig() {
        // Default constructor with default values
    }
    
    // Getters
    public float getHeadlineTextSize() {
        return headlineTextSize;
    }
    
    public float getBodyTextSize() {
        return bodyTextSize;
    }
    
    public float getCallToActionTextSize() {
        return callToActionTextSize;
    }
    
    public float getPriceTextSize() {
        return priceTextSize;
    }
    
    public float getAdvertiserTextSize() {
        return advertiserTextSize;
    }
    
    public int getHeadlineTextColor() {
        return headlineTextColor;
    }
    
    public int getBodyTextColor() {
        return bodyTextColor;
    }
    
    public int getCallToActionTextColor() {
        return callToActionTextColor;
    }
    
    public int getPriceTextColor() {
        return priceTextColor;
    }
    
    public int getAdvertiserTextColor() {
        return advertiserTextColor;
    }
    
    public int getCallToActionBackgroundColor() {
        return callToActionBackgroundColor;
    }
    
    public int getAdViewBackgroundColor() {
        return adViewBackgroundColor;
    }
    
    public int getIconSize() {
        return iconSize;
    }
    
    public int getStarRatingSize() {
        return starRatingSize;
    }
    
    public Typeface getHeadlineTypeface() {
        return headlineTypeface;
    }
    
    public Typeface getBodyTypeface() {
        return bodyTypeface;
    }
    
    public Typeface getCallToActionTypeface() {
        return callToActionTypeface;
    }
    public String getHeadlineFontAsset() { return headlineFontAsset; }
    public String getBodyFontAsset() { return bodyFontAsset; }
    public String getCallToActionFontAsset() { return callToActionFontAsset; }
    
    public float getCallToActionCornerRadius() {
        return callToActionCornerRadius;
    }
    
    public int getCallToActionHeight() {
        return callToActionHeight;
    }
    
    public int getContentPadding() {
        return contentPadding;
    }
    
    public float getMediaViewSize() {
        return mediaViewSize;
    }
    
    // Setters with builder pattern
    public NativeAdConfig setHeadlineTextSize(float headlineTextSize) {
        this.headlineTextSize = headlineTextSize;
        return this;
    }
    
    public NativeAdConfig setBodyTextSize(float bodyTextSize) {
        this.bodyTextSize = bodyTextSize;
        return this;
    }
    
    public NativeAdConfig setCallToActionTextSize(float callToActionTextSize) {
        this.callToActionTextSize = callToActionTextSize;
        return this;
    }
    
    public NativeAdConfig setPriceTextSize(float priceTextSize) {
        this.priceTextSize = priceTextSize;
        return this;
    }
    
    public NativeAdConfig setAdvertiserTextSize(float advertiserTextSize) {
        this.advertiserTextSize = advertiserTextSize;
        return this;
    }
    
    public NativeAdConfig setHeadlineTextColor(int headlineTextColor) {
        this.headlineTextColor = headlineTextColor;
        return this;
    }
    
    public NativeAdConfig setBodyTextColor(int bodyTextColor) {
        this.bodyTextColor = bodyTextColor;
        return this;
    }
    
    public NativeAdConfig setCallToActionTextColor(int callToActionTextColor) {
        this.callToActionTextColor = callToActionTextColor;
        return this;
    }
    
    public NativeAdConfig setPriceTextColor(int priceTextColor) {
        this.priceTextColor = priceTextColor;
        return this;
    }
    
    public NativeAdConfig setAdvertiserTextColor(int advertiserTextColor) {
        this.advertiserTextColor = advertiserTextColor;
        return this;
    }
    
    public NativeAdConfig setCallToActionBackgroundColor(int callToActionBackgroundColor) {
        this.callToActionBackgroundColor = callToActionBackgroundColor;
        return this;
    }
    
    public NativeAdConfig setAdViewBackgroundColor(int adViewBackgroundColor) {
        this.adViewBackgroundColor = adViewBackgroundColor;
        return this;
    }
    
    public NativeAdConfig setIconSize(int iconSize) {
        this.iconSize = iconSize;
        return this;
    }
    
    public NativeAdConfig setStarRatingSize(int starRatingSize) {
        this.starRatingSize = starRatingSize;
        return this;
    }
    
    public NativeAdConfig setHeadlineTypeface(Typeface headlineTypeface) {
        this.headlineTypeface = headlineTypeface;
        return this;
    }
    
    public NativeAdConfig setBodyTypeface(Typeface bodyTypeface) {
        this.bodyTypeface = bodyTypeface;
        return this;
    }
    
    public NativeAdConfig setCallToActionTypeface(Typeface callToActionTypeface) {
        this.callToActionTypeface = callToActionTypeface;
        return this;
    }
    public NativeAdConfig setHeadlineFontAsset(String headlineFontAsset) {
        this.headlineFontAsset = headlineFontAsset;
        return this;
    }
    public NativeAdConfig setBodyFontAsset(String bodyFontAsset) {
        this.bodyFontAsset = bodyFontAsset;
        return this;
    }
    public NativeAdConfig setCallToActionFontAsset(String callToActionFontAsset) {
        this.callToActionFontAsset = callToActionFontAsset;
        return this;
    }
    
    public NativeAdConfig setCallToActionCornerRadius(float callToActionCornerRadius) {
        this.callToActionCornerRadius = callToActionCornerRadius;
        return this;
    }
    
    public NativeAdConfig setCallToActionHeight(int callToActionHeight) {
        this.callToActionHeight = callToActionHeight;
        return this;
    }
    
    public NativeAdConfig setContentPadding(int contentPadding) {
        this.contentPadding = contentPadding;
        return this;
    }
    
    public NativeAdConfig setMediaViewSize(float mediaViewSize) {
        this.mediaViewSize = mediaViewSize;
        return this;
    }

    public static NativeAdConfig fromJson(Context context, String jsonString) throws JSONException {
        JSONObject obj = new JSONObject(jsonString);
        return fromJson(context, obj);
    }

    public static NativeAdConfig fromJson(Context context, JSONObject obj) {
        NativeAdConfig cfg = new NativeAdConfig();
        // Sizes
        if (obj.has("headlineTextSize")) cfg.setHeadlineTextSize((float) obj.optDouble("headlineTextSize", cfg.getHeadlineTextSize()));
        if (obj.has("bodyTextSize")) cfg.setBodyTextSize((float) obj.optDouble("bodyTextSize", cfg.getBodyTextSize()));
        if (obj.has("callToActionTextSize")) cfg.setCallToActionTextSize((float) obj.optDouble("callToActionTextSize", cfg.getCallToActionTextSize()));
        if (obj.has("priceTextSize")) cfg.setPriceTextSize((float) obj.optDouble("priceTextSize", cfg.getPriceTextSize()));
        if (obj.has("advertiserTextSize")) cfg.setAdvertiserTextSize((float) obj.optDouble("advertiserTextSize", cfg.getAdvertiserTextSize()));
        if (obj.has("iconSize")) cfg.setIconSize(obj.optInt("iconSize", cfg.getIconSize()));
        if (obj.has("starRatingSize")) cfg.setStarRatingSize(obj.optInt("starRatingSize", cfg.getStarRatingSize()));
        if (obj.has("contentPadding")) cfg.setContentPadding(obj.optInt("contentPadding", cfg.getContentPadding()));
        if (obj.has("mediaViewSize")) cfg.setMediaViewSize((float) obj.optDouble("mediaViewSize", cfg.getMediaViewSize()));

        // Colors (support hex strings like #RRGGBB/#AARRGGBB)
        if (obj.has("headlineTextColor")) cfg.setHeadlineTextColor(parseColor(obj.optString("headlineTextColor"), cfg.getHeadlineTextColor()));
        if (obj.has("bodyTextColor")) cfg.setBodyTextColor(parseColor(obj.optString("bodyTextColor"), cfg.getBodyTextColor()));
        if (obj.has("callToActionTextColor")) cfg.setCallToActionTextColor(parseColor(obj.optString("callToActionTextColor"), cfg.getCallToActionTextColor()));
        if (obj.has("priceTextColor")) cfg.setPriceTextColor(parseColor(obj.optString("priceTextColor"), cfg.getPriceTextColor()));
        if (obj.has("advertiserTextColor")) cfg.setAdvertiserTextColor(parseColor(obj.optString("advertiserTextColor"), cfg.getAdvertiserTextColor()));
        if (obj.has("callToActionBackgroundColor")) cfg.setCallToActionBackgroundColor(parseColor(obj.optString("callToActionBackgroundColor"), cfg.getCallToActionBackgroundColor()));
        if (obj.has("adViewBackgroundColor")) cfg.setAdViewBackgroundColor(parseColor(obj.optString("adViewBackgroundColor"), cfg.getAdViewBackgroundColor()));

        // Corner radius and height
        if (obj.has("callToActionCornerRadius")) cfg.setCallToActionCornerRadius((float) obj.optDouble("callToActionCornerRadius", cfg.getCallToActionCornerRadius()));
        if (obj.has("callToActionHeight")) cfg.setCallToActionHeight(obj.optInt("callToActionHeight", cfg.getCallToActionHeight()));

        // Fonts via assets
        String headlineAsset = obj.optString("headlineFontAsset", null);
        String bodyAsset = obj.optString("bodyFontAsset", null);
        String ctaAsset = obj.optString("callToActionFontAsset", null);
        if (headlineAsset != null && !headlineAsset.isEmpty()) {
            cfg.setHeadlineFontAsset(headlineAsset);
            try { cfg.setHeadlineTypeface(Typeface.createFromAsset(context.getAssets(), headlineAsset)); } catch (Exception ignored) {}
        }
        if (bodyAsset != null && !bodyAsset.isEmpty()) {
            cfg.setBodyFontAsset(bodyAsset);
            try { cfg.setBodyTypeface(Typeface.createFromAsset(context.getAssets(), bodyAsset)); } catch (Exception ignored) {}
        }
        if (ctaAsset != null && !ctaAsset.isEmpty()) {
            cfg.setCallToActionFontAsset(ctaAsset);
            try { cfg.setCallToActionTypeface(Typeface.createFromAsset(context.getAssets(), ctaAsset)); } catch (Exception ignored) {}
        }

        return cfg;
    }

    private static int parseColor(String value, int fallback) {
        if (value == null || value.isEmpty()) return fallback;
        try {
            return Color.parseColor(value);
        } catch (Exception e) {
            return fallback;
        }
    }
}