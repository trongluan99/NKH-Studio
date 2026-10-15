package com.ads.nkh.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.ads.nkh.R
import com.ads.nkh.ads.NkhAd

class ResumeLoadingDialog : Dialog {

    var customLayoutId: Int = R.layout.dialog_resume_loading
        private set

    constructor(context: Context) : super(context, R.style.AppThemeDialog) {
        val globalLayout = NkhAd.getInstance().resumeLoadingDialogLayout
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_resume_loading)
    }
}
