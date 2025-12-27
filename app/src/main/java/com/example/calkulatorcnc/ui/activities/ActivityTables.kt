package com.example.calkulatorcnc.ui.activities

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.data.db.AppDatabase
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.google.android.gms.ads.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActivityTables : AppCompatActivity() {

    private var spinnerIdx: Int = 0
    private var tableIdx: Int = 0

    private lateinit var spinner1: Spinner
    private lateinit var buttonsPanel: LinearLayout
    private lateinit var tvTableHeader: TextView
    private var adView: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tables)
        createViewAEdgetoEdgeForAds()
        initUI()
        setupSpinner()

        spinnerIdx = intent.getIntExtra("spinner_idx", 0)
        spinner1.setSelection(spinnerIdx)

        onBackPressedDispatcher.addCallback(this) { finish() }
    }

    private fun createViewAEdgetoEdgeForAds(){

        val mainRoot = findViewById<View>(R.id.main)
        val customHeader = findViewById<LinearLayout>(R.id.customHeader)
        val bottomBar = findViewById<LinearLayout>(R.id.bottom_bar)
        val leftPanel = findViewById<LinearLayout>(R.id.left_panel)
        val rightPanel = findViewById<LinearLayout>(R.id.right_panel)
        val layoutBack = findViewById<LinearLayout>(R.id.layout_back)
        val adContainer = findViewById<FrameLayout>(R.id.adContainer)

        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            mainRoot.setPadding(0, 0, 0, 0)

            customHeader?.updatePadding(top = systemBars.top)
            bottomBar?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
            }

            leftPanel?.updatePadding(top = systemBars.top, left = systemBars.left)
            rightPanel?.updatePadding(top = systemBars.top, right = systemBars.right)


            layoutBack?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
            }
            adContainer?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
            }

            insets
        }
        adContainer?.post {
            setupAds()
        }
    }

    private fun initUI() {
        spinner1 = findViewById(R.id.spinner1)
        buttonsPanel = findViewById(R.id.buttons_panel)
        tvTableHeader = findViewById(R.id.textView_TableHeader)
        findViewById<ImageButton>(R.id.button_back).setOnClickListener { finish() }
    }

    private fun setupSpinner() {
        val items = resources.getStringArray(R.array.spinner3_items)
        val adapter = ArrayAdapter(this, R.layout.spinner_item, R.id.spinner_text, items)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinner1.adapter = adapter

        spinner1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                spinnerIdx = pos
                updateDynamicButtons(pos)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun updateDynamicButtons(categoryPos: Int) {
        buttonsPanel.removeAllViews()
        val f = getString(R.string.forming_abbr)
        val m = getString(R.string.mixed_abbr)
        val eg = getString(R.string.thread_eg)
        // TWOJA MAPA: Przypisanie etykiet do table_id
        val buttonLabels = when (categoryPos) {
            0 -> arrayOf("M", "MF", "M $f", "MF $f")
            1 -> arrayOf("$eg M", "$eg MF", "$eg UNC", "$eg UNF", "$eg M $f")
            2 -> arrayOf("UNC", "UNF", "UNEF", "UNC $f", "UNF $f", "UNEF $f", "8-UN", "UN $m", "UNJC", "UNJF")
            3 -> arrayOf("NPT", "NPSM", "NPTF")
            4 -> arrayOf("G", "G $f")
            5 -> arrayOf("BSW", "BSW $f")
            6 -> arrayOf("MJ")
            7 -> arrayOf("PG")
            else -> emptyArray()
        }

        buttonLabels.forEachIndexed { index, label ->
            val btn = com.google.android.material.button.MaterialButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(48)
                ).apply { setMargins(dpToPx(4), 0, dpToPx(4), 0) }

                text = label
                textSize = 12f
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.button_style)
                backgroundTintList = null
                setPadding(dpToPx(12), 0, dpToPx(12), 0)

                setOnClickListener {
                    tableIdx = index
                    loadTableLayout()
                }
            }
            buttonsPanel.addView(btn)
        }

        tableIdx = 0
        loadTableLayout()
    }

    private fun loadTableLayout() {
        val tableLayout = findViewById<TableLayout>(R.id.mainTableLayout) ?: return
        val scrollView = findViewById<ScrollView>(R.id.table_scroll_view)
        tableLayout.removeAllViews()

        // Używamy Coroutines, aby nie blokować interfejsu przy odczycie z Room
        lifecycleScope.launch {
            val data = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@ActivityTables).threadDao().getThreads(spinnerIdx, tableIdx)
            }

            updateTableHeaders(tableLayout)

            data.forEach { thread ->
                val row = LayoutInflater.from(this@ActivityTables).inflate(R.layout.item_thread_row, tableLayout, false) as TableRow
                row.findViewById<TextView>(R.id.col1).text = thread.name
                row.findViewById<TextView>(R.id.col2).text = thread.pitch
                row.findViewById<TextView>(R.id.col3).text = thread.holeMin
                row.findViewById<TextView>(R.id.col4).text = thread.holeMax
                row.findViewById<TextView>(R.id.col5).text = thread.holeSize
                tableLayout.addView(row)
            }

            tvTableHeader.text = "${getString(R.string.holeName)} ${getSuffix(spinnerIdx, tableIdx)}"
            scrollView?.post {
                scrollView.fullScroll(ScrollView.FOCUS_UP)
            }
        }
    }

    private fun updateTableHeaders(tableLayout: TableLayout) {
        val headerRow = TableRow(this).apply {
            setBackgroundColor(Color.parseColor("#333333"))
            setPadding(0, dpToPx(4), 0, dpToPx(4))
        }

        // DYNAMICZNE NAGŁÓWKI: mm vs TPI
        val isInch = spinnerIdx in listOf(2, 3, 4, 5) || (spinnerIdx == 1 && tableIdx in listOf(2, 3))
        val pitchLabel = if (isInch) "Zwoje/cal" else getString(R.string.holePitch)

        val headers = arrayOf("Nazwa", pitchLabel, getString(R.string.holeMin), getString(R.string.holeMax), getString(R.string.hole))

        headers.forEach { title ->
            val tv = TextView(this).apply {
                text = title
                setTextColor(Color.YELLOW)
                gravity = Gravity.CENTER
                setPadding(dpToPx(4), dpToPx(8), dpToPx(4), dpToPx(8))
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            }
            headerRow.addView(tv)
        }
        tableLayout.addView(headerRow)
    }

    private fun getSuffix(sIdx: Int, tIdx: Int): String {
        return try {
            val labels = when (sIdx) {
                0 -> arrayOf("M", "MF", "M Wygniatanie", "MF Wygniatanie")
                1 -> arrayOf("EG M", "EG MF", "EG UNC", "EG UNF", "EG M Wyg.")
                2 -> arrayOf("UNC", "UNF", "UNEF", "UNC Wyg.", "UNF Wyg.", "UNEF Wyg.", "8-UN", "UN Miesz.", "UNJC", "UNJF")
                3 -> arrayOf("NPT", "NPSM", "NPTF")
                4 -> arrayOf("G", "G Wygniatanie")
                5 -> arrayOf("BSW", "BSW Wygniatanie")
                6 -> arrayOf("MJ")
                7 -> arrayOf("PG")
                else -> emptyArray()
            }
            labels[tIdx]
        } catch (e: Exception) { "" }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun setupAds() {
        val adContainer = findViewById<FrameLayout>(R.id.adContainer) ?: return

        // Obserwuj stan Premium przez SubscriptionManager
        SubscriptionManager.getInstance(this).isPremium.observe(this) { isPremium ->
            if (isPremium) {
                // Jeśli Premium: Ukryj kontener i zniszcz AdView
                adContainer.visibility = View.GONE
                adView?.destroy()
                adView = null
            } else {
                // Jeśli darmowa: Pokaż kontener i załaduj reklamę
                adContainer.visibility = View.VISIBLE
                if (adView == null) {
                    adView = AdView(this).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = BuildConfig.ADMOB_BANNER_ID
                        adContainer.addView(this)
                        loadAd(AdRequest.Builder().build())
                    }
                }
            }
        }
    }

    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        adView?.resume()
    }

    override fun onDestroy() {
        adView?.destroy()
        super.onDestroy()
    }
}