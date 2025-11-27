package com.ads.nkh.ads.native_ads;

/**
 * Configuration class for customizing Native Ad appearance
 */
public class NativeAdConfigV2 {
    private String colorCTA = "default";
    private int heightCTA = 40;

    public String getColorCTA() {
        return colorCTA;
    }

    public void setColorCTA(String colorCTA) {
        this.colorCTA = colorCTA;
    }

    public int getHeightCTA() {
        return heightCTA;
    }

    public void setHeightCTA(int heightCTA) {
        this.heightCTA = heightCTA;
    }

    public NativeAdConfigV2(String colorCTA, int heightCTA) {
        this.colorCTA = colorCTA;
        this.heightCTA = heightCTA;
    }

    public NativeAdConfigV2() {}
}