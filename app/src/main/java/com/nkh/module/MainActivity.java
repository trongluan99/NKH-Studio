package com.nkh.module;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.ads.nkh.admob.AppOpenManager;
import com.ads.nkh.ads.NkhAd;
import com.ads.nkh.ads.wrapper.ApInterstitialAd;
import com.ads.nkh.ads.wrapper.ApInterstitialPriority2Ad;
import com.ads.nkh.ads.wrapper.ApNativeAd;
import com.ads.nkh.billing.AppPurchase;
import com.ads.nkh.funtion.AdCallback;
import com.ads.nkh.funtion.AdType;
import com.ads.nkh.funtion.PurchaseListener;
import com.ads.nkh.funtion.RewardCallback;
import com.ads.nkh.util.AppConstant;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity_Ads";
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    // Tabs & Containers
    private TabLayout tabLayout;
    private View tabBannerContainer, tabNativeContainer, tabInterstitialContainer;
    private View tabRewardContainer, tabAppOpenContainer, tabIapContainer;

    // Live Log Console
    private TextView tvLiveLog;
    private NestedScrollView scrollLiveLog;
    private TextView btnClearLog;

    // Banner Views & Buttons
    private Button btnLoadBanner, btnLoadCollapsibleBanner, btnLoadCollapsibleTop;
    private FrameLayout frBanner;

    // Native Views & Buttons
    private Button btnLoadNativeDirect, btnLoadNativeCustomCta, btnPreloadNative, btnShowPreloadNative;
    private FrameLayout frAds;
    private ShimmerFrameLayout shimmerAds;
    private ApNativeAd mPreloadedNativeAd;

    // Interstitial Views & Buttons
    private Button btnLoadInterPriority, btnShowInterPriority, btnLoadAndShowInter;
    private TextView tvInterStatus;
    private ApInterstitialPriority2Ad mInterstitialPriority2Ad;

    // Reward Views & Buttons
    private Button btnLoadReward, btnShowReward, btnLoadRewardInter, btnShowRewardInter;
    private TextView tvRewardStatus;
    private RewardedAd mRewardedAd;
    private RewardedInterstitialAd mRewardedInterstitialAd;
    private int totalRewardPoints = 0;
    private boolean isRewardEarned = false;
    private boolean isRewardInterEarned = false;

    // App Open Views & Buttons
    private Button btnTestSplashOpen, btnToggleResume;
    private TextView tvResumeStatus;
    private boolean isResumeEnabled = true;

    // IAP Views & Buttons
    private Button btnIap, btnCheckIap;
    private TextView tvIapStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupTabs();
        setupLiveConsole();

        setupBannerTab();
        setupNativeTab();
        setupInterstitialTab();
        setupRewardedTab();
        setupAppOpenTab();
        setupIapTab();

        log("System", "Ứng dụng khởi động thành công. Sẵn sàng test tất cả định dạng Ads.");
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        tabBannerContainer = findViewById(R.id.tabBannerContainer);
        tabNativeContainer = findViewById(R.id.tabNativeContainer);
        tabInterstitialContainer = findViewById(R.id.tabInterstitialContainer);
        tabRewardContainer = findViewById(R.id.tabRewardContainer);
        tabAppOpenContainer = findViewById(R.id.tabAppOpenContainer);
        tabIapContainer = findViewById(R.id.tabIapContainer);

        // Console
        tvLiveLog = findViewById(R.id.tvLiveLog);
        scrollLiveLog = findViewById(R.id.scrollLiveLog);
        btnClearLog = findViewById(R.id.btnClearLog);

        // Banner
        btnLoadBanner = findViewById(R.id.btnLoadBanner);
        btnLoadCollapsibleBanner = findViewById(R.id.btnLoadCollapsibleBanner);
        btnLoadCollapsibleTop = findViewById(R.id.btnLoadCollapsibleTop);
        frBanner = findViewById(R.id.frBanner);

        // Native
        btnLoadNativeDirect = findViewById(R.id.btnLoadNativeDirect);
        btnLoadNativeCustomCta = findViewById(R.id.btnLoadNativeCustomCta);
        btnPreloadNative = findViewById(R.id.btnPreloadNative);
        btnShowPreloadNative = findViewById(R.id.btnShowPreloadNative);
        frAds = findViewById(R.id.fr_ads);
        shimmerAds = findViewById(R.id.shimmer_native);

        // Interstitial
        btnLoadInterPriority = findViewById(R.id.btnLoadInterPriority);
        btnShowInterPriority = findViewById(R.id.btnShowInterPriority);
        btnLoadAndShowInter = findViewById(R.id.btnLoadAndShowInter);
        tvInterStatus = findViewById(R.id.tvInterStatus);

        // Reward
        btnLoadReward = findViewById(R.id.btnLoadReward);
        btnShowReward = findViewById(R.id.btnShowReward);
        btnLoadRewardInter = findViewById(R.id.btnLoadRewardInter);
        btnShowRewardInter = findViewById(R.id.btnShowRewardInter);
        tvRewardStatus = findViewById(R.id.tvRewardStatus);

        // App Open
        btnTestSplashOpen = findViewById(R.id.btnTestSplashOpen);
        btnToggleResume = findViewById(R.id.btnToggleResume);
        tvResumeStatus = findViewById(R.id.tvResumeStatus);

        // IAP
        btnIap = findViewById(R.id.btnIap);
        btnCheckIap = findViewById(R.id.btnCheckIap);
        tvIapStatus = findViewById(R.id.tvIapStatus);
    }

    // ==========================================
    // TAB NAVIGATION
    // ==========================================
    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("🏷️ Banner"));
        tabLayout.addTab(tabLayout.newTab().setText("📰 Native"));
        tabLayout.addTab(tabLayout.newTab().setText("🎬 Interstitial"));
        tabLayout.addTab(tabLayout.newTab().setText("🎁 Rewarded"));
        tabLayout.addTab(tabLayout.newTab().setText("🚀 App Open"));
        tabLayout.addTab(tabLayout.newTab().setText("💳 IAP"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switchTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        switchTab(0);
    }

    private void switchTab(int position) {
        tabBannerContainer.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        tabNativeContainer.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        tabInterstitialContainer.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
        tabRewardContainer.setVisibility(position == 3 ? View.VISIBLE : View.GONE);
        tabAppOpenContainer.setVisibility(position == 4 ? View.VISIBLE : View.GONE);
        tabIapContainer.setVisibility(position == 5 ? View.VISIBLE : View.GONE);

        String[] tabNames = {"Banner", "Native", "Interstitial", "Rewarded", "App Open", "IAP"};
        if (position >= 0 && position < tabNames.length) {
            log("Tab", "Chuyển sang Tab: " + tabNames[position]);
        }
    }

    // ==========================================
    // REAL-TIME ON-SCREEN LOG CONSOLE
    // ==========================================
    private void setupLiveConsole() {
        btnClearLog.setOnClickListener(v -> {
            tvLiveLog.setText("");
            log("System", "Log console đã được xóa.");
        });
    }

    @SuppressLint("SetTextI18n")
    private void log(String tag, String message) {
        String time = timeFormat.format(new Date());
        String logEntry = "[" + time + "] [" + tag + "] " + message;
        Log.d(TAG, logEntry);

        runOnUiThread(() -> {
            if (tvLiveLog != null) {
                tvLiveLog.append(logEntry + "\n");
                if (scrollLiveLog != null) {
                    scrollLiveLog.post(() -> scrollLiveLog.fullScroll(View.FOCUS_DOWN));
                }
            }
        });
    }

    // ==========================================
    // 1. BANNER ADS TAB
    // ==========================================
    private void setupBannerTab() {
        btnLoadBanner.setOnClickListener(v -> {
            log("Banner", "Bắt đầu tải Standard Banner (id: " + BuildConfig.ad_banner + ")");
            Toast.makeText(this, "Đang tải Standard Banner...", Toast.LENGTH_SHORT).show();

            NkhAd.getInstance().loadBanner(this, BuildConfig.ad_banner, new AdCallback() {
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    log("Banner", "✅ Standard Banner onAdLoaded thành công!");
                    Toast.makeText(MainActivity.this, "Banner Loaded", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAdFailedToLoad(@Nullable LoadAdError i) {
                    super.onAdFailedToLoad(i);
                    log("Banner", "❌ Standard Banner onAdFailedToLoad: " + (i != null ? i.getMessage() : "null"));
                }

                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    log("Banner", "👆 Standard Banner onAdClicked");
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    log("Banner", "👁️ Standard Banner onAdImpression");
                }

                @Override
                public void onAdLogRev(AdValue adValue, String adUnitId, String mediationAdapterClassName, AdType adType) {
                    super.onAdLogRev(adValue, adUnitId, mediationAdapterClassName, adType);
                    log("Banner", "💰 onAdLogRev: " + adValue.getValueMicros() + " " + adValue.getCurrencyCode() + " | adapter: " + mediationAdapterClassName);
                }
            });
        });

        btnLoadCollapsibleBanner.setOnClickListener(v -> {
            log("Banner", "Bắt đầu tải Collapsible Banner BOTTOM");
            Toast.makeText(this, "Đang tải Collapsible Banner (Bottom)...", Toast.LENGTH_SHORT).show();

            NkhAd.getInstance().loadCollapsibleBanner(this, BuildConfig.ad_banner, AppConstant.CollapsibleGravity.BOTTOM, new AdCallback() {
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    log("Banner", "✅ Collapsible Banner (Bottom) onAdLoaded thành công!");
                }

                @Override
                public void onAdFailedToLoad(@Nullable LoadAdError i) {
                    super.onAdFailedToLoad(i);
                    log("Banner", "❌ Collapsible Banner onAdFailedToLoad: " + (i != null ? i.getMessage() : "null"));
                }

                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    log("Banner", "👆 Collapsible Banner onAdClicked");
                }

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    log("Banner", "👁️ Collapsible Banner onAdImpression");
                }
            });
        });

        btnLoadCollapsibleTop.setOnClickListener(v -> {
            log("Banner", "Bắt đầu tải Collapsible Banner TOP");
            Toast.makeText(this, "Đang tải Collapsible Banner (Top)...", Toast.LENGTH_SHORT).show();

            NkhAd.getInstance().loadCollapsibleBanner(this, BuildConfig.ad_banner, AppConstant.CollapsibleGravity.TOP, new AdCallback() {
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    log("Banner", "✅ Collapsible Banner (Top) onAdLoaded thành công!");
                }

                @Override
                public void onAdFailedToLoad(@Nullable LoadAdError i) {
                    super.onAdFailedToLoad(i);
                    log("Banner", "❌ Collapsible Banner (Top) onAdFailedToLoad: " + (i != null ? i.getMessage() : "null"));
                }
            });
        });
    }

    // ==========================================
    // 2. NATIVE ADS TAB
    // ==========================================
    private void setupNativeTab() {
        btnLoadNativeDirect.setOnClickListener(v -> {
            log("Native", "Bắt đầu Load & Show Native trực tiếp");
            Toast.makeText(this, "Đang tải Native Ad trực tiếp...", Toast.LENGTH_SHORT).show();

            NkhAd.getInstance().loadNativeAd(
                    this,
                    BuildConfig.ad_native,
                    R.layout.native_large,
                    frAds,
                    shimmerAds,
                    new AdCallback() {
                        @Override
                        public void onNativeAdLoaded(@NonNull ApNativeAd nativeAd) {
                            super.onNativeAdLoaded(nativeAd);
                            log("Native", "✅ Native Direct onNativeAdLoaded & đã hiển thị vào khung!");
                        }

                        @Override
                        public void onAdFailedToLoad(@Nullable LoadAdError i) {
                            super.onAdFailedToLoad(i);
                            log("Native", "❌ Native Direct onAdFailedToLoad: " + (i != null ? i.getMessage() : "null"));
                            if (frAds != null) frAds.removeAllViews();
                        }

                        @Override
                        public void onAdImpression() {
                            super.onAdImpression();
                            log("Native", "👁️ Native Direct onAdImpression");
                        }

                        @Override
                        public void onAdClicked() {
                            super.onAdClicked();
                            log("Native", "👆 Native Direct onAdClicked");
                        }
                    }
            );
        });

        btnLoadNativeCustomCta.setOnClickListener(v -> {
            log("Native", "Bắt đầu tải Native với Custom CTA (Màu #4CAF50, cao 48dp, bo góc 16dp)");
            Toast.makeText(this, "Đang tải Native Custom CTA...", Toast.LENGTH_SHORT).show();

            NkhAd.getInstance().loadNativeAd(
                    this,
                    BuildConfig.ad_native,
                    R.layout.native_large,
                    frAds,
                    shimmerAds,
                    "#4CAF50",
                    48,
                    16,
                    new AdCallback() {
                        @Override
                        public void onNativeAdLoaded(@NonNull ApNativeAd nativeAd) {
                            super.onNativeAdLoaded(nativeAd);
                            log("Native", "✅ Native Custom CTA Loaded & hiển thị thành công!");
                        }

                        @Override
                        public void onAdFailedToLoad(@Nullable LoadAdError i) {
                            super.onAdFailedToLoad(i);
                            log("Native", "❌ Native Custom CTA onAdFailedToLoad: " + (i != null ? i.getMessage() : "null"));
                        }
                    }
            );
        });

        btnPreloadNative.setOnClickListener(v -> {
            log("Native", "Bắt đầu 1. Preload Native Ad (tải sẵn vào bộ nhớ)");
            Toast.makeText(this, "Đang Preload Native...", Toast.LENGTH_SHORT).show();

            NkhAd.getInstance().loadNativeAdResultCallback(
                    this,
                    BuildConfig.ad_native,
                    R.layout.native_large,
                    new AdCallback() {
                        @Override
                        public void onNativeAdLoaded(@NonNull ApNativeAd nativeAd) {
                            super.onNativeAdLoaded(nativeAd);
                            mPreloadedNativeAd = nativeAd;
                            log("Native", "✅ 1. Preload Native THÀNH CÔNG! Sẵn sàng hiển thị.");
                            Toast.makeText(MainActivity.this, "Preload Native Ready!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onAdFailedToLoad(@Nullable LoadAdError i) {
                            super.onAdFailedToLoad(i);
                            mPreloadedNativeAd = null;
                            log("Native", "❌ 1. Preload Native onAdFailedToLoad: " + (i != null ? i.getMessage() : "null"));
                        }
                    }
            );
        });

        btnShowPreloadNative.setOnClickListener(v -> {
            if (mPreloadedNativeAd != null) {
                log("Native", "2. Hiển thị Preloaded Native vào khung");
                NkhAd.getInstance().populateNativeAdView(this, mPreloadedNativeAd, frAds, shimmerAds);
                log("Native", "✅ 2. Preloaded Native đã được populate vào view!");
            } else {
                log("Native", "⚠️ Chưa có preloaded native ad! Vui lòng bấm '1. Preload Native' trước.");
                Toast.makeText(this, "Chưa có preloaded ad! Bấm Preload Native trước.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==========================================
    // 3. INTERSTITIAL ADS TAB
    // ==========================================
    private void setupInterstitialTab() {
        mInterstitialPriority2Ad = new ApInterstitialPriority2Ad(BuildConfig.ad_interstitial_splash, BuildConfig.ad_interstitial_splash);

        btnLoadInterPriority.setOnClickListener(v -> {
            log("Interstitial", "Bắt đầu tải Interstitial Priority 2 (High & Normal Floor)");
            tvInterStatus.setText("Trạng thái: Đang tải Priority 2...");

            NkhAd.getInstance().loadPriority2InterstitialAds(this, mInterstitialPriority2Ad, new AdCallback() {
                @Override
                public void onApInterstitialLoad(@Nullable ApInterstitialAd apInterstitialAd) {
                    super.onApInterstitialLoad(apInterstitialAd);
                    log("Interstitial", "✅ Inter Priority 2 đã tải xong và sẵn sàng!");
                    tvInterStatus.setText("Trạng thái: ĐÃ SẴN SÀNG HIỂN THỊ ✅");
                    Toast.makeText(MainActivity.this, "Inter Priority 2 Ready", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAdFailedToLoad(@Nullable LoadAdError i) {
                    super.onAdFailedToLoad(i);
                    log("Interstitial", "❌ Inter Priority 2 onAdFailedToLoad: " + (i != null ? i.getMessage() : "null"));
                    tvInterStatus.setText("Trạng thái: Tải thất bại ❌");
                }
            });
        });

        btnShowInterPriority.setOnClickListener(v -> {
            if (mInterstitialPriority2Ad.isReady()) {
                log("Interstitial", "Hiển thị Interstitial Priority 2...");
                NkhAd.getInstance().forceShowInterstitialPriority2(this, mInterstitialPriority2Ad, new AdCallback() {
                    @Override
                    public void onInterstitialShow() {
                        super.onInterstitialShow();
                        log("Interstitial", "👁️ Inter Priority 2 onInterstitialShow");
                    }

                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        log("Interstitial", "🚪 Inter Priority 2 onAdClosed (Đang tự động reload cho lần sau)");
                        tvInterStatus.setText("Trạng thái: Đã đóng - Đang auto reload...");
                    }

                    @Override
                    public void onNextAction() {
                        super.onNextAction();
                        log("Interstitial", "➡️ Inter Priority 2 onNextAction");
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable AdError adError) {
                        super.onAdFailedToShow(adError);
                        log("Interstitial", "❌ Inter Priority 2 onAdFailedToShow: " + (adError != null ? adError.getMessage() : "null"));
                    }
                }, true);
            } else {
                log("Interstitial", "⚠️ Inter Priority 2 chưa sẵn sàng. Bắt đầu tải ngay...");
                Toast.makeText(this, "Chưa sẵn sàng! Đang tải...", Toast.LENGTH_SHORT).show();
                btnLoadInterPriority.performClick();
            }
        });

        btnLoadAndShowInter.setOnClickListener(v -> {
            log("Interstitial", "Gọi loadAndShowInterstitial (1 call, timeout 15s)");
            Toast.makeText(this, "Đang tải và chuẩn bị hiển thị Interstitial...", Toast.LENGTH_SHORT).show();

            NkhAd.getInstance().loadAndShowInterstitial(this, BuildConfig.ad_interstitial_splash, 15000, false, new AdCallback() {
                @Override
                public void onInterstitialShow() {
                    super.onInterstitialShow();
                    log("Interstitial", "👁️ 1-Call Inter onInterstitialShow");
                }

                @Override
                public void onAdClosed() {
                    super.onAdClosed();
                    log("Interstitial", "🚪 1-Call Inter onAdClosed");
                }

                @Override
                public void onNextAction() {
                    super.onNextAction();
                    log("Interstitial", "➡️ 1-Call Inter onNextAction (quảng cáo đã đóng hoặc timeout)");
                }

                @Override
                public void onAdFailedToShow(@Nullable AdError adError) {
                    super.onAdFailedToShow(adError);
                    log("Interstitial", "❌ 1-Call Inter onAdFailedToShow: " + (adError != null ? adError.getMessage() : "null"));
                }
            });
        });
    }

    // ==========================================
    // 4. REWARDED ADS TAB
    // ==========================================
    private void setupRewardedTab() {
        // Rewarded Video
        btnLoadReward.setOnClickListener(v -> {
            log("Reward", "Bắt đầu tải Rewarded Video (id: " + BuildConfig.ad_reward + ")");
            Toast.makeText(this, "Đang tải Rewarded Video...", Toast.LENGTH_SHORT).show();

            NkhAd.getInstance().initRewardAds(this, BuildConfig.ad_reward, new AdCallback() {
                @Override
                public void onRewardAdLoaded(RewardedAd rewardedAd) {
                    super.onRewardAdLoaded(rewardedAd);
                    mRewardedAd = rewardedAd;
                    log("Reward", "✅ Rewarded Video onRewardAdLoaded thành công!");
                    Toast.makeText(MainActivity.this, "Rewarded Video Sẵn sàng!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAdFailedToLoad(@Nullable LoadAdError i) {
                    super.onAdFailedToLoad(i);
                    mRewardedAd = null;
                    log("Reward", "❌ Rewarded Video onAdFailedToLoad: " + (i != null ? i.getMessage() : "null"));
                }
            });
        });

        btnShowReward.setOnClickListener(v -> {
            if (mRewardedAd == null) {
                log("Reward", "⚠️ Rewarded Video chưa được tải! Hãy bấm 'Load Reward' trước.");
                Toast.makeText(this, "Chưa load Reward Video!", Toast.LENGTH_SHORT).show();
                return;
            }

            log("Reward", "Hiển thị Rewarded Video...");
            isRewardEarned = false;

            NkhAd.getInstance().showRewardAds(this, mRewardedAd, new RewardCallback() {
                @Override
                public void onUserEarnedReward(RewardItem rewardItem) {
                    isRewardEarned = true;
                    int amount = rewardItem != null ? rewardItem.getAmount() : 10;
                    totalRewardPoints += amount;
                    log("Reward", "🎁 CHÚC MỪNG! User đã nhận thưởng: +" + amount + " điểm!");
                    updateRewardPointsDisplay();
                }

                @Override
                public void onRewardedAdClosed() {
                    log("Reward", "🚪 Rewarded Video đã đóng. Trạng thái nhận thưởng: " + isRewardEarned);
                    mRewardedAd = null;
                }

                @Override
                public void onRewardedAdFailedToShow(int codeError) {
                    log("Reward", "❌ Rewarded Video onRewardedAdFailedToShow, error code: " + codeError);
                    mRewardedAd = null;
                }

                @Override
                public void onAdClicked() {
                    log("Reward", "👆 Rewarded Video onAdClicked");
                }

                @Override
                public void onAdClicked(String adUnitId, String mediationAdapterClassName, AdType adType) {
                    log("Reward", "👆 Rewarded Video onAdClicked: " + adUnitId);
                }

                @Override
                public void onAdImpression() {
                    log("Reward", "👁️ Rewarded Video onAdImpression");
                }

                @Override
                public void onAdLogRev(AdValue adValue, String adUnitId, String mediationAdapterClassName, AdType adType) {
                    log("Reward", "💰 Rewarded Video onAdLogRev: " + adValue.getValueMicros() + " " + adValue.getCurrencyCode());
                }
            });
        });

        // Rewarded Interstitial
        btnLoadRewardInter.setOnClickListener(v -> {
            log("RewardInter", "Bắt đầu tải Rewarded Interstitial (id: " + BuildConfig.ad_reward_inter + ")");
            Toast.makeText(this, "Đang tải Rewarded Interstitial...", Toast.LENGTH_SHORT).show();

            NkhAd.getInstance().getRewardInterstitial(this, BuildConfig.ad_reward_inter, new AdCallback() {
                @Override
                public void onRewardAdLoaded(RewardedInterstitialAd rewardedAd) {
                    super.onRewardAdLoaded(rewardedAd);
                    mRewardedInterstitialAd = rewardedAd;
                    log("RewardInter", "✅ Rewarded Interstitial onRewardAdLoaded thành công!");
                    Toast.makeText(MainActivity.this, "Rewarded Interstitial Sẵn sàng!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAdFailedToLoad(@Nullable LoadAdError i) {
                    super.onAdFailedToLoad(i);
                    mRewardedInterstitialAd = null;
                    log("RewardInter", "❌ Rewarded Interstitial onAdFailedToLoad: " + (i != null ? i.getMessage() : "null"));
                }
            });
        });

        btnShowRewardInter.setOnClickListener(v -> {
            if (mRewardedInterstitialAd == null) {
                log("RewardInter", "⚠️ Rewarded Interstitial chưa được tải! Hãy bấm 'Load Rewarded Inter' trước.");
                Toast.makeText(this, "Chưa load Rewarded Interstitial!", Toast.LENGTH_SHORT).show();
                return;
            }

            log("RewardInter", "Hiển thị Rewarded Interstitial...");
            isRewardInterEarned = false;

            NkhAd.getInstance().showRewardInterstitial(this, mRewardedInterstitialAd, new RewardCallback() {
                @Override
                public void onUserEarnedReward(RewardItem rewardItem) {
                    isRewardInterEarned = true;
                    int amount = rewardItem != null ? rewardItem.getAmount() : 20;
                    totalRewardPoints += amount;
                    log("RewardInter", "🎁 CHÚC MỪNG! User đã nhận thưởng từ Rewarded Inter: +" + amount + " điểm!");
                    updateRewardPointsDisplay();
                }

                @Override
                public void onRewardedAdClosed() {
                    log("RewardInter", "🚪 Rewarded Interstitial đã đóng. Nhận thưởng: " + isRewardInterEarned);
                    mRewardedInterstitialAd = null;
                }

                @Override
                public void onRewardedAdFailedToShow(int codeError) {
                    log("RewardInter", "❌ Rewarded Interstitial onRewardedAdFailedToShow, code: " + codeError);
                    mRewardedInterstitialAd = null;
                }

                @Override
                public void onAdClicked() {
                    log("RewardInter", "👆 Rewarded Interstitial onAdClicked");
                }

                @Override
                public void onAdClicked(String adUnitId, String mediationAdapterClassName, AdType adType) {
                    log("RewardInter", "👆 Rewarded Interstitial onAdClicked: " + adUnitId);
                }

                @Override
                public void onAdImpression() {
                    log("RewardInter", "👁️ Rewarded Interstitial onAdImpression");
                }

                @Override
                public void onAdLogRev(AdValue adValue, String adUnitId, String mediationAdapterClassName, AdType adType) {
                    log("RewardInter", "💰 Rewarded Interstitial onAdLogRev: " + adValue.getValueMicros() + " " + adValue.getCurrencyCode());
                }
            });
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateRewardPointsDisplay() {
        runOnUiThread(() -> {
            if (tvRewardStatus != null) {
                tvRewardStatus.setText("🪙 Phần thưởng tích luỹ: " + totalRewardPoints + " điểm ✅");
            }
        });
    }

    // ==========================================
    // 5. APP OPEN ADS TAB
    // ==========================================
    @SuppressLint("SetTextI18n")
    private void setupAppOpenTab() {
        btnTestSplashOpen.setOnClickListener(v -> {
            log("AppOpen", "Mở lại màn hình SplashActivity để kiểm thử Open Splash 2 ID cùng lúc");
            startActivity(new Intent(this, SplashActivity.class));
        });

        btnToggleResume.setOnClickListener(v -> {
            isResumeEnabled = !isResumeEnabled;
            if (isResumeEnabled) {
                AppOpenManager.getInstance().enableAppResume();
                tvResumeStatus.setText("🟢 App Resume Ads: ĐANG BẬT");
                log("AppOpen", "🟢 Đã BẬT App Resume Ads (Quảng cáo sẽ hiện khi quay lại app)");
            } else {
                AppOpenManager.getInstance().disableAppResume();
                tvResumeStatus.setText("🔴 App Resume Ads: ĐÃ TẮT");
                log("AppOpen", "🔴 Đã TẮT App Resume Ads");
            }
        });
    }

    // ==========================================
    // 6. IN-APP PURCHASE (IAP) TAB
    // ==========================================
    @SuppressLint("SetTextI18n")
    private void setupIapTab() {
        AppPurchase.getInstance().setPurchaseListener(new PurchaseListener() {
            @Override
            public void onProductPurchased(String productId, String transactionDetails) {
                log("IAP", "🎉 Mua hàng thành công! Gói: " + productId + " | Chi tiết: " + transactionDetails);
                tvIapStatus.setText("Trạng thái IAP: ĐÃ MUA THÀNH CÔNG (" + productId + ") ✅");
                Toast.makeText(MainActivity.this, "Mua hàng thành công!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void displayErrorMessage(String errorMsg) {
                log("IAP", "❌ Lỗi thanh toán Billing: " + errorMsg);
                Toast.makeText(MainActivity.this, "Billing error: " + errorMsg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUserCancelBilling() {
                log("IAP", "⚠️ Người dùng đã hủy giao dịch thanh toán.");
                Toast.makeText(MainActivity.this, "Đã hủy thanh toán", Toast.LENGTH_SHORT).show();
            }
        });

        btnIap.setOnClickListener(v -> {
            log("IAP", "Bắt đầu gọi flow thanh toán Google Play cho 'android.test.purchased'");
            AppPurchase.getInstance().purchase(this, "android.test.purchased");
        });

        btnCheckIap.setOnClickListener(v -> {
            boolean isPurchased = AppPurchase.getInstance().isPurchased(this);
            log("IAP", "Kiểm tra quyền sở hữu IAP: " + (isPurchased ? "ĐÃ MUA ✅" : "CHƯA MUA ❌"));
            tvIapStatus.setText("Trạng thái IAP: " + (isPurchased ? "Đã mua (Quảng cáo tự động ẩn) ✅" : "Chưa mua ❌"));
        });
    }
}
