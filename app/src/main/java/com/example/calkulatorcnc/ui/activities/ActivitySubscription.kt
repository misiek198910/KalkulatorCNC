package com.example.calkulatorcnc.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.BillingManager
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.google.android.gms.ads.*

class ActivitySubscription : AppCompatActivity(), BillingManager.BillingManagerListener {

    private var billingManager: BillingManager? = null
    private lateinit var statusTextView: TextView
    private lateinit var buyButton: Button
    private lateinit var restoreButton: Button
    private lateinit var btnBack: ImageButton
    private lateinit var adContainerLayout: FrameLayout
    private lateinit var adContainer: FrameLayout
    private var adView: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_subscription)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViews()
        setupBilling()
    }

    private fun setupViews() {
        statusTextView = findViewById(R.id.subscription_status_text)
        buyButton = findViewById(R.id.buy_subscription_button)
        restoreButton = findViewById(R.id.restore_purchases_button)
        btnBack = findViewById(R.id.btnBack)
        adContainerLayout = findViewById(R.id.adContainerLayout)
        adContainer = findViewById(R.id.adContainer)

        btnBack.setOnClickListener { finish() }

        buyButton.setOnClickListener { handleBuyButtonClick() }

        restoreButton.setOnClickListener {
            billingManager?.queryPurchasesAsync()
            Toast.makeText(this, getString(R.string.status), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBilling() {
        // Korzystamy z managera, aby pobrać billingManager
        billingManager = SubscriptionManager.getInstance(this).billingManager
        billingManager?.setListener(this)

        // Obserwowanie statusu Premium
        billingManager?.isPremium?.observe(this) { hasPremium ->
            updateUI(hasPremium)
            handleAds(hasPremium)
        }

        // Obserwowanie szczegółów produktu (cena)
        billingManager?.productDetails?.observe(this) { details ->
            val isPremium = billingManager?.isPremium?.value ?: false

            if (!isPremium && details != null) {
                val offerDetails = details.subscriptionOfferDetails?.firstOrNull()
                val pricingPhase = offerDetails?.pricingPhases?.pricingPhaseList?.firstOrNull()
                val price = pricingPhase?.formattedPrice ?: "..."

                buyButton.text = "${getString(R.string.text_info2)} ($price)"
                buyButton.isEnabled = true
            } else if (isPremium) {
                buyButton.text = getString(R.string.settings_subs)
                buyButton.isEnabled = true
            } else {
                buyButton.isEnabled = false
            }
        }
    }

    private fun updateUI(hasSubscription: Boolean) {
        if (hasSubscription) {
            statusTextView.text = getString(R.string.subs_active)
            statusTextView.setTextColor(getColor(android.R.color.holo_green_light))
        } else {
            statusTextView.text = getString(R.string.subs_deactive)
            statusTextView.setTextColor(getColor(android.R.color.darker_gray))
        }
    }

    private fun handleAds(isPremium: Boolean) {
        if (isPremium) {
            adView?.destroy()
            adView = null
            adContainer.removeAllViews()
            adContainerLayout.visibility = View.GONE
        } else {
            if (adView == null) {
                adContainerLayout.visibility = View.VISIBLE
                loadBannerAd()
            }
        }
    }

    private fun loadBannerAd() {
        val adBannerId = BuildConfig.ADMOB_BANNER_ID
        if (adBannerId == "BRAK_ID" || adBannerId.isEmpty()) {
            adContainerLayout.visibility = View.GONE
            return
        }

        val newAdView = AdView(this)

        // POPRAWKA: Wywołujemy funkcję getAdSize() z nawiasami
        newAdView.setAdUnitId(adBannerId)
        newAdView.setAdSize(getAdSize())

        this.adView = newAdView

        adContainer.removeAllViews()
        adContainer.addView(newAdView)

        val adRequest = AdRequest.Builder().build()
        newAdView.loadAd(adRequest)
    }

    // Funkcja obliczająca rozmiar reklamy adaptacyjnej
    private fun getAdSize(): AdSize {
        val displayMetrics = resources.displayMetrics
        val widthPixels = if (adContainer.width > 0) adContainer.width else displayMetrics.widthPixels
        val adWidth = (widthPixels / displayMetrics.density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }

    private fun handleBuyButtonClick() {
        val isPremium = billingManager?.isPremium?.value ?: false

        if (isPremium) {
            // Zarządzanie subskrypcją w Google Play
            val sku = BillingManager.SKU_REMOVE_ADS
            val url = "https://play.google.com/store/account/subscriptions?sku=$sku&package=$packageName"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } else {
            val details = billingManager?.productDetails?.value
            if (details != null) {
                billingManager?.launchPurchaseFlow(this, details)
            } else {
                Toast.makeText(this, getString(R.string.load_data), Toast.LENGTH_SHORT).show()
                billingManager?.queryProductDetails()
            }
        }
    }

    override fun onPurchaseAcknowledged() {
        runOnUiThread {
            Toast.makeText(this, getString(R.string.subs_on), Toast.LENGTH_LONG).show()
        }
    }

    override fun onPurchaseError(error: String?) {
        runOnUiThread {
            Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() { adView?.pause(); super.onPause() }
    override fun onResume() { super.onResume(); adView?.resume() }
    override fun onDestroy() {
        adView?.destroy()
        billingManager?.setListener(null)
        super.onDestroy()
    }
}