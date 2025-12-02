package com.ads.nkh.reload_ads;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.ads.nkh.ads.NkhAd;
import com.ads.nkh.ads.wrapper.ApNativeAd;
import com.ads.nkh.funtion.AdCallback;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.LoadAdError;

import java.lang.ref.WeakReference;

public class NativeManager implements LifecycleEventObserver {
    private static final String TAG = "NativeManager";
    private final WeakReference<Activity> activityRef;
    private final WeakReference<FrameLayout> frAdsRef;
    private final LifecycleOwner lifecycleOwner;
    private final String id;
    private final int layoutCustomNative;
    private final int layoutShimmerAds;
    private final AdCallback callback;
    private final boolean isReloadAds;
    private boolean isStop = false;
    private final boolean isCondition;
    private boolean isWaitingReload = false;
    private ApNativeAd nativeAd;
    private final Handler reloadHandler = new Handler(Looper.getMainLooper());
    private Runnable reloadRunnable;
    private int reloadCount = 0;
    private long timeReloadMs = 5000;
    private int maxReloadCount = 3;
    private boolean isShowClose = false;

    public NativeManager(@NonNull Activity activity, @NonNull LifecycleOwner lifecycleOwner, boolean isCondition, boolean isReloadAds, @NonNull String id, int layoutCustomNative, int layoutShimmerAds, @NonNull FrameLayout frAds, @NonNull AdCallback callback) {
        this.activityRef = new WeakReference<>(activity);
        this.frAdsRef = new WeakReference<>(frAds);
        this.lifecycleOwner = lifecycleOwner;
        this.isCondition = isCondition;
        this.isReloadAds = isReloadAds;
        this.id = id;
        this.layoutCustomNative = layoutCustomNative;
        this.layoutShimmerAds = layoutShimmerAds;
        this.callback = callback;
        this.lifecycleOwner.getLifecycle().addObserver(this);
    }

    public void setShowClose(boolean showClose) {
        isShowClose = showClose;
    }

    public void setTimeReloadMs(long timeReloadMs) {
        this.timeReloadMs = timeReloadMs;
    }

    public void setMaxReloadCount(int maxReloadCount) {
        this.maxReloadCount = maxReloadCount;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_CREATE:
                loadNativeAds();
                break;
            case ON_RESUME:
                Log.d(TAG, "onStateChanged: ON_RESUME | isStop=" + isStop + ", reload=" + isReloadAds);
                if (isStop && isReloadAds) {
                    cancelPendingReload();
                    loadNativeAds();
                }
                isStop = false;
                break;
            case ON_PAUSE:
                Log.d(TAG, "onStateChanged: ON_PAUSE");
                isStop = true;
                break;
            case ON_DESTROY:
                Log.d(TAG, "onStateChanged: ON_DESTROY → Cleanup");
                cleanup();
                break;
        }
    }

    public void loadNativeAds() {
        Activity activity = activityRef.get();
        FrameLayout frAds = frAdsRef.get();
        if (activity == null || frAds == null) return;
        if (activity.isFinishing() || activity.isDestroyed()) return;
        if (reloadCount >= maxReloadCount) {
            Log.d(TAG, "scheduleReload: Reached max reload count (" + maxReloadCount + "), stop reloading");
            cancelPendingReload();
            return;
        }
        if (isCondition) {
            ShimmerFrameLayout shimmerAds = (ShimmerFrameLayout) LayoutInflater.from(activity).inflate(layoutShimmerAds, frAds, false);
            frAds.removeAllViews();
            frAds.addView(shimmerAds);

            NkhAd.getInstance().loadNativeAd(activity, id, layoutCustomNative, frAds, shimmerAds, isShowClose, new AdCallback() {
                @Override
                public void onAdFailedToLoad(@Nullable LoadAdError i) {
                    super.onAdFailedToLoad(i);
                    if (callback != null) callback.onAdFailedToLoad(i);
                    frAds.removeAllViews();
                    reloadCount++;
                    reloadWithDelay();
                }

                @Override
                public void onNativeAdLoaded(@NonNull ApNativeAd ad) {
                    super.onNativeAdLoaded(nativeAd);
                    nativeAd = ad;
                    reloadCount++;
                    reloadWithDelay();
                }
            });
        } else {
            frAds.removeAllViews();
        }
    }

    private void cancelPendingReload() {
        if (reloadRunnable != null) {
            reloadHandler.removeCallbacks(reloadRunnable);
            reloadRunnable = null;
            isWaitingReload = false;
        }
    }

    private void reloadWithDelay() {
        Activity activity = activityRef.get();
        if (activity == null || !isReloadAds) return;
        if (isWaitingReload) return;
        isWaitingReload = true;
        if (reloadCount >= maxReloadCount) {
            Log.d(TAG, "scheduleReload: Reached max reload count (" + maxReloadCount + "), stop reloading");
            cancelPendingReload();
            return;
        }
        reloadRunnable = () -> {
            Activity a = activityRef.get();
            FrameLayout fr = frAdsRef.get();
            if (a != null && fr != null && !a.isFinishing() && !a.isDestroyed()) {
                isWaitingReload = false;
                loadNativeAds();
            } else {
                isWaitingReload = false;
            }
        };
        reloadHandler.postDelayed(reloadRunnable, timeReloadMs);
    }

    private void cleanup() {
        cancelPendingReload();
        reloadCount = 0;
        if (nativeAd != null && nativeAd.getAdmobNativeAd() != null) {
            nativeAd.getAdmobNativeAd().destroy();
        }
        nativeAd = null;
        FrameLayout frAds = frAdsRef.get();
        if (frAds != null) frAds.removeAllViews();
        lifecycleOwner.getLifecycle().removeObserver(this);
    }
}