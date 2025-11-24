package com.ads.nkh.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import com.ads.nkh.R;
import com.ads.nkh.ads.NkhAd;

public class ResumeLoadingDialog extends Dialog {
    private int customLayoutId = R.layout.dialog_resume_loading;

    public ResumeLoadingDialog(Context context) {
        super(context, R.style.AppThemeDialog);

        int globalLayout = NkhAd.getInstance().getResumeLoadingDialogLayout();
        if (globalLayout != -1) {
            this.customLayoutId = globalLayout;
        }
    }

    public ResumeLoadingDialog(Context context, int customLayoutId) {
        super(context, R.style.AppThemeDialog);
        this.customLayoutId = customLayoutId;
    }

    public void setCustomLayout(int customLayoutId) {
        this.customLayoutId = customLayoutId;
        if (isShowing()) {
            dismiss();
            show();
        }
    }

    public int getCustomLayoutId() {
        return customLayoutId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_resume_loading);
    }
}
