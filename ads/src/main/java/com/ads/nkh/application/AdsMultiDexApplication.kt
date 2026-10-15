package com.ads.nkh.application

import androidx.multidex.MultiDexApplication
import com.ads.nkh.config.NkhAdConfig
import com.ads.nkh.util.AppUtil
import com.ads.nkh.util.SharePreferenceUtils
import java.util.ArrayList

abstract class AdsMultiDexApplication : MultiDexApplication() {

    @JvmField
    protected var mNkhAdConfig: NkhAdConfig? = null

    @JvmField
    protected var listTestDevice: MutableList<String>? = null

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
