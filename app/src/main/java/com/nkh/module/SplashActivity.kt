package com.nkh.module

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.ads.nkh.ads.NkhAd
import com.ads.nkh.funtion.AdCallback
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private var isNavigated = false
    private var isSplashShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        animateSplashEntrance()

        Log.d(TAG, "onCreate: organic=" + NkhAd.getInstance().organic)

        NkhAd.getInstance().loadInterSplashPriority2SameTime(
            this,
            BuildConfig.ad_interstitial_splash,
            BuildConfig.ad_interstitial_splash,
            15000,
            2000,
            object : AdCallback() {
                override fun onAdSplashHigh1Ready() {
                    super.onAdSplashHigh1Ready()
                    Log.d(TAG, "onAdSplashHigh1Ready -> showing splash priority 2")
                    showSplashAndNavigate()
                }

                override fun onAdSplashNormalReady() {
                    super.onAdSplashNormalReady()
                    Log.d(TAG, "onAdSplashNormalReady -> showing splash priority 2")
                    showSplashAndNavigate()
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    Log.e(TAG, "Splash onAdFailedToLoad: ${i?.message}")
                }

                override fun onNextAction() {
                    super.onNextAction()
                    Log.d(TAG, "Splash load onNextAction -> navigateToMain")
                    navigateToMain()
                }
            }
        )
    }

    private fun animateSplashEntrance() {
        findViewById<View>(R.id.layoutSplashBrand)?.apply {
            alpha = 0f
            scaleX = 0.92f
            scaleY = 0.92f
            translationY = 24f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(420L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
        findViewById<View>(R.id.layoutSplashBottom)?.apply {
            alpha = 0f
            translationY = 16f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(120L)
                .setDuration(360L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun showSplashAndNavigate() {
        if (isSplashShowing || isNavigated || isFinishing || isDestroyed) return
        isSplashShowing = true
        NkhAd.getInstance().onShowSplashPriority2(
            this@SplashActivity,
            object : AdCallback() {
                override fun onAdImpression() {
                    super.onAdImpression()
                    Log.d(TAG, "Splash onAdImpression")
                }

                override fun onAdFailedToShow(adError: FullScreenContentError?) {
                    super.onAdFailedToShow(adError)
                    Log.e(TAG, "Splash onAdFailedToShow: ${adError?.message}")
                    navigateToMain()
                }

                override fun onNextAction() {
                    super.onNextAction()
                    Log.d(TAG, "Splash show onNextAction -> navigateToMain")
                    navigateToMain()
                }
            }
        )
    }

    @Suppress("DEPRECATION")
    private fun navigateToMain() {
        if (isNavigated || isFinishing || isDestroyed) return
        isNavigated = true
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    companion object {
        private const val TAG = "NkhSplashTest"
    }
}
