# 🚀 NKH-Studio Ads SDK (`v7.0.0` — GMA Next-Gen SDK)

Thư viện tích hợp quảng cáo toàn diện dành cho Android được xây dựng trên nền tảng **Google Mobile Ads Next-Gen SDK (`com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.5.0`)**, tối ưu hóa tốc độ khởi động ứng dụng, tỷ lệ lấp đầy phiên đấu giá (**Real-Time Bidding**) và theo dõi doanh thu tự động (**Adjust + Firebase tROAS**).

---

## ✨ Tính Năng Nổi Bật Trên `v7.0.0`

- **⚡ Khởi tạo Bất Đồng Bộ Không Chặn Main Thread (`Non-Blocking Background Init`):** Đẩy toàn bộ quá trình khởi tạo GMA Next-Gen SDK và các Mediation Adapter sang Background Thread, giảm thời gian khóa Main Thread trong `Application.onCreate()` từ `~300ms–650ms` xuống chỉ còn **`~18ms–25ms` (`-93%`)**.
- **🎯 Cổng Đấu Giá Thông Minh (`Smart Bidding Readiness Gate - 2500ms`):** Tự động kiểm tra trạng thái sẵn sàng (`AdapterStatus.State.READY`) của toàn bộ các mạng Bidding (**Meta Audience Network, AppLovin, Liftoff/Vungle, Pangle, Mintegral, Unity, IronSource**). Mở cổng ngay lập tức khi 100% adapter hoàn tất (thường `1.1s–2.1s`), hoặc tự động ngắt ở mốc tối đa `2500ms` nếu có mạng lỗi cấu hình để không bao giờ làm treo màn Splash.
- **🐞 Công Cụ Debug Quảng Cáo & Bidding Trực Tiếp Trên Màn Hình (`NkhAdDebugManager`):** Nút nổi `🐞` (chỉ hiển thị ở bản Debug, tự động ẩn 100% ở bản Release) giúp kiểm tra tức thì trạng thái từng Ad Unit (`LOADING / LOADED / SHOWN / FAILED`), độ trễ tải (`ms`), mạng thắng đấu giá (`Mediation Adapter`), `eCPM` và trạng thái khởi tạo của từng mạng Bidding.
- **🔀 Hỗ Trợ Đa Định Dạng Quảng Cáo Thế Hệ Mới:**
  - **Splash Interstitial:** Đơn luồng (`loadSplashInterstitialAds`) & Đa luồng song song 3-way (`High-Floor` / `All-Price` / `Secondary`).
  - **Banner:** Standard Adaptive Banner, Collapsible Banner (`TOP` / `BOTTOM`), Inline Adaptive Banner.
  - **Native Ad:** Standard Native, Preload Native (`ApNativeAd`), Fullscreen Native Ad (phong cách TikTok/Reels), tùy biến động nút CTA (`colorCTA`, `heightCTA`, `positionCTA`) & thứ tự thành phần từ Remote Config.
  - **Interstitial, App Open (Resume Ad), Rewarded Ad, Rewarded Interstitial, Swipeable In-Feed Interstitial, Picture-in-Picture (PiP) Ad.**
- **💳 Google Play Billing (`billing:9.0.0`) Có Bộ Nhớ Đệm:** Tự động lưu đệm trạng thái `isPurchased` vào `SharedPreferences` và giải phóng luồng Splash tức thì nếu thiết bị không có Google Play Services.
- **📊 Tracking Doanh Thu Tự Động:** Tích hợp sẵn **Adjust SDK (`5.8.0`)** và **Firebase Analytics (`23.2.0`)** tự động bắn sự kiện `OnPaidEvent` / `tROAS`.

---

## 📦 1. Cài Đặt & Khai Báo Gradle

### 1.1. Khai báo Repository (`build.gradle` cấp Project hoặc `settings.gradle`)

```groovy
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url "https://jitpack.io" }
        maven { url "https://artifact.bytedance.com/repository/pangle/" }
        maven { url "https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea" }
        maven { url "https://android-sdk.is.com/" }
    }
}
```

