package com.ads.nkh.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import com.ads.nkh.R
import com.ads.nkh.ads.NkhAd

class PrepareLoadingAdsDialog : Dialog {

    var customLayoutId: Int = R.layout.dialog_prepair_loading_ads
        private set

    constructor(context: Context) : super(context, R.style.AppThemeDialog) {
        val globalLayout = NkhAd.getInstance().prepareLoadingAdsDialogLayout
        if (globalLayout != -1) {
            this.customLayoutId = globalLayout
        }
    }

    constructor(context: Context, customLayoutId: Int) : super(context, R.style.AppThemeDialog) {
        this.customLayoutId = customLayoutId
    }

    fun setCustomLayout(customLayoutId: Int) {
        this.customLayoutId = customLayoutId
        if (isShowing) {
            dismiss()
            show()
        }
    }

    fun hideLoadingAdsText() {
        findViewById<View>(R.id.loading_dialog_tv)?.visibility = View.INVISIBLE
        findViewById<View>(R.id.loading_dialog_sub_tv)?.visibility = View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(customLayoutId)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
            setWindowAnimations(R.style.AdsLoadingDialogWindowAnimation)
        }
    }

    override fun onStart() {
        super.onStart()
        startSmoothAnimations()
    }

    override fun onStop() {
        stopSmoothAnimations()
        super.onStop()
    }

    private fun startSmoothAnimations() {
        stopSmoothAnimations()
        val card = findViewById<View>(R.id.card_loading_container)
        if (card != null) {
            card.alpha = 0f
            card.scaleX = 0.92f
            card.scaleY = 0.92f
            card.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun stopSmoothAnimations() {
        findViewById<View>(R.id.card_loading_container)?.animate()?.cancel()
    }
}
