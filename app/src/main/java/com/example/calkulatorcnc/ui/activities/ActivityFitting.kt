package com.example.calkulatorcnc.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

class ActivityFitting : AppCompatActivity() {

    private lateinit var spinner1: Spinner
    private lateinit var btnBack: ImageButton
    private lateinit var spinner1Array: Array<String>

    private var adView: AdView? = null
    private var spinnerIdx: Int = 0
    private var tableIdx: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fitting)

        setupInsets()
        initViews()
        setupAds() // Logika subskrypcji i reklam
        setupSpinner()

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }

    private fun initViews() {
        spinner1 = findViewById(R.id.spinner1)
        btnBack = findViewById(R.id.button_back)
        btnBack.setOnClickListener { finish() }
    }

    private fun setupAds() {
        // Inicjalizacja SDK (jeśli jeszcze nie było zrobione)
        MobileAds.initialize(this)

        val adContainer = findViewById<FrameLayout>(R.id.adContainer)
        val subManager = SubscriptionManager.getInstance(this)

        // Obserwujemy status Premium
        subManager.isPremium.observe(this) { isPremium ->
            if (isPremium) {
                // UKRYWAMY REKLAMY DLA PREMIUM
                adContainer.visibility = View.GONE
                adContainer.removeAllViews()
                adView = null
            } else {
                // POKAZUJEMY REKLAMY DLA NON-PREMIUM
                val screenHeightDp = resources.configuration.screenHeightDp
                if (screenHeightDp < 400) {
                    adContainer.visibility = View.GONE
                } else {
                    adContainer.visibility = View.VISIBLE
                    // Ładujemy reklamę tylko jeśli kontener jest pusty
                    if (adContainer.childCount == 0) {
                        val newAdView = AdView(this).apply {
                            setAdSize(AdSize.BANNER)
                            setAdUnitId(BuildConfig.ADMOB_BANNER_ID)
                        }
                        adView = newAdView
                        adContainer.addView(newAdView)
                        val adRequest = AdRequest.Builder().build()
                        newAdView.loadAd(adRequest)
                    }
                }
            }
        }
    }

    private fun setupSpinner() {
        // Bezpieczne pobranie tablicy (wymuszenie typu nie-nullowalnego)
        spinner1Array = resources.getStringArray(R.array.spinner3_items).map { it ?: "" }.toTypedArray()

        val adapter = ArrayAdapter(this, R.layout.spinner_item, spinner1Array)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinner1.adapter = adapter

        spinner1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                spinnerIdx = position
                updateButtonsPanel(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateButtonsPanel(position: Int) {
        val buttonsPanel = findViewById<LinearLayout>(R.id.buttons_panel)
        buttonsPanel.removeAllViews()

        val hints = when (position) {
            0 -> arrayOf(R.string.M, R.string.MF1, R.string.MF2, R.string.MJ, R.string.UNJC, R.string.UNJF)
            1 -> arrayOf(R.string.EGM, R.string.EGMF, R.string.EGUNC, R.string.EGUNF)
            2 -> arrayOf(R.string.UNC, R.string.UNF, R.string.UNEF, R.string.UN)
            3 -> arrayOf(R.string.NPT, R.string.NPTF, R.string.NPSM)
            4 -> arrayOf(R.string.G)
            5 -> arrayOf(R.string.BSW)
            6 -> arrayOf(R.string.S_M, R.string.S_MF, R.string.S_UNC, R.string.S_UNF, R.string.S_UNEF, R.string.S_G, R.string.S_EGM, R.string.S_BSW)
            7 -> arrayOf(R.string.PG)
            else -> emptyArray()
        }

        hints.forEachIndexed { index, stringRes ->
            val btn = createButton(index, getString(stringRes))
            buttonsPanel.addView(btn)
        }
    }

    private fun createButton(tagIndex: Int, text: String): Button {
        val params = LinearLayout.LayoutParams(dpToPx(200), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(dpToPx(2), dpToPx(5), dpToPx(2), dpToPx(5))
        }

        return Button(this).apply {
            this.text = text
            textSize = 18f
            tag = tagIndex
            gravity = Gravity.CENTER
            setTextColor(context.getColor(R.color.white))
            setBackgroundResource(R.drawable.button_style)
            layoutParams = params
            setOnClickListener {
                val intent = Intent(this@ActivityFitting, ActivityTables::class.java).apply {
                    putExtra("spinner_idx", spinnerIdx)
                    putExtra("table_idx", tagIndex)
                }
                startActivity(intent)
            }
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}