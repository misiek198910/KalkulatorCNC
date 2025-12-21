package com.example.calkulatorcnc.billing

import android.content.Context
import androidx.lifecycle.LiveData

class SubscriptionManager private constructor(context: Context) {

    val billingManager: BillingManager = BillingManager.getInstance(context)

    // 1. Dodajemy brakującą obserwację LiveData
    val isPremium: LiveData<Boolean> = billingManager.isPremium

    // 2. Dodajemy status (już był, zostawiamy go)
    val subscriptionStatus: LiveData<SubscriptionStatus> = billingManager.subscriptionStatus

    val productDetails = billingManager.productDetails

    // 3. Opcjonalnie: Szybki dostęp do samej wartości (bez obserwatora)
    val isPremiumValue: Boolean
        get() = billingManager.isPremium.value ?: false

    companion object {
        @Volatile
        private var INSTANCE: SubscriptionManager? = null

        fun getInstance(context: Context): SubscriptionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SubscriptionManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}