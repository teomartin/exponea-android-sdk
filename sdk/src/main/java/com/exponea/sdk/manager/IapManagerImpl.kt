package com.exponea.sdk.manager

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.DeviceProperties
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.PurchasedItem
import com.exponea.sdk.util.Logger

/**
 * In-App Purchase class handles all the purchases made inside the
 * app using the Google Play Store listener.
 * After capture the purchased item it will be send to the database
 * in order to be flushed and send to the Exponea API.
 *
 * @param context Application Context
 */
internal class IapManagerImpl(context: Context) : IapManager, PurchasesUpdatedListener {

    private val billingClient: BillingClient by lazy {  BillingClient.newBuilder(context).setListener(this).build() }
    private val device = DeviceProperties()
    private val skuList: ArrayList<SkuDetails> = ArrayList()

    /**
     * Starts the connection and implement the billing listener.
     */
    override fun configure(skuList: List<String>) {
        // Starts up BillingClient setup process asynchronously.
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(@BillingResponse billingResponseCode: Int) {
                // Store the products at the Play Store using the skuList for future use when
                // user purchase an item
                getAvailableProducts(skuList)
                Logger.d(this, "Billing service was initiated")
            }

            override fun onBillingServiceDisconnected() {
                Logger.d(this, "Billing service was disconnected")
            }
        })
    }

    /**
     * Check if the listener was successfully started.
     */
    override fun startObservingPayments() {
        // Checks if the client is currently connected to the service.
        if (billingClient.isReady) {
            Logger.d(this, "Billing client was successfully started")
        } else {
            Logger.e(this, "Billing client was not properly started")
        }
    }

    /**
     * Close the connection and release all held resources such as service connections.
     */
    override fun stopObservingPayments() {
        billingClient.endConnection()
    }

    /**
     * Receive the purchased item and send it to the database.
     */
    override fun trackPurchase(properties: HashMap<String, Any>) {
        Exponea.track(
                eventType = "payment",
                properties = properties,
                type = EventType.PAYMENT
        )
    }

    override fun getAvailableProducts(skuList: List<String>) {
        billingClient.let { bc ->
            val params = SkuDetailsParams.newBuilder().apply {
                setType(BillingClient.SkuType.INAPP)
                setSkusList(skuList)
            }
            bc.querySkuDetailsAsync(params.build()) { responseCode, skuDetailsList ->
                if (responseCode == BillingClient.BillingResponse.OK && skuDetailsList != null) {
                    this.skuList.addAll(skuDetailsList)
                }
            }
        }
    }

    override fun onPurchasesUpdated(
            @BillingResponse responseCode: Int,
            purchases: List<Purchase>?
    ) {
        if (responseCode == BillingResponse.OK && purchases != null) {
            for (purchase in purchases) {

                val sku = skuList.find { it.sku == purchase.sku }

                sku?.let {
                    val product = PurchasedItem(
                            value = it.price.toDouble(),
                            currency = it.priceCurrencyCode,
                            paymentSystem = Constants.General.GooglePlay,
                            productId = it.sku,
                            productTitle = it.title,
                            receipt = null,
                            deviceModel = device.deviceModel,
                            deviceType = device.deviceType,
                            ip = null,
                            osName = device.osName,
                            osVersion = device.osVersion,
                            sdk = device.sdk,
                            sdkVersion = device.sdkVersion
                    )
                    trackPurchase(product.toHashMap())
                }

            }
        } else if (responseCode == BillingResponse.USER_CANCELED) {
            Logger.w(this, "User has canceled the purchased item.")
        } else {
            Logger.e(this, "Could not load the purchase item.")
        }
    }
}