### 1.2. Khai báo Dependency (`app/build.gradle`)

> [!IMPORTANT]
> Bản `7.0.0` sử dụng **`ads-mobile-sdk:1.5.0`** (GMA Next-Gen SDK) thay cho `play-services-ads` cũ. Nếu dự án có dùng kèm các thư viện bên thứ ba (như `Module-Update-GDPR`) vẫn kéo theo `play-services-ads` cũ, hãy thêm `exclude` như bên dưới để tránh xung đột class.

```groovy
dependencies {
    implementation 'com.github.trongluan99:NKH-Studio:7.0.0'
    implementation 'com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.5.0'

    // Nếu dùng Module-Update-GDPR, loại bỏ play-services-ads cũ để không trùng class với GMA Next-Gen:
    implementation('com.github.Infinity-Technologies-Global:Module-Update-GDPR:2.0.2') {
        exclude group: 'com.google.android.gms', module: 'play-services-ads'
        exclude group: 'com.google.android.gms', module: 'play-services-ads-api'
        exclude group: 'com.google.android.gms', module: 'play-services-ads-lite'
    }

    implementation 'com.facebook.shimmer:shimmer:0.5.0'
    implementation 'androidx.multidex:multidex:2.0.1'
    implementation 'com.android.billingclient:billing:9.0.0'
}
```

### 1.3. Cấu hình `AndroidManifest.xml`

```xml
<application
    android:name=".app.GlobalApp"
    ...>

    <!-- AdMob App ID -->
    <meta-data
        android:name="com.google.android.gms.ads.APPLICATION_ID"
        android:value="${app_id}" />

    <!-- Facebook SDK Config -->
    <meta-data
        android:name="com.facebook.sdk.ApplicationId"
        android:value="@string/facebook_app_id" />
    <meta-data
        android:name="com.facebook.sdk.ClientToken"
        android:value="@string/facebook_client_token" />
    <meta-data
        android:name="com.facebook.sdk.AutoInitEnabled"
        android:value="true" />
    <meta-data
        android:name="com.facebook.sdk.AutoLogAppEventsEnabled"
        android:value="true" />
    <meta-data
        android:name="com.facebook.sdk.AdvertiserIDCollectionEnabled"
        android:value="true" />
</application>
```

### 1.4. Cấu hình R8 / ProGuard (`app/proguard-rules.pro`)

```proguard
-keep class com.ads.nkh.** { *; }
-keep class com.google.android.libraries.ads.mobile.sdk.** { *; }
-keep class com.adjust.sdk.** { *; }
# Nếu dùng Moshi KotlinJsonAdapterFactory để đọc ad_config.json:
-keep class kotlin.reflect.** { *; }
-keep class **.ads.** { *; }
```

---

## ⚙️ 2. Khởi Tạo Trong Class `Application`

Kế thừa `AdsMultiDexApplication` và khởi tạo `NkhAd` trong `onCreate()`:

### Kotlin (`GlobalApp.kt`)

