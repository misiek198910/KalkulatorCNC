package com.example.calkulatorcnc.billing

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.data.db.AppDatabase
import com.example.calkulatorcnc.entity.SubscriptionEntity
import kotlinx.coroutines.*

class BillingManager private constructor(context: Context) {
    private val billingClient: BillingClient
    private val database = AppDatabase.getDatabase(context.applicationContext)
    private val dao = database.subscriptionDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isPremium = MutableLiveData(false)
    val isPremium: LiveData<Boolean> = _isPremium

    private val _subscriptionStatus = MutableLiveData(SubscriptionStatus.CHECKING)
    val subscriptionStatus: LiveData<SubscriptionStatus> = _subscriptionStatus

    private val _productDetails = MutableLiveData<ProductDetails?>()
    val productDetails: LiveData<ProductDetails?> = _productDetails

    interface BillingManagerListener {
        fun onPurchaseAcknowledged()
        fun onPurchaseError(error: String?)
    }

    private var listener: BillingManagerListener? = null
    fun setListener(listener: BillingManagerListener?) { this.listener = listener }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) { handlePurchase(purchase) }
        } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
            listener?.onPurchaseError("Anulowano zakup.")
        } else {
            listener?.onPurchaseError("Błąd zakupu. Kod: ${billingResult.responseCode}")
        }
    }

    init {
        val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .enablePrepaidPlans()
            .build()

        billingClient = BillingClient.newBuilder(context.applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(pendingPurchasesParams)
            .build()

        connectToGooglePlay()

        scope.launch {
            val status = dao.getStatus()
            val isFull = status?.isPremium ?: false
            _isPremium.postValue(isFull)
            _subscriptionStatus.postValue(if (isFull) SubscriptionStatus.PREMIUM else SubscriptionStatus.NON_PREMIUM)
        }
    }

    private fun connectToGooglePlay() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    queryPurchasesAsync()
                    queryProductDetails()
                } else {
                    _subscriptionStatus.postValue(SubscriptionStatus.NON_PREMIUM)
                }
            }
            override fun onBillingServiceDisconnected() { connectToGooglePlay() }
        })
    }

    fun queryPurchasesAsync() {
        if (!billingClient.isReady) return
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                var hasPremium = false
                var token: String? = null
                purchases.forEach { purchase ->
                    if (purchase.products.contains(SKU_REMOVE_ADS) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        hasPremium = true
                        token = purchase.purchaseToken
                        if (!purchase.isAcknowledged) handlePurchase(purchase)
                    }
                }
                updateLocalStatus(hasPremium, token)
            }
        }
    }

    private fun updateLocalStatus(hasPremium: Boolean, token: String?) {
        scope.launch {
            dao.insert(SubscriptionEntity(isPremium = hasPremium, purchaseToken = token))
            _isPremium.postValue(hasPremium)
            _subscriptionStatus.postValue(if (hasPremium) SubscriptionStatus.PREMIUM else SubscriptionStatus.NON_PREMIUM)
        }
    }

    fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_REMOVE_ADS)
                .setProductType(ProductType.SUBS)
                .build()
        )
        billingClient.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(productList).build()) { _, details ->
            if (!details.isNullOrEmpty()) _productDetails.postValue(details[0])
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetailsToPurchase: ProductDetails) {
        // 1. Szukamy oferty powiązanej z Twoim nowym planem automatycznym
        val offerDetails = productDetailsToPurchase.subscriptionOfferDetails?.find {
            it.basePlanId == "cnc-premium-auto"
        } ?: productDetailsToPurchase.subscriptionOfferDetails?.firstOrNull() // Fallback do czegokolwiek, żeby nie zablokować zakupu

        if (offerDetails == null) {
            listener?.onPurchaseError("Nie znaleziono ofert subskrypcji.")
            return
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetailsToPurchase)
                    .setOfferToken(offerDetails.offerToken) // Ten token zawiera informację o trialu!
                    .build()
            )).build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            billingClient.acknowledgePurchase(params) { result ->
                if (result.responseCode == BillingResponseCode.OK) {
                    updateLocalStatus(true, purchase.purchaseToken)
                    listener?.onPurchaseAcknowledged()
                }
            }
        } else if (purchase.isAcknowledged) {
            updateLocalStatus(true, purchase.purchaseToken)
        }
    }

    fun getSubscriptionOfferInfo(context: Context, productDetails: ProductDetails?): String {
        // Szukamy Twojego nowego planu
        val offer = productDetails?.subscriptionOfferDetails?.find { it.basePlanId == "cnc-premium-auto" }

        // Szukamy fazy darmowej (Trial)
        val trialPhase = offer?.pricingPhases?.pricingPhaseList?.find { it.priceAmountMicros == 0L }
        // Szukamy fazy płatnej (Cena bazowa)
        val basePhase = offer?.pricingPhases?.pricingPhaseList?.lastOrNull()

        return when {
            trialPhase != null && basePhase != null -> {
                // "Zacznij 7 dni za darmo, potem [CENA] / rok"
                context.getString(R.string.subs_trial_button, basePhase.formattedPrice)
            }
            basePhase != null -> {
                // "Subskrypcja roczna: [CENA]"
                context.getString(R.string.subs_annual_button, basePhase.formattedPrice)
            }
            else -> context.getString(R.string.subs_buy_button)
        }
    }

    companion object {
        @Volatile private var INSTANCE: BillingManager? = null
        fun getInstance(context: Context): BillingManager = INSTANCE ?: synchronized(this) {
            INSTANCE ?: BillingManager(context).also { INSTANCE = it }
        }
        const val SKU_REMOVE_ADS = "remove_ads_for_year"
    }
}