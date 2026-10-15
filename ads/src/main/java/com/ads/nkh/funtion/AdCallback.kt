package com.ads.nkh.funtion

import com.ads.nkh.ads.wrapper.ApInterstitialAd
import com.ads.nkh.ads.wrapper.ApNativeAd
import com.ads.nkh.ads.wrapper.ApPipAd
import com.ads.nkh.ads.wrapper.ApSwipeableAd
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.swipeableinterstitial.SwipeableInterstitialAd

open class AdCallback {

    open fun onNextAction() {}

    open fun onAdClosed() {}

    open fun onAdFailedToLoad(i: LoadAdError?) {}

    open fun onAdFailedToShow(adError: FullScreenContentError?) {}

    open fun onAdFailedToShowHigh(adError: FullScreenContentError?) {}

    open fun onAdFailedToShowMedium(adError: FullScreenContentError?) {}

    open fun onAdFailedToShowAll(adError: FullScreenContentError?) {}

    open fun onAdLoaded() {}

    open fun onAdLoadedHigh() {}

    open fun onAdLoadedAll() {}

    open fun onAdSplashReady() {}

    open fun onInterstitialLoad(interstitialAd: InterstitialAd?) {}

    open fun onApInterstitialLoad(apInterstitialAd: ApInterstitialAd?) {}

    open fun onAdClicked() {}

    open fun onAdClickedHigh() {}

    open fun onAdClickedMedium() {}

    open fun onAdClickedAll() {}

    open fun onAdImpression() {}

    open fun onRewardAdLoaded(rewardedAd: RewardedAd?) {}

    open fun onRewardAdLoaded(rewardedAd: RewardedInterstitialAd?) {}

    open fun onUnifiedNativeAdLoaded(unifiedNativeAd: NativeAd) {}

    open fun onNativeAdLoaded(nativeAd: ApNativeAd) {}

    open fun onPipAdLoaded(pipAd: PictureInPictureAd?) {}

    open fun onApPipAdLoaded(apPipAd: ApPipAd?) {}

    open fun onPipAdShown() {}

    open fun onPipAdHidden() {}

    open fun onSwipeableAdLoaded(swipeableAd: SwipeableInterstitialAd?) {}

    open fun onApSwipeableAdLoaded(apSwipeableAd: ApSwipeableAd?) {}

    open fun onSwipeableScreenHoldTimerStarted() {}

    open fun onInterstitialShow() {}

    open fun onAdSplashHigh1Ready() {}

    open fun onAdSplashHigh2Ready() {}

    open fun onAdSplashHigh3Ready() {}

    open fun onAdSplashNormalReady() {}

    open fun onAdHighFailedToLoad() {}

    open fun onAdPriorityFailedToLoad(adError: LoadAdError?) {}

    open fun onAdPriorityFailedToShow(adError: FullScreenContentError?) {}

    // Log Rev
    open fun onAdLogRev(
        adValue: AdValue?,
        adUnitId: String?,
        mediationAdapterClassName: String?,
        adType: AdType?
    ) {}

    open fun onAdClicked(
        adUnitId: String?,
        mediationAdapterClassName: String?,
        adType: AdType?
    ) {}
}
