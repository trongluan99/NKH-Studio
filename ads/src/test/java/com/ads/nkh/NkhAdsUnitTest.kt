package com.ads.nkh

import com.ads.nkh.ads.wrapper.ApInterstitialAd
import com.ads.nkh.ads.wrapper.ApInterstitialPriority2Ad
import com.ads.nkh.ads.wrapper.ApInterstitialPriorityAd
import com.ads.nkh.ads.wrapper.ApNativeAd
import com.ads.nkh.ads.wrapper.ApPipAd
import com.ads.nkh.ads.wrapper.ApRewardAd
import com.ads.nkh.ads.wrapper.ApSwipeableAd
import com.ads.nkh.ads.wrapper.StatusAd
import com.ads.nkh.billing.PurchaseResult
import com.ads.nkh.config.AdjustConfig
import com.ads.nkh.funtion.AdType
import com.ads.nkh.util.AppConstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NkhAdsUnitTest {

    @Test
    fun testApInterstitialAdInitialState() {
        val apInterstitialAd = ApInterstitialAd()
        assertEquals(StatusAd.AD_INIT, apInterstitialAd.getStatus())
        assertFalse(apInterstitialAd.isReady())
        assertTrue(apInterstitialAd.isNotReady())
    }

    @Test
    fun testApInterstitialPriority2AdInitialization() {
        val priority2 = ApInterstitialPriority2Ad("ca-app-pub-test/high", "ca-app-pub-test/normal")
        assertEquals("ca-app-pub-test/high", priority2.highPriorityId)
        assertEquals("ca-app-pub-test/normal", priority2.normalPriorityId)
        assertNotNull(priority2.highPriorityInterstitialAd)
        assertNotNull(priority2.normalPriorityInterstitialAd)
        assertFalse(priority2.highPriorityInterstitialAd!!.isReady())
        assertFalse(priority2.normalPriorityInterstitialAd!!.isReady())
    }

    @Test
    fun testApInterstitialPriority4AdInitialization() {
        val priority4 = ApInterstitialPriorityAd(
            "ca-app-pub-test/high1",
            "ca-app-pub-test/high2",
            "ca-app-pub-test/high3",
            "ca-app-pub-test/normal"
        )
        assertEquals("ca-app-pub-test/high1", priority4.high1PriorityId)
        assertEquals("ca-app-pub-test/high2", priority4.high2PriorityId)
        assertEquals("ca-app-pub-test/high3", priority4.high3PriorityId)
        assertEquals("ca-app-pub-test/normal", priority4.normalPriorityId)
        assertNotNull(priority4.high1PriorityInterstitialAd)
        assertNotNull(priority4.high2PriorityInterstitialAd)
        assertNotNull(priority4.high3PriorityInterstitialAd)
        assertNotNull(priority4.normalPriorityInterstitialAd)
    }

    @Test
    fun testApNativeAdWrapper() {
        val nativeAd = ApNativeAd(StatusAd.AD_LOADING)
        assertEquals(StatusAd.AD_LOADING, nativeAd.getStatus())
        assertTrue(nativeAd.isLoading())
        assertFalse(nativeAd.isReady())
    }

    @Test
    fun testApRewardAdWrapper() {
        val rewardAd = ApRewardAd()
        assertEquals(StatusAd.AD_INIT, rewardAd.getStatus())
        assertFalse(rewardAd.isReady())
        rewardAd.clean()
        assertFalse(rewardAd.isReady())
    }

    @Test
    fun testApPipAndSwipeableWrappers() {
        val pipAd = ApPipAd(StatusAd.AD_LOADING)
        assertEquals(StatusAd.AD_LOADING, pipAd.getStatus())
        assertTrue(pipAd.isLoading())
        assertFalse(pipAd.isReady())
        pipAd.hide()

        val swipeableAd = ApSwipeableAd(StatusAd.AD_LOADING)
        assertEquals(StatusAd.AD_LOADING, swipeableAd.getStatus())
        assertTrue(swipeableAd.isLoading())
        assertFalse(swipeableAd.isReady())
    }

    @Test
    fun testAdjustConfigAndAdTypes() {
        val adjustConfig = AdjustConfig(true, "test_adjust_token")
        assertTrue(adjustConfig.isEnableAdjust)
        assertEquals("test_adjust_token", adjustConfig.adjustToken)

        assertEquals("top", AppConstant.CollapsibleGravity.TOP)
        assertEquals("bottom", AppConstant.CollapsibleGravity.BOTTOM)
        assertEquals(8, AdType.entries.size)
        assertTrue(AdType.entries.contains(AdType.PIP))
        assertTrue(AdType.entries.contains(AdType.SWIPEABLE))
    }

    @Test
    fun testPurchaseResultModel() {
        val result = PurchaseResult(
            "GPA.1234-5678",
            "com.nkh.module",
            listOf("sub_yearly"),
            1700000000L,
            1,
            "token_xyz",
            1,
            true,
            true
        )
        assertEquals("com.nkh.module", result.getPackageName())
        assertEquals("sub_yearly", result.getProductId().first())
        assertEquals(1, result.getPurchaseState())
        assertTrue(result.isAutoRenewing())
    }
}
