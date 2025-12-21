package com.example.calkulatorcnc.ui.activities

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

class ActivityTables : AppCompatActivity() {

    private var spinnerIdx: Int = 0
    private var tableIdx: Int = 0
    private var tableName: String? = null

    // Widoki (używamy małych liter i bezpiecznych typów)
    private var btnBack: ImageButton? = null
    private var tvPitch: TextView? = null
    private var tvName: TextView? = null
    private var tvHoleMin: TextView? = null
    private var tvHoleMax: TextView? = null
    private var tvHole: TextView? = null
    private var adView: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Pobranie danych z Intent
        spinnerIdx = intent.getIntExtra("spinner_idx", 0)
        tableIdx = intent.getIntExtra("table_idx", 0)

        // Wybór layoutu i nazwy tabeli
        setupTableConfiguration()

        // Obsługa Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Obsługa przycisku wstecz (systemowego)
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }

        initViews()
        setupAds()
    }

    private fun setupTableConfiguration() {
        // Para: LayoutID to SuffixNazwy
        val config: Pair<Int, String> = when (spinnerIdx) {
            0 -> when (tableIdx) {
                0 -> R.layout.table_m to "M"
                1 -> R.layout.table_mf1 to "MF1"
                2 -> R.layout.table_mf2 to "MF2"
                3 -> R.layout.table_mj to "MJ"
                4 -> R.layout.table_unjc to "UNJC"
                5 -> R.layout.table_unjf to "UNJF"
                else -> R.layout.table_m to "M"
            }
            1 -> when (tableIdx) {
                0 -> R.layout.table_egm to "EGM"
                1 -> R.layout.table_egmf to "EGMF"
                2 -> R.layout.table_egunc to "EGUNC"
                3 -> R.layout.table_egunf to "EGUNF"
                else -> R.layout.table_m to "M"
            }
            2 -> when (tableIdx) {
                0 -> R.layout.table_unc to "UNC"
                1 -> R.layout.table_unf to "UNF"
                2 -> R.layout.table_unef to "UNEF"
                3 -> R.layout.table_un to "UN"
                else -> R.layout.table_m to "M"
            }
            3 -> when (tableIdx) {
                0 -> R.layout.table_npt to "NPT"
                1 -> R.layout.table_nptf to "NPTF"
                2 -> R.layout.table_npsm to "NPSM"
                else -> R.layout.table_m to "M"
            }
            4 -> R.layout.table_g to "G"
            5 -> R.layout.table_bsw to "BSW"
            6 -> when (tableIdx) {
                0 -> R.layout.table_s_m to "M"
                1 -> R.layout.table_s_mf to "MF"
                2 -> R.layout.table_s_unc to "UNC"
                3 -> R.layout.table_s_unf to "UNF"
                4 -> R.layout.table_s_unef to "UNEF"
                5 -> R.layout.table_s_g to "G"
                6 -> R.layout.table_s_egm to "EGM"
                7 -> R.layout.table_s_bsw to "BSW"
                else -> R.layout.table_m to "M"
            }
            7 -> R.layout.table_pg to "PG"
            else -> R.layout.table_m to "M"
        }

        setContentView(config.first)
        tableName = "${getString(R.string.holeName)} ${config.second}"
    }

    private fun initViews() {
        tvPitch = findViewById(R.id.textView_HolePitch)
        tvName = findViewById(R.id.textView_Name)
        tvHoleMin = findViewById(R.id.textView_HoleMin)
        tvHoleMax = findViewById(R.id.textView_HoleMax)
        tvHole = findViewById(R.id.textView_Hole)
        btnBack = findViewById(R.id.button_back)

        btnBack?.setOnClickListener { finish() }

        // Ustawianie tekstów z bezpiecznym dostępem
        tvPitch?.setText(R.string.holePitch)
        tvName?.text = tableName
        tvHoleMin?.setText(R.string.holeMin)
        tvHoleMax?.setText(R.string.holeMax)

        setupHoleText()
    }

    private fun setupHoleText() {
        tvHole?.let { textView ->
            textView.setText(R.string.hole)

            if (spinnerIdx == 2) {
                textView.setText(R.string.holePitch)
                // Jeśli tableIdx to 2 lub 3, wracamy do "hole"
                if (tableIdx == 2 || tableIdx == 3) {
                    textView.setText(R.string.hole)
                }
            }
        }
    }

    private fun setupAds() {
        MobileAds.initialize(this)
        val adContainer = findViewById<FrameLayout>(R.id.adContainer) ?: return

        val mAdView = AdView(this).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BuildConfig.ADMOB_BANNER_ID
        }

        adView = mAdView
        adContainer.addView(mAdView)
        mAdView.loadAd(AdRequest.Builder().build())
    }
}