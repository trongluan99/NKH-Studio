package com.ads.nkh.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.ads.nkh.R;
import com.ads.nkh.ads.NkhAd;

public class PrepareLoadingAdsDialog extends Dialog {

    private int customLayoutId = R.layout.dialog_prepair_loading_ads; // Default layout

    public PrepareLoadingAdsDialog(Context context) {
        super(context, R.style.AppThemeDialog);
        int globalLayout = NkhAd.getInstance().getPrepareLoadingAdsDialogLayout();
        if (globalLayout != -1) {
            this.customLayoutId = globalLayout;
        }
    }

    public PrepareLoadingAdsDialog(Context context, int customLayoutId) {
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

    public void hideLoadingAdsText() {
        View loadingText = findViewById(R.id.loading_dialog_tv);
        if (loadingText != null) {
            loadingText.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(customLayoutId);
    }

}
