package com.ads.nkh.billing

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import com.ads.nkh.R
import com.ads.nkh.funtion.PurchaseListener
import com.ads.nkh.util.AppUtil
import com.android.billingclient.api.ProductDetails
import com.google.android.material.bottomsheet.BottomSheetDialog

class PurchaseDevBottomSheet(
    private val typeIap: Int,
    private val productDetails: ProductDetails?,
    context: Context,
    private val purchaseListener: PurchaseListener?
) : BottomSheetDialog(context) {

    private var txtTitle: TextView? = null
    private var txtDescription: TextView? = null
    private var txtId: TextView? = null
    private var txtPrice: TextView? = null
    private var txtContinuePurchase: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_billing_test)
        txtTitle = findViewById(R.id.txtTitle)
        txtDescription = findViewById(R.id.txtDescription)
        txtId = findViewById(R.id.txtId)
        txtPrice = findViewById(R.id.txtPrice)
        txtContinuePurchase = findViewById(R.id.txtContinuePurchase)
        if (productDetails == null) {
            if (AppUtil.VARIANT_DEV) {
                txtContinuePurchase?.setOnClickListener {
                    AppPurchase.getInstance().setPurchase(true)
                    purchaseListener?.onProductPurchased(
                        "android.test.purchased",
                        "{\"productId\":\"android.test.purchased\",\"type\":\"inapp\",\"title\":\"Tiêu đề mẫu\",\"description\":\"Mô tả mẫu về sản phẩm: android.test.purchased.\",\"skuDetailsToken\":\"AEuhp4Izz50wTvd7YM9wWjPLp8hZY7jRPhBEcM9GAbTYSdUM_v2QX85e8UYklstgqaRC\",\"oneTimePurchaseOfferDetails\":{\"priceAmountMicros\":23207002450,\"priceCurrencyCode\":\"VND\",\"formattedPrice\":\"23.207 ₫\"}}', parsedJson={\"productId\":\"android.test.purchased\",\"type\":\"inapp\",\"title\":\"Tiêu đề mẫu\",\"description\":\"Mô tả mẫu về sản phẩm: android.test.purchased.\",\"skuDetailsToken\":\"AEuhp4Izz50wTvd7YM9wWjPLp8hZY7jRPhBEcM9GAbTYSdUM_v2QX85e8UYklstgqaRC\",\"oneTimePurchaseOfferDetails\":{\"priceAmountMicros\":23207002450,\"priceCurrencyCode\":\"VND\",\"formattedPrice\":\"23.207 ₫\"}}, productId='android.test.purchased', productType='inapp', title='Tiêu đề mẫu', productDetailsToken='AEuhp4Izz50wTvd7YM9wWjPLp8hZY7jRPhBEcM9GAbTYSdUM_v2QX85e8UYklstgqaRC', subscriptionOfferDetails=null}"
                    )
                    dismiss()
                }
            } else {
                txtContinuePurchase?.setOnClickListener {
                    dismiss()
                }
            }
        } else {
            txtTitle?.text = productDetails.title
            txtDescription?.text = productDetails.description
            txtId?.text = productDetails.productId
            var price = "$2.00"
            if (typeIap == AppPurchase.TYPE_IAP.PURCHASE) {
                val oneTimePurchaseOfferDetails = productDetails.oneTimePurchaseOfferDetails
                if (oneTimePurchaseOfferDetails != null) {
                    price = oneTimePurchaseOfferDetails.formattedPrice
                }
                txtPrice?.text = price
            } else {
                val subscriptionOfferDetails = productDetails.subscriptionOfferDetails
                if (!subscriptionOfferDetails.isNullOrEmpty()) {
                    price = subscriptionOfferDetails[0].pricingPhases.pricingPhaseList[0].formattedPrice
                }
                txtPrice?.text = price
            }

            txtContinuePurchase?.setOnClickListener {
                AppPurchase.getInstance().setPurchase(true)
                purchaseListener?.onProductPurchased(
                    productDetails.productId,
                    "{\"productId\":\"android.test.purchased\",\"type\":\"inapp\",\"title\":\"Tiêu đề mẫu\",\"description\":\"Mô tả mẫu về sản phẩm: android.test.purchased.\",\"skuDetailsToken\":\"AEuhp4Izz50wTvd7YM9wWjPLp8hZY7jRPhBEcM9GAbTYSdUM_v2QX85e8UYklstgqaRC\",\"oneTimePurchaseOfferDetails\":{\"priceAmountMicros\":23207002450,\"priceCurrencyCode\":\"VND\",\"formattedPrice\":\"23.207 ₫\"}}', parsedJson={\"productId\":\"android.test.purchased\",\"type\":\"inapp\",\"title\":\"Tiêu đề mẫu\",\"description\":\"Mô tả mẫu về sản phẩm: android.test.purchased.\",\"skuDetailsToken\":\"AEuhp4Izz50wTvd7YM9wWjPLp8hZY7jRPhBEcM9GAbTYSdUM_v2QX85e8UYklstgqaRC\",\"oneTimePurchaseOfferDetails\":{\"priceAmountMicros\":23207002450,\"priceCurrencyCode\":\"VND\",\"formattedPrice\":\"23.207 ₫\"}}, productId='android.test.purchased', productType='inapp', title='Tiêu đề mẫu', productDetailsToken='AEuhp4Izz50wTvd7YM9wWjPLp8hZY7jRPhBEcM9GAbTYSdUM_v2QX85e8UYklstgqaRC', subscriptionOfferDetails=null}"
                )
                dismiss()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val w = ViewGroup.LayoutParams.MATCH_PARENT
        val h = ViewGroup.LayoutParams.WRAP_CONTENT
        window?.setLayout(w, h)
    }
}