```kotlin
class GlobalApp : AdsMultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        initAds()
        initBilling()
    }

    private fun initAds() {
        val environment = if (BuildConfig.DEBUG) {
            NkhAdConfig.ENVIRONMENT_DEVELOP
        } else {
            NkhAdConfig.ENVIRONMENT_PRODUCTION
        }
        mNkhAdConfig = NkhAdConfig(this, environment)

        // Cấu hình Adjust & Token
        mNkhAdConfig.adjustConfig = AdjustConfig(true, getString(R.string.adjust_token))
        mNkhAdConfig.facebookClientToken = getString(R.string.facebook_client_token)
        mNkhAdConfig.adjustTokenTiktok = getString(R.string.event_token)

        // Khoảng cách tối thiểu giữa 2 lần hiển thị Interstitial (giây)
        mNkhAdConfig.intervalInterstitialAd = 35

        // App Open Resume Ad ID (có thể đặt sau ở SplashActivity khi tải xong RemoteConfig)
        mNkhAdConfig.idAdResume = ""

        // Bật/tắt nút nổi Debug Inspector (Tự động chỉ hiển thị khi bản build là Debug)
        mNkhAdConfig.isEnableDebugMode = BuildConfig.DEBUG

        NkhAd.getInstance().init(this, mNkhAdConfig)

        // Tự động tắt Resume Ad khi người dùng bấm vào quảng cáo
        Admob.getInstance().setDisableAdResumeWhenClickAds(true)
        Admob.getInstance().setOpenActivityAfterShowInterAds(false)

        // Tắt Resume Ad trên các màn hình đặc biệt
        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(LanguageActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(OnBoardingActivity::class.java)
    }

    private fun initBilling() {
        val listINAPId = arrayListOf<String>()
        val listSubsId = arrayListOf("sub_weekly", "sub_monthly", "sub_yearly")
        AppPurchase.getInstance().initBilling(this, listINAPId, listSubsId)
    }
}
```

---

## 📱 3. Hướng Dẫn Sử Dụng Các Định Dạng Quảng Cáo

### 3.1. Quảng Cáo Màn Hình Chờ (`Splash Ad` & `Open Resume HighFloor` — Load Tuần Tự)

#### A. Splash Interstitial (1 ID hoặc HighFloor -> Normal tuần tự)

```kotlin
// 1. Load Splash đơn (1 ID):
NkhAd.getInstance().loadSplashInterstitialAds(
    this,
    AdRemoteConfig.inter_splash.id,
    30000L, // timeOut tối đa (ms)
    5000L,  // timeDelay tối thiểu (ms)
    object : AdCallback() {
        override fun onNextAction() {
            super.onNextAction()
            startNextScreen()
        }
    }
)

// 2. Load Splash với HighFloor TUẦN TỰ (Sequential - KHÔNG phải SameTime):
// Request idHigh trước -> chỉ khi idHigh fail (onAdFailedToLoad) mới request idNormal:
NkhAd.getInstance().loadSplashInterstitialAdsHighFloor(
    this,
    AdRemoteConfig.inter_splash_high.id, // ID High Floor (ưu tiên load trước)
    AdRemoteConfig.inter_splash.id,      // ID Normal (fallback khi High fail)
    30000L, // timeOut tổng cho cả 2 ad (ms)
    2000L,  // timeDelay tối thiểu (ms)
    true,   // showSplashIfReady
    object : AdCallback() {
        override fun onAdLoaded() {
            super.onAdLoaded()
            // Dùng chung onAdLoaded() cho cả HighFloor lẫn Normal khi 1 trong 2 tải thành công
        }

        override fun onNextAction() {
            super.onNextAction()
            startNextScreen()
        }
    }
)
```

#### B. Open Resume (`open_resume`) với HighFloor tuần tự

```kotlin
// Cách 1: Cấu hình ngay trong Application (NkhAdConfig):
config.setIdAdResumeHighFloor(idResumeHigh, idResumeNormal)

// Cách 2: Cấu hình tại SplashActivity sau khi tải xong RemoteConfig:
NkhAd.getInstance().setAppResumeHighFloor(
    AdRemoteConfig.open_resume_high.id, // Load trước khi app resume
    AdRemoteConfig.open_resume.id       // Chỉ load khi HighFloor fail
)
AppOpenManager.getInstance().enableAppResume()
```

Trong `onResume()` của `SplashActivity`, gọi thêm `onCheckShowSplashWhenFail` để xử lý trường hợp người dùng thoát ra ngoài rồi quay lại màn Splash:

```kotlin
override fun onResume() {
    super.onResume()
    NkhAd.getInstance().onCheckShowSplashWhenFail(this, object : AdCallback() {
        override fun onNextAction() {
            super.onNextAction()
            startNextScreen()
        }
    }, 1000)
}
```

---

### 3.2. Quảng Cáo Banner (`Standard`, `Collapsible`, `Inline Adaptive`)

