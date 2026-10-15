package com.ads.nkh.ads.wrapper

import com.applovin.mediation.MaxReward
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem

class ApRewardItem {
    var admobRewardItem: RewardItem? = null
    var maxRewardItem: MaxReward? = null

    constructor(maxRewardItem: MaxReward?) {
        this.maxRewardItem = maxRewardItem
    }

    constructor(admobRewardItem: RewardItem?) {
        this.admobRewardItem = admobRewardItem
    }
}
