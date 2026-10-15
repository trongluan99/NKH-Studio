package com.nkh.module

import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ads.nkh.admob.Admob
import com.ads.nkh.admob.AppOpenManager
import com.ads.nkh.ads.NkhAd
import com.ads.nkh.ads.wrapper.ApInterstitialAd
import com.ads.nkh.ads.wrapper.ApInterstitialPriority2Ad
import com.ads.nkh.ads.wrapper.ApInterstitialPriorityAd
import com.ads.nkh.ads.wrapper.ApNativeAd
import com.ads.nkh.funtion.AdCallback
import com.ads.nkh.util.AppConstant
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class NkhAdsInstrumentedTest {

    @Test
    fun test01_loadAndPopulateNativeLargeAd() {
        val latch = CountDownLatch(1)
        val loadedNativeRef = AtomicReference<ApNativeAd?>(null)
        val errorRef = AtomicReference<LoadAdError?>(null)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val frAds = activity.findViewById<FrameLayout>(R.id.fr_ads)
                val shimmer = activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_native)
                NkhAd.getInstance().loadNativeAd(
                    activity,
                    BuildConfig.ad_native,
                    R.layout.native_large,
                    frAds,
                    shimmer,
                    object : AdCallback() {
                        override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                            super.onNativeAdLoaded(nativeAd)
                            loadedNativeRef.set(nativeAd)
                            latch.countDown()
                        }

                        override fun onAdFailedToLoad(i: LoadAdError?) {
                            super.onAdFailedToLoad(i)
                            errorRef.set(i)
                            latch.countDown()
                        }
                    }
                )
            }
            assertTrue("Timeout waiting for Native Large Ad", latch.await(20, TimeUnit.SECONDS))
            assertNotNull("Expected NativeAd loaded (error=${errorRef.get()?.message})", loadedNativeRef.get())
            assertNotNull("Expected underlying Next-Gen NativeAd", loadedNativeRef.get()?.admobNativeAd)

            scenario.onActivity { activity ->
                val frAds = activity.findViewById<FrameLayout>(R.id.fr_ads)
                assertEqualsVisibility(View.VISIBLE, frAds.visibility)
                assertTrue("Expected NativeAdView added to fr_ads", frAds.childCount > 0)
            }
        }
    }

    @Test
    fun test02_loadNativePriority4SameTimeAndPopulateCustomCta() {
        val latch = CountDownLatch(1)
        val loadedNativeRef = AtomicReference<ApNativeAd?>(null)
        val errorRef = AtomicReference<LoadAdError?>(null)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val frAds = activity.findViewById<FrameLayout>(R.id.fr_ads)
                val shimmer = activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_native)
                NkhAd.getInstance().loadNative4SameTime(
                    activity,
                    BuildConfig.ad_native,
                    BuildConfig.ad_native,
                    BuildConfig.ad_native,
                    BuildConfig.ad_native,
                    R.layout.native_large,
                    object : AdCallback() {
                        override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                            super.onNativeAdLoaded(nativeAd)
                            NkhAd.getInstance().populateNativeAdView(
                                activity,
                                nativeAd,
                                frAds,
                                shimmer,
                                "#2563EB",
                                44,
                                16
                            )
                            loadedNativeRef.set(nativeAd)
                            latch.countDown()
                        }

                        override fun onAdFailedToLoad(i: LoadAdError?) {
                            super.onAdFailedToLoad(i)
                            errorRef.set(i)
                            latch.countDown()
                        }
                    }
                )
            }
            assertTrue("Timeout waiting for Native Priority 4", latch.await(20, TimeUnit.SECONDS))
            assertNotNull("Expected Native Priority 4 loaded (error=${errorRef.get()?.message})", loadedNativeRef.get())
        }
    }

    @Test
    fun test03_loadAdaptiveAndCollapsibleBannerAds() {
        val adaptiveLatch = CountDownLatch(1)
        val collapsibleLatch = CountDownLatch(1)
        var adaptiveLoaded = false
        var collapsibleLoaded = false
        val errorRef = AtomicReference<LoadAdError?>(null)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                NkhAd.getInstance().loadBanner(
                    activity,
                    BuildConfig.ad_banner,
                    object : AdCallback() {
                        override fun onAdLoaded() {
                            super.onAdLoaded()
                            adaptiveLoaded = true
                            adaptiveLatch.countDown()
                        }

                        override fun onAdFailedToLoad(i: LoadAdError?) {
                            super.onAdFailedToLoad(i)
                            errorRef.set(i)
                            adaptiveLatch.countDown()
                        }
                    }
                )
            }
            assertTrue("Timeout waiting for Adaptive Banner", adaptiveLatch.await(20, TimeUnit.SECONDS))
            assertTrue("Expected Adaptive Banner loaded (error=${errorRef.get()?.message})", adaptiveLoaded)

            scenario.onActivity { activity ->
                NkhAd.getInstance().loadCollapsibleBanner(
                    activity,
                    BuildConfig.ad_banner,
                    AppConstant.CollapsibleGravity.BOTTOM,
                    object : AdCallback() {
                        override fun onAdLoaded() {
                            super.onAdLoaded()
                            collapsibleLoaded = true
                            collapsibleLatch.countDown()
                        }

                        override fun onAdFailedToLoad(i: LoadAdError?) {
                            super.onAdFailedToLoad(i)
                            errorRef.set(i)
                            collapsibleLatch.countDown()
                        }
                    }
                )
            }
            assertTrue("Timeout waiting for Collapsible Banner", collapsibleLatch.await(20, TimeUnit.SECONDS))
            assertTrue("Expected Collapsible Banner loaded (error=${errorRef.get()?.message})", collapsibleLoaded)
        }
    }

    @Test
    fun test04_loadInterstitialSingleAndPriority2AndPriority4() {
        val singleLatch = CountDownLatch(1)
        val p2Latch = CountDownLatch(1)
        val p4Latch = CountDownLatch(1)

        val singleRef = AtomicReference<InterstitialAd?>(null)
        val p2Ref = AtomicReference<ApInterstitialAd?>(null)
        val p4Ref = AtomicReference<ApInterstitialAd?>(null)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                Admob.getInstance().getInterstitialAds(
                    activity,
                    BuildConfig.ad_interstitial_splash,
                    object : AdCallback() {
                        override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                            super.onInterstitialLoad(interstitialAd)
                            singleRef.set(interstitialAd)
                            singleLatch.countDown()
                        }

                        override fun onAdFailedToLoad(i: LoadAdError?) {
                            super.onAdFailedToLoad(i)
                            singleLatch.countDown()
                        }
                    }
                )

                val p2 = ApInterstitialPriority2Ad(
                    BuildConfig.ad_interstitial_splash,
                    BuildConfig.ad_interstitial_splash
                )
                NkhAd.getInstance().loadPriority2InterstitialAds(
                    activity,
                    p2,
                    object : AdCallback() {
                        override fun onApInterstitialLoad(apInterstitialAd: ApInterstitialAd?) {
                            super.onApInterstitialLoad(apInterstitialAd)
                            if (p2Ref.compareAndSet(null, apInterstitialAd)) {
                                p2Latch.countDown()
                            }
                        }

                        override fun onAdFailedToLoad(i: LoadAdError?) {
                            super.onAdFailedToLoad(i)
                            p2Latch.countDown()
                        }
                    }
                )

                val p4 = ApInterstitialPriorityAd(
                    BuildConfig.ad_interstitial_splash,
                    BuildConfig.ad_interstitial_splash,
                    BuildConfig.ad_interstitial_splash,
                    BuildConfig.ad_interstitial_splash
                )
                NkhAd.getInstance().loadPriorityInterstitialAds(
                    activity,
                    p4,
                    object : AdCallback() {
                        override fun onApInterstitialLoad(apInterstitialAd: ApInterstitialAd?) {
                            super.onApInterstitialLoad(apInterstitialAd)
                            if (p4Ref.compareAndSet(null, apInterstitialAd)) {
                                p4Latch.countDown()
                            }
                        }

                        override fun onAdFailedToLoad(i: LoadAdError?) {
                            super.onAdFailedToLoad(i)
                            p4Latch.countDown()
                        }
                    }
                )
            }

            assertTrue("Timeout waiting for Single Interstitial", singleLatch.await(20, TimeUnit.SECONDS))
            assertNotNull("Expected Single Interstitial loaded", singleRef.get())

            assertTrue("Timeout waiting for Priority 2 Interstitial", p2Latch.await(20, TimeUnit.SECONDS))
            assertNotNull("Expected Priority 2 Interstitial loaded", p2Ref.get()?.interstitialAd)

            assertTrue("Timeout waiting for Priority 4 Interstitial", p4Latch.await(20, TimeUnit.SECONDS))
            assertNotNull("Expected Priority 4 Interstitial loaded", p4Ref.get()?.interstitialAd)
        }
    }

    @Test
    fun test05_loadRewardedAndRewardedInterstitialAndAppOpenAds() {
        val rewardLatch = CountDownLatch(1)
        val rewardInterLatch = CountDownLatch(1)
        val appOpenLatch = CountDownLatch(1)

        val rewardRef = AtomicReference<RewardedAd?>(null)
        val rewardInterRef = AtomicReference<RewardedInterstitialAd?>(null)
        var appOpenReady = false

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                NkhAd.getInstance().initRewardAds(
                    activity,
                    BuildConfig.ad_reward,
                    object : AdCallback() {
                        override fun onRewardAdLoaded(rewardedAd: RewardedAd?) {
                            super.onRewardAdLoaded(rewardedAd)
                            rewardRef.set(rewardedAd)
                            rewardLatch.countDown()
                        }

                        override fun onAdFailedToLoad(i: LoadAdError?) {
                            super.onAdFailedToLoad(i)
                            rewardLatch.countDown()
                        }
                    }
                )

                NkhAd.getInstance().getRewardInterstitial(
                    activity,
                    BuildConfig.ad_reward_inter,
                    object : AdCallback() {
                        override fun onRewardAdLoaded(rewardedAd: RewardedInterstitialAd?) {
                            super.onRewardAdLoaded(rewardedAd)
                            rewardInterRef.set(rewardedAd)
                            rewardInterLatch.countDown()
                        }

                        override fun onAdFailedToLoad(i: LoadAdError?) {
                            super.onAdFailedToLoad(i)
                            rewardInterLatch.countDown()
                        }
                    }
                )

                AppOpenManager.getInstance().loadOpenAppAdSplash(
                    activity,
                    BuildConfig.ads_open_app,
                    0,
                    15000,
                    false,
                    object : AdCallback() {
                        override fun onAdSplashReady() {
                            super.onAdSplashReady()
                            appOpenReady = true
                            appOpenLatch.countDown()
                        }

                        override fun onAdFailedToLoad(i: LoadAdError?) {
                            super.onAdFailedToLoad(i)
                            appOpenLatch.countDown()
                        }
                    }
                )
            }

            assertTrue("Timeout waiting for RewardedAd", rewardLatch.await(20, TimeUnit.SECONDS))
            assertNotNull("Expected RewardedAd loaded", rewardRef.get())

            assertTrue("Timeout waiting for RewardedInterstitialAd", rewardInterLatch.await(20, TimeUnit.SECONDS))
            assertNotNull("Expected RewardedInterstitialAd loaded", rewardInterRef.get())

            assertTrue("Timeout waiting for AppOpenAd", appOpenLatch.await(20, TimeUnit.SECONDS))
            assertTrue("Expected AppOpenAd splash ready", appOpenReady)
            assertNotNull("Expected AppOpenManager.splashAd non-null", AppOpenManager.getInstance().splashAd)
        }
    }

    @Test
    fun test06_loadSwipeableNativeAndPipAndSwipeableInterstitialAds() {
        val swipeNativeLatch = CountDownLatch(1)
        val pipLatch = CountDownLatch(1)
        val swipeInterLatch = CountDownLatch(1)

        val swipeNativeRef = AtomicReference<ApNativeAd?>(null)
        val pipRef = AtomicReference<com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAd?>(null)
        val swipeInterRef = AtomicReference<com.google.android.libraries.ads.mobile.sdk.swipeableinterstitial.SwipeableInterstitialAd?>(null)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val frAds = activity.findViewById<FrameLayout>(R.id.fr_ads)
                val shimmer = activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_native)

                NkhAd.getInstance().loadSwipeableNativeAd(
                    activity,
                    BuildConfig.ad_native,
                    R.layout.native_large,
                    frAds,
                    shimmer,
                    com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd.SwipeGestureDirection.UP,
                    true,
                    object : AdCallback() {
                        override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                            swipeNativeRef.set(nativeAd)
                            swipeNativeLatch.countDown()
                        }

                        override fun onAdFailedToLoad(i: LoadAdError?) {
                            swipeNativeLatch.countDown()
                        }
                    }
                )

                NkhAd.getInstance().getPipAd(
                    activity,
                    BuildConfig.ad_pip,
                    object : AdCallback() {
                        override fun onPipAdLoaded(pipAd: com.google.android.libraries.ads.mobile.sdk.pip.PictureInPictureAd?) {
                            pipRef.set(pipAd)
                            pipLatch.countDown()
                        }

                        override fun onAdFailedToLoad(i: LoadAdError?) {
                            pipLatch.countDown()
                        }
                    }
                )

                NkhAd.getInstance().getSwipeableInterstitialAd(
                    activity,
                    BuildConfig.ad_swipeable_inter,
                    maxScreenHoldDurationSeconds = 5,
                    swipeDirection = null,
                    tapsAllowed = true,
                    adCallback = object : AdCallback() {
                        override fun onSwipeableAdLoaded(swipeableAd: com.google.android.libraries.ads.mobile.sdk.swipeableinterstitial.SwipeableInterstitialAd?) {
                            swipeInterRef.set(swipeableAd)
                            swipeInterLatch.countDown()
                        }

                        override fun onAdFailedToLoad(i: LoadAdError?) {
                            swipeInterLatch.countDown()
                        }
                    }
                )
            }

            assertTrue("Timeout waiting for Swipeable NativeAd", swipeNativeLatch.await(20, TimeUnit.SECONDS))
            assertNotNull("Expected Swipeable NativeAd loaded", swipeNativeRef.get()?.admobNativeAd)

            assertTrue("Timeout waiting for PiP Ad", pipLatch.await(20, TimeUnit.SECONDS))
            assertNotNull("Expected PictureInPictureAd loaded with ad_pip", pipRef.get())

            assertTrue("Timeout waiting for SwipeableInterstitialAd", swipeInterLatch.await(20, TimeUnit.SECONDS))
            assertNotNull("Expected SwipeableInterstitialAd loaded with ad_swipeable_inter", swipeInterRef.get())
        }
    }

    private fun assertEqualsVisibility(expected: Int, actual: Int) {
        assertTrue("Expected visibility $expected but was $actual", expected == actual)
    }
}