#### A. Standard Adaptive Banner
```kotlin
NkhAd.getInstance().loadBanner(this, AdRemoteConfig.banner_home.id)
```

#### B. Collapsible Banner (`TOP` hoặc `BOTTOM`)
```kotlin
NkhAd.getInstance().loadCollapsibleBanner(
    this,
    AdRemoteConfig.banner_home.id,
    AppConstant.CollapsibleGravity.BOTTOM,
    object : AdCallback() {}
)
```

#### C. Inline Adaptive Banner (hiển thị trong ScrollView / RecyclerView)
```kotlin
Admob.getInstance().loadInlineBanner(
    this,
    AdRemoteConfig.banner_inline.id,
    Admob.BANNER_INLINE_LARGE_STYLE,
    object : AdCallback() {}
)
```

---

### 3.3. Quảng Cáo Native (`Native Ad`)

> [!WARNING]
> **Lưu ý quan trọng về Layout XML trên `v7.0.0`:**
> Trong các file layout XML (`res/layout/layout_native_*.xml`), bắt buộc sử dụng thẻ của GMA Next-Gen SDK:
> - `com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView`
> - `com.google.android.libraries.ads.mobile.sdk.nativead.MediaView`

#### A. Tải trước (`Preload`) và Hiển thị sau (`Populate`) — *Khuyên dùng cho Language / Onboarding*

```kotlin
// 1. Tải trước Native Ad và lưu vào biến ApNativeAd:
var cachedNativeAd: ApNativeAd? = null

NkhAd.getInstance().loadNativeAdResultCallback(
    activity,
    AdRemoteConfig.native_language_1.id,
    R.layout.layout_native_language,
    object : AdCallback() {
        override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
            super.onNativeAdLoaded(nativeAd)
            cachedNativeAd = nativeAd
        }

        override fun onAdFailedToLoad(adError: LoadAdError?) {
            super.onAdFailedToLoad(adError)
            cachedNativeAd = null
        }
    }
)

// 2. Hiển thị lên FrameLayout & ShimmerFrameLayout khi cần:
if (cachedNativeAd != null) {
    NkhAd.getInstance().populateNativeAdView(
        activity,
        cachedNativeAd,
        binding.frAds,
        binding.shimmerAds.shimmerNative
    )
}
```

#### B. Tải và Hiển thị trực tiếp (`Load & Show`)

```kotlin
NkhAd.getInstance().loadNativeAd(
    this,
    AdRemoteConfig.native_home.id,
    R.layout.layout_native_home,
    binding.frAds,
    binding.shimmerAds,
    object : AdCallback() {
        override fun onAdFailedToLoad(i: LoadAdError?) {
            super.onAdFailedToLoad(i)
            binding.frAds.removeAllViews()
        }
    }
)
```

---

### 3.4. Quảng Cáo Xen Kẽ (`Interstitial Ad`)

```kotlin
// 1. Tải trước Interstitial Ad:
private var interHomeAd: ApInterstitialAd? = null

fun loadInterHome(context: Context) {
    interHomeAd = NkhAd.getInstance().getInterstitialAds(
        context,
        AdRemoteConfig.inter_home.id,
        object : AdCallback() {}
    )
}

// 2. Hiển thị Interstitial Ad (tự động kiểm tra intervalInterstitialAd):
fun showInterHome(context: Context, onNext: () -> Unit) {
    val interstitial = interHomeAd
    if (interstitial != null && interstitial.isReady) {
        NkhAd.getInstance().forceShowInterstitial(
            context,
            interstitial,
            object : AdCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    onNext()
                }
            },
            true // shouldReloadAds
        )
    } else {
        onNext()
    }
}
```

---

### 3.5. Quảng Cáo Phần Thưởng (`Rewarded Ad`)

