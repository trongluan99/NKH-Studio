package com.nkh.module;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;
import android.view.View;

import java.util.Arrays;
import java.util.List;

import com.ads.nkh.admob.AppOpenManager;
import com.ads.nkh.ads.NkhAd;
import com.ads.nkh.funtion.AdCallback;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.LoadAdError;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private TextView tvSplashStatus;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initViews();

        Log.d(TAG, "onCreate: " + NkhAd.getInstance().getOrganic());

        // Cách 1: Open App Splash tuần tự (Sequential Waterfall từ danh sách ID):
        loadOpenAppSplashSequentially();

        // Cách 2: Open App Splash 2 ID cùng lúc (Song song High & Normal):
        // loadOpenAppSplash();

        // Cách 3: Interstitial Splash tuần tự (Sequential Waterfall từ danh sách ID):
        // loadInterSplashSequentially();

        // Cách 4: Interstitial Splash 2 ID cùng lúc (Song song High & Normal):
        // loadInterSplash();
    }

    private void initViews() {
        tvSplashStatus = findViewById(R.id.tvSplashStatus);

        View cardLogo = findViewById(R.id.cardSplashLogo);
        View tvTitle = findViewById(R.id.tvSplashTitle);
        View tvSubtitle = findViewById(R.id.tvSplashSubtitle);
        View tvBadge = findViewById(R.id.tvSplashBadge);

        if (cardLogo != null) {
            cardLogo.setAlpha(0f);
            cardLogo.setScaleX(0.75f);
            cardLogo.setScaleY(0.75f);
            cardLogo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600).start();
        }
        if (tvTitle != null) {
            tvTitle.setAlpha(0f);
            tvTitle.setTranslationY(20f);
            tvTitle.animate().alpha(1f).translationY(0f).setStartDelay(200).setDuration(500).start();
        }
        if (tvSubtitle != null) {
            tvSubtitle.setAlpha(0f);
            tvSubtitle.animate().alpha(1f).setStartDelay(350).setDuration(500).start();
        }
        if (tvBadge != null) {
            tvBadge.setAlpha(0f);
            tvBadge.animate().alpha(1f).setStartDelay(450).setDuration(500).start();
        }
    }

    private void updateStatus(String status) {
        runOnUiThread(() -> {
            if (tvSplashStatus != null) {
                tvSplashStatus.setText(status);
            }
        });
    }

    /**
     * Ví dụ 1: Load Open App Splash 2 ID cùng lúc (High và Normal)
     */
    private void loadOpenAppSplash() {
        updateStatus("Đang kiểm tra và tải quảng cáo Splash...");
        AppOpenManager.getInstance().loadAndShowOpenSplash2SameTime(
                this,
                BuildConfig.ads_open_app,
                BuildConfig.ads_open_app,
                25000,
                new AdCallback() {
                    @Override
                    public void onAdSplashHigh1Ready() {
                        super.onAdSplashHigh1Ready();
                        Log.d(TAG, "onAdSplashHigh1Ready: Hiển thị ad High");
                        updateStatus("Quảng cáo High Priority sẵn sàng...");
                        showOpenSplash();
                    }

                    @Override
                    public void onAdSplashNormalReady() {
                        super.onAdSplashNormalReady();
                        Log.d(TAG, "onAdSplashNormalReady: Hiển thị ad Normal (khi High fail)");
                        updateStatus("Quảng cáo Normal Priority sẵn sàng...");
                        showOpenSplash();
                    }

                    @Override
                    public void onAdPriorityFailedToLoad(@Nullable AdError i) {
                        super.onAdPriorityFailedToLoad(i);
                        Log.e(TAG, "onAdPriorityFailedToLoad: " + (i != null ? i.getMessage() : "null"));
                        updateStatus("Đang tối ưu quảng cáo...");
                    }

                    @Override
                    public void onNextAction() {
                        super.onNextAction();
                        Log.d(TAG, "onNextAction: Cả 2 ad cùng fail hoặc timeout");
                        updateStatus("Đang mở trang chính...");
                        gotoMainActivity();
                    }
                }
        );
    }

    private void showOpenSplash() {
        AppOpenManager.getInstance().showOpenSplash2SameTime(SplashActivity.this, new AdCallback() {
            @Override
            public void onNextAction() {
                super.onNextAction();
                gotoMainActivity();
            }

            @Override
            public void onAdFailedToShow(@Nullable AdError adError) {
                super.onAdFailedToShow(adError);
                gotoMainActivity();
            }
        });
    }

    /**
     * Ví dụ 1b: Load Open App Splash tuần tự (Sequential) từ 1 danh sách ID
     */
    private void loadOpenAppSplashSequentially() {
        updateStatus("Đang kiểm tra và tải Open App Splash tuần tự...");
        List<String> listIdOpen = Arrays.asList(
                BuildConfig.ads_open_app, // ID High
                BuildConfig.ads_open_app  // ID Normal
        );

        AppOpenManager.getInstance().loadAndShowOpenSplashSequentially(
                this,
                listIdOpen,
                25000,
                new AdCallback() {
                    @Override
                    public void onAdSplashReady() {
                        super.onAdSplashReady();
                        updateStatus("Open App Splash sẵn sàng, đang hiển thị...");
                        AppOpenManager.getInstance().showOpenSplashSequentially(SplashActivity.this, new AdCallback() {
                            @Override
                            public void onNextAction() {
                                super.onNextAction();
                                gotoMainActivity();
                            }

                            @Override
                            public void onAdClosed() {
                                super.onAdClosed();
                                gotoMainActivity();
                            }

                            @Override
                            public void onAdFailedToShow(@Nullable AdError adError) {
                                super.onAdFailedToShow(adError);
                                gotoMainActivity();
                            }
                        });
                    }

                    @Override
                    public void onAdPriorityFailedToLoad(@Nullable AdError adError) {
                        super.onAdPriorityFailedToLoad(adError);
                        updateStatus("1 ID Open App tải thất bại, đang thử ID kế tiếp...");
                    }

                    @Override
                    public void onNextAction() {
                        super.onNextAction();
                        updateStatus("Tất cả ID thất bại hoặc timeout, mở trang chính...");
                        gotoMainActivity();
                    }
                }
        );
    }

    /**
     * Ví dụ 2: Load Interstitial Splash 2 ID cùng lúc (High và Normal)
     */
    private void loadInterSplash() {
        updateStatus("Đang tải Interstitial Splash...");
        NkhAd.getInstance().loadInterSplashPriority4SameTime(
                this,
                BuildConfig.ad_interstitial_splash,
                BuildConfig.ad_interstitial_splash,
                25000,
                5000,
                new AdCallback() {
                    @Override
                    public void onAdSplashHigh1Ready() {
                        super.onAdSplashHigh1Ready();
                        updateStatus("Interstitial High sẵn sàng...");
                        NkhAd.getInstance().onShowSplashPriority4(SplashActivity.this, new AdCallback() {
                            @Override
                            public void onNextAction() {
                                super.onNextAction();
                                gotoMainActivity();
                            }
                        });
                    }

                    @Override
                    public void onAdSplashNormalReady() {
                        super.onAdSplashNormalReady();
                        updateStatus("Interstitial Normal sẵn sàng...");
                        NkhAd.getInstance().onShowSplashPriority4(SplashActivity.this, new AdCallback() {
                            @Override
                            public void onNextAction() {
                                super.onNextAction();
                                gotoMainActivity();
                            }
                        });
                    }

                    @Override
                    public void onNextAction() {
                        super.onNextAction();
                        updateStatus("Đang mở trang chính...");
                        gotoMainActivity();
                    }
                }
        );
    }

    /**
     * Ví dụ 3: Load Interstitial Splash tuần tự (Sequential) từ 1 danh sách ID
     * Load theo thứ tự từ trên xuống dưới, không load song song.
     */
    private void loadInterSplashSequentially() {
        updateStatus("Đang tải Interstitial Splash tuần tự...");
        List<String> listIdSplash = Arrays.asList(
                BuildConfig.ad_interstitial_splash, // ID High
                BuildConfig.ad_interstitial_splash  // ID Normal
        );

        NkhAd.getInstance().loadInterSplashPrioritySequentially(
                this,
                listIdSplash,
                25000,
                3000,
                new AdCallback() {
                    @Override
                    public void onAdSplashReady() {
                        super.onAdSplashReady();
                        updateStatus("Interstitial Splash sẵn sàng, đang hiển thị...");
                        NkhAd.getInstance().onShowSplashPrioritySequentially(SplashActivity.this, new AdCallback() {
                            @Override
                            public void onNextAction() {
                                super.onNextAction();
                                gotoMainActivity();
                            }

                            /*@Override
                            public void onAdClosed() {
                                super.onAdClosed();
                                gotoMainActivity();
                            }

                            @Override
                            public void onAdFailedToShow(@Nullable AdError adError) {
                                super.onAdFailedToShow(adError);
                                gotoMainActivity();
                            }*/
                        });
                    }

                    @Override
                    public void onAdPriorityFailedToLoad(@Nullable AdError adError) {
                        super.onAdPriorityFailedToLoad(adError);
                        updateStatus("1 ID quảng cáo tải thất bại, đang thử ID kế tiếp...");
                    }

                    @Override
                    public void onNextAction() {
                        super.onNextAction();
                        updateStatus("Tất cả ID thất bại hoặc timeout, mở trang chính...");
                        gotoMainActivity();
                    }
                }
        );
    }

    private boolean isNavigated = false;

    private void gotoMainActivity() {
        if (isNavigated) {
            return;
        }
        isNavigated = true;
        runOnUiThread(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        });
    }
}
