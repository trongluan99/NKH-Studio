package com.ads.nkh.util

import android.content.Context
import android.content.SharedPreferences

object SharePreferenceUtils {
    const val PREF_NAME = "nkh_ad_pref"
    const val KEY_INSTALL_TIME = "KEY_INSTALL_TIME"
    const val KEY_CURRENT_TOTAL_REVENUE_AD = "KEY_CURRENT_TOTAL_REVENUE_AD"
    const val KEY_CURRENT_TOTAL_REVENUE_001_AD = "KEY_CURRENT_TOTAL_REVENUE_001_AD"
    const val KEY_PUSH_EVENT_REVENUE_3_DAY = "KEY_PUSH_EVENT_REVENUE_3_DAY"
    const val KEY_PUSH_EVENT_REVENUE_7_DAY = "KEY_PUSH_EVENT_REVENUE_7_DAY"
    const val KEY_LAST_IMPRESSION_INTERSTITIAL_TIME = "KEY_LAST_IMPRESSION_INTERSTITIAL_TIME"
    const val KEY_IS_ORGANIC = "KEY_IS_ORGANIC"

    @Volatile
    private var cachedPrefs: SharedPreferences? = null
    @Volatile
    private var cachedIsOrganic: Boolean? = null
    @Volatile
    private var cachedLastImpressionTime: Long = -1L

    private fun prefs(context: Context): SharedPreferences {
        cachedPrefs?.let { return it }
        synchronized(this) {
            cachedPrefs?.let { return it }
            val appCtx = context.applicationContext ?: context
            return appCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).also {
                cachedPrefs = it
            }
        }
    }

    @JvmStatic
    fun getInstallTime(context: Context): Long {
        return prefs(context).getLong(KEY_INSTALL_TIME, 0)
    }

    @JvmStatic
    fun setInstallTime(context: Context) {
        prefs(context).edit().putLong(KEY_INSTALL_TIME, System.currentTimeMillis()).apply()
    }

    @JvmStatic
    fun getCurrentTotalRevenueAd(context: Context): Float {
        return prefs(context).getFloat(KEY_CURRENT_TOTAL_REVENUE_AD, 0f)
    }

    @JvmStatic
    fun updateCurrentTotalRevenueAd(context: Context, revenue: Float) {
        val pre = prefs(context)
        var currentTotalRevenue = pre.getFloat(KEY_CURRENT_TOTAL_REVENUE_AD, 0f)
        currentTotalRevenue += (revenue / 1000000.0).toFloat()
        pre.edit().putFloat(KEY_CURRENT_TOTAL_REVENUE_AD, currentTotalRevenue).apply()
    }

    @JvmStatic
    fun getCurrentTotalRevenue001Ad(context: Context): Float {
        return prefs(context).getFloat(KEY_CURRENT_TOTAL_REVENUE_001_AD, 0f)
    }

    @JvmStatic
    fun updateCurrentTotalRevenue001Ad(context: Context, revenue: Float) {
        prefs(context).edit().putFloat(KEY_CURRENT_TOTAL_REVENUE_001_AD, revenue).apply()
    }

    @JvmStatic
    fun isPushRevenue3Day(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_PUSH_EVENT_REVENUE_3_DAY, false)
    }

    @JvmStatic
    fun setPushedRevenue3Day(context: Context) {
        prefs(context).edit().putBoolean(KEY_PUSH_EVENT_REVENUE_3_DAY, true).apply()
    }

    @JvmStatic
    fun isPushRevenue7Day(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_PUSH_EVENT_REVENUE_7_DAY, false)
    }

    @JvmStatic
    fun setPushedRevenue7Day(context: Context) {
        prefs(context).edit().putBoolean(KEY_PUSH_EVENT_REVENUE_7_DAY, true).apply()
    }

    @JvmStatic
    fun getLastImpressionInterstitialTime(context: Context): Long {
        val mem = cachedLastImpressionTime
        if (mem >= 0L) return mem
        val loaded = prefs(context).getLong(KEY_LAST_IMPRESSION_INTERSTITIAL_TIME, 0L)
        cachedLastImpressionTime = loaded
        return loaded
    }

    @JvmStatic
    fun setLastImpressionInterstitialTime(context: Context) {
        val now = System.currentTimeMillis()
        cachedLastImpressionTime = now
        prefs(context).edit().putLong(KEY_LAST_IMPRESSION_INTERSTITIAL_TIME, now).apply()
    }

    @JvmStatic
    fun getIsOrganic(context: Context?): Boolean {
        cachedIsOrganic?.let { return it }
        if (context == null) return true
        val loaded = prefs(context).getBoolean(KEY_IS_ORGANIC, true)
        cachedIsOrganic = loaded
        return loaded
    }

    @JvmStatic
    fun setIsOrganic(context: Context?, isOrganic: Boolean) {
        cachedIsOrganic = isOrganic
        if (context == null) return
        prefs(context).edit().putBoolean(KEY_IS_ORGANIC, isOrganic).apply()
    }
}
