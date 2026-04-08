package com.ads.nkh.funtion;

import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.rewarded.RewardItem;

public interface RewardCallbackPreload {
    void onNextAction();

    void onAdClosed();

    void onAdFailedToShow(@Nullable AdError adError);

    void onAdClicked();

    void onAdImpression();

    void onUserEarnedReward(RewardItem var1);
}
