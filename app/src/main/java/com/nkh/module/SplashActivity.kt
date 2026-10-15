package com.nkh.module

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ads.nkh.ads.NkhAd
import com.ads.nkh.funtion.AdCallback
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private var isNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

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

    private fun showSplashAndNavigate() {
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

    private fun navigateToMain() {
        if (isNavigated || isFinishing || isDestroyed) return
        isNavigated = true
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val TAG = "NkhSplashTest"
    }
}
