package com.example.calkulatorcnc.ui.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.data.preferences.ClassPrefs
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import java.util.concurrent.atomic.AtomicBoolean
import androidx.core.view.isEmpty
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {

    private lateinit var viewRedDot: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        viewRedDot = findViewById(R.id.viewRedDot)
        createViewAEdgetoEdgeForAds()
        checkFirstStartAndLocale()
        setupClickListeners()
        checkForNewNews()
    }

    private fun createViewAEdgetoEdgeForAds() {

        val mainRoot = findViewById<View>(R.id.main)
        val gridLayout = findViewById<android.widget.GridLayout>(R.id.mainGrid)
        val adContainerLayout = findViewById<FrameLayout>(R.id.adContainerLayout)
        val adContainer = findViewById<FrameLayout>(R.id.adContainer)

        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            mainRoot.setPadding(0, 0, 0, 0)
            gridLayout.updatePadding(
                top = systemBars.top + (resources.displayMetrics.density * 12).toInt()
            )
            adContainerLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
                leftMargin = systemBars.left
                rightMargin = systemBars.right
            }
            insets
        }

        adContainer.postDelayed({
            setupAds()
        }, 100)
    }

    private fun setupAds() {
        val adContainer = findViewById<FrameLayout>(R.id.adContainer)
        val subManager = SubscriptionManager.getInstance(this)

        subManager.isPremium.observe(this) { isPremium ->
            if (isPremium) {
                adContainer.visibility = View.GONE
                adContainer.removeAllViews()
            } else {
                val screenHeightDp = resources.configuration.screenHeightDp
                if (screenHeightDp < 400) {
                    adContainer.visibility = View.GONE
                } else {
                    adContainer.visibility = View.VISIBLE
                    if (adContainer.isEmpty()) {
                        // OBLICZANIE ROZMIARU ADAPTACYJNEGO
                        val adView = AdView(this)
                        adContainer.addView(adView)

                        val adSize = getAdSize(adContainer)
                        adView.setAdSize(adSize)
                        adView.adUnitId = BuildConfig.ADMOB_BANNER_ID

                        val adRequest = AdRequest.Builder().build()
                        adView.loadAd(adRequest)
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

    private fun setupClickListeners() {
        findViewById<View>(R.id.card_calc).setOnClickListener {
            startActivity(Intent(this, ActivityMilling::class.java))
        }

        findViewById<View>(R.id.card_tools).setOnClickListener {
            startActivity(Intent(this, ActivityTourning::class.java))
        }

        findViewById<View>(R.id.card_fits).setOnClickListener {
            startActivity(Intent(this, ActivityTools::class.java))
        }

        findViewById<View>(R.id.card_other).setOnClickListener {
            startActivity(Intent(this, ActivityTables::class.java))
        }

        findViewById<View>(R.id.card_settings).setOnClickListener {
            startActivity(Intent(this, ActivitySettings::class.java))
        }

        findViewById<View>(R.id.card_notifications).setOnClickListener {
            val pref = ClassPrefs()

            findViewById<View>(R.id.card_notifications).setOnClickListener {
                // 1. Ukrywamy kropkę po wejściu w newsy
                viewRedDot.visibility = View.GONE

                // 2. Zapisujemy aktualny czas jako moment ostatniego "przejrzenia"
                pref.savePrefString(this, "last_seen_news_date", System.currentTimeMillis().toString())

                // 3. Otwieramy aktywność (upewnij się, że nazwa jest poprawna w projekcie)
            startActivity(Intent(this, ActivityNews::class.java))
        }

        findViewById<View>(R.id.card_no_ads).setOnClickListener {
            startActivity(Intent(this, ActivitySubscription::class.java))
        }

        }
    }

    private fun checkFirstStartAndLocale() {
        val pref = ClassPrefs()
        val firstStart = pref.loadPrefString(this, "firststart_data")

        if (firstStart.isEmpty()) {
            // Pobieranie języka urządzenia
            val currentLocale = resources.configuration.locales[0]
            val languageTag = currentLocale.toLanguageTag()

            // 0 dla Polskiego, 1 dla reszty (np. Angielski)
            val languageData = if (languageTag == "pl-PL") 0 else 1

            pref.savePrefInt(this, "language_data", languageData)
            pref.savePrefString(this, "firststart_data", "true")
        }
    }

    private fun checkForNewNews() {
        val pref = ClassPrefs()
        val db = FirebaseFirestore.getInstance()

        db.collection("news")
            .whereEqualTo("isVisible", true)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val latestNewsDate = documents.documents[0].getTimestamp("date")?.toDate()?.time ?: 0L
                    val lastSeenDate = pref.loadPrefString(this, "last_seen_news_date").toLongOrNull() ?: 0L

                    if (latestNewsDate > lastSeenDate) {
                        viewRedDot.visibility = View.VISIBLE
                    }
                }
            }
    }
}