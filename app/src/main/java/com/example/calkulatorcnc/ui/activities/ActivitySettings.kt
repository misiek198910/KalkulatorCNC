package com.example.calkulatorcnc.ui.activities

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.example.calkulatorcnc.billing.SubscriptionStatus
import com.example.calkulatorcnc.data.preferences.ClassPrefs
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class ActivitySettings : AppCompatActivity() {

    private lateinit var sp1: Spinner
    private lateinit var sp2: Spinner
    private lateinit var tvToolbarTitle: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var adContainer: FrameLayout
    private var adView: AdView? = null
    private lateinit var spinner1Array: Array<String>
    private lateinit var spinner2Array: Array<String>
    private var isSpinner1Initialized = true
    private var isSpinner2Initialized = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        setupInsets()
        initViews()
        setupAds() // Logika banerów zależna od Premium
        setupSpinners()

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }

    private fun initViews() {
        sp1 = findViewById(R.id.spinner1)
        sp2 = findViewById(R.id.spinner2)
        btnBack = findViewById(R.id.button_back)
        tvToolbarTitle = findViewById(R.id.toolbar_title)
        adContainer = findViewById(R.id.adContainer)

        tvToolbarTitle.setText(R.string.main_button6)

        btnBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setupAds() {
        MobileAds.initialize(this)
        val subManager = SubscriptionManager.getInstance(this)

        // Obserwowanie statusu Premium dla banera
        subManager.isPremium.observe(this) { isPremium ->
            if (isPremium) {
                // UKRYWAMY BANER DLA PREMIUM
                adContainer.visibility = View.GONE
                adContainer.removeAllViews()
                adView?.destroy()
                adView = null
            } else {
                // POKAZUJEMY BANER DLA NON-PREMIUM
                val screenHeightDp = resources.configuration.screenHeightDp
                if (screenHeightDp < 400) {
                    adContainer.visibility = View.GONE
                } else {
                    adContainer.visibility = View.VISIBLE
                    if (adContainer.childCount == 0) {
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

    private fun setupSpinners() {
        spinner1Array = resources.getStringArray(R.array.spinner1_items)
        spinner2Array = resources.getStringArray(R.array.spinner2_items)

        // Adapter dla systemu miar
        val adapter1 = ArrayAdapter(this, R.layout.spinner_item, spinner1Array)
        adapter1.setDropDownViewResource(R.layout.spinner_dropdown_item)
        sp1.adapter = adapter1

        // Adapter dla języka
        val adapter2 = ArrayAdapter(this, R.layout.spinner_item, spinner2Array)
        adapter2.setDropDownViewResource(R.layout.spinner_dropdown_item)
        sp2.adapter = adapter2

        // Logika wyboru systemu miar
        sp1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isSpinner1Initialized) {
                    isSpinner1Initialized = false
                } else {
                    ClassPrefs().savePrefInt(this@ActivitySettings, "calcsys_data", position)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Logika wyboru języka
        sp2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isSpinner2Initialized) {
                    isSpinner2Initialized = false
                } else {
                    val pref = ClassPrefs()
                    val iso = if (position == 0) {
                        pref.savePrefInt(this@ActivitySettings, "language_data", 0)
                        "pl"
                    } else {
                        pref.savePrefInt(this@ActivitySettings, "language_data", 1)
                        "en"
                    }
                    val appLocale = LocaleListCompat.forLanguageTags(iso)
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Ustawienie aktualnej selekcji języka
        val currentLangIdx = ClassPrefs().loadPrefInt(this, "language_data")
        sp2.setSelection(currentLangIdx)
    }

    fun settings_button1_Clicked(view: View?) {
        startActivity(Intent(this, ActivityInformation::class.java))
    }

    fun settings_button2_Clicked(view: View?) {
        startActivity(Intent(this, ActivityApps::class.java))
    }

    fun settings_button3_Clicked(view: View?) {
        openMarketPage(packageName)
    }

    fun settings_button4_Clicked(view: View?) {
        startActivity(Intent(this, ActivityKofi::class.java))
    }

    fun settings_button5_Clicked(view: View?) {
        startActivity(Intent(this, ActivitySubscription::class.java))
    }

    private fun openMarketPage(appId: String) {
        val uri = Uri.parse("market://details?id=$appId")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show()
        }
    }
    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}