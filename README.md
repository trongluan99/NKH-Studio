# NKH Studio - Ads & Monetization SDK

Thư viện Android hỗ trợ tích hợp quảng cáo **Google AdMob**, **Mediation** (Facebook, AppLovin, Pangle, Mintegral), **Adjust**, **Firebase tROAS** và **In-App Purchase** (Billing) toàn diện, tối ưu doanh thu với cơ chế **Dual-Floor Priority (High/Normal)** và **Waterfall Sequentially**.

---

## 📑 Mục lục
1. [Cài đặt & Dependencies](#1-cài-đặt--dependencies)
2. [Cấu hình AndroidManifest.xml](#2-cấu-hình-androidmanifestxml)
3. [Khởi tạo Application (App.java)](#3-khởi-tạo-application-appjava)
4. [Màn hình Splash (Splash Ads)](#4-màn-hình-splash-splash-ads)
   - [Cách 1: App Open Splash tuần tự (Sequential)](#cách-1-app-open-splash-tuần-tự-sequential---khuyên-dùng)
   - [Cách 2: App Open Splash 2 ID song song (Same Time)](#cách-2-app-open-splash-2-id-song-song-same-time)
   - [Cách 3: Interstitial Splash tuần tự (Sequential)](#cách-3-interstitial-splash-tuần-tự-sequential---khuyên-dùng)
   - [Cách 4: Interstitial Splash 2 ID song song (Same Time)](#cách-4-interstitial-splash-2-id-song-song-same-time)
5. [Banner Ads & Collapsible Banner](#5-banner-ads--collapsible-banner)
6. [Native Ads (Shimmer & Priority)](#6-native-ads-shimmer--priority)
7. [Interstitial Ads](#7-interstitial-ads)
8. [Rewarded & Rewarded Interstitial Ads](#8-rewarded--rewarded-interstitial-ads)
9. [In-App Purchase (IAP) & Billing](#9-in-app-purchase-iap--billing)
10. [Quản lý App Resume (Open Ads)](#10-quản-lý-app-resume-open-ads)

---

## 1. Cài đặt & Dependencies

### `settings.gradle` hoặc `build.gradle (Project)`:
```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url "https://jitpack.io" }
        maven { url 'https://artifact.bytedance.com/repository/pangle/' }
        maven { url 'https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea' }
    }
}
```

### `build.gradle (:app)`:
```groovy
android {
    compileSdkVersion 36

    defaultConfig {
        minSdkVersion 24
        targetSdkVersion 36
        multiDexEnabled true
    }

    buildFeatures {
        buildConfig = true
    }

    flavorDimensions "default"
    productFlavors {
        appDev {
            manifestPlaceholders = [ad_app_id: "ca-app-pub-3940256099942544~3347511713"]
            buildConfigField "String", "ads_open_app", "\"ca-app-pub-3940256099942544/9257395921\""
            buildConfigField "String", "ad_interstitial_splash", "\"ca-app-pub-3940256099942544/1033173712\""
            buildConfigField "String", "ad_appopen_resume_high", "\"ca-app-pub-3940256099942544/9257395921\""
            buildConfigField "String", "ad_appopen_resume", "\"ca-app-pub-3940256099942544/9257395921\""
            buildConfigField "String", "ad_banner", "\"ca-app-pub-3940256099942544/9214589741\""
            buildConfigField "String", "ad_native", "\"ca-app-pub-3940256099942544/2247696110\""
            buildConfigField "String", "ad_reward", "\"ca-app-pub-3940256099942544/5224354917\""
            buildConfigField "String", "ad_reward_inter", "\"ca-app-pub-3940256099942544/5354046379\""
            buildConfigField "Boolean", "env_dev", "true"
        }
        appProd {
            manifestPlaceholders = [ad_app_id: "ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy"]
            // Thêm các ID production thật ở đây
            buildConfigField "Boolean", "env_dev", "false"
        }
    }
}

dependencies {
    implementation project(':ads') // Hoặc implementation 'com.github.trongluan99:NKH-Studio:$version'
    implementation 'com.google.android.gms:play-services-ads:25.3.0'
    implementation 'com.facebook.shimmer:shimmer:0.5.0'
    implementation 'androidx.multidex:multidex:2.0.1'
}
```

---

## 2. Cấu hình AndroidManifest.xml

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:name=".App"
        android:theme="@style/Theme.AndModuleAds">

        <!-- AdMob App ID -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="${ad_app_id}" />

        <!-- Facebook Client Token (Tùy chọn nếu dùng Facebook Mediation) -->
        <meta-data
            android:name="com.facebook.sdk.ApplicationId"
            android:value="@string/facebook_app_id" />
        <meta-data
            android:name="com.facebook.sdk.ClientToken"
            android:value="@string/facebook_client_token" />

    </application>
</manifest>
```

---

## 3. Khởi tạo Application (`App.java`)

```java
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

        // 1. Cấu hình Adjust
        AdjustConfig adjustConfig = new AdjustConfig(true, getString(R.string.adjust_token));
        mNkhAdConfig.setAdjustConfig(adjustConfig);
        mNkhAdConfig.setFacebookClientToken(getString(R.string.facebook_client_token));
        mNkhAdConfig.setAdjustTokenTiktok(getString(R.string.tiktok_token));

        // 2. Cấu hình App Open Resume với 2 tầng ID (High Floor & Normal Floor)
        mNkhAdConfig.setIdAdResume(BuildConfig.ad_appopen_resume_high, BuildConfig.ad_appopen_resume);
        mNkhAdConfig.setShowAdDebugDialog(BuildConfig.DEBUG);

        // 3. Khởi tạo SDK
        NkhAd.getInstance().init(this, mNkhAdConfig);
        Admob.getInstance().setDisableAdResumeWhenClickAds(true);
        Admob.getInstance().setOpenActivityAfterShowInterAds(true);

        // Không hiển thị App Resume khi ở màn hình Splash
        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity.class);
    }

    private void initBilling() {
        List<String> listIAP = new ArrayList<>();
        listIAP.add("android.test.purchased");
        List<String> listSub = new ArrayList<>();
        AppPurchase.getInstance().initBilling(this, listIAP, listSub);
    }
}
```

---

## 4. Màn hình Splash (Splash Ads)

Tại `SplashActivity`, hỗ trợ cả 2 dạng quảng cáo mở đầu (**App Open Splash** & **Interstitial Splash**) theo 2 cơ chế: **Tuần tự (Sequentially)** hoặc **Song song (2 Same Time)**.

### Cách 1: App Open Splash tuần tự (Sequential - Khuyên dùng)
Load ID High trước, nếu High fail thì tự động load tiếp ID Normal. Không request song song:
```java
List<String> listIdOpen = Arrays.asList(
        BuildConfig.ads_open_app, // ID High
        BuildConfig.ads_open_app  // ID Normal
);

AppOpenManager.getInstance().loadAndShowOpenSplashSequentially(
        this,
        listIdOpen,
        25000, // Timeout (ms)
        new AdCallback() {
            @Override
            public void onAdSplashReady() {
                super.onAdSplashReady();
                // Quảng cáo đã sẵn sàng -> Hiển thị
                AppOpenManager.getInstance().showOpenSplashSequentially(SplashActivity.this, new AdCallback() {
                    @Override
                    public void onNextAction() {
                        super.onNextAction();
                        gotoMainActivity();
                    }
                });
            }

            @Override
            public void onAdPriorityFailedToLoad(@Nullable AdError adError) {
                super.onAdPriorityFailedToLoad(adError);
                // 1 ID bị lỗi, SDK tự động nhảy sang ID kế tiếp
            }

            @Override
            public void onNextAction() {
                super.onNextAction();
                // Tất cả ID đều lỗi hoặc timeout -> Next màn
                gotoMainActivity();
            }
        }
);
```

### Cách 2: App Open Splash 2 ID song song (Same Time)
Yêu cầu cả 2 ID cùng lúc để tối ưu thời gian chờ:
```java
AppOpenManager.getInstance().loadAndShowOpenSplash2SameTime(
        this,
        BuildConfig.ads_open_app, // ID High
        BuildConfig.ads_open_app, // ID Normal
        25000,
        new AdCallback() {
            @Override
            public void onAdSplashHigh1Ready() {
                super.onAdSplashHigh1Ready();
                showOpenSplash();
            }

            @Override
            public void onAdSplashNormalReady() {
                super.onAdSplashNormalReady();
                showOpenSplash();
            }

            @Override
            public void onNextAction() {
                super.onNextAction();
                gotoMainActivity();
            }
        }
);

private void showOpenSplash() {
    AppOpenManager.getInstance().showOpenSplash2SameTime(SplashActivity.this, new AdCallback() {
        @Override
        public void onNextAction() {
            super.onNextAction();
            gotoMainActivity();
        }
    });
}
```

### Cách 3: Interstitial Splash tuần tự (Sequential - Khuyên dùng)
```java
List<String> listIdSplash = Arrays.asList(
        BuildConfig.ad_interstitial_splash, // ID High
        BuildConfig.ad_interstitial_splash  // ID Normal
);

NkhAd.getInstance().loadInterSplashPrioritySequentially(
        this,
        listIdSplash,
        25000, // Timeout
        3000,  // Delay tối thiểu trước khi hiển thị (ms)
        new AdCallback() {
            @Override
            public void onAdSplashReady() {
                super.onAdSplashReady();
                NkhAd.getInstance().onShowSplashPrioritySequentially(SplashActivity.this, new AdCallback() {
                    @Override
                    public void onNextAction() {
                        super.onNextAction();
                        gotoMainActivity();
                    }

                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        // Được gọi khi user bấm nút đóng (X) quảng cáo
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable AdError adError) {
                        super.onAdFailedToShow(adError);
                        // Được gọi khi show quảng cáo bị lỗi
                    }
                });
            }

            @Override
            public void onAdPriorityFailedToLoad(@Nullable AdError adError) {
                super.onAdPriorityFailedToLoad(adError);
                // Tự động chuyển ID kế tiếp trong danh sách
            }

            @Override
            public void onNextAction() {
                super.onNextAction();
                // Tất cả ID đều thất bại hoặc timeout -> Chuyển màn hình
                gotoMainActivity();
            }
        }
);
```

> **Ý nghĩa các Callback quan trọng trong Splash:**
> - `onNextAction()`: Được SDK tự động gọi trong mọi kịch bản kết thúc (khi tắt ad, lỗi load, timeout, hoặc show thất bại). **Đặt logic `gotoMainActivity()` tại đây là an toàn và chuẩn nhất.**
> - `onAdClosed()`: Gọi riêng biệt khi user click nút đóng quảng cáo `(X)`.
> - `onAdFailedToShow(AdError)`: Gọi khi quảng cáo đã load thành công nhưng không thể render lên màn hình.


### Cách 4: Interstitial Splash 2 ID song song (Same Time)
```java
NkhAd.getInstance().loadInterSplashPriority4SameTime(
        this,
        BuildConfig.ad_interstitial_splash, // ID High
        BuildConfig.ad_interstitial_splash, // ID Normal
        25000,
        3000,
        new AdCallback() {
            @Override
            public void onAdSplashHigh1Ready() {
                super.onAdSplashHigh1Ready();
                showInterSplash();
            }

            @Override
            public void onAdSplashNormalReady() {
                super.onAdSplashNormalReady();
                showInterSplash();
            }

            @Override
            public void onNextAction() {
                super.onNextAction();
                gotoMainActivity();
            }
        }
);

private void showInterSplash() {
    NkhAd.getInstance().onShowSplashPriority4(SplashActivity.this, new AdCallback() {
        @Override
        public void onNextAction() {
            super.onNextAction();
            gotoMainActivity();
        }
    });
}
```

---

## 5. Banner Ads & Collapsible Banner

### Standard Banner:
```java
NkhAd.getInstance().loadBanner(this, BuildConfig.ad_banner);
```

### Collapsible Banner (Thu gọn / mở rộng):
```java
// Gravity: BOTTOM hoặc TOP
NkhAd.getInstance().loadCollapsibleBanner(
        this,
        BuildConfig.ad_banner,
        AppConstant.CollapsibleGravity.BOTTOM,
        new AdCallback() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
            }
        }
);
```

---

## 6. Native Ads (Shimmer & Priority)

### 1. Load và Show trực tiếp (Kèm hiệu ứng Shimmer Loading):
```java
FrameLayout frAds = findViewById(R.id.frAds);
ShimmerFrameLayout shimmerAds = findViewById(R.id.shimmerAds);

NkhAd.getInstance().loadNativeAd(
        this,
        BuildConfig.ad_native,
        R.layout.native_large, // Layout xml tùy chỉnh Native
        frAds,
        shimmerAds,
        new AdCallback() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                frAds.removeAllViews();
            }
        }
);
```

### 2. Pre-load Native Ad trước, hiển thị sau:
```java
private ApNativeAd mApNativeAd;

// Bước 1: Pre-load
NkhAd.getInstance().loadNativeAdResultCallback(
        this,
        BuildConfig.ad_native,
        R.layout.native_large,
        new AdCallback() {
            @Override
            public void onNativeAdLoaded(@NonNull ApNativeAd nativeAd) {
                super.onNativeAdLoaded(nativeAd);
                mApNativeAd = nativeAd;
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                mApNativeAd = null;
            }
        }
);

// Bước 2: Hiển thị khi cần
if (mApNativeAd != null) {
    NkhAd.getInstance().populateNativeAdView(this, mApNativeAd, frAds, shimmerAds);
}
```

### 3. Native 4 ID Song song (Priority Floor):
```java
NkhAd.getInstance().loadNative4SameTime(
        this,
        idHigh1, idHigh2, idHigh3, idNormal,
        R.layout.native_large,
        new AdCallback() {
            @Override
            public void onNativeAdLoaded(@NonNull ApNativeAd nativeAd) {
                super.onNativeAdLoaded(nativeAd);
                NkhAd.getInstance().populateNativeAdView(MainActivity.this, nativeAd, frAds, shimmerAds);
            }
        }
);
```

---

## 7. Interstitial Ads

### Load và Show trực tiếp:
```java
NkhAd.getInstance().loadAndShowInterstitial(
        this,
        BuildConfig.ad_interstitial_splash,
        25000,
        new AdCallback() {
            @Override
            public void onNextAction() {
                super.onNextAction();
                // Thực hiện hành động sau khi đóng hoặc fail
            }
        }
);
```

### Pre-load trước và Force Show:
```java
private ApInterstitialAd mInterstitialAd;

// Preload
mInterstitialAd = NkhAd.getInstance().getInterstitialAds(this, BuildConfig.ad_interstitial_splash, new AdCallback() {
    @Override
    public void onApInterstitialLoad(@Nullable ApInterstitialAd apInterstitialAd) {
        super.onApInterstitialLoad(apInterstitialAd);
        mInterstitialAd = apInterstitialAd;
    }
});

// Show khi cần
if (mInterstitialAd != null && mInterstitialAd.isReady()) {
    NkhAd.getInstance().forceShowInterstitial(this, mInterstitialAd, new AdCallback() {
        @Override
        public void onNextAction() {
            super.onNextAction();
        }
    });
}
```

### Pre-load Interstitial Priority 2 (High Floor & Normal Floor):
Cơ chế tự động thử load ID High trước, nếu High fail thì tự động chuyển sang ID Normal:
```java
ApInterstitialPriority2Ad mInterPriority2 = new ApInterstitialPriority2Ad(
        BuildConfig.ad_interstitial_splash, // ID High Floor
        BuildConfig.ad_interstitial_splash  // ID Normal Floor
);

// 1. Tải trước quảng cáo
NkhAd.getInstance().loadPriority2InterstitialAds(this, mInterPriority2, new AdCallback() {
    @Override
    public void onApInterstitialLoad(@Nullable ApInterstitialAd apInterstitialAd) {
        super.onApInterstitialLoad(apInterstitialAd);
        // Đã sẵn sàng hiển thị
    }

    @Override
    public void onAdFailedToLoad(@Nullable LoadAdError i) {
        super.onAdFailedToLoad(i);
        // Cả 2 ID đều load thất bại
    }
});

// 2. Hiển thị khi người dùng tương tác (tự động reload sau khi đóng)
if (mInterPriority2.isReady()) {
    NkhAd.getInstance().forceShowInterstitialPriority2(this, mInterPriority2, new AdCallback() {
        @Override
        public void onAdClosed() {
            super.onAdClosed();
            // Đã đóng quảng cáo (SDK sẽ tự động tải lại cho lần hiển thị kế tiếp)
        }

        @Override
        public void onNextAction() {
            super.onNextAction();
            // Tiếp tục luồng màn hình của ứng dụng
        }
    });
}
```

---

## 8. Rewarded & Rewarded Interstitial Ads

### Rewarded Ad:
```java
private RewardedAd mRewardedAd;

// 1. Khởi tạo
NkhAd.getInstance().initRewardAds(this, BuildConfig.ad_reward, new AdCallback() {
    @Override
    public void onRewardAdLoaded(RewardedAd rewardedAd) {
        super.onRewardAdLoaded(rewardedAd);
        mRewardedAd = rewardedAd;
    }
});

// 2. Hiển thị
if (mRewardedAd != null) {
    NkhAd.getInstance().showRewardAds(this, mRewardedAd, new RewardCallback() {
        @Override
        public void onUserEarnedReward(RewardItem rewardItem) {
            // Người dùng đã xem xong và nhận thưởng
        }

        @Override
        public void onRewardedAdClosed() {
            // Khi đóng ad
        }

        @Override
        public void onRewardedAdFailedToShow(int errorCode) {
        }
    });
}
```

### Rewarded Interstitial Ad:
```java
private RewardedInterstitialAd mRewardInterAd;

// 1. Khởi tạo
NkhAd.getInstance().initRewardInterAds(this, BuildConfig.ad_reward_inter, new AdCallback() {
    @Override
    public void onRewardAdLoaded(RewardedInterstitialAd rewardedInterstitialAd) {
        super.onRewardAdLoaded(rewardedInterstitialAd);
        mRewardInterAd = rewardedInterstitialAd;
    }
});

// 2. Hiển thị
if (mRewardInterAd != null) {
    NkhAd.getInstance().showRewardInterAds(this, mRewardInterAd, new RewardCallback() {
        @Override
        public void onUserEarnedReward(RewardItem rewardItem) {
        }

        @Override
        public void onRewardedAdClosed() {
        }
    });
}
```

---

## 9. In-App Purchase (IAP) & Billing

```java
// Đăng ký lắng nghe sự kiện mua hàng
AppPurchase.getInstance().setPurchaseListener(new PurchaseListener() {
    @Override
    public void onProductPurchased(String productId, String transactionDetails) {
        Toast.makeText(context, "Mua thành công gói: " + productId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void displayErrorMessage(String errorMsg) {
        Toast.makeText(context, "Lỗi thanh toán: " + errorMsg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUserCancelBilling() {
        Toast.makeText(context, "Hủy thanh toán", Toast.LENGTH_SHORT).show();
    }
});

// Mua In-app (Một lần)
AppPurchase.getInstance().purchase(activity, "android.test.purchased");

// Mua Subscription (Gói gia hạn)
AppPurchase.getInstance().subscribe(activity, "premium_monthly");

// Kiểm tra user đã mua gói xóa quảng cáo chưa:
boolean isPurchased = AppPurchase.getInstance().isPurchased(context);
```

---

## 10. Quản lý App Resume (Open Ads)

- **Bỏ qua App Resume ở Activity chỉ định (e.g. Splash, Login):**
  ```java
  AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity.class);
  ```

- **Bật / tắt tạm thời cơ chế Resume:**
  ```java
  AppOpenManager.getInstance().disableAppResume(); // Tạm tắt
  AppOpenManager.getInstance().enableAppResume();  // Bật lại
  ```

---

## License
Copyright © 2026 NKH Studio. All rights reserved.
