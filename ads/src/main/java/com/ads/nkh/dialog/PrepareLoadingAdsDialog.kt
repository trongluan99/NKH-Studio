package com.ads.nkh.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
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
        val loadingText = findViewById<View>(R.id.loading_dialog_tv)
        if (loadingText != null) {
            loadingText.visibility = View.INVISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(customLayoutId)
    }
}
