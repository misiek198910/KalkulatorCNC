package com.example.calkulatorcnc.ui.activities

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.example.calkulatorcnc.data.db.AppDatabase
import com.example.calkulatorcnc.data.preferences.ClassPrefs
import com.example.calkulatorcnc.ui.adapters.PremiumSpinnerAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
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
        setContentView(R.layout.activity_milling)
        createViewAEdgetoEdgeForAds()
        initUI()
        setupAds()
        setupSpinner()

        calcSys = if (ClassPrefs().loadPrefInt(this, "calcsys_data") == 1) 12 else 1000
    }

    private fun createViewAEdgetoEdgeForAds() {
        val mainRoot = findViewById<View>(R.id.main)
        val customHeader = findViewById<LinearLayout>(R.id.customHeader)
        val buttonsPanel = findViewById<LinearLayout>(R.id.layout)
        val adContainerLayout = findViewById<FrameLayout>(R.id.adContainerLayout)
        val adContainer = findViewById<FrameLayout>(R.id.adContainer)

        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density

            val isChromebook = android.os.Build.MODEL.contains("sdk_gpc", ignoreCase = true) ||
                    android.os.Build.DEVICE.contains("emu64a", ignoreCase = true) ||
                    packageManager.hasSystemFeature("org.chromium.arc") ||
                    android.os.Build.DEVICE?.startsWith("arc") == true

            mainRoot.setPadding(0, 0, 0, 0)
            customHeader?.updatePadding(top = systemBars.top)

            val bottomExtraFix = if (isChromebook) (32 * density).toInt() else 0
            val horizontalMargin = (16 * density).toInt()

            buttonsPanel?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom + bottomExtraFix
                leftMargin = systemBars.left + horizontalMargin
            }

            adContainerLayout?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom + bottomExtraFix
                rightMargin = systemBars.right + horizontalMargin
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
        val density = displayMetrics.density

        var adWidthPixels = adContainer.width.toFloat()

        if (adWidthPixels == 0f) {
            // Jeśli szerokość kontenera jeszcze nie jest znana (wynosi 0):
            // W trybie Landscape na Chromebooku/Pixelu, reklama zajmuje tylko część ekranu.
            // Odejmijmy szerokość przycisków (np. ok. 200dp) i marginesy.
            val estimateButtonsWidthPx = 200 * density
            val horizontalPaddingPx = 32 * density

            adWidthPixels = displayMetrics.widthPixels.toFloat() - estimateButtonsWidthPx - horizontalPaddingPx

            // Zabezpieczenie, żeby szerokość nie była ujemna
            if (adWidthPixels <= 0) adWidthPixels = displayMetrics.widthPixels.toFloat()
        }

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

        // Ujednolicony LayoutParams
        layoutParams = if (isLandscape) {
            // W Landscape: rozciąganie (weight 1.0)
            LinearLayout.LayoutParams(0, dpToPx(40), 1.0f).apply {
                setMargins(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
            }
        } else {
            // W Portrait: stała szerokość 220dp, wyśrodkowane
            LinearLayout.LayoutParams(dpToPx(220), dpToPx(48)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            }
        }

        // Wygląd i Styl
        textSize = if (isLandscape) 16f else 20f
        setTextColor(Color.WHITE)
        // Używamy 'context', aby metoda była uniwersalna dla obu aktywności
        setHintTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.textColor_hint))
        hint = hintText
        tag = index
        gravity = Gravity.CENTER
        setPadding(dpToPx(10), 0, dpToPx(10), 0)

        // Wybierz jeden spójny drawable (wskazałeś, że milling jest poprawny, więc używamy edittext_style)
        setBackgroundResource(R.drawable.edittext_style)

        inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_CLASS_NUMBER

        // Logika przycisku Clear
        addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
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
                            if (editTexts.isNotEmpty()) {
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

        val editTexts = (0 until edtPanel.childCount)
            .map { edtPanel.getChildAt(it) }
            .filterIsInstance<EditText>()

        if (editTexts.isEmpty()) return

        // 2. Mapowanie tekstu na liczby (Double)
        val vals = editTexts.map { it.text.toString().toDoubleOrNull() ?: 0.0 }
        val pos = spinner1.selectedItemPosition

        // Tryb 8 (Środek) dopuszcza wartości 0 lub ujemne (współrzędne)
        val isZeroPointMode = pos == 8

        // 3. Walidacja: Dla trybów obliczeniowych (0-7) wartości muszą być > 0
        if (!isZeroPointMode && vals.any { it <= 0.0 }) {
            Toast.makeText(this, getString(R.string.error2), Toast.LENGTH_SHORT).show()
            return
        }

        // 4. Blok obliczeń
        val result = try {
            when (pos) {
                0 -> { // FREZOWANIE (Vc, D, fz, z)
                    val vc = vals[0]
                    val d = vals[1]
                    val fz = vals[2]
                    val z = vals[3]

                    val n = (vc * calcSys) / (Math.PI * d)
                    val vf = fz * z * n
                    CalculationResult(f = vf, s = n)
                }
                1 -> { // WIERCENIE / TOCZENIE (Vc, D, fn)
                    val vc = vals[0]
                    val d = vals[1]
                    val fn = vals[2]

                    val n = (vc * calcSys) / (Math.PI * d)
                    val vf = fn * n
                    CalculationResult(f = vf, s = n)
                }
                2, 3, 4, 5 -> { // TRYBY PIŁOWANIA / POSUWU NA ZĄB (fz)
                    // Ustalenie parametrów zależnie od specyfiki trybu
                    val fz = when (pos) {
                        2 -> (vals[0] * vals[1]) / (vals[3] * vals[2] * calcSys)
                        3 -> (vals[0] * vals[3]) / (vals[1] * vals[2] * calcSys)
                        4 -> (vals[0] * vals[2] * Math.PI) / (vals[4] * vals[1] * vals[3] * calcSys)
                        5 -> (vals[0] * vals[3] * Math.PI) / (vals[1] * vals[2] * calcSys)
                        else -> 0.0
                    }
                    CalculationResult(fz = fz, isSawMode = true)
                }
                6 -> { // KOMPENSACJA POSUWU (f_nom, D_zew, d_narz)
                    val dWorkpiece = vals[0]
                    val dTool = vals[1]
                    val fNominal = vals[2]

                    if (dWorkpiece == 0.0) throw ArithmeticException("D = 0")
                    val fActual = fNominal * ((dWorkpiece - dTool) / dWorkpiece)
                    CalculationResult(f = fActual)
                }
                7 -> { // DODATKOWE PARAMETRY (Vc, D, f)
                    val vc = vals[0]
                    val d = vals[1]
                    val f = vals[2]

                    val n = (vc * calcSys) / (Math.PI * d)
                    val vf = f * n
                    CalculationResult(f = vf, s = n)
                }
                8 -> { // ŚRODEK DETALU (X1, X2)
                    val x1 = vals[0]
                    val x2 = vals[1]
                    CalculationResult(center = (x1 + x2) / 2, isCenterMode = true)
                }
                else -> CalculationResult()
            }
        } catch (e: Exception) {
            val errorMsg = getString(R.string.calculation_error, e.message ?: "NaN")
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            CalculationResult()
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
                value3.text = String.format(java.util.Locale.US, "%.3f", res.center)
            }
            res.isSawMode -> {
                row1.visibility = View.GONE
                row2.visibility = View.GONE
                separator.visibility = View.GONE
                label3.text = getString(R.string.fz_colon)
                value3.text = String.format(java.util.Locale.US, "%.4f", res.fz)
            }
            else -> {
                label1.text = getString(R.string.f_colon)
                value1.text = res.f.roundToLong().toString()
                label2.text = getString(R.string.s_colon)
                value2.text = res.s.roundToLong().toString()

                if (res.fz > 0 && spinner1.selectedItemPosition != 0) {
                    label3.text = getString(R.string.fz_colon)
                    value3.text = String.format(java.util.Locale.US, "%.3f", res.fz)
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

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val resId = if (calcSys == 1000) R.string.info_1_m else R.string.info_1_inch

        tvInstructions.text = androidx.core.text.HtmlCompat.fromHtml(
            getString(resId),
            androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
        )

        fun refreshTable(sIdx: Int, tIdx: Int) {
            tableLayout.removeAllViews()
            lifecycleScope.launch {
                val data = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(this@ActivityMilling).threadDao().getThreads(sIdx, tIdx)
                }

                addTableHeaders(tableLayout, sIdx)

                data.forEach { thread ->
                    val row = LayoutInflater.from(this@ActivityMilling)
                        .inflate(R.layout.item_thread_row, tableLayout, false) as TableRow

                    row.findViewById<TextView>(R.id.col1).text = thread.name
                    row.findViewById<TextView>(R.id.col2).text = thread.pitch
                    row.findViewById<TextView>(R.id.col3).text = thread.holeMin
                    row.findViewById<TextView>(R.id.col4).text = thread.holeMax
                    row.findViewById<TextView>(R.id.col5).text = thread.holeSize

                    row.setOnClickListener {
                        val editTexts = (0 until edtPanel.childCount)
                            .map { edtPanel.getChildAt(it) }
                            .filterIsInstance<EditText>()

                        if (editTexts.size >= 3) {
                            val nominalDiameter = thread.name.replace(Regex("[^0-9.,]"), "").replace(",", ".")

                            editTexts[1].setText(nominalDiameter)
                            editTexts[2].setText(thread.pitch.replace(",", "."))

                            val message = getString(R.string.toast_thread_selected, thread.name)
                            Toast.makeText(this@ActivityMilling, message, Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                    }
                    tableLayout.addView(row)
                }
            }
        }

        // 3. GENEROWANIE PRZYCISKÓW KATEGORII
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
                    // Mapowanie kategorii na parametry bazy danych
                    val sIdx = if (index > 1) 2 else 0
                    refreshTable(sIdx, index % 2)
                }
            }
            buttonsPanel.addView(btn)
        }

        refreshTable(0, 0)
        dialogView.findViewById<Button>(R.id.btn_close_dialog).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun addTableHeaders(tableLayout: TableLayout, sIdx: Int) {
        val headerRow = TableRow(this).apply {
            setBackgroundColor("#333333".toColorInt())
            setPadding(0, dpToPx(4), 0, dpToPx(4))
        }

        val isInch = sIdx in listOf(2, 3, 4, 5)

        val headers = arrayOf(
            getString(R.string.header_name),
            if (isInch) getString(R.string.header_tpi) else getString(R.string.header_pitch),
            getString(R.string.header_min),
            getString(R.string.header_max),
            getString(R.string.header_drill)
        )

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

        if (item == 1) {
            showThreadDatabaseDialog()
            return
        }

        val isMetric = calcSys == 1000
        val resId = getInfoResourceId(item, isMetric)

        if (resId != 0) {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.window_information_modern, null)
            val content = dialogView.findViewById<TextView>(R.id.infoContent)
            val btnOk = dialogView.findViewById<Button>(R.id.btnOk)

            content.text = androidx.core.text.HtmlCompat.fromHtml(
                getString(resId),
                androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
            )

            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            btnOk.setOnClickListener { dialog.dismiss() }
            dialog.show()
        }
    }
    private fun getInfoResourceId(pos: Int, isMetric: Boolean): Int {
        return when (pos) {
            0 -> if (isMetric) R.string.info_0_m else R.string.info_0_inch
            2 -> if (isMetric) R.string.info_4_m else R.string.info_4_inch
            3 -> if (isMetric) R.string.info_5_m else R.string.info_5_inch
            4 -> if (isMetric) R.string.info_6_m else R.string.info_6_inch
            5 -> if (isMetric) R.string.info_7_m else R.string.info_7_inch
            6 -> if (isMetric) R.string.info_2_m else R.string.info_2_inch
            7 -> if (isMetric) R.string.info_3_m else R.string.info_3_inch
            8 -> if (isMetric) R.string.info_12_m else R.string.info_12_inch
            else -> 0 // Brak informacji dla danej pozycji
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