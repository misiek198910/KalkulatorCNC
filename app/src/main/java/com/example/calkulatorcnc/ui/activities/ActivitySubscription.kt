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

class ActivitySubscription : AppCompatActivity(), BillingManager.BillingManagerListener {

    private var billingManager: BillingManager? = null
    private lateinit var statusTextView: TextView
    private lateinit var buyButton: Button
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
        val adContainerLayout = findViewById<FrameLayout>(R.id.adContainerLayout) // Zmieniamy na Layout

        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            mainRoot.setPadding(0, 0, 0, 0)
            customHeader?.updatePadding(top = systemBars.top)

            // KLUCZ: Margines ustawiamy na CAŁY kontener reklamy (rodzica)
            adContainerLayout?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
                leftMargin = systemBars.left
                rightMargin = systemBars.right
            }

            insets
        }
    }

    private fun initUI() {
        statusTextView = findViewById(R.id.subscription_status_text)
        buyButton = findViewById(R.id.buy_subscription_button)
        restoreButton = findViewById(R.id.restore_purchases_button)
        adContainerLayout = findViewById(R.id.adContainerLayout)
        adContainer = findViewById(R.id.adContainer)

        // Obsługa nowego przycisku powrotu w customHeader
        findViewById<ImageButton>(R.id.button_back).setOnClickListener { finish() }

        buyButton.setOnClickListener { handleBuyButtonClick() }

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
                // KLUCZOWA ZMIANA: Korzystamy z nowej funkcji managera
                // Funkcja sama sprawdzi czy jest trial i zwróci odpowiedni tekst
                val offerInfo = billingManager?.getSubscriptionOfferInfo(this, details)

                buyButton.text = offerInfo
                buyButton.isEnabled = true
            } else if (isPremium) {
                buyButton.text = getString(R.string.settings_subs)
                buyButton.isEnabled = true
            } else {
                buyButton.isEnabled = false
                buyButton.text = getString(R.string.load_data)
            }
        }
    }
    private fun updateUI(hasSubscription: Boolean) {
        if (hasSubscription) {
            statusTextView.text = getString(R.string.subs_active)
            // Jasny zielony, dobrze widoczny na ciemnym tle
            statusTextView.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        } else {
            statusTextView.text = getString(R.string.subs_deactive)
            // Jasny czerwony/koralowy
            statusTextView.setTextColor(android.graphics.Color.parseColor("#FF5252"))
        }
    }

    private fun handleAds(isPremium: Boolean) {
        if (isPremium) {
            adView?.destroy()
            adView = null
            adContainer.removeAllViews()
            adContainerLayout.visibility = View.GONE
        } else {
            adContainerLayout.visibility = View.VISIBLE
            // Używamy post, aby upewnić się, że szerokość kontenera jest już obliczona
            adContainer.post {
                if (adView == null) {
                    setupAds()
                }
            }
        }
    }

    private fun setupAds() {
        val adBannerId = BuildConfig.ADMOB_BANNER_ID

        // 1. Zabezpieczenie przed brakiem ID
        if (adBannerId == "BRAK_ID" || adBannerId.isEmpty()) {
            findViewById<View>(R.id.adContainerLayout)?.visibility = View.GONE
            return
        }

        val adContainerLayout = findViewById<FrameLayout>(R.id.adContainerLayout) ?: return
        val adContainer = findViewById<FrameLayout>(R.id.adContainer) ?: return

        // 2. Obserwacja stanu subskrypcji
        SubscriptionManager.getInstance(this).isPremium.observe(this) { isPremium ->
            if (isPremium) {
                // Logika PREMIUM: czyścimy i usuwamy wszystko
                adContainerLayout.visibility = View.GONE
                adContainer.removeAllViews()
                adView?.destroy()
                adView = null
            } else {
                // Logika FREE: sprawdzamy wysokość ekranu
                if (resources.configuration.screenHeightDp < 400) {
                    adContainerLayout.visibility = View.GONE
                } else {
                    adContainerLayout.visibility = View.VISIBLE

                    // Ładujemy reklamę tylko jeśli jeszcze nie istnieje
                    if (adView == null) {
                        // Wywołujemy dopasowaną funkcję rozmiaru
                        val adSize = getAdSize(adContainer)

                        val newAdView = AdView(this).apply {
                            setAdUnitId(adBannerId)
                            setAdSize(adSize)
                        }

                        adView = newAdView
                        adContainer.removeAllViews() // Czyścimy na wypadek duplikacji
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
        if (adWidthPixels == 0f) {
            adWidthPixels = displayMetrics.widthPixels.toFloat()
        }
        val density = displayMetrics.density
        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }

    private fun handleBuyButtonClick() {
        val isPremium = billingManager?.isPremium?.value ?: false

        if (isPremium) {
            val url = "https://play.google.com/store/account/subscriptions?package=$packageName"
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
            finish()
        }
    }

    override fun onPurchaseError(error: String?) {
        runOnUiThread {
            Toast.makeText(this, "Błąd: $error", Toast.LENGTH_SHORT).show()
        }
    }
    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onPause() { adView?.pause(); super.onPause() }

    override fun onResume() {
        super.onResume(); adView?.resume()
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, "Subskrypcje")
            param(FirebaseAnalytics.Param.SCREEN_CLASS, "ActivitySubscription")
        }}

    override fun onDestroy() {
        adView?.destroy()
        billingManager?.setListener(null)
        super.onDestroy()
    }
}