package com.example.calkulatorcnc.ui.activities

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isEmpty
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.example.calkulatorcnc.data.db.AppDatabase
import com.example.calkulatorcnc.data.preferences.ClassPrefs
import com.example.calkulatorcnc.ui.adapters.PremiumSpinnerAdapter
import com.google.android.gms.ads.*
import getMaterialsList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong

data class CalculationResult(
    val f: Double = 0.0,
    val s: Double = 0.0,
    val fz: Double = 0.0,
    val center: Double = 0.0,
    val isCenterMode: Boolean = false,
    val isSawMode: Boolean = false
)

class ActivityMilling : AppCompatActivity() {
    private lateinit var edtPanel: LinearLayout
    private lateinit var materialContainer: LinearLayout
    private lateinit var spinner1: Spinner
    private lateinit var btnClear: Button

    private var adView: AdView? = null
    private var calcSys: Int = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createViewAEdgetoEdgeForAds()
        initUI()
        setupAds()
        setupSpinner()

        calcSys = if (ClassPrefs().loadPrefInt(this, "calcsys_data") == 1) 12 else 1000
    }

    private fun createViewAEdgetoEdgeForAds() {
        enableEdgeToEdge() // Obowiązkowe dla efektu Edge-to-Edge
        setContentView(R.layout.activity_milling)

        val mainRoot = findViewById<View>(R.id.main)
        val customHeader = findViewById<LinearLayout>(R.id.customHeader)
        val adContainerLayout = findViewById<FrameLayout>(R.id.adContainerLayout)
        val adContainer = findViewById<FrameLayout>(R.id.adContainer)

        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            mainRoot.setPadding(0, 0, 0, 0)

            customHeader?.updatePadding(top = systemBars.top)

            adContainerLayout?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
                leftMargin = systemBars.left
                rightMargin = systemBars.right
            }

            insets
        }

        adContainer?.post {
            setupAds()
        }
    }

    private fun initUI() {
        edtPanel = findViewById(R.id.buttons_panel)
        materialContainer = findViewById(R.id.material_container)
        btnClear = findViewById(R.id.milingbutton2)
        spinner1 = findViewById(R.id.spinner1)

        findViewById<View>(R.id.main).setOnClickListener { hideKeyboard() }
        findViewById<ImageButton>(R.id.button_back).setOnClickListener { finish() }
        findViewById<Button>(R.id.milingbutton1).setOnClickListener { calculate() }
        findViewById<Button>(R.id.milingbutton3).setOnClickListener { showInfo() }

        btnClear.setOnClickListener { clearInputs() }
    }

    private fun setupAds() {
        val adContainer = findViewById<FrameLayout>(R.id.adContainer) ?: return
        val adContainerLayout = findViewById<FrameLayout>(R.id.adContainerLayout)

        SubscriptionManager.getInstance(this).isPremium.observe(this) { isPremium ->
            if (isPremium) {
                // 1. Logika PREMIUM: Czyścimy i niszczymy reklamę
                // Ukrywamy adContainerLayout, aby odzyskać miejsce na ekranie
                adContainerLayout?.visibility = View.GONE
                adContainer.visibility = View.GONE
                adContainer.removeAllViews()
                adView?.destroy()
                adView = null
            } else {
                // 2. Logika FREE: Sprawdzamy wysokość ekranu (min. 400dp)
                val screenHeightDp = resources.configuration.screenHeightDp
                if (screenHeightDp < 400) {
                    adContainerLayout?.visibility = View.GONE
                    adContainer.visibility = View.GONE
                } else {
                    adContainerLayout?.visibility = View.VISIBLE
                    adContainer.visibility = View.VISIBLE

                    // Ładujemy reklamę tylko jeśli jeszcze nie istnieje
                    if (adView == null) {
                        val newAdView = AdView(this).apply {
                            adUnitId = BuildConfig.ADMOB_BANNER_ID
                            // KLUCZOWA ZMIANA: Używamy adaptacyjnego rozmiaru zamiast BANNER
                            setAdSize(getAdSize(adContainer))
                        }

                        adView = newAdView
                        adContainer.removeAllViews() // Zabezpieczenie przed duplikacją
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

    private fun setupSpinner() {
        val isPremium = SubscriptionManager.getInstance(this).isPremium.value ?: false
        val spinnerArray = resources.getStringArray(R.array.miling_spinner1_data)

        // Pozycje wymagające Premium (np. specjalistyczne frezy)
        val premiumPositions = listOf(6, 7, 8)

        val adapter = PremiumSpinnerAdapter(
            this,
            R.layout.spinner_item, // Widok główny (biały tekst)
            spinnerArray,
            isPremium,
            premiumPositions
        )

        // Przypisanie widoku listy rozwijanej (półprzezroczyste tło)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)

        val spinner1 = findViewById<Spinner>(R.id.spinner1)
        spinner1.adapter = adapter

        // Dodajemy tło dropdownu dla efektu Glassmorphism w kodzie (opcjonalnie)
        spinner1.setPopupBackgroundResource(R.drawable.bg_spinner_popup_solid)

        spinner1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                updateDynamicInputs(pos)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun updateDynamicInputs(position: Int) {
        val isPremium = SubscriptionManager.getInstance(this).isPremium.value ?: false
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // Blokada premium dla pozycji 6, 7, 8
        if (!isPremium && (position in 6..8)) {
            edtPanel.removeAllViews()
            materialContainer.removeAllViews()
            findViewById<Button>(R.id.milingbutton1).isEnabled = false
            showPremiumRequired()
            return
        }

        findViewById<Button>(R.id.milingbutton1).isEnabled = true
        edtPanel.removeAllViews()
        materialContainer.removeAllViews()

        // POPRAWKA: Przycisk pojawia się dla pozycji 0, 1, 6, 7 (dodaj więcej jeśli trzeba)
        if (position in listOf(0, 1, 2, 3, 4, 5, 7)) {
            val btnMaterial = com.google.android.material.button.MaterialButton(this).apply {
                val btnHeight = if (isLandscape) dpToPx(40) else dpToPx(55)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    btnHeight
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    val marginSide = if (isLandscape) dpToPx(12) else dpToPx(8)
                    setMargins(marginSide, dpToPx(4), marginSide, if (isLandscape) dpToPx(4) else dpToPx(12))
                }

                text = getString(R.string.material)
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.button_style)
                backgroundTintList = null
                cornerRadius = dpToPx(12)

                // Przekazujemy position do dialogu
                setOnClickListener { showMaterialSelectionDialog(position) }
            }
            materialContainer.addView(btnMaterial)
        }

        edtPanel.orientation = if (isLandscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL

        // Hints dla różnych pozycji
        val hints = when (position) {
            0 -> arrayOf(R.string.VC, R.string.DC, R.string.FZ, R.string.Z)           // Skrawanie
            1 -> arrayOf(R.string.VC, R.string.DC, R.string.JT)                       // Gwintowanie
            2 -> arrayOf(R.string.AS, R.string.T, R.string.VC, R.string.L)            // Piły Taśmowe 1
            3 -> arrayOf(R.string.VF, R.string.VC, R.string.Z, R.string.LB)           // Piły Taśmowe 2
            4 -> arrayOf(R.string.AS, R.string.VC, R.string.D, R.string.Z, R.string.L)// Piły Tarczowe 1
            5 -> arrayOf(R.string.VF, R.string.VC, R.string.Z, R.string.D)            // Piły Tarczowe 2
            6 -> arrayOf(R.string.HD, R.string.TD, R.string.F)                        // Interpolacja
            7 -> arrayOf(R.string.VC, R.string.DM, R.string.Fn)                       // Wytaczanie
            8 -> arrayOf(R.string.PointA, R.string.PointB)                            // Punkt Zerowy
            else -> emptyArray()
        }

        hints.forEachIndexed { index, hintRes ->
            edtPanel.addView(createEditText(index, getString(hintRes)))
        }
    }

    private fun createEditText(index: Int, hintText: String) = EditText(this).apply {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        layoutParams = if (isLandscape) {
            // W Landscape: 0dp szerokości + waga 1.0, aby pola dzieliły się miejscem obok przycisku (lub pod nim)
            LinearLayout.LayoutParams(0, dpToPx(40), 1.0f).apply {
                setMargins(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
            }
        } else {
            LinearLayout.LayoutParams(dpToPx(220), dpToPx(48)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            }
        }

        textSize = if (isLandscape) 16f else 20f
        setTextColor(Color.WHITE)
        setHintTextColor(androidx.core.content.ContextCompat.getColor(this@ActivityMilling, R.color.textColor_hint))
        hint = hintText
        tag = index
        gravity = Gravity.CENTER
        setPadding(dpToPx(10), 0, dpToPx(10), 0)
        setBackgroundResource(R.drawable.edittext_style)
        inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_CLASS_NUMBER

        addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // POPRAWKA: Sprawdzamy, czy JAKIEKOLWIEK pole EditText ma wpisany tekst
                val anyFieldNotEmpty = (0 until edtPanel.childCount)
                    .map { edtPanel.getChildAt(it) }
                    .filterIsInstance<EditText>()
                    .any { it.text.isNotEmpty() }

                btnClear.isEnabled = anyFieldNotEmpty
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showMaterialSelectionDialog(operationPos: Int) {
        val isPremium = SubscriptionManager.getInstance(this).isPremium.value ?: false
        val materials = getMaterialsList(this)

        val dialogView = LayoutInflater.from(this).inflate(R.layout.window_materials_modern, null)
        val container = dialogView.findViewById<LinearLayout>(R.id.materialsContainer)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelMaterials)

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        materials.forEachIndexed { index, material ->
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_material, container, false)
            val tvName = itemView.findViewById<TextView>(R.id.materialName)
            val ivLock = itemView.findViewById<ImageView>(R.id.lockIcon)
            ivLock.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.gold))
            tvName.text = material.name

            if (!isPremium && index > 0) ivLock.visibility = View.VISIBLE

            itemView.setOnClickListener {
                if (!isPremium && index > 0) {
                    dialog.dismiss()
                    showPremiumRequired()
                } else {
                    // Pobieramy listę pól tekstowych z panelu
                    val editTexts = (0 until edtPanel.childCount)
                        .map { edtPanel.getChildAt(it) }
                        .filterIsInstance<EditText>()

                    // LOGIKA MAPOWANIA DANYCH ZGODNIE Z TWOIMI HINTS
                    when (operationPos) {
                        0 -> { // FREZOWANIE (VC, DC, Fz, Z)
                            if (editTexts.size >= 3) {
                                editTexts[0].setText(material.vcMilling.toString())
                                editTexts[2].setText(material.fzMilling.toString())
                            }
                        }
                        1 -> { // GWINTOWANIE (VC, DC, JT)
                            if (editTexts.size >= 1) {
                                editTexts[0].setText(material.vcTapping.toString())
                            }
                        }
                        2 -> { // PIŁY TAŚMOWE 1 (AS, T, VC, L)
                            if (editTexts.size >= 3) {
                                editTexts[2].setText(material.vcSawing.toString()) // VC jest na 3. pozycji
                            }
                        }
                        3, 4, 5 -> { // POZOSTAŁE PIŁY (VF/AS, VC, Z/D...)
                            if (editTexts.size >= 2) {
                                editTexts[1].setText(material.vcSawing.toString()) // VC jest na 2. pozycji
                            }
                        }
                        7 -> { // WYTACZANIE (VC, DM, Fn)
                            if (editTexts.size >= 3) {
                                editTexts[0].setText(material.vcParting.toString())
                                editTexts[2].setText(material.fnParting.toString())
                            }
                        }
                    }

                    Toast.makeText(this, "${material.name}: OK", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
            container.addView(itemView)
        }
        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun calculate() {
        val editTexts = (0 until edtPanel.childCount).map { edtPanel.getChildAt(it) }.filterIsInstance<EditText>()
        if (editTexts.isEmpty()) return
        val vals = editTexts.map { it.text.toString().toDoubleOrNull() ?: 0.0 }
        val pos = spinner1.selectedItemPosition

        val isZeroPointMode = pos == 8

        if (!isZeroPointMode && vals.any { it <= 0.0 }) {
            Toast.makeText(this, getString(R.string.error2), Toast.LENGTH_SHORT).show()
            return
        }

        val result = when (pos) {
            0 -> {
                val s = (vals[0] * calcSys) / 3.14 / vals[1]
                CalculationResult(f = vals[2] * vals[3] * s, s = s)
            }
            1 -> {
                val s = (vals[0] * calcSys) / 3.14 / vals[1]
                CalculationResult(f = vals[2] * s, s = s)
            }
            2 -> CalculationResult(fz = (vals[0] * vals[1]) / (vals[3] * vals[2] * calcSys), isSawMode = true)
            3 -> CalculationResult(fz = (vals[0] * vals[3]) / (vals[1] * vals[2] * calcSys), isSawMode = true)
            4 -> CalculationResult(fz = (vals[0] * vals[2] * 3.14) / (vals[4] * vals[1] * vals[3] * calcSys), isSawMode = true)
            5 -> CalculationResult(fz = (vals[0] * vals[3] * 3.14) / (vals[1] * vals[2] * calcSys), isSawMode = true)
            6 -> CalculationResult(f = vals[2] * ((vals[0] - vals[1]) / vals[0]))
            7 -> {
                val s = (vals[0] * calcSys) / (3.14 * vals[1])
                CalculationResult(f = vals[2] * s, s = s)
            }
            8 -> CalculationResult(center = (vals[0] + vals[1]) / 2, isCenterMode = true)
            else -> CalculationResult()
        }
        showResultDialog(result)
    }

    private fun showResultDialog(res: CalculationResult) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.window_calculation_modern, null)

        val label1 = dialogView.findViewById<TextView>(R.id.label1)
        val value1 = dialogView.findViewById<TextView>(R.id.value1)
        val row1 = dialogView.findViewById<RelativeLayout>(R.id.row1)

        val label2 = dialogView.findViewById<TextView>(R.id.label2)
        val value2 = dialogView.findViewById<TextView>(R.id.value2)
        val row2 = dialogView.findViewById<RelativeLayout>(R.id.row2)

        val label3 = dialogView.findViewById<TextView>(R.id.label3)
        val value3 = dialogView.findViewById<TextView>(R.id.value3)
        val row3 = dialogView.findViewById<RelativeLayout>(R.id.row3)

        val separator = dialogView.findViewById<View>(R.id.separator)
        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseResults)

        when {
            res.isCenterMode -> {
                row1.visibility = View.GONE
                row2.visibility = View.GONE
                separator.visibility = View.GONE
                label3.text = getString(R.string.center_colon)
                value3.text = String.format("%.3f", res.center)
            }
            res.isSawMode -> {
                row1.visibility = View.GONE
                row2.visibility = View.GONE
                separator.visibility = View.GONE
                label3.text = getString(R.string.fz_colon)
                value3.text = String.format("%.4f", res.fz)
            }
            else -> {
                label1.text = getString(R.string.f_colon)
                value1.text = res.f.roundToLong().toString()
                label2.text = getString(R.string.s_colon)
                value2.text = res.s.roundToLong().toString()

                if (res.fz > 0 && spinner1.selectedItemPosition != 0) {
                    label3.text = getString(R.string.fz_colon)
                    value3.text = String.format("%.3f", res.fz)
                } else {
                    row3.visibility = View.GONE
                    separator.visibility = View.GONE
                }
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showThreadDatabaseDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_thread_db, null)
        val tvInstructions = dialogView.findViewById<TextView>(R.id.tvFieldInstructions)
        val tableLayout = dialogView.findViewById<TableLayout>(R.id.dialog_main_table)
        val buttonsPanel = dialogView.findViewById<LinearLayout>(R.id.dialog_buttons_panel)

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val suffix = if (calcSys == 1000) "m" else "inch"
        val resId = resources.getIdentifier("info_1_$suffix", "string", packageName)
        if (resId != 0) {
            tvInstructions.text = android.text.Html.fromHtml(
                getString(resId),
                android.text.Html.FROM_HTML_MODE_COMPACT
            )
        }

        // 2. LOGIKA BAZY DANYCH (Identyczna jak w ActivityTables)
        fun refreshTable(sIdx: Int, tIdx: Int) {
            tableLayout.removeAllViews()
            lifecycleScope.launch {
                val data = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(this@ActivityMilling).threadDao().getThreads(sIdx, tIdx)
                }

                addTableHeaders(tableLayout, sIdx, tIdx)

                data.forEach { thread ->
                    val row = LayoutInflater.from(this@ActivityMilling).inflate(R.layout.item_thread_row, tableLayout, false) as TableRow

                    // Wypełnienie tekstem
                    row.findViewById<TextView>(R.id.col1).text = thread.name     // np. "M10"
                    row.findViewById<TextView>(R.id.col2).text = thread.pitch    // np. "1.5"
                    row.findViewById<TextView>(R.id.col3).text = thread.holeMin
                    row.findViewById<TextView>(R.id.col4).text = thread.holeMax
                    row.findViewById<TextView>(R.id.col5).text = thread.holeSize // np. "8.5"

                    // Logika przenoszenia danych po kliknięciu wiersza
                    row.setOnClickListener {
                        val editTexts = (0 until edtPanel.childCount)
                            .map { edtPanel.getChildAt(it) }
                            .filterIsInstance<EditText>()

                        // Sprawdzamy, czy jesteśmy w trybie gwintowania i mamy odpowiednie pola
                        if (editTexts.size >= 3) {
                            // 1. Wyciągamy samą liczbę z nazwy gwintu (np. "M10" -> "10")
                            val nominalDiameter = thread.name.replace(Regex("[^0-9.,]"), "").replace(",", ".")

                            // 2. Przypisujemy: DC (Średnica) to pole indeks 1, JT (Skok) to pole indeks 2
                            editTexts[1].setText(nominalDiameter)
                            editTexts[2].setText(thread.pitch.replace(",", "."))

                            Toast.makeText(this@ActivityMilling, "Wybrano: ${thread.name}", Toast.LENGTH_SHORT).show()
                            dialog.dismiss() // Zamykamy dialog po wyborze
                        }
                    }
                    tableLayout.addView(row)
                }
            }
        }

        // 3. GENEROWANIE PRZYCISKÓW KATEGORII (M, MF, UNC...)
        val categories = arrayOf("M", "MF", "UNC", "UNF", "G")
        categories.forEachIndexed { index, label ->
            val btn = com.google.android.material.button.MaterialButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(70), dpToPx(38)).apply {
                    setMargins(dpToPx(4), 0, dpToPx(4), 0)
                }
                text = label
                textSize = 10f
                setBackgroundResource(R.drawable.button_style)
                setTextColor(Color.WHITE)
                backgroundTintList = null
                setOnClickListener {
                    val sIdx = if (index > 1) 2 else 0 // Proste mapowanie: 0,1 -> Metryczne, 2,3 -> Calowe
                    refreshTable(sIdx, index % 2)
                }
            }
            buttonsPanel.addView(btn)
        }

        refreshTable(0, 0) // Startujemy od tabeli M
        dialogView.findViewById<Button>(R.id.btn_close_dialog).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun addTableHeaders(tableLayout: TableLayout, sIdx: Int, tIdx: Int) {
        val headerRow = TableRow(this).apply {
            setBackgroundColor(Color.parseColor("#333333"))
            setPadding(0, dpToPx(4), 0, dpToPx(4))
        }
        val isInch = sIdx in listOf(2, 3, 4, 5)
        val headers = arrayOf("Nazwa", if(isInch) "TPI" else "P", "Min", "Max", "Wiertło")

        headers.forEach { title ->
            headerRow.addView(TextView(this).apply {
                text = title
                setTextColor(Color.YELLOW)
                gravity = Gravity.CENTER
                textSize = 12f
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
        tableLayout.addView(headerRow)
    }

    private fun showPremiumRequired() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.window_premium_upgrade, null)
        val btnGo = dialogView.findViewById<Button>(R.id.btnGoToPremium)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelPremium)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnGo.setOnClickListener {
            dialog.dismiss()
            spinner1.setSelection(0)
            startActivity(Intent(this, ActivitySubscription::class.java))
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            spinner1.setSelection(0)
        }

        dialog.show()
    }

    private fun clearInputs() {
        for (i in 0 until edtPanel.childCount) (edtPanel.getChildAt(i) as? EditText)?.setText("")
        btnClear.isEnabled = false
    }

    private fun showInfo() {
        val item = spinner1.selectedItemPosition

        // Jeśli wybrano Gwintowanie (pozycja 1)
        if (item == 1) {
            showThreadDatabaseDialog()
        } else {
            val infoIndex = when (item) {
                2 -> 4; 3 -> 5; 4 -> 6; 5 -> 7; 6 -> 2; 7 -> 3; 8 -> 12
                else -> item
            }

            val suffix = if (calcSys == 1000) "m" else "inch"
            val resourceName = "info_${infoIndex}_$suffix"
            val resId = resources.getIdentifier(resourceName, "string", packageName)

            if (resId != 0) {
                val dialogView =
                    LayoutInflater.from(this).inflate(R.layout.window_information_modern, null)
                val content = dialogView.findViewById<TextView>(R.id.infoContent)
                val btnOk = dialogView.findViewById<Button>(R.id.btnOk)
                content.text = android.text.Html.fromHtml(
                    getString(resId),
                    android.text.Html.FROM_HTML_MODE_COMPACT
                )
                val dialog = AlertDialog.Builder(this).setView(dialogView).create()
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                btnOk.setOnClickListener { dialog.dismiss() }
                dialog.show()
            }
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    override fun onPause() { adView?.pause(); super.onPause() }
    override fun onResume() { super.onResume(); adView?.resume() }
    override fun onDestroy() { adView?.destroy(); super.onDestroy() }
}