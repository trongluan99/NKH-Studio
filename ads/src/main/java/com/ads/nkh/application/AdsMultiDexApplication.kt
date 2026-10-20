package com.ads.nkh.application

import androidx.multidex.MultiDexApplication
import com.ads.nkh.config.NkhAdConfig
import com.ads.nkh.util.AppUtil
import com.ads.nkh.util.SharePreferenceUtils
import java.util.ArrayList

abstract class AdsMultiDexApplication : MultiDexApplication() {

    protected lateinit var mNkhAdConfig: NkhAdConfig

    protected lateinit var listTestDevice: MutableList<String>

    override fun onCreate() {
        super.onCreate()
        listTestDevice = ArrayList()
        mNkhAdConfig = NkhAdConfig(this)
        if (SharePreferenceUtils.getInstallTime(this) == 0L) {
            SharePreferenceUtils.setInstallTime(this)
        }
        AppUtil.currentTotalRevenue001Ad = SharePreferenceUtils.getCurrentTotalRevenue001Ad(this)
    }
}
