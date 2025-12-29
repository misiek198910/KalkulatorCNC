package com.example.calkulatorcnc.ui.activities

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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.example.calkulatorcnc.data.preferences.ClassPrefs
import com.example.calkulatorcnc.ui.adapters.PremiumSpinnerAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import getMaterialsList
import kotlin.math.atan
import kotlin.math.roundToLong

data class TurningResult(
    val f: Double = 0.0,
    val s: Double = 0.0,
    val tc: Double = 0.0,
    val q: Double = 0.0,
    val ra: Double = 0.0,
    val angle: Double = 0.0,
    val mode: ResultDisplayMode = ResultDisplayMode.STANDARD
)

enum class ResultDisplayMode { STANDARD, TIME, VOLUME, ROUGHNESS, TAPER, NONE }

class ActivityTourning : AppCompatActivity() {

    private lateinit var edtPanel: LinearLayout
    private lateinit var spinner1: Spinner
    private lateinit var btnClear: Button
    private lateinit var materialContainer: LinearLayout

    private var adView: AdView? = null
    private var calcSys: Int = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_turning)
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
        btnClear = findViewById(R.id.milingbutton2)
        spinner1 = findViewById(R.id.spinner1)
        materialContainer = findViewById(R.id.material_container)

        findViewById<View>(R.id.main).setOnClickListener { hideKeyboard() }
        findViewById<ImageButton>(R.id.button_back).setOnClickListener { finish() }
        findViewById<Button>(R.id.milingbutton1).setOnClickListener { calculate() }
        findViewById<Button>(R.id.milingbutton3).setOnClickListener { showInfo() }

        btnClear.setOnClickListener { clearInputs() }
    }

    private fun setupAds() {
        val adContainer = findViewById<FrameLayout>(R.id.adContainer)

        SubscriptionManager.getInstance(this).isPremium.observe(this) { isPremium ->
            if (isPremium) {
                // 1. CZYSZCZENIE: Ukrywamy i niszczymy reklamę
                adContainer.visibility = View.GONE
                adContainer.removeAllViews()
                adView?.destroy()
                adView = null
            } else if (resources.configuration.screenHeightDp >= 400) {
                // 2. WYŚWIETLANIE: Tylko jeśli nie ma aktywnej subskrypcji
                adContainer.visibility = View.VISIBLE

                if (adView == null) {
                    val newAdView = AdView(this)
                    adContainer.addView(newAdView)

                    // Ustawiamy dynamiczny rozmiar zamiast AdSize.BANNER
                    val adSize = getAdSize(adContainer)
                    newAdView.setAdSize(adSize)
                    newAdView.adUnitId = BuildConfig.ADMOB_BANNER_ID

                    adView = newAdView
                    newAdView.loadAd(AdRequest.Builder().build())
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
        val spinnerArray = resources.getStringArray(R.array.tourning_spinner1_data)
        val premiumPositions = listOf(3,4,5,6)

        val adapter = PremiumSpinnerAdapter(this, R.layout.spinner_item, spinnerArray, isPremium, premiumPositions)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item) // To jest teraz kluczowe!
        spinner1.adapter = adapter
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

        if (!isPremium && (position in listOf(3, 4, 5, 6))) {
            edtPanel.removeAllViews()
            materialContainer.removeAllViews()
            findViewById<Button>(R.id.milingbutton1).isEnabled = false
            showPremiumRequired()
            return
        }

        findViewById<Button>(R.id.milingbutton1).isEnabled = true
        edtPanel.removeAllViews()
        materialContainer.removeAllViews()

        // Przycisk materiału dla Tokowania i Wytaczania
        if (position in listOf(0, 1, 2)) {
            val btnMaterial = com.google.android.material.button.MaterialButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(55)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    setMargins(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(12))
                }
                text = getString(R.string.material)
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.button_style)
                backgroundTintList = null
                cornerRadius = dpToPx(12)
                setOnClickListener { showMaterialSelectionDialog(position) }
            }
            materialContainer.addView(btnMaterial)
        }

        edtPanel.orientation = if (isLandscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL

        val hints = when (position) {
            0 -> arrayOf(R.string.VC, R.string.AP, R.string.Fn, R.string.KC, R.string.DM) //skrawanie
            1 -> arrayOf(R.string.VC, R.string.DM, R.string.Fn) // Wytaczanie
            2 -> arrayOf(R.string.VC, R.string.DM, R.string.P) // Gwintowanie (Przeniesione tutaj)
            3 -> arrayOf(R.string.LM, R.string.VC, R.string.DC, R.string.Fn) // Czas
            4 -> arrayOf(R.string.VC, R.string.AP, R.string.Fn) // Wydajność
            5 -> arrayOf(R.string.Fn, R.string.RE)             // Chropowatość
            6 -> arrayOf(R.string.DC, R.string.dm, R.string.LM) // Stożek
            else -> emptyArray()
        }

        hints.forEachIndexed { index, hintRes ->
            edtPanel.addView(createEditText(index, getString(hintRes)))
        }
    }

    private fun showMaterialSelectionDialog(operationPos: Int) {
        val isPremium = SubscriptionManager.getInstance(this).isPremium.value ?: false
        val materials = getMaterialsList(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.window_materials_modern, null)
        val container = dialogView.findViewById<LinearLayout>(R.id.materialsContainer)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        materials.forEachIndexed { index, material ->
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_material, container, false)
            val tvName = itemView.findViewById<TextView>(R.id.materialName)
            val ivLock = itemView.findViewById<ImageView>(R.id.lockIcon)

            tvName.text = material.name

            if (!isPremium && index > 0) {
                ivLock.visibility = View.VISIBLE
                ivLock.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.gold))
            }

            itemView.setOnClickListener {
                if (!isPremium && index > 0) {
                    dialog.dismiss()
                    showPremiumRequired()
                } else {
                    // Pobieramy aktualną listę pól tekstowych
                    val editTexts = (0 until edtPanel.childCount)
                        .map { edtPanel.getChildAt(it) }
                        .filterIsInstance<EditText>()

                    when (operationPos) {
                        0 -> { // PARAMETRY SKRAWANIA (VC, AP, Fn, KC, DM)
                            if (editTexts.size >= 5) {
                                editTexts[0].setText(material.vcTurning.toString())
                                editTexts[2].setText(material.fnTurning.toString())
                                editTexts[3].setText(material.kc.toString())
                            }
                        }
                        1 -> { // WYTACZANIE (VC, DM, Fn)
                            if (editTexts.size >= 3) {
                                editTexts[0].setText(material.vcTurning.toString())
                                editTexts[2].setText(material.fnTurning.toString())
                            }
                        }
                        2 -> { // GWINTOWANIE (VC, DM, JT)
                            if (editTexts.isNotEmpty()) {
                                editTexts[0].setText(material.vcThreading.toString())
                            }
                        }
                    }

                    val msg = getString(R.string.toast_material_selected, material.name)
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

                    dialog.dismiss()
                }
            }
            container.addView(itemView)
        }

        dialogView.findViewById<Button>(R.id.btnCancelMaterials).setOnClickListener { dialog.dismiss() }
        dialog.show()
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

    private fun calculate() {
        // 1. Pobieranie widoków EditText z panelu
        val editTexts = (0 until edtPanel.childCount)
            .map { edtPanel.getChildAt(it) }
            .filterIsInstance<EditText>()

        if (editTexts.isEmpty()) return

        // 2. Mapowanie tekstu na liczby (Double)
        val vals = editTexts.map { it.text.toString().toDoubleOrNull() ?: 0.0 }

        // 3. Walidacja: Wszystkie pola muszą być większe od 0 (z wyjątkiem średnicy 'd' w stożku)
        val currentPos = spinner1.selectedItemPosition
        if (vals.any { it <= 0.0 } && currentPos != 6) {
            Toast.makeText(this, getString(R.string.error2), Toast.LENGTH_SHORT).show()
            return
        }

        // 4. Obliczenia zależne od wybranego trybu
        val result = try {
            when (currentPos) {
                0, 1, 2 -> { // PARAMETRY / WYTACZANIE / GWINTOWANIE
                    // vals[0]: Vc (m/min), vals[1]: D (mm), vals[2]: fn (mm/obr)
                    val vc = vals[0]
                    val d = vals[1]
                    val fn = vals[2]

                    val n = (vc * calcSys) / (Math.PI * d)
                    val vf = fn * n
                    TurningResult(s = n, f = vf, mode = ResultDisplayMode.STANDARD)
                }

                3 -> { // CZAS MASZYNOWY
                    // vals[0]: L (mm), vals[1]: Vc (m/min), vals[2]: D (mm), vals[3]: fn (mm/obr)
                    val l = vals[0]
                    val vc = vals[1]
                    val d = vals[2]
                    val fn = vals[3]

                    val n = (vc * calcSys) / (Math.PI * d)
                    val tc = l / (fn * n)
                    TurningResult(tc = tc, mode = ResultDisplayMode.TIME)
                }

                4 -> { // WYDAJNOŚĆ (Q)
                    // vals[0]: Vc, vals[1]: ap, vals[2]: fn
                    val q = vals[0] * vals[1] * vals[2]
                    TurningResult(q = q, mode = ResultDisplayMode.VOLUME)
                }

                5 -> { // CHROPOWATOŚĆ (Ra)
                    // vals[0]: fn (mm/obr), vals[1]: re (promień płytki mm)
                    val fn = vals[0]
                    val re = vals[1]

                    // Teoretyczna chropowatość Ra w mikrometrach (µm)
                    val ra = ((fn * fn) / (32 * re)) * 1000
                    TurningResult(ra = ra, mode = ResultDisplayMode.ROUGHNESS)
                }

                6 -> { // STOŻEK (Kąt alfa)
                    // vals[0]: D (duża śr.), vals[1]: d (mała śr.), vals[2]: L (długość)
                    val bigD = vals[0]
                    val smallD = vals[1]
                    val length = vals[2]

                    if (length <= 0) throw ArithmeticException(getString(R.string.angleInfo))

                    val angleRad = atan((bigD - smallD) / (2 * length))
                    val angleDeg = Math.toDegrees(angleRad)
                    TurningResult(angle = angleDeg, mode = ResultDisplayMode.TAPER)
                }

                else -> TurningResult()
            }
        } catch (e: Exception) {
            val errorText = getString(R.string.calculation_error, e.message ?: "Unknown")
            Toast.makeText(this, errorText, Toast.LENGTH_SHORT).show()
            TurningResult()
        }

        // 5. Wyświetlenie okna z wynikiem
        if (result.mode != ResultDisplayMode.NONE) {
            showResultDialog(result)
        }
    }

    private fun showResultDialog(res: TurningResult) {
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

        val locale = java.util.Locale.US // Wymuszamy kropkę jako separator

        when (res.mode) {
            ResultDisplayMode.TIME -> {
                setupSingleRowDisplay(row1, row2, separator)
                label3.text = getString(R.string.tc_colon)
                value3.text = String.format(locale, getString(R.string.unit_min), res.tc)
            }
            ResultDisplayMode.VOLUME -> {
                setupSingleRowDisplay(row1, row2, separator)
                label3.text = getString(R.string.q_colon)
                value3.text = String.format(locale, getString(R.string.unit_cm3_min), res.q)
            }
            ResultDisplayMode.STANDARD -> {
                label1.text = getString(R.string.f_colon)
                value1.text = String.format(locale, "%.2f", res.f)
                label2.text = getString(R.string.s_colon)
                value2.text = res.s.roundToLong().toString()
                row3.visibility = View.GONE
                separator.visibility = View.GONE
            }
            ResultDisplayMode.ROUGHNESS -> {
                setupSingleRowDisplay(row1, row2, separator)
                label3.text = getString(R.string.roughness_label)
                value3.text = String.format(locale, getString(R.string.unit_um), res.ra)
            }
            ResultDisplayMode.TAPER -> {
                setupSingleRowDisplay(row1, row2, separator)
                label3.text = getString(R.string.taper_label)
                value3.text = String.format(locale, getString(R.string.unit_deg), res.angle)
            }
            ResultDisplayMode.NONE -> {
                return
            }
        }

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun setupSingleRowDisplay(row1: View, row2: View, separator: View) {
        row1.visibility = View.GONE
        row2.visibility = View.GONE
        separator.visibility = View.GONE
    }

    private fun showPremiumRequired() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.window_premium_upgrade, null)
        val btnGo = dialogView.findViewById<Button>(R.id.btnGoToPremium)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelPremium)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
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
        val position = spinner1.selectedItemPosition
        val isMetric = (calcSys == 1000)

        // Mapowanie pozycji spinnera na konkretne ID zasobów (Type-safe)
        val resId = when (position) {
            0 -> if (isMetric) R.string.info_8_m else R.string.info_8_inch
            1 -> if (isMetric) R.string.info_9_m else R.string.info_9_inch
            2 -> if (isMetric) R.string.info_13_m else R.string.info_13_inch // Gwintowanie na poz 2
            3 -> if (isMetric) R.string.info_10_m else R.string.info_10_inch // Czas na poz 3
            4 -> if (isMetric) R.string.info_11_m else R.string.info_11_inch // Wydajność na poz 4
            5 -> if (isMetric) R.string.info_14_m else R.string.info_14_inch // Chropowatość
            6 -> if (isMetric) R.string.info_15_m else R.string.info_15_inch // Stożek
            else -> null
        }

        resId?.let { id ->
            val dialogView = LayoutInflater.from(this).inflate(R.layout.window_information_modern, null)
            val content = dialogView.findViewById<TextView>(R.id.infoContent)
            val btnOk = dialogView.findViewById<Button>(R.id.btnOk)

            // Wyświetlanie tekstu z obsługą HTML (tagi <br>, <b> itp.)
            content.text = android.text.Html.fromHtml(
                getString(id),
                android.text.Html.FROM_HTML_MODE_LEGACY
            )

            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            // Ustawienie przezroczystego tła dla okna, aby widoczne były zaokrąglone rogi MaterialCardView
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            btnOk.setOnClickListener { dialog.dismiss() }
            dialog.show()
        } ?: run {
            // Opcjonalnie: obsługa błędu, jeśli nie znaleziono mapowania
            Toast.makeText(this, "Brak informacji dla tego parametru", Toast.LENGTH_SHORT).show()
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