package com.example.calkulatorcnc.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
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

class MainActivity : AppCompatActivity() {

    private var adView: AdView? = null
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private var consentInformation: ConsentInformation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Obsługa marginesów systemowych (pasek statusu i nawigacji)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupAds()
        checkFirstStartAndLocale()
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
                    if (adContainer.childCount == 0) { // Ładuj tylko jeśli jeszcze nie ma reklamy
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

    // Metody kliknięć - uproszczone wywołania Intent
    fun main_button1_clicked(view: View?) {
        startActivity(Intent(this, ActivityMilling::class.java))
    }

    fun main_button2_clicked(view: View?) {
        startActivity(Intent(this, ActivityTourning::class.java))
    }

    fun main_button3_clicked(view: View?) {
        startActivity(Intent(this, ActivityTools::class.java))
    }

    fun main_button4_clicked(view: View?) {
        startActivity(Intent(this, ActivityFitting::class.java))
    }

    fun main_button6_clicked(view: View?) {
        startActivity(Intent(this, ActivitySettings::class.java))
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