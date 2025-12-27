package com.example.calkulatorcnc.ui.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
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

class MainActivity : AppCompatActivity() {

    private var adView: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        createViewAEdgetoEdgeForAds()
        setupAds()
        checkFirstStartAndLocale()
        setupClickListeners()
    }
    private fun createViewAEdgetoEdgeForAds(){
        val gridLayout = findViewById<android.widget.GridLayout>(R.id.mainGrid)
        val adContainer = findViewById<FrameLayout>(R.id.adContainer)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            gridLayout.setPadding(
                gridLayout.paddingLeft,
                systemBars.top,
                gridLayout.paddingRight,
                gridLayout.paddingBottom
            )

            adContainer.setPadding(0, 0, 0, systemBars.bottom)

            insets
        }
    }

    private fun setupAds() {
        val adContainer = findViewById<FrameLayout>(R.id.adContainer)
        val subManager = SubscriptionManager.getInstance(this)

        subManager.isPremium.observe(this) { isPremium ->
            if (isPremium) {
                // UKRYWAMY REKLAMY
                adContainer.visibility = View.GONE
                // Opcjonalnie usuwamy widok reklamy, jeśli już tam był
                adContainer.removeAllViews()
                adView = null
            } else {
                // POKAZUJEMY REKLAMY
                val screenHeightDp = resources.configuration.screenHeightDp
                if (screenHeightDp < 400) {
                    adContainer.visibility = View.GONE
                } else {
                    adContainer.visibility = View.VISIBLE
                    if (adContainer.isEmpty()) { // Ładuj tylko jeśli jeszcze nie ma reklamy
                        val newAdView = AdView(this).apply {
                            setAdSize(AdSize.BANNER)
                            adUnitId = BuildConfig.ADMOB_BANNER_ID
                        }
                        adView = newAdView
                        adContainer.addView(newAdView)
                        newAdView.loadAd(AdRequest.Builder().build())
                    }
                }
            }
        }
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

        }

        findViewById<View>(R.id.card_no_ads).setOnClickListener {
            startActivity(Intent(this, ActivitySubscription::class.java))
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
}