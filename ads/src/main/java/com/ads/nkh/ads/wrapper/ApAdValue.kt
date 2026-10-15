package com.ads.nkh.ads.wrapper

import com.applovin.mediation.MaxAd
import com.google.android.libraries.ads.mobile.sdk.common.AdValue

class ApAdValue {
    var admobAdValue: AdValue? = null
        private set
    var maxAdValue: MaxAd? = null
        private set

    constructor(maxAdValue: MaxAd?) {
        this.maxAdValue = maxAdValue
    }

    constructor(admobAdValue: AdValue?) {
        this.admobAdValue = admobAdValue
    }
}
