package com.ads.nkh.util

import android.content.Context

object SharePreferenceUtils {
    const val PREF_NAME = "nkh_ad_pref"
    const val KEY_INSTALL_TIME = "KEY_INSTALL_TIME"
    const val KEY_CURRENT_TOTAL_REVENUE_AD = "KEY_CURRENT_TOTAL_REVENUE_AD"
    const val KEY_CURRENT_TOTAL_REVENUE_001_AD = "KEY_CURRENT_TOTAL_REVENUE_001_AD"
    const val KEY_PUSH_EVENT_REVENUE_3_DAY = "KEY_PUSH_EVENT_REVENUE_3_DAY"
    const val KEY_PUSH_EVENT_REVENUE_7_DAY = "KEY_PUSH_EVENT_REVENUE_7_DAY"
    const val KEY_LAST_IMPRESSION_INTERSTITIAL_TIME = "KEY_LAST_IMPRESSION_INTERSTITIAL_TIME"
    const val KEY_IS_ORGANIC = "KEY_IS_ORGANIC"

    @JvmStatic
    fun getInstallTime(context: Context): Long {
        val pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pre.getLong(KEY_INSTALL_TIME, 0)
    }

    @JvmStatic
    fun setInstallTime(context: Context) {
        val pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pre.edit().putLong(KEY_INSTALL_TIME, System.currentTimeMillis()).apply()
    }

    @JvmStatic
    fun getCurrentTotalRevenueAd(context: Context): Float {
        val pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pre.getFloat(KEY_CURRENT_TOTAL_REVENUE_AD, 0f)
    }

    @JvmStatic
    fun updateCurrentTotalRevenueAd(context: Context, revenue: Float) {
        val pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var currentTotalRevenue = pre.getFloat(KEY_CURRENT_TOTAL_REVENUE_AD, 0f)
        currentTotalRevenue += (revenue / 1000000.0).toFloat()
        pre.edit().putFloat(KEY_CURRENT_TOTAL_REVENUE_AD, currentTotalRevenue).apply()
    }

    @JvmStatic
    fun getCurrentTotalRevenue001Ad(context: Context): Float {
        val pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pre.getFloat(KEY_CURRENT_TOTAL_REVENUE_001_AD, 0f)
    }

    @JvmStatic
    fun updateCurrentTotalRevenue001Ad(context: Context, revenue: Float) {
        val pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pre.edit().putFloat(KEY_CURRENT_TOTAL_REVENUE_001_AD, revenue).apply()
    }

    @JvmStatic
    fun isPushRevenue3Day(context: Context): Boolean {
        val pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pre.getBoolean(KEY_PUSH_EVENT_REVENUE_3_DAY, false)
    }

    @JvmStatic
    fun setPushedRevenue3Day(context: Context) {
        val pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pre.edit().putBoolean(KEY_PUSH_EVENT_REVENUE_3_DAY, true).apply()
    }

    @JvmStatic
    fun isPushRevenue7Day(context: Context): Boolean {
        val pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pre.getBoolean(KEY_PUSH_EVENT_REVENUE_7_DAY, false)
    }

    @JvmStatic
    fun setPushedRevenue7Day(context: Context) {
        val pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pre.edit().putBoolean(KEY_PUSH_EVENT_REVENUE_7_DAY, true).apply()
    }

    @JvmStatic
    fun getLastImpressionInterstitialTime(context: Context): Long {
        val pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pre.getLong(KEY_LAST_IMPRESSION_INTERSTITIAL_TIME, 0)
    }

    @JvmStatic
    fun setLastImpressionInterstitialTime(context: Context) {
        val pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pre.edit().putLong(KEY_LAST_IMPRESSION_INTERSTITIAL_TIME, System.currentTimeMillis()).apply()
    }

    @JvmStatic
    fun getIsOrganic(context: Context?): Boolean {
        if (context == null) return true
        val pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pre.getBoolean(KEY_IS_ORGANIC, true)
    }

    @JvmStatic
    fun setIsOrganic(context: Context?, isOrganic: Boolean) {
        if (context == null) return
        val pre = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pre.edit().putBoolean(KEY_IS_ORGANIC, isOrganic).apply()
    }
}
