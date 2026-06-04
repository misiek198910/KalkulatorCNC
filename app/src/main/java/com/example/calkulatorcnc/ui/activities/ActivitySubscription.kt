package com.example.calkulatorcnc.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.BillingManager
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.google.android.gms.ads.*
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri

class ActivitySubscription : AppCompatActivity(), BillingManager.BillingManagerListener {

    private var billingManager: BillingManager? = null
    private lateinit var statusTextView: TextView

    // NOWE PRZYCISKI
    private lateinit var buyMonthlyButton: Button
    private lateinit var buyYearlyButton: Button
    private lateinit var trialInfoText: TextView

    private lateinit var restoreButton: Button
    private lateinit var adContainerLayout: FrameLayout
    private lateinit var adContainer: FrameLayout
    private var adView: AdView? = null
    private lateinit var analytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_subscription)

        createViewAEdgetoEdgeForAds()
        initUI()
        setupBilling()
        analytics = Firebase.analytics
    }

    private fun createViewAEdgetoEdgeForAds() {
        val mainRoot = findViewById<View>(R.id.main)
        val customHeader = findViewById<View>(R.id.customHeader)
        val adLayout = findViewById<FrameLayout>(R.id.adContainerLayout)

        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            mainRoot.setPadding(0, 0, 0, 0)
            customHeader?.updatePadding(top = systemBars.top)

            adLayout?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
                leftMargin = systemBars.left
                rightMargin = systemBars.right
            }
            insets
        }
    }

    private fun initUI() {
        statusTextView = findViewById(R.id.subscription_status_text)

        // Inicjalizacja nowych elementów z XML
        buyMonthlyButton = findViewById(R.id.buy_monthly_button)
        buyYearlyButton = findViewById(R.id.buy_yearly_button)
        trialInfoText = findViewById(R.id.trial_info_text)

        restoreButton = findViewById(R.id.restore_purchases_button)
        adContainerLayout = findViewById(R.id.adContainerLayout)
        adContainer = findViewById(R.id.adContainer)

        findViewById<ImageButton>(R.id.button_back).setOnClickListener { finish() }

        // Click listenery dla obu planów
        buyMonthlyButton.setOnClickListener {
            handleBuyButtonClick(BillingManager.SKU_REMOVE_ADS_MONTH)
        }

        buyYearlyButton.setOnClickListener {
            handleBuyButtonClick(BillingManager.BASE_PLAN_YEARLY_TRIAL)
        }

        restoreButton.setOnClickListener {
            billingManager?.queryPurchasesAsync()
            Toast.makeText(this, getString(R.string.buy_back), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBilling() {
        billingManager = SubscriptionManager.getInstance(this).billingManager
        billingManager?.setListener(this)

        billingManager?.isPremium?.observe(this) { hasPremium ->
            updateUI(hasPremium)
            handleAds(hasPremium)
        }

        billingManager?.productDetails?.observe(this) { details ->
            val isPremium = billingManager?.isPremium?.value ?: false

            if (!isPremium && details != null) {
                // Pobieramy sformatowane ceny z managera dla obu planów
                buyMonthlyButton.text = billingManager?.getPlanOfferInfo(this, details, BillingManager.SKU_REMOVE_ADS_MONTH)
                buyYearlyButton.text = billingManager?.getPlanOfferInfo(this, details, BillingManager.BASE_PLAN_YEARLY_TRIAL)

                buyMonthlyButton.isEnabled = true
                buyYearlyButton.isEnabled = true
                buyYearlyButton.visibility = View.VISIBLE
                trialInfoText.visibility = View.VISIBLE
            } else if (isPremium) {
                // Jeśli premium jest aktywne, zostawiamy jeden przycisk do zarządzania
                buyMonthlyButton.text = getString(R.string.settings_subs)
                buyMonthlyButton.isEnabled = true
                buyYearlyButton.visibility = View.GONE
                trialInfoText.visibility = View.GONE
            } else {
                buyMonthlyButton.isEnabled = false
                buyYearlyButton.isEnabled = false
                buyMonthlyButton.text = getString(R.string.load_data)
                buyYearlyButton.text = getString(R.string.load_data)
            }
        }
    }

    private fun handleBuyButtonClick(basePlanId: String) {
        val isPremium = billingManager?.isPremium?.value ?: false

        if (isPremium) {
            // Przekierowanie do Google Play w celu zarządzania
            val url = "https://play.google.com/store/account/subscriptions?package=$packageName"
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } else {
            val details = billingManager?.productDetails?.value
            if (details != null) {
                // Wywołujemy zakup dla konkretnego planu
                billingManager?.launchPurchaseFlow(this, details, basePlanId)
            } else {
                Toast.makeText(this, getString(R.string.load_data), Toast.LENGTH_SHORT).show()
                billingManager?.queryProductDetails()
            }
        }
    }

    private fun updateUI(hasSubscription: Boolean) {
        if (hasSubscription) {
            statusTextView.text = getString(R.string.subs_active)
            statusTextView.setTextColor("#4CAF50".toColorInt())
        } else {
            statusTextView.text = getString(R.string.subs_deactive)
            statusTextView.setTextColor("#FF5252".toColorInt())
        }
    }

    // --- Reszta metod (handleAds, setupAds, onPurchaseAcknowledged itd.) pozostaje bez zmian ---

    private fun handleAds(isPremium: Boolean) {
        if (isPremium) {
            adView?.destroy()
            adView = null
            adContainer.removeAllViews()
            adContainerLayout.visibility = View.GONE
        } else {
            adContainerLayout.visibility = View.VISIBLE
            adContainer.post { if (adView == null) setupAds() }
        }
    }

    private fun setupAds() {
        val adBannerId = BuildConfig.ADMOB_BANNER_ID
        if (adBannerId == "BRAK_ID" || adBannerId.isEmpty()) {
            adContainerLayout.visibility = View.GONE
            return
        }

        SubscriptionManager.getInstance(this).isPremium.observe(this) { isPremium ->
            if (isPremium) {
                adContainerLayout.visibility = View.GONE
                adContainer.removeAllViews()
                adView?.destroy()
                adView = null
            } else {
                if (resources.configuration.screenHeightDp < 400) {
                    adContainerLayout.visibility = View.GONE
                } else {
                    adContainerLayout.visibility = View.VISIBLE
                    if (adView == null) {
                        val adSize = getAdSize(adContainer)
                        val newAdView = AdView(this).apply {
                            setAdUnitId(adBannerId)
                            setAdSize(adSize)
                        }
                        adView = newAdView
                        adContainer.removeAllViews()
                        adContainer.addView(newAdView)
                        newAdView.loadAd(AdRequest.Builder().build())
                    }
                }
            }
        }
    }

    private fun getAdSize(adContainer: FrameLayout): AdSize {
        val displayMetrics = resources.displayMetrics
        var adWidthPixels = adContainer.width.toFloat()
        if (adWidthPixels == 0f) adWidthPixels = displayMetrics.widthPixels.toFloat()
        val adWidth = (adWidthPixels / displayMetrics.density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }

    override fun onPurchaseAcknowledged() {
        runOnUiThread {
            Toast.makeText(this, getString(R.string.subs_on), Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onPurchaseError(error: String?) {
        runOnUiThread { Toast.makeText(this, "Błąd: $error", Toast.LENGTH_SHORT).show() }
    }

    override fun onPause() { adView?.pause(); super.onPause() }
    override fun onResume() {
        super.onResume(); adView?.resume()
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, "Subskrypcje")
            param(FirebaseAnalytics.Param.SCREEN_CLASS, "ActivitySubscription")
        }
    }
    override fun onDestroy() {
        adView?.destroy()
        billingManager?.setListener(null)
        super.onDestroy()
    }
}