```kotlin
// 1. Khởi tạo / Tải Rewarded Ad:
private var rewardedAds: RewardedAd? = null

NkhAd.getInstance().initRewardAds(this, AdRemoteConfig.reward_id.id, object : AdCallback() {
    override fun onRewardAdLoaded(rewardedAd: RewardedAd?) {
        super.onRewardAdLoaded(rewardedAd)
        rewardedAds = rewardedAd
    }
})

// 2. Hiển thị Rewarded Ad:
var isEarned = false
NkhAd.getInstance().showRewardAds(this, rewardedAds, object : RewardCallback {
    override fun onUserEarnedReward(rewardItem: RewardItem?) {
        isEarned = true
    }

    override fun onRewardedAdClosed() {
        if (isEarned) {
            // Trao thưởng cho người dùng
        }
    }

    override fun onRewardedAdFailedToShow(codeError: Int) {}
    override fun onAdClicked() {}
})
```

---

### 3.6. Công Cụ Kiểm Tra Quảng Cáo Trực Tiếp (`Ad Debug Inspector 🐞`)

- Khi `mNkhAdConfig.isEnableDebugMode = true` và chạy bản **Debug**, nút nổi `🐞` tự động xuất hiện ở cạnh màn hình để mở **Ad Debug Inspector**.
- Bạn cũng có thể mở hộp thoại Inspector bằng code bất kỳ lúc nào:
  ```kotlin
  NkhAd.getInstance().openDebugInspector(activity)
  ```
- Để tắt nút nổi nhưng vẫn giữ ghi nhận log debug:
  ```kotlin
  NkhAd.getInstance().setEnableDebugMode(enable = true, showFloatingButton = false)
  ```

---

### 3.7. Thanh Toán Trong Ứng Dụng (`In-App Purchase & Subscription`)

```kotlin
AppPurchase.getInstance().setPurchaseListener(object : PurchaseListener {
    override fun onProductPurchased(productId: String?, transactionDetails: String?) {
        // Giao dịch thành công -> Tự động vô hiệu hóa toàn bộ quảng cáo
    }

    override fun displayErrorMessage(errorMsg: String?) {}
    override fun onUserCancelBilling() {}
})

// Mua gói In-App (One-time purchase):
AppPurchase.getInstance().purchase(activity, "product_id")

// Đăng ký gói Thuê bao (Subscription):
AppPurchase.getInstance().subscribe(activity, "sub_weekly_id")
```

---

## 🔄 4. Hướng Dẫn Nâng Cấp Nhanh Từ `NKH-Studio:6.7` Lên `7.0.0`

| Hạng mục | Bản cũ (`NKH-Studio:6.7`) | Bản mới (`NKH-Studio:7.0.0`) |
| :--- | :--- | :--- |
| **Dependency chính** | `com.google.android.gms:play-services-ads:24.7.0` | `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.5.0` |
| **Import `AdError`** | `com.google.android.gms.ads.AdError` | `com.google.android.libraries.ads.mobile.sdk.common.AdError` |
| **Import `LoadAdError`** | `com.google.android.gms.ads.LoadAdError` | `com.google.android.libraries.ads.mobile.sdk.common.LoadAdError` |
| **Import `AdValue`** | `com.google.android.gms.ads.AdValue` | `com.google.android.libraries.ads.mobile.sdk.common.AdValue` |
| **Import `AdView` (Banner)** | `com.google.android.gms.ads.AdView` | `com.google.android.libraries.ads.mobile.sdk.banner.AdView` |
| **Import & XML `NativeAdView`** | `com.google.android.gms.ads.nativead.NativeAdView` | `com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView` |
| **Import & XML `MediaView`** | `com.google.android.gms.ads.nativead.MediaView` | `com.google.android.libraries.ads.mobile.sdk.nativead.MediaView` |

📄 **Xem thêm báo cáo Benchmark chi tiết giữa `6.7` và `7.0.0`:** [`ADS_SDK_6.7_VS_7.0.0_COMPARISON.md`](file:///Users/ducanh/Project/Infinity/The-Sun-IPTV-2/ADS_SDK_6.7_VS_7.0.0_COMPARISON.md)
