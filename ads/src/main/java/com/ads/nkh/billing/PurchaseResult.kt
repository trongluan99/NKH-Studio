package com.ads.nkh.billing

class PurchaseResult {
    private var orderId: String? = null
    private var packageName: String
    private var productId: List<String>
    private var purchaseTime: Long = 0
    private var purchaseState: Int
    private var purchaseToken: String? = null
    private var quantity: Int = 0
    private var autoRenewing: Boolean
    private var acknowledged: Boolean = false

    constructor(
        packageName: String,
        productId: List<String>,
        purchaseState: Int,
        autoRenewing: Boolean
    ) {
        this.packageName = packageName
        this.productId = productId
        this.purchaseState = purchaseState
        this.autoRenewing = autoRenewing
    }

    constructor(
        orderId: String?,
        packageName: String,
        productId: List<String>,
        purchaseTime: Long,
        purchaseState: Int,
        purchaseToken: String?,
        quantity: Int,
        autoRenewing: Boolean,
        acknowledged: Boolean
    ) {
        this.orderId = orderId
        this.packageName = packageName
        this.productId = productId
        this.purchaseTime = purchaseTime
        this.purchaseState = purchaseState
        this.purchaseToken = purchaseToken
        this.quantity = quantity
        this.autoRenewing = autoRenewing
        this.acknowledged = acknowledged
    }

    fun getPackageName(): String {
        return packageName
    }

    fun setPackageName(packageName: String) {
        this.packageName = packageName
    }

    fun getProductId(): List<String> {
        return productId
    }

    fun setProductId(productId: List<String>) {
        this.productId = productId
    }

    fun getPurchaseState(): Int {
        return purchaseState
    }

    fun setPurchaseState(purchaseState: Int) {
        this.purchaseState = purchaseState
    }

    fun isAutoRenewing(): Boolean {
        return autoRenewing
    }

    fun setAutoRenewing(autoRenewing: Boolean) {
        this.autoRenewing = autoRenewing
    }
}
