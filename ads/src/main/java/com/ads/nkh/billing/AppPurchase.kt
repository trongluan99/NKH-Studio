package com.ads.nkh.billing

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.IntDef
import com.ads.nkh.event.NkhLogEventManager
import com.ads.nkh.funtion.BillingListener
import com.ads.nkh.funtion.PurchaseListener
import com.ads.nkh.funtion.UpdatePurchaseListener
import com.ads.nkh.util.AppUtil
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.common.collect.ImmutableList
import java.text.NumberFormat
import java.util.Collections
import java.util.Currency

class AppPurchase private constructor() {

    @SuppressLint("StaticFieldLeak")
    private var price: String? = "1.49$"
    private var oldPrice: String? = "2.99$"

    @Deprecated("")
    private var productId: String? = null
    private var listSubscriptionId: ArrayList<QueryProductDetailsParams.Product>? = null
    private var listINAPId: ArrayList<QueryProductDetailsParams.Product>? = null
    private var purchaseListener: PurchaseListener? = null
    private var updatePurchaseListener: UpdatePurchaseListener? = null
    private var billingListener: BillingListener? = null
    private var isInitBillingFinish: Boolean = false
    private var billingClient: BillingClient? = null
    private var skuListINAPFromStore: List<ProductDetails>? = null
    private var skuListSubsFromStore: List<ProductDetails>? = null
    private val skuDetailsINAPMap: MutableMap<String, ProductDetails> = HashMap()
    private val skuDetailsSubsMap: MutableMap<String, ProductDetails> = HashMap()
    @get:JvmName("_isAvailable_prop")
    var isAvailable: Boolean = false
        private set
    private var isListGot: Boolean = false
    private var isConsumePurchase: Boolean = false
    private var idPurchaseCurrent: String = ""
    private var offerTokenCurrent: String = ""
    private var typeIap: Int = 0
    private var verifyFinish: Boolean = false

    private var isVerifyINAP: Boolean = false
    private var isVerifySUBS: Boolean = false
    private var isUpdateInapps: Boolean = false
    private var isUpdateSubs: Boolean = false

    private var isPurchase: Boolean = false // state purchase on app
    var isPurchased: Boolean
        @JvmName("_isPurchased_prop") get() = isPurchase
        @JvmName("_setPurchased_prop") set(value) {
            isPurchase = value
        }
    val initBillingFinish: Boolean
        @JvmName("_getInitBillingFinish_prop") get() = isInitBillingFinish
    private var idPurchased: String = "" // id purchased
    private val ownerIdSubs: MutableList<PurchaseResult> = ArrayList() // id sub
    private val ownerIdInapps: MutableList<String> = ArrayList() // id inapp

    private var handlerTimeout: Handler? = null
    private var rdTimeout: Runnable? = null

    private var application: Application? = null

    fun setPurchaseListener(purchaseListener: PurchaseListener?) {
        this.purchaseListener = purchaseListener
    }

    fun setUpdatePurchaseListener(listener: UpdatePurchaseListener?) {
        this.updatePurchaseListener = listener
    }

    /**
     * Listener init billing app
     * When init available auto call onInitBillingFinish with resultCode = 0
     *
     * @param billingListener
     */
    fun setBillingListener(billingListener: BillingListener?) {
        this.billingListener = billingListener
        if (isAvailable && verifyFinish) {
            isInitBillingFinish = true
            billingListener?.onInitBillingFinished(0)
        }
    }

    fun isAvailable(): Boolean {
        return isAvailable
    }

    fun getInitBillingFinish(): Boolean {
        return isInitBillingFinish
    }

    fun setEventConsumePurchaseTest(view: View) {
        view.setOnClickListener {
            if (AppUtil.VARIANT_DEV) {
                Log.d(TAG, "setEventConsumePurchaseTest: success")
                getInstance().consumePurchase(PRODUCT_ID_TEST)
            }
        }
    }

    /**
     * Listener init billing app with timeout
     * When init available auto call onInitBillingFinish with resultCode = 0
     *
     * @param billingListener
     * @param timeout
     */
    fun setBillingListener(billingListener: BillingListener?, timeout: Int) {
        Log.d(TAG, "setBillingListener: timeout $timeout")
        this.billingListener = billingListener
        if (isAvailable && verifyFinish) {
            Log.d(TAG, "setBillingListener: finish")
            isInitBillingFinish = true
            billingListener?.onInitBillingFinished(0)
            return
        }
        isInitBillingFinish = false
        handlerTimeout = Handler(Looper.getMainLooper())
        rdTimeout = Runnable {
            Log.d(TAG, "setBillingListener: timeout run ")
            if (!isInitBillingFinish) {
                isInitBillingFinish = true
                billingListener?.onInitBillingFinished(BillingClient.BillingResponseCode.ERROR)
            }
        }
        handlerTimeout?.postDelayed(rdTimeout!!, timeout.toLong())
    }

    fun setPrice(price: String?) {
        this.price = price
    }

    fun setConsumePurchase(consumePurchase: Boolean) {
        isConsumePurchase = consumePurchase
    }

    fun setOldPrice(oldPrice: String?) {
        this.oldPrice = oldPrice
    }

