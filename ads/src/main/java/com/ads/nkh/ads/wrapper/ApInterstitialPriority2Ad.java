package com.ads.nkh.ads.wrapper;

public class ApInterstitialPriority2Ad {
    private String highPriorityId;
    private String normalPriorityId;
    private ApInterstitialAd highPriorityInterstitialAd;
    private ApInterstitialAd normalPriorityInterstitialAd;

    public ApInterstitialPriority2Ad(String highPriorityId, String normalPriorityId) {
        this.highPriorityId = highPriorityId;
        this.normalPriorityId = normalPriorityId;
        if (!this.highPriorityId.isEmpty() && this.highPriorityInterstitialAd == null) {
            this.highPriorityInterstitialAd = new ApInterstitialAd();
        }
        if (!this.normalPriorityId.isEmpty() && this.normalPriorityInterstitialAd == null) {
            this.normalPriorityInterstitialAd = new ApInterstitialAd();
        }
    }

    public void setHighPriorityId(String highPriorityId) {
        this.highPriorityId = highPriorityId;
        if (!this.highPriorityId.isEmpty() && this.highPriorityInterstitialAd == null) {
            this.highPriorityInterstitialAd = new ApInterstitialAd();
        }
    }

    public void setNormalPriorityId(String normalPriorityId) {
        this.normalPriorityId = normalPriorityId;
        if (!this.normalPriorityId.isEmpty() && this.normalPriorityInterstitialAd == null) {
            this.normalPriorityInterstitialAd = new ApInterstitialAd();
        }
    }

    public String getHighPriorityId() {
        return highPriorityId;
    }

    public ApInterstitialAd getHighPriorityInterstitialAd() {
        return highPriorityInterstitialAd;
    }

    public String getNormalPriorityId() {
        return normalPriorityId;
    }

    public ApInterstitialAd getNormalPriorityInterstitialAd() {
        return normalPriorityInterstitialAd;
    }
}
