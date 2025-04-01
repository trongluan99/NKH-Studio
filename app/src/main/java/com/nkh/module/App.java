package com.nkh.module;

import com.ads.nkh.admob.Admob;
import com.ads.nkh.admob.AppOpenManager;
import com.ads.nkh.ads.NkhAd;
import com.ads.nkh.application.AdsMultiDexApplication;
import com.ads.nkh.billing.AppPurchase;
import com.ads.nkh.config.AdjustConfig;
import com.ads.nkh.config.NkhAdConfig;

import java.util.ArrayList;
import java.util.List;

public class App extends AdsMultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        initAds();
        initBilling();
    }

    private void initAds() {
        String environment = BuildConfig.DEBUG ? NkhAdConfig.ENVIRONMENT_DEVELOP : NkhAdConfig.ENVIRONMENT_PRODUCTION;
        mNkhAdConfig = new NkhAdConfig(this, environment);

        AdjustConfig adjustConfig = new AdjustConfig(true,getString(R.string.adjust_token));
        mNkhAdConfig.setAdjustConfig(adjustConfig);
        mNkhAdConfig.setFacebookClientToken(getString(R.string.facebook_client_token));
        mNkhAdConfig.setAdjustTokenTiktok(getString(R.string.tiktok_token));

        mNkhAdConfig.setIdAdResume(BuildConfig.ad_appopen_resume);

        NkhAd.getInstance().init(this, mNkhAdConfig);
        Admob.getInstance().setDisableAdResumeWhenClickAds(true);
        Admob.getInstance().setOpenActivityAfterShowInterAds(true);
        AppOpenManager.getInstance().disableAppResumeWithActivity(MainActivity.class);
    }

    private void initBilling(){
        List<String> listIAP = new ArrayList<>();
        listIAP.add("android.test.purchased");
        List<String> listSub = new ArrayList<>();
        AppPurchase.getInstance().initBilling(this, listIAP, listSub);
    }
}