    @JvmField
    var purchasesUpdatedListener: PurchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, list ->
            Log.d(TAG, "onPurchasesUpdated code: ${billingResult.responseCode}, msg: ${billingResult.debugMessage}")
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    if (list != null) {
                        for (purchase in list) {
                            handlePurchase(purchase)
                        }
                    }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    purchaseListener?.onUserCancelBilling()
                    Log.d(TAG, "onPurchasesUpdated: USER_CANCELED")
                }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    Log.d(TAG, "onPurchasesUpdated: ITEM_ALREADY_OWNED -> syncing ownership")
                    verifyPurchased(false)
                    purchaseListener?.displayErrorMessage("Selected item is already owned")
                }
                else -> {
                    val errMsg = billingResult.debugMessage.ifEmpty {
                        "Billing error (code=${billingResult.responseCode})"
                    }
                    purchaseListener?.displayErrorMessage(errMsg)
                    Log.w(TAG, "onPurchasesUpdated error: $errMsg")
                }
            }
        }

    @JvmField
    var purchaseClientStateListener: BillingClientStateListener =
        object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                isAvailable = false

                Handler(Looper.getMainLooper()).postDelayed({
                    billingClient?.startConnection(this)
                }, 2000)
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                val code = billingResult.responseCode
                Log.d(TAG, "onBillingSetupFinished: $code")

                if (code != BillingClient.BillingResponseCode.OK) {
                    // Unblock Splash immediately if Play Store is unavailable (e.g. no Google account)
                    verifyFinish = true
                    if (billingListener != null && !isInitBillingFinish) {
                        isInitBillingFinish = true
                        if (handlerTimeout != null && rdTimeout != null) {
                            handlerTimeout?.removeCallbacks(rdTimeout!!)
                        }
                        Handler(Looper.getMainLooper()).post {
                            billingListener?.onInitBillingFinished(code)
                        }
                    }
                    // Retry connection in background for transient errors
                    if (code == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED ||
                        code == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ||
                        code == BillingClient.BillingResponseCode.NETWORK_ERROR
                    ) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            billingClient?.startConnection(this)
                        }, 2000)
                    }
                    return
                }

                // ====== BILLING READY ======
                isAvailable = true

                // 1️⃣ LOAD INAPP PRODUCT DETAILS
                val inapList = listINAPId
                if (!inapList.isNullOrEmpty()) {
                    val paramsINAP = QueryProductDetailsParams.newBuilder()
                        .setProductList(inapList)
                        .build()

                    billingClient?.queryProductDetailsAsync(paramsINAP) { _, result ->
                        val list = result.productDetailsList
                        if (list != null && list.isNotEmpty()) {
                            skuListINAPFromStore = list
                            addSkuINAPToMap(list)
                        }
                    }
                }

                // 2️⃣ LOAD SUBS PRODUCT DETAILS
                val subsList = listSubscriptionId
                if (!subsList.isNullOrEmpty()) {
                    val paramsSUBS = QueryProductDetailsParams.newBuilder()
                        .setProductList(subsList)
                        .build()

                    billingClient?.queryProductDetailsAsync(paramsSUBS) { _, result ->
                        val list = result.productDetailsList
                        if (list != null && list.isNotEmpty()) {
                            skuListSubsFromStore = list
                            addSkuSubsToMap(list)
                        }
                    }
                }

                // 3️⃣ VERIFY PURCHASE OWNERSHIP BEFORE FIRING onInitBillingFinished
                verifyPurchased(true)
            }
        }

    fun getOwnerIdSubs(): List<PurchaseResult> {
        return ownerIdSubs
    }

    fun getOwnerIdInapps(): List<String> {
        return ownerIdInapps
    }

    fun initBilling(
        application: Application,
        listINAPId: List<String>,
        listSubsId: List<String>
    ) {
        this.application = application
        this.isPurchase = loadCachedPurchased()

        val mutableInapList = ArrayList(listINAPId)
        if (AppUtil.VARIANT_DEV && !mutableInapList.contains(PRODUCT_ID_TEST)) {
            mutableInapList.add(PRODUCT_ID_TEST)
        }
        this.listSubscriptionId = listIdToListProduct(listSubsId, BillingClient.ProductType.SUBS)
        this.listINAPId = listIdToListProduct(mutableInapList, BillingClient.ProductType.INAPP)

        billingClient = BillingClient.newBuilder(application)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        billingClient?.startConnection(purchaseClientStateListener)
    }

    private fun addSkuSubsToMap(skuList: List<ProductDetails>) {
        for (skuDetails in skuList) {
            skuDetailsSubsMap[skuDetails.productId] = skuDetails
        }
    }

    private fun addSkuINAPToMap(skuList: List<ProductDetails>) {
        for (skuDetails in skuList) {
            skuDetailsINAPMap[skuDetails.productId] = skuDetails
        }
    }

    fun setPurchase(purchase: Boolean) {
        isPurchase = purchase
        saveCachedPurchased(purchase)
    }

    fun isPurchased(): Boolean {
        return isPurchase
    }

    fun isPurchased(context: Context?): Boolean {
        return isPurchase
    }

    fun getIdPurchased(): String {
        return idPurchased
    }

    private fun addOrUpdateOwnerIdSub(purchaseResult: PurchaseResult, id: String) {
        var isExistId = false
        for (p in ownerIdSubs) {
            if (p.getProductId().contains(id)) {
                isExistId = true
                ownerIdSubs.remove(p)
                ownerIdSubs.add(purchaseResult)
                break
            }
        }
        if (!isExistId) {
            ownerIdSubs.add(purchaseResult)
        }
    }

    fun verifyPurchased(isCallback: Boolean) {
        val client = billingClient
        if (client == null || !client.isReady) {
            Log.e(TAG, "BillingClient is not ready")
            return
        }

        verifyFinish = false
        isVerifyINAP = false
        isVerifySUBS = false
        ownerIdInapps.clear()
        ownerIdSubs.clear()
        isPurchase = false
        val productIdsINAP = getProductIds(listINAPId)
        val productIdsSUBS = getProductIds(listSubscriptionId)

        if (productIdsINAP.isNotEmpty()) {
            client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
            ) { billingResult, list ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && list != null) {
                    for (purchase in list) {
                        for (productId in productIdsINAP) {
                            if (purchase.products.contains(productId)) {
                                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                                    if (!ownerIdInapps.contains(productId)) {
                                        ownerIdInapps.add(productId)
                                    }
                                    idPurchased = productId
                                    isPurchase = true
                                    trackPurchaseIfNeeded(purchase, productId, TYPE_IAP.PURCHASE)
                                    if (!purchase.isAcknowledged) {
                                        val acknowledgePurchaseParams =
                                            AcknowledgePurchaseParams.newBuilder()
                                                .setPurchaseToken(purchase.purchaseToken)
                                                .build()
                                        client.acknowledgePurchase(acknowledgePurchaseParams) { billingResult1 ->
                                            Log.d(
                                                TAG,
                                                "verifyPurchased (INAP) acknowledgePurchase: " + billingResult1.debugMessage
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                isVerifyINAP = true
                checkAndFinishCallback(isCallback, billingResult)
            }
        } else {
            isVerifyINAP = true
        }

        if (productIdsSUBS.isNotEmpty()) {
            client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
            ) { billingResult, list ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && list != null) {
                    for (purchase in list) {
                        for (productId in productIdsSUBS) {
                            if (purchase.products.contains(productId)) {
                                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                                    val purchaseResult = PurchaseResult(
                                        purchase.packageName,
                                        purchase.products,
                                        purchase.purchaseState,
                                        purchase.isAutoRenewing
                                    )
                                    addOrUpdateOwnerIdSub(purchaseResult, productId)
                                    idPurchased = productId
                                    isPurchase = true
                                    trackPurchaseIfNeeded(purchase, productId, TYPE_IAP.SUBSCRIPTION)
                                    if (!purchase.isAcknowledged) {
                                        val acknowledgePurchaseParams =
                                            AcknowledgePurchaseParams.newBuilder()
                                                .setPurchaseToken(purchase.purchaseToken)
                                                .build()
                                        client.acknowledgePurchase(acknowledgePurchaseParams) { billingResult1 ->
                                            Log.d(
                                                TAG,
                                                "verifyPurchased (SUBS) acknowledgePurchase: " + billingResult1.debugMessage
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                isVerifySUBS = true
                checkAndFinishCallback(isCallback, billingResult)
            }
        } else {
            isVerifySUBS = true
        }

        if (isVerifyINAP && isVerifySUBS) {
            checkAndFinishCallback(
                isCallback,
                BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()
            )
        }
    }

    private fun getProductIds(productList: ArrayList<QueryProductDetailsParams.Product>?): ArrayList<String> {
        val productIds = ArrayList<String>()
        if (productList != null) {
            for (product in productList) {
                productIds.add(product.zza())
            }
        }
        return productIds
    }

    private fun checkAndFinishCallback(isCallback: Boolean, billingResult: BillingResult) {
        if (isVerifyINAP && isVerifySUBS) {
            verifyFinish = true
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                saveCachedPurchased(isPurchase)
            }
            if (billingListener != null && isCallback && !isInitBillingFinish) {
                isInitBillingFinish = true
                if (handlerTimeout != null && rdTimeout != null) {
                    handlerTimeout?.removeCallbacks(rdTimeout!!)
                }
                Handler(Looper.getMainLooper()).post {
                    billingListener?.onInitBillingFinished(billingResult.responseCode)
                }
            }
        }
    }

    fun updatePurchaseStatus() {
        isUpdateInapps = false
        isUpdateSubs = false
        ownerIdInapps.clear()
        ownerIdSubs.clear()
        isPurchase = false

        val inapList = listINAPId
        if (inapList != null) {
            billingClient?.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
            ) { billingResult, list ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && list != null) {
                    for (purchase in list) {
                        for (id in inapList) {
                            if (purchase.products.contains(id.zza())) {
                                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                                    if (!ownerIdInapps.contains(id.zza())) {
                                        ownerIdInapps.add(id.zza())
                                    }
                                    isPurchase = true
                                    trackPurchaseIfNeeded(purchase, id.zza(), TYPE_IAP.PURCHASE)
                                }
                            }
                        }
                    }
                }
                isUpdateInapps = true
                if (isUpdateSubs) {
                    saveCachedPurchased(isPurchase)
                    updatePurchaseListener?.onUpdateFinished()
                }
            }
        }

        val subsList = listSubscriptionId
        if (subsList != null) {
            billingClient?.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
            ) { billingResult, list ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && list != null) {
                    for (purchase in list) {
                        for (id in subsList) {
                            if (purchase.products.contains(id.zza())) {
                                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                                    val purchaseResult = PurchaseResult(
                                        purchase.packageName,
                                        purchase.products,
                                        purchase.purchaseState,
                                        purchase.isAutoRenewing
                                    )
                                    addOrUpdateOwnerIdSub(purchaseResult, id.zza())
                                    isPurchase = true
                                    trackPurchaseIfNeeded(purchase, id.zza(), TYPE_IAP.SUBSCRIPTION)
                                }
                            }
                        }
                    }
                }
                isUpdateSubs = true
                if (isUpdateInapps) {
                    saveCachedPurchased(isPurchase)
                    updatePurchaseListener?.onUpdateFinished()
                }
            }
        }
    }

    @Deprecated("")
    fun purchase(activity: Activity) {
        if (productId == null) {
            Toast.makeText(activity, "Product id must not be empty!", Toast.LENGTH_SHORT).show()
            return
        }

        purchase(activity, productId!!)
    }

    private var isPurchasePayWall: Boolean = false

    fun isPurchasePayWall(): Boolean {
        return isPurchasePayWall
    }

    fun setPurchasePayWall(purchasePayWall: Boolean) {
        isPurchasePayWall = purchasePayWall
    }

    fun purchase(activity: Activity, productId: String): String {
        var currentProductId = productId
        val productDetails = skuDetailsINAPMap[currentProductId]
        if (AppUtil.VARIANT_DEV && !isPurchasePayWall) {
            currentProductId = PRODUCT_ID_TEST
            val purchaseDevBottomSheet = PurchaseDevBottomSheet(
                TYPE_IAP.PURCHASE,
                productDetails,
                activity,
                purchaseListener
            )
            purchaseDevBottomSheet.show()
            return ""
        }
        if (skuListINAPFromStore == null) {
            purchaseListener?.displayErrorMessage("Billing error init")
            return ""
        }

        if (productDetails == null) {
            return "Product ID invalid"
        }

        idPurchaseCurrent = currentProductId
        typeIap = TYPE_IAP.PURCHASE

        val productDetailsParamsList = ImmutableList.of(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient!!.launchBillingFlow(activity, billingFlowParams)

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                purchaseListener?.displayErrorMessage("Billing not supported for type of request")
                return "Billing not supported for type of request"
            }

            BillingClient.BillingResponseCode.ITEM_NOT_OWNED,
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> return ""

            BillingClient.BillingResponseCode.ERROR -> {
                purchaseListener?.displayErrorMessage("Error completing request")
                return "Error completing request"
            }

            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> return "Error processing request."

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> return "Selected item is already owned"

            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> return "Item not available"

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> return "Play Store service is not connected now"

            BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> return "Timeout"

            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                purchaseListener?.displayErrorMessage("Network error.")
                return "Network Connection down"
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                purchaseListener?.displayErrorMessage("Request Canceled")
                return "Request Canceled"
            }

            BillingClient.BillingResponseCode.OK -> {
                offerTokenCurrent = ""
                return "Subscribed Successfully"
            }
        }
        return ""
    }

    fun subscribe(activity: Activity, SubsId: String): String {
        if (AppUtil.VARIANT_DEV && !isPurchasePayWall) {
            purchase(activity, PRODUCT_ID_TEST)
            return "Billing test"
        } else {
            if (skuListSubsFromStore == null) {
                purchaseListener?.displayErrorMessage("Billing error init")
                return ""
            }
        }
        val productDetails = skuDetailsSubsMap[SubsId] ?: return "Product ID invalid"
        val subsDetail = productDetails.subscriptionOfferDetails
        if (subsDetail.isNullOrEmpty()) {
            return "No available offers for this subscription"
        }

        val offerToken = getOfferToken(subsDetail)
        idPurchaseCurrent = SubsId
        typeIap = TYPE_IAP.SUBSCRIPTION

        val productDetailsParamsList = ImmutableList.of(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient!!.launchBillingFlow(activity, billingFlowParams)
        offerTokenCurrent = offerToken

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                purchaseListener?.displayErrorMessage("Billing not supported for type of request")
                return "Billing not supported for type of request"
            }

            BillingClient.BillingResponseCode.ITEM_NOT_OWNED,
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> return ""

            BillingClient.BillingResponseCode.ERROR -> {
                purchaseListener?.displayErrorMessage("Error completing request")
                return "Error completing request"
            }

            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> return "Error processing request."

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> return "Selected item is already owned"

            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> return "Item not available"

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> return "Play Store service is not connected now"

            BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> return "Timeout"

            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                purchaseListener?.displayErrorMessage("Network error.")
                return "Network Connection down"
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                purchaseListener?.displayErrorMessage("Request Canceled")
                return "Request Canceled"
            }

            BillingClient.BillingResponseCode.OK -> return "Subscribed Successfully"
        }
        return ""
    }

    /**
     * Subscribe with specific offer token
     *
     * @param activity   current activity
     * @param SubsId     subscription product ID
     * @param offerToken specific offer token to use
     * @return status message
     */
    fun subscribe(activity: Activity, SubsId: String, offerToken: String?): String {
        if (AppUtil.VARIANT_DEV && !isPurchasePayWall) {
            purchase(activity, PRODUCT_ID_TEST)
            return "Billing test"
        } else {
            if (skuListSubsFromStore == null) {
                purchaseListener?.displayErrorMessage("Billing error init")
                return ""
            }
        }

        val productDetails = skuDetailsSubsMap[SubsId] ?: return "Product ID invalid"

        if (offerToken.isNullOrEmpty()) {
            return "Offer token is required"
        }

        idPurchaseCurrent = SubsId
        typeIap = TYPE_IAP.SUBSCRIPTION

        val productDetailsParamsList = ImmutableList.of(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient!!.launchBillingFlow(activity, billingFlowParams)
        offerTokenCurrent = offerToken

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                purchaseListener?.displayErrorMessage("Billing not supported for type of request")
                return "Billing not supported for type of request"
            }

            BillingClient.BillingResponseCode.ITEM_NOT_OWNED,
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> return ""

            BillingClient.BillingResponseCode.ERROR -> {
                purchaseListener?.displayErrorMessage("Error completing request")
                return "Error completing request"
            }

            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> return "Error processing request."

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> return "Selected item is already owned"

            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> return "Item not available"

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> return "Play Store service is not connected now"

            BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> return "Timeout"

            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                purchaseListener?.displayErrorMessage("Network error.")
                return "Network Connection down"
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                purchaseListener?.displayErrorMessage("Request Canceled")
                return "Request Canceled"
            }

            BillingClient.BillingResponseCode.OK -> return "Subscribed Successfully"
        }
        return ""
    }

    private fun getOfferToken(subsDetail: List<ProductDetails.SubscriptionOfferDetails>): String {
        var offerToken: String? = null
        for (offer in subsDetail) {
            val pricingPhases = offer.pricingPhases.pricingPhaseList
            for (phase in pricingPhases) {
                if (phase.priceAmountMicros == 0L) { // Free trial
                    offerToken = offer.offerToken
                    break
                }
            }
            if (offerToken != null) break
        }

        if (offerToken == null) {
            offerToken = subsDetail[0].offerToken
        }
        offerTokenCurrent = offerToken
        return offerToken
    }

    fun consumePurchase() {
        if (productId == null) {
            Log.e(TAG, "Consume Purchase false:productId null ")
            return
        }
        consumePurchase(productId!!)
    }

    fun consumePurchase(productId: String) {
        val client = billingClient
        if (client == null || !client.isReady) {
            Log.e(TAG, "BillingClient is not ready")
            return
        }

        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { billingResult, list ->
            var pc: Purchase? = null
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && list != null) {
                for (purchase in list) {
                    if (purchase.products.contains(productId)) {
                        pc = purchase
                    }
                }
            }

            if (pc == null) {
                Log.e(TAG, "No purchases found to consume.")
                return@queryPurchasesAsync
            }

            try {
                val consumeParams = ConsumeParams.newBuilder()
                    .setPurchaseToken(pc.purchaseToken)
                    .build()

                client.consumeAsync(consumeParams) { billingResult1, _ ->
                    if (billingResult1.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.e(TAG, "onConsumeResponse: OK")
                        verifyPurchased(false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getListInappId(): List<String> {
        val list = ArrayList<String>()
        val inapList = listINAPId
        if (inapList != null) {
            for (product in inapList) {
                list.add(product.zza())
            }
        }
        return list
    }

    private fun getListSubId(): List<String> {
        val list = ArrayList<String>()
        val subsList = listSubscriptionId
        if (subsList != null) {
            for (product in subsList) {
                list.add(product.zza())
            }
        }
        return list
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "handlePurchase: Purchase is PENDING (${purchase.products}), waiting for completion")
            purchaseListener?.displayErrorMessage("Purchase is pending approval")
            return
        }
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            Log.w(TAG, "handlePurchase: Ignored purchase with state=${purchase.purchaseState}")
            return
        }

        val sku = if (purchase.products.isNotEmpty()) purchase.products[0] else idPurchaseCurrent
        val resolvedTypeIap = when {
            skuDetailsSubsMap.containsKey(sku) || getListSubId().contains(sku) -> TYPE_IAP.SUBSCRIPTION
            skuDetailsINAPMap.containsKey(sku) || getListInappId().contains(sku) -> TYPE_IAP.PURCHASE
            typeIap != 0 -> typeIap
            else -> TYPE_IAP.PURCHASE
        }

        idPurchased = sku
        if (!isConsumePurchase) {
            isPurchase = true
            saveCachedPurchased(true)
            if (resolvedTypeIap == TYPE_IAP.SUBSCRIPTION) {
                val purchaseResult = PurchaseResult(
                    purchase.packageName,
                    purchase.products,
                    purchase.purchaseState,
                    purchase.isAutoRenewing
                )
                addOrUpdateOwnerIdSub(purchaseResult, sku)
            } else if (sku.isNotEmpty() && !ownerIdInapps.contains(sku)) {
                ownerIdInapps.add(sku)
            }
        }

        try {
            val token = purchase.purchaseToken
            if (!isTokenTracked(token)) {
                val price = getPriceWithoutCurrency(sku, resolvedTypeIap, offerTokenCurrent)
                val currency = getCurrency(sku, resolvedTypeIap, offerTokenCurrent)

                if (resolvedTypeIap == TYPE_IAP.SUBSCRIPTION) {
                    // Subscription: report the raw purchase (token/signature/orderId) to Adjust so
                    // Adjust can server-side verify with Google and auto-track renew/cancel/refund
                    // via RTDN. No further client-side call is needed on renewal.
                    NkhLogEventManager.trackPlayStoreSubscription(
                        price.toLong(),
                        currency,
                        sku,
                        purchase.orderId,
                        purchase.signature,
                        token,
                        purchase.purchaseTime
                    )
                } else {
                    NkhLogEventManager.onTrackRevenuePurchase(price.toFloat(), currency, sku, resolvedTypeIap)
                }
                markTokenTracked(token)
            }
        } catch (e: Exception) {
            // Never let a tracking/SDK failure crash the purchase flow itself.
            Log.e(TAG, "handlePurchase: track revenue failed - " + e.message)
        }

        if (purchaseListener != null) {
            isPurchase = true
            saveCachedPurchased(true)
            purchaseListener?.onProductPurchased(purchase.orderId, purchase.originalJson)
        }
        if (isConsumePurchase) {
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            val listener = ConsumeResponseListener { billingResult, _ ->
                Log.d(TAG, "onConsumeResponse: " + billingResult.debugMessage)
            }

            billingClient?.consumeAsync(consumeParams, listener)
        } else {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            if (!purchase.isAcknowledged) {
                billingClient?.acknowledgePurchase(
                    acknowledgePurchaseParams,
                    AcknowledgePurchaseResponseListener { billingResult ->
                        Log.d(TAG, "onAcknowledgePurchaseResponse: " + billingResult.debugMessage)
                    }
                )
            }
        }
    }

    private fun loadCachedPurchased(): Boolean {
        val app = application ?: return false
        return try {
            val prefs = app.getSharedPreferences(PREF_NAME_TRACKED_IAP, Context.MODE_PRIVATE)
            prefs.getBoolean(KEY_CACHED_IS_PURCHASED, false)
        } catch (e: Exception) {
            Log.e(TAG, "loadCachedPurchased: " + e.message)
            false
        }
    }

    private fun saveCachedPurchased(purchased: Boolean) {
        val app = application ?: return
        try {
            val prefs = app.getSharedPreferences(PREF_NAME_TRACKED_IAP, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_CACHED_IS_PURCHASED, purchased).apply()
        } catch (e: Exception) {
            Log.e(TAG, "saveCachedPurchased: " + e.message)
        }
    }

    private fun isTokenTracked(purchaseToken: String?): Boolean {
        val app = application
        if (app == null || purchaseToken == null) {
            return false
        }
        return try {
            val prefs = app.getSharedPreferences(PREF_NAME_TRACKED_IAP, Context.MODE_PRIVATE)
            (prefs.getStringSet(KEY_TRACKED_TOKENS, Collections.emptySet()) ?: Collections.emptySet())
                .contains(purchaseToken)
        } catch (e: Exception) {
            Log.e(TAG, "isTokenTracked: " + e.message)
            false
        }
    }

    private fun markTokenTracked(purchaseToken: String?) {
        val app = application
        if (app == null || purchaseToken == null) {
            return
        }
        try {
            val prefs = app.getSharedPreferences(PREF_NAME_TRACKED_IAP, Context.MODE_PRIVATE)
            val existing = prefs.getStringSet(KEY_TRACKED_TOKENS, Collections.emptySet()) ?: Collections.emptySet()
            val tokens = HashSet(existing)
            tokens.add(purchaseToken)
            prefs.edit().putStringSet(KEY_TRACKED_TOKENS, tokens).apply()
        } catch (e: Exception) {
            Log.e(TAG, "markTokenTracked: " + e.message)
        }
    }

    /**
     * Track a purchase found via verifyPurchased()/updatePurchaseStatus() (app relaunch,
     * restore on a new device) that was never sent to Adjust because handlePurchase() only
     * fires for a purchase made in the current billing-flow session.
     * No-op (safe to call every time) if the token was already tracked, or if ProductDetails
     * for the product hasn't loaded yet - in that case it silently retries on the next call.
     */
    private fun trackPurchaseIfNeeded(purchase: Purchase, productId: String, typeIAP: Int) {
        try {
            val token = purchase.purchaseToken
            if (token.isNullOrEmpty() || isTokenTracked(token)) {
                return
            }
            val price = getPriceWithoutCurrency(productId, typeIAP)
            val currency = getCurrency(productId, typeIAP)
            if (price <= 0 || currency.isEmpty()) {
                return
            }
            if (typeIAP == TYPE_IAP.SUBSCRIPTION) {
                NkhLogEventManager.trackPlayStoreSubscription(
                    price.toLong(),
                    currency,
                    productId,
                    purchase.orderId,
                    purchase.signature,
                    token,
                    purchase.purchaseTime
                )
            } else {
                NkhLogEventManager.onTrackRevenuePurchase(price.toFloat(), currency, productId, typeIAP)
            }
            markTokenTracked(token)
        } catch (e: Exception) {
            // Never let a tracking/SDK failure crash the verify/update flow.
            Log.e(TAG, "trackPurchaseIfNeeded: " + e.message)
        }
    }

    @Deprecated("")
    fun getPrice(): String {
        return getPrice(productId)
    }

    fun getPrice(productId: String?): String {
        val skuDetails = (if (productId != null) skuDetailsINAPMap[productId] else null) ?: return ""
        val offerDetails = skuDetails.oneTimePurchaseOfferDetails ?: return ""
        Log.e(TAG, "getPrice: " + offerDetails.formattedPrice)
        return offerDetails.formattedPrice
    }

    fun getPrice(productId: String?, typeIap: Int, offerToken: String?): String {
        return if (typeIap == TYPE_IAP.PURCHASE) {
            getPrice(productId)
        } else {
            getPriceSub(productId, offerToken)
        }
    }

    fun getPriceSub(productId: String?): String {
        val skuDetails = (if (productId != null) skuDetailsSubsMap[productId] else null) ?: return ""
        val subsDetail = skuDetails.subscriptionOfferDetails ?: return ""
        val pricingPhaseList = subsDetail[subsDetail.size - 1].pricingPhases.pricingPhaseList
        Log.e(TAG, "getPriceSub: " + pricingPhaseList[pricingPhaseList.size - 1].formattedPrice)
        return pricingPhaseList[pricingPhaseList.size - 1].formattedPrice
    }

    fun getPriceSub(productId: String?, offerToken: String?): String {
        val skuDetails = if (productId != null) skuDetailsSubsMap[productId] else null
        val subsDetail = skuDetails?.subscriptionOfferDetails ?: return getPriceSub(productId)

        for (offer in subsDetail) {
            if (offer.offerToken == offerToken) {
                val phases = offer.pricingPhases.pricingPhaseList
                if (phases.isNotEmpty()) {
                    // Return the first phase price (introductory price or free trial)
                    return phases[0].formattedPrice
                }
            }
        }
        return getPriceSub(productId)
    }

    /**
     * Get all available subscription offers for a product
     *
     * @param productId subscription product ID
     * @return list of SubscriptionOfferDetails or null if not found
     */
    fun getSubscriptionOffers(productId: String?): List<ProductDetails.SubscriptionOfferDetails>? {
        val skuDetails = (if (productId != null) skuDetailsSubsMap[productId] else null) ?: return null
        return skuDetails.subscriptionOfferDetails
    }

    /**
     * Get Price Pricing Phase List Subs
     *
     * @param productId
     * @return
     */
    fun getPricePricingPhaseList(productId: String?): List<ProductDetails.PricingPhase>? {
        val skuDetails = (if (productId != null) skuDetailsSubsMap[productId] else null) ?: return null
        val subsDetail = skuDetails.subscriptionOfferDetails ?: return null
        return subsDetail[subsDetail.size - 1].pricingPhases.pricingPhaseList
    }

    /**
     * Get Formatted Price by country
     * Get final price with id
     *
     * @param productId
     * @return
     */
    fun getIntroductorySubPrice(productId: String?): String {
        val skuDetails = (if (productId != null) skuDetailsSubsMap[productId] else null) ?: return ""
        return if (skuDetails.oneTimePurchaseOfferDetails != null) {
            skuDetails.oneTimePurchaseOfferDetails!!.formattedPrice
        } else if (skuDetails.subscriptionOfferDetails != null) {
            val subsDetail = skuDetails.subscriptionOfferDetails!!
            val pricingPhaseList = subsDetail[subsDetail.size - 1].pricingPhases.pricingPhaseList
            pricingPhaseList[pricingPhaseList.size - 1].formattedPrice
        } else {
            ""
        }
    }

    /**
     * Get Currency subs or IAP by country
     *
     * @param productId
     * @param typeIAP
     * @return
     */
    fun getCurrency(productId: String?, typeIAP: Int): String {
        return getCurrency(productId, typeIAP, "")
    }

    fun getCurrency(productId: String?, typeIAP: Int, offerToken: String?): String {
        val skuDetails = if (productId == null) {
            null
        } else if (typeIAP == TYPE_IAP.PURCHASE) {
            skuDetailsINAPMap[productId]
        } else {
            skuDetailsSubsMap[productId]
        }
        if (skuDetails == null) {
            return ""
        }
        return if (typeIAP == TYPE_IAP.PURCHASE) {
            skuDetails.oneTimePurchaseOfferDetails?.priceCurrencyCode ?: ""
        } else {
            val subsDetail = skuDetails.subscriptionOfferDetails
            if (subsDetail.isNullOrEmpty()) return ""

            var targetOffer = subsDetail[subsDetail.size - 1]
            if (!offerToken.isNullOrEmpty()) {
                for (offer in subsDetail) {
                    if (offer.offerToken == offerToken) {
                        targetOffer = offer
                        break
                    }
                }
            }

            val pricingPhaseList = targetOffer.pricingPhases.pricingPhaseList
            pricingPhaseList[0].priceCurrencyCode
        }
    }

    fun getSkuDetailsINAPMap(): Map<String, ProductDetails> {
        return skuDetailsINAPMap
    }

    fun getSkuDetailsSubsMap(): Map<String, ProductDetails> {
        return skuDetailsSubsMap
    }

    /**
     * Get Price Amount Micros subs or IAP
     * Get final price with id
     *
     * @param productId
     * @param typeIAP
     * @return
     */
    fun getPriceWithoutCurrency(productId: String?, typeIAP: Int): Double {
        return getPriceWithoutCurrency(productId, typeIAP, "")
    }

    fun getPriceWithoutCurrency(productId: String?, typeIAP: Int, offerToken: String?): Double {
        val skuDetails = if (productId == null) {
            null
        } else if (typeIAP == TYPE_IAP.PURCHASE) {
            skuDetailsINAPMap[productId]
        } else {
            skuDetailsSubsMap[productId]
        }
        if (skuDetails == null) {
            return 0.0
        }
        return if (typeIAP == TYPE_IAP.PURCHASE) {
            (skuDetails.oneTimePurchaseOfferDetails?.priceAmountMicros ?: 0L).toDouble()
        } else {
            val subsDetail = skuDetails.subscriptionOfferDetails
            if (subsDetail.isNullOrEmpty()) return 0.0

            var targetOffer = subsDetail[subsDetail.size - 1]
            if (!offerToken.isNullOrEmpty()) {
                for (offer in subsDetail) {
                    if (offer.offerToken == offerToken) {
                        targetOffer = offer
                        break
                    }
                }
            }
            val pricingPhaseList = targetOffer.pricingPhases.pricingPhaseList
            pricingPhaseList[0].priceAmountMicros.toDouble()
        }
    }

    /**
     * Format currency and price by country
     *
     * @param price
     * @param currency
     * @return
     */
    private fun formatCurrency(price: Double, currency: String): String {
        val format = NumberFormat.getCurrencyInstance()
        format.maximumFractionDigits = 0
        format.currency = Currency.getInstance(currency)
        return format.format(price)
    }

    /**
     * Check if a subscription has free trial offer
     *
     * @param productId subscription product ID
     * @return true if subscription has free trial, false otherwise
     */
    fun hasFreeTrial(productId: String?): Boolean {
        val skuDetails = if (productId != null) skuDetailsSubsMap[productId] else null
        val offerDetailsList = skuDetails?.subscriptionOfferDetails ?: return false

        for (offerDetails in offerDetailsList) {
            val pricingPhases = offerDetails.pricingPhases.pricingPhaseList
            // Check if first pricing phase is a free trial (price = 0)
            if (pricingPhases.size > 1 && pricingPhases[0].priceAmountMicros == 0L) {
                return true
            }
        }
        return false
    }

    /**
     * Get free trial period for a subscription
     *
     * @param productId subscription product ID
     * @return free trial period string (e.g., "P7D" for 7 days, "P1M" for 1 month), or empty string if no trial
     */
    fun getFreeTrialPeriod(productId: String?): String {
        val skuDetails = if (productId != null) skuDetailsSubsMap[productId] else null
        val offerDetailsList = skuDetails?.subscriptionOfferDetails ?: return ""

        for (offerDetails in offerDetailsList) {
            val pricingPhases = offerDetails.pricingPhases.pricingPhaseList
            // Check if first pricing phase is a free trial
            if (pricingPhases.size > 1 && pricingPhases[0].priceAmountMicros == 0L) {
                return pricingPhases[0].billingPeriod
            }
        }
        return ""
    }

    /**
     * Get formatted free trial period string
     *
     * @param productId subscription product ID
     * @return formatted string (e.g., "7 days free", "1 month free"), or empty string if no trial
     */
    fun getFormattedFreeTrialPeriod(productId: String?): String {
        var period = getFreeTrialPeriod(productId)
        if (period.isEmpty()) {
            return ""
        }

        // Parse ISO 8601 duration format (e.g., P7D, P1M, P1Y)
        if (period.startsWith("P")) {
            period = period.substring(1)
            if (period.endsWith("D")) {
                val days = period.replace("D", "").toInt()
                return days.toString() + if (days == 1) " day free" else " days free"
            } else if (period.endsWith("W")) {
                val weeks = period.replace("W", "").toInt()
                return weeks.toString() + if (weeks == 1) " week free" else " weeks free"
            } else if (period.endsWith("M")) {
                val months = period.replace("M", "").toInt()
                return months.toString() + if (months == 1) " month free" else " months free"
            } else if (period.endsWith("Y")) {
                val years = period.replace("Y", "").toInt()
                return years.toString() + if (years == 1) " year free" else " years free"
            }
        }
        return period
    }

    /**
     * Get the offer token for free trial subscription
     *
     * @param productId subscription product ID
     * @return offer token string, or null if no free trial available
     */
    fun getFreeTrialOfferToken(productId: String?): String? {
        val skuDetails = if (productId != null) skuDetailsSubsMap[productId] else null
        val offerDetailsList = skuDetails?.subscriptionOfferDetails ?: return null

        for (offerDetails in offerDetailsList) {
            val pricingPhases = offerDetails.pricingPhases.pricingPhaseList
            // Check if first pricing phase is a free trial
            if (pricingPhases.size > 1 && pricingPhases[0].priceAmountMicros == 0L) {
                return offerDetails.offerToken
            }
        }
        return null
    }

    /**
     * Purchase subscription with free trial if available
     * If free trial is not available, it will use the default subscription offer
     *
     * @param activity  current activity
     * @param productId subscription product ID
     */
    fun subscribeWithFreeTrial(activity: Activity, productId: String) {
        if (skuDetailsSubsMap.containsKey(productId)) {
            val offerToken = getFreeTrialOfferToken(productId)

            if (offerToken != null) {
                // Has free trial - use the free trial offer token
                subscribe(activity, productId, offerToken)
            } else {
                // No free trial - use default subscription
                subscribe(activity, productId)
            }
        } else {
            Log.e(TAG, "subscribeWithFreeTrial: Product not found - $productId")
        }
    }

    private var discount: Double = 1.0

    fun setDiscount(discount: Double) {
        this.discount = discount
    }

    fun getDiscount(): Double {
        return discount
    }

    private fun listIdToListProduct(
        listId: List<String>,
        styleBilling: String
    ): ArrayList<QueryProductDetailsParams.Product> {
        val listProduct = ArrayList<QueryProductDetailsParams.Product>()
        for (id in listId) {
            val product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(styleBilling)
                .build()
            listProduct.add(product)
        }
        return listProduct
    }

    @IntDef(TYPE_IAP.PURCHASE, TYPE_IAP.SUBSCRIPTION)
    @Retention(AnnotationRetention.SOURCE)
    annotation class TYPE_IAP {
        companion object {
            const val PURCHASE = 1
            const val SUBSCRIPTION = 2
        }
    }

    companion object {
        private const val TAG = "PurchaseEG"
        const val PRODUCT_ID_TEST = "android.test.purchased"

        @SuppressLint("StaticFieldLeak")
        private var instance: AppPurchase? = null

        private const val PREF_NAME_TRACKED_IAP = "nkh_iap_tracked_tokens"
        private const val KEY_TRACKED_TOKENS = "tracked_tokens"
        private const val KEY_CACHED_IS_PURCHASED = "cached_is_purchased"

        @JvmStatic
        fun getInstance(): AppPurchase {
            if (instance == null) {
                instance = AppPurchase()
            }
            return instance!!
        }
    }
}
