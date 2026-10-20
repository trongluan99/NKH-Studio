package com.ads.nkh.application

import android.app.Application
import com.ads.nkh.config.NkhAdConfig
import com.ads.nkh.util.AppUtil
import com.ads.nkh.util.SharePreferenceUtils
import java.util.ArrayList

@Deprecated("Use AdsMultiDexApplication instead")
abstract class AdsApplication : Application() {

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
