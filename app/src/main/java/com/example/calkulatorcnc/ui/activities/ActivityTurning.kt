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
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.example.calkulatorcnc.data.db.AppDatabase
import com.example.calkulatorcnc.data.preferences.ClassPrefs
import com.example.calkulatorcnc.data.repository.ToolRepository
import com.example.calkulatorcnc.ui.adapters.MainDropdownAdapter
import com.example.calkulatorcnc.viewModel.ToolViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import kotlinx.coroutines.launch
import kotlin.math.atan
import kotlin.math.roundToLong
import androidx.core.view.isNotEmpty

enum class TurningResultMode { STANDARD, TIME, VOLUME, ROUGHNESS, TAPER, NONE }

data class TurningCalculationResult(
    val f: Double = 0.0,
    val s: Double = 0.0,
    val tc: Double = 0.0,
    val q: Double = 0.0,
    val ra: Double = 0.0,
    val angle: Double = 0.0,
    val mode: TurningResultMode = TurningResultMode.NONE
)

class ActivityTourning : AppCompatActivity() {
    private lateinit var edtPanel: LinearLayout
    private lateinit var mainDropdown: AutoCompleteTextView
    private var selectedOperationIndex: Int = 0
    private lateinit var btnClear: androidx.appcompat.widget.AppCompatImageButton
    private var adView: AdView? = null
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var toolViewModel: ToolViewModel

    private var vcTextWatcher: TextWatcher? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_turning)

        createViewAEdgetoEdgeForAds()
        initUI()
        setupAds()
        setupSpinner()
        analytics = Firebase.analytics

        // Inicjalizacja ViewModelu narzędziowego
        val dao = AppDatabase.getDatabase(this).toolNewDao()
        val repository = ToolRepository(dao)
        toolViewModel = ToolViewModel(repository)

        setupToolCascade()
        observeToolData()

        findViewById<LinearLayout>(R.id.collapsibleHeader).setOnClickListener {
            val content = findViewById<View>(R.id.expandableContent)
            toggleSelectionCard(!content.isVisible)
        }
    }

    private fun createViewAEdgetoEdgeForAds() {
        val mainRoot = findViewById<View>(R.id.main)
        val customHeader = findViewById<LinearLayout>(R.id.customHeader)
        val buttonsPanel = findViewById<LinearLayout>(R.id.layout)
        val bottomBar = findViewById<LinearLayout>(R.id.bottom_container)
        val adContainer = findViewById<FrameLayout>(R.id.adContainer)
        val adContainerLayout = findViewById<FrameLayout>(R.id.adContainerLayout)

        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density

            mainRoot.setPadding(0, 0, 0, 0)

            // 1. Góra - standardowo
            customHeader?.updatePadding(top = systemBars.top)

            val isChromebook = android.os.Build.MODEL.contains("sdk_gpc", ignoreCase = true)
            val bottomExtraFix = if (isChromebook) (32 * density).toInt() else 0

            // 2. Kontener dolny - usuwamy systemBars.bottom z paddingu!
            // Zostawiamy tylko boczny padding i ewentualny fix dla Chromebooka.
            bottomBar?.updatePadding(
                left = systemBars.left + (10 * density).toInt(),
                right = systemBars.right + (8 * density).toInt(),
                bottom = bottomExtraFix
            )

            // 3. KLUCZOWA POPRAWKA: Reklama dostaje margines od systemowych barów
            // To wypchnie reklamę w górę, poza obszar gestów "Home".
            adContainerLayout?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom // TO JEST ROZWIĄZANIE
                topMargin = 0
                leftMargin = 0
                rightMargin = 0
            }

            // 4. Panel przycisków - upewniamy się, że nie ma zbędnych wcięć
            buttonsPanel?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = 0
                rightMargin = 0
                bottomMargin = 0
            }

            insets
        }
        adContainer?.post { setupAds() }
    }

    private fun initUI() {
        edtPanel = findViewById(R.id.buttons_panel)
        mainDropdown = findViewById(R.id.mainOperationAutoComplete)
        btnClear = findViewById(R.id.milingbutton2)

        findViewById<View>(R.id.main).setOnClickListener { hideKeyboard() }
        findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.button_back).setOnClickListener { finish() }

        findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.milingbutton1).setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            hideKeyboard()
            calculate()
        }

        btnClear.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            clearInputs()
            updateMaterialVisuals(-1)
            toolViewModel.setIsoGroup("")
        }

        findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.milingbutton3).setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            showInfo()
        }
    }

    private fun setupAds() {
        val adContainer = findViewById<FrameLayout>(R.id.adContainer) ?: return
        val adContainerLayout = findViewById<FrameLayout>(R.id.adContainerLayout)

        SubscriptionManager.getInstance(this).isPremium.observe(this) { isPremium ->
            if (isPremium) {
                adContainerLayout?.visibility = View.GONE
                adContainer.visibility = View.GONE
                adContainer.removeAllViews()
                adView?.destroy()
                adView = null
            } else {
                val screenHeightDp = resources.configuration.screenHeightDp
                if (screenHeightDp >= 400) {
                    adContainerLayout?.visibility = View.VISIBLE
                    adContainer.visibility = View.VISIBLE
                    if (adView == null) {
                        val newAdView = AdView(this).apply {
                            adUnitId = BuildConfig.ADMOB_BANNER_ID
                            setAdSize(getAdSize(adContainer))
                        }
                        adView = newAdView
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
        if (adWidthPixels == 0f) adWidthPixels = displayMetrics.widthPixels.toFloat()
        val density = displayMetrics.density
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, (adWidthPixels / density).toInt())
    }

    private fun setupSpinner() {
        val isPremium = SubscriptionManager.getInstance(this).isPremium.value ?: false
        val operationsArray = resources.getStringArray(R.array.tourning_spinner1_data)
        // Czas, Wydajność, Chropowatość, Stożek są Premium
        val premiumPositions = listOf(3, 4, 5, 6)

        val adapter = MainDropdownAdapter(
            this,
            R.layout.spinner_dropdown_item,
            operationsArray,
            isPremium,
            premiumPositions
        )

        mainDropdown.setAdapter(adapter)
        mainDropdown.setText(operationsArray[selectedOperationIndex], false)
        updateDynamicInputs(selectedOperationIndex)

        mainDropdown.setOnItemClickListener { _, _, position, _ ->
            if (!isPremium && premiumPositions.contains(position)) {
                mainDropdown.setText(operationsArray[selectedOperationIndex], false)
                showPremiumRequired()
            } else {
                selectedOperationIndex = position
                updateDynamicInputs(position)
                clearInputs()
                resetToolCascade()
                setupToolCascade()
            }
        }
    }

    private fun resetToolCascade() {

        findViewById<TextInputLayout>(R.id.layoutDiameter).visibility = View.GONE
        findViewById<TextInputLayout>(R.id.layoutModel).visibility = View.GONE
        findViewById<AutoCompleteTextView>(R.id.categoryAutoComplete).setText("", false)
        findViewById<AutoCompleteTextView>(R.id.diameterAutoComplete).setText("", false)
        findViewById<AutoCompleteTextView>(R.id.modelAutoComplete).setText("", false)
        findViewById<TextView>(R.id.tvSelectionSummary).text = getString(R.string.select_tool_hint)

        edtPanel.findViewById<View>(R.id.vc_chips_container)?.let { edtPanel.removeView(it) }
    }

    private fun updateDynamicInputs(position: Int) {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val showToolSelection = (position in 0..2)
        findViewById<View>(R.id.toolSelectionCard).visibility = if (showToolSelection) View.VISIBLE else View.GONE

        edtPanel.removeAllViews()
        edtPanel.orientation = if (isLandscape) LinearLayout.VERTICAL else LinearLayout.VERTICAL

        val hints = when (position) {
            0 -> arrayOf(R.string.DM, R.string.VC, R.string.Fn, R.string.AP, R.string.KC)
            1 -> arrayOf(R.string.VC, R.string.DM, R.string.Fn)
            2 -> arrayOf(R.string.VC, R.string.DM, R.string.JT)
            3 -> arrayOf(R.string.VC, R.string.DM, R.string.Fn, R.string.LM)
            4 -> arrayOf(R.string.VC, R.string.AP, R.string.Fn)
            5 -> arrayOf(R.string.Fn, R.string.RE)
            6 -> arrayOf(R.string.D, R.string.d, R.string.L)
            else -> emptyArray()
        }

        hints.forEachIndexed { index, hintRes ->
            val isLast = index == hints.size - 1
            edtPanel.addView(createEditText(index, getString(hintRes), isLast))
        }
    }

    private fun createEditText(index: Int, hintText: String, isLast: Boolean) = EditText(this).apply {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // Dostosowanie wymiarów i marginesów do orientacji
        layoutParams = if (isLandscape) {
            LinearLayout.LayoutParams(dpToPx(240), dpToPx(44)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
            }
        } else {
            LinearLayout.LayoutParams(dpToPx(280), dpToPx(48)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            }
        }
        setPadding(dpToPx(12), 0, dpToPx(12), 0)

        if (index == 0) {
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    // Wywołujemy istniejącą funkcję zwijania
                    toggleSelectionCard(false)
                }
            }
        }

        isFocusable = true
        isFocusableInTouchMode = true
        elevation = dpToPx(2).toFloat()

        textSize = if (isLandscape) 14f else 20f
        setTextColor(Color.WHITE)
        setHintTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.textColor_hint))
        hint = hintText
        gravity = Gravity.CENTER
        setBackgroundResource(R.drawable.edittext_style)

        // Konfiguracja typu wprowadzania
        inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL or
                InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        // --- NOWA LOGIKA KLAWIATURY ---
        imeOptions = if (isLast) {
            android.view.inputmethod.EditorInfo.IME_ACTION_DONE
        } else {
            android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
        }

        if (isLast) {
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard()
                    clearFocus()
                    calculate() // Wywołuje Twoją funkcję obliczeń
                    true
                } else false
            }
        }
        // ------------------------------

        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO

        addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val anyNotEmpty = (0 until edtPanel.childCount)
                    .map { edtPanel.getChildAt(it) }
                    .filterIsInstance<EditText>()
                    .any { it.text.isNotEmpty() }
                btnClear.isEnabled = anyNotEmpty
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupToolCascade() {
        val isPremium = SubscriptionManager.getInstance(this).isPremium.value ?: false
        val materialButtons = mapOf(R.id.btnIsoP to "P", R.id.btnIsoM to "M", R.id.btnIsoK to "K", R.id.btnIsoN to "N", R.id.btnIsoS to "S", R.id.btnIsoH to "H")

        materialButtons.forEach { (id, group) ->
            findViewById<View>(id)?.setOnClickListener {
                clearInputs()
                toolViewModel.setIsoGroup(group)
                updateMaterialVisuals(id)
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }
        }

        val categoryView = findViewById<AutoCompleteTextView>(R.id.categoryAutoComplete)
        var originalCategories = listOf<String>()

        lifecycleScope.launch {
            toolViewModel.categories.collect { categories ->

                val techKeywords = when (selectedOperationIndex) {
                    0 -> listOf("Toczenie", "Płytki")
                    1 -> listOf("Wytacz")
                    2 -> listOf("Gwint", "Wygniat", "Toczenie gwintów", "Płytki")
                    else -> emptyList()
                }

                val filtered = if (techKeywords.isEmpty()) {
                    emptyList()
                } else {
                    categories.filter { category ->
                        val matchesKeyword = techKeywords.any { key -> category.contains(key, ignoreCase = true) }

                        when (selectedOperationIndex) {
                            0 -> {
                                // W ogólnym toczeniu: bierzemy "Toczenie/Płytki", ale WYKLUCZAMY gwinty
                                matchesKeyword && !category.contains("gwint", ignoreCase = true)
                            }

                            2 -> {

                                matchesKeyword && (category.contains("gwint", ignoreCase = true) ||
                                        category.contains("wygniat", ignoreCase = true))
                            }

                            else -> matchesKeyword
                        }
                    }.sorted()
                }

                originalCategories = filtered
                val translatedCategories = filtered.map { getTranslatedCategory(it) }

                val premiumKeywords = listOf("FreeTurn", "Głowica")
                val toolPremiumPositions = filtered.indices.filter { i ->
                    premiumKeywords.any { key -> filtered[i].contains(key, ignoreCase = true) }
                }

                val adapter = MainDropdownAdapter(
                    this@ActivityTourning,
                    R.layout.spinner_dropdown_item,
                    translatedCategories.toTypedArray(),
                    isPremium,
                    toolPremiumPositions
                )
                categoryView.setAdapter(adapter)
            }
        }

        categoryView.setOnItemClickListener { _, _, position, _ ->
            val selectedPolishName = originalCategories[position]
            findViewById<View>(R.id.layoutDiameter).visibility = View.VISIBLE
            toolViewModel.setCategory(selectedPolishName)
        }

        // Średnica / Promień (Krok 2)
        val diameterView = findViewById<AutoCompleteTextView>(R.id.diameterAutoComplete)
        lifecycleScope.launch {
            toolViewModel.availableDiameters.collect { diameters ->
                val adapter = MainDropdownAdapter(this@ActivityTourning, R.layout.spinner_dropdown_item, diameters.map { it.toString() }.toTypedArray(), isPremium, emptyList())
                diameterView.setAdapter(adapter)
            }
        }

        diameterView.setOnItemClickListener { _, _, position, _ ->
            val selected = diameterView.adapter.getItem(position).toString().toDouble()
            toolViewModel.setDiameter(selected)
            findViewById<View>(R.id.layoutModel).visibility = View.VISIBLE
        }

        // Model (Krok 3)
        val modelView = findViewById<AutoCompleteTextView>(R.id.modelAutoComplete)
        lifecycleScope.launch {
            toolViewModel.availableModels.collect { models ->
                // Mapujemy listę modeli z bazy przez nasz translator
                val translatedModels = models.map { getTranslatedModel(it) }

                val adapter = MainDropdownAdapter(
                    this@ActivityTourning, // lub ActivityMilling
                    R.layout.spinner_dropdown_item,
                    translatedModels.toTypedArray(), // Używamy przetłumaczonej listy
                    isPremium,
                    emptyList()
                )
                modelView.setAdapter(adapter)

                modelView.setOnItemClickListener { parent, _, position, _ ->
                    val adapter = modelView.adapter as MainDropdownAdapter
                    if (!isPremium && adapter.premiumPositions.contains(position)) {
                        modelView.setText("", false)
                        showPremiumRequired()
                    } else {
                        // POBIERAMY ORYGINAŁ Z ViewModelu (nie z przetłumaczonego adaptera!)
                        val originalModels = toolViewModel.availableModels.value
                        val selectedOriginal = originalModels[position]

                        toolViewModel.setModel(selectedOriginal)
                        toggleSelectionCard(false)
                    }
                }
            }
        }
    }

    private fun observeToolData() {
        lifecycleScope.launch {
            toolViewModel.toolParameters.collect { tool ->
                tool?.let { currentTool ->
                    val modelAutoComplete = findViewById<AutoCompleteTextView>(R.id.modelAutoComplete)
                    if (modelAutoComplete.text.isNotEmpty()) {
                        edtPanel.isVisible = true
                        val editTexts = (0 until edtPanel.childCount).map { edtPanel.getChildAt(it) }.filterIsInstance<EditText>()

                        if (editTexts.size >= 3) {
                            val vcEditText = editTexts[0]
                            val diameterEditText = editTexts[1]
                            val feedEditText = editTexts[2] // fn w toczeniu / P w gwintowaniu

                            val isThreading = selectedOperationIndex == 2 // LOGIKA GWINTOWANIA

                            // A. RESET POPRZEDNIEGO STANU
                            vcTextWatcher?.let { vcEditText.removeTextChangedListener(it) }
                            edtPanel.findViewById<View>(R.id.vc_chips_container)?.let { edtPanel.removeView(it) }
                            edtPanel.findViewById<View>(R.id.tv_drill_hint_text)?.let { edtPanel.removeView(it) } // Reset podpowiedzi wiertła

                            val rawMin = currentTool.vcMin
                            val rawMax = currentTool.vcMax
                            val optRounded = Math.round((rawMin + rawMax) / 2).toInt()

                            // B. NOWY WATCHER (KOLOROWANIE VC)
                            vcTextWatcher = object : TextWatcher {
                                override fun afterTextChanged(s: Editable?) {
                                    val input = s.toString().toDoubleOrNull() ?: 0.0
                                    applyVcColor(vcEditText, input, rawMin, rawMax)
                                }
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            }

                            // C. WPISANIE DANYCH STARTOWYCH
                            vcEditText.setText(optRounded.toString())
                            vcEditText.addTextChangedListener(vcTextWatcher)
                            applyVcColor(vcEditText, optRounded.toDouble(), rawMin, rawMax)

                            // D. OBSŁUGA POSUWU / SKOKU (fn / P)
                            feedEditText.setText(currentTool.feedStep.toString())

                            if (isThreading) {
                                // W gwintowaniu skok (P) jest narzucony przez narzędzie
                                feedEditText.alpha = 0.7f
                                // Opcjonalnie: feedEditText.isEnabled = false
                            } else {
                                feedEditText.alpha = 1.0f
                                feedEditText.isEnabled = true
                            }

                            // E. DODANIE PRZYCISKÓW CHIPS (MIN / OPT / MAX)
                            val chipsLayout = LinearLayout(this@ActivityTourning).apply {
                                id = R.id.vc_chips_container
                                orientation = LinearLayout.HORIZONTAL
                                gravity = Gravity.CENTER
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply { setMargins(0, 0, 0, dpToPx(8)) }
                            }

                            val fastSettings = listOf(
                                Triple(getString(R.string.label_min), rawMin.toInt(), "#FFD740"),
                                Triple(getString(R.string.label_opt), optRounded, "#4CAF50"),
                                Triple(getString(R.string.label_max), rawMax.toInt(), "#FF5252")
                            )

                            fastSettings.forEach { (label, value, colorHex) ->
                                val chip = Button(this@ActivityTourning, null, 0, com.google.android.material.R.style.Widget_MaterialComponents_Button_TextButton).apply {
                                    text = label
                                    textSize = 13f
                                    setTypeface(null, android.graphics.Typeface.BOLD)
                                    setTextColor(colorHex.toColorInt())
                                    layoutParams = LinearLayout.LayoutParams(dpToPx(80), dpToPx(40))
                                    setOnClickListener {
                                        it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                                        vcEditText.setText(value.toString())
                                    }
                                }
                                chipsLayout.addView(chip)
                            }
                            edtPanel.addView(chipsLayout, 1)

                            // F. PODPOWIEDŹ: WIERTŁO POD GWINT (Dla gwintowników/wygniataków tokarskich)
                            val isTap = currentTool.toolCategory.contains("Gwint", ignoreCase = true) ||
                                    currentTool.toolCategory.contains("Wygniat", ignoreCase = true)

                            if (isThreading && isTap) {
                                val drillSize = if (currentTool.toolCategory.contains("Wygniat", ignoreCase = true)) {
                                    currentTool.diameter - (0.5 * currentTool.feedStep)
                                } else {
                                    currentTool.diameter - currentTool.feedStep
                                }
                                val tvDrillHint = TextView(this@ActivityTourning).apply {
                                    id = R.id.tv_drill_hint_text
                                    text = "${getString(R.string.drill_hint_label)} Ø${String.format("%.2f", drillSize)}"
                                    setTextColor(Color.parseColor("#FFD740"))
                                    textSize = 14f
                                    setTypeface(null, android.graphics.Typeface.BOLD)
                                    gravity = Gravity.CENTER
                                    setPadding(0, dpToPx(4), 0, dpToPx(8))
                                }
                                edtPanel.addView(tvDrillHint)
                            }

                            // G. AKTUALIZACJA PASKA PODSUMOWANIA
                            // W wytaczaniu (index 1) używamy rε, w reszcie Ø
                            val label = if (selectedOperationIndex == 1) "rε" else "Ø"
                            findViewById<TextView>(R.id.tvSelectionSummary).text =
                                "${currentTool.materialGroup} | ${getTranslatedCategory(currentTool.toolCategory)} | $label: ${currentTool.diameter}"

                            Toast.makeText(this@ActivityTourning, getString(R.string.parameters_load), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun applyVcColor(editText: EditText, value: Double, min: Double, max: Double) {
        when {
            value >= (max - 0.5) -> editText.setTextColor("#FF5252".toColorInt())
            value <= (min + 0.5) -> editText.setTextColor("#FFD740".toColorInt())
            else -> editText.setTextColor("#4CAF50".toColorInt())
        }
    }

    private fun calculate() {
        val editTexts = (0 until edtPanel.childCount).map { edtPanel.getChildAt(it) }.filterIsInstance<EditText>()
        if (editTexts.isEmpty()) return

        val vals = editTexts.map { it.text.toString().toDoubleOrNull() ?: 0.0 }
        val currentCalcSys = if (ClassPrefs().loadPrefInt(this, "calcsys_data") == 1) 12 else 1000

        if (vals.any { it <= 0.0 } && selectedOperationIndex != 6) {
            Toast.makeText(this, getString(R.string.error2), Toast.LENGTH_SHORT).show()
            return
        }

        val result = try {
            when (selectedOperationIndex) {
                0, 1, 2 -> { // STANDARD / WYTACZANIE / GWINTOWANIE
                    // vals[0] = DM, vals[1] = VC, vals[2] = Fn
                    val n = (vals[1] * currentCalcSys) / (Math.PI * vals[0])
                    val vf = vals[2] * n
                    TurningCalculationResult(s = n, f = vf, mode = TurningResultMode.STANDARD)
                }
                3 -> { // CZAS (Zakładając kolejność: L, DM, VC, Fn)
                    // vals[0]=Długość(L), vals[1]=DM, vals[2]=VC, vals[3]=Fn
                    val n = (vals[2] * currentCalcSys) / (Math.PI * vals[1])
                    val tc = vals[0] / (vals[3] * n)
                    TurningCalculationResult(tc = tc, mode = TurningResultMode.TIME)
                }
                4 -> { // OBJĘTOŚĆ (Zakładając: AP, Fn, VC)
                    // Q = ap * fn * vc
                    TurningCalculationResult(q = vals[0] * vals[1] * vals[2], mode = TurningResultMode.VOLUME)
                }
                5 -> { // Ra (Zakładając: Fn, Promień płytki R)
                    // Ra ≈ (fn² / (32 * R)) * 1000
                    val ra = ((vals[0] * vals[0]) / (32 * vals[1])) * 1000
                    TurningCalculationResult(ra = ra, mode = TurningResultMode.ROUGHNESS)
                }
                6 -> { // STOŻEK (D, d, L)
                    // vals[0]=D, vals[1]=d, vals[2]=L
                    val angle = Math.toDegrees(kotlin.math.atan((vals[0] - vals[1]) / (2 * vals[2])))
                    TurningCalculationResult(angle = angle, mode = TurningResultMode.TAPER)
                }
                else -> TurningCalculationResult()
            }
        } catch (e: Exception) {
            TurningCalculationResult()
        }

        showResultDialog(result)
    }

    private fun showResultDialog(res: TurningCalculationResult) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.window_calculation_modern, null)

        val l1 = dialogView.findViewById<TextView>(R.id.label1)
        val v1 = dialogView.findViewById<TextView>(R.id.value1)
        val l2 = dialogView.findViewById<TextView>(R.id.label2)
        val v2 = dialogView.findViewById<TextView>(R.id.value2)
        val l3 = dialogView.findViewById<TextView>(R.id.label3)
        val v3 = dialogView.findViewById<TextView>(R.id.value3)
        val btnCopy = dialogView.findViewById<ImageButton>(R.id.btnCopyResults)

        // Pobieramy informację o systemie (0 = mm, 1 = cale)
        val isInch = ClassPrefs().loadPrefInt(this, "calcsys_data") == 1

        when (res.mode) {
            TurningResultMode.STANDARD -> {
                // Ustawiamy etykiety zależnie od systemu
                l1.text = getString(if (isInch) R.string.res_f_inch else R.string.res_f_m)
                l2.text = getString(if (isInch) R.string.res_n_inch else R.string.res_n_m)

                // FORMATOWANIE:
                // n (obroty) - zazwyczaj całkowite, więc %.0f
                v2.text = String.format("%.0f", res.s)

                // F (posuw) - tutaj kluczowa zmiana!
                // Dla cali dajemy 2 miejsca po przecinku (%.2f), dla metrycznych 1 (%.1f)
                v1.text = if (isInch) String.format("%.2f", res.f) else String.format("%.1f", res.f)

                dialogView.findViewById<View>(R.id.row3).visibility = View.GONE
            }
            TurningResultMode.TIME -> {
                l3.text = getString(R.string.res_tc)
                v3.text = String.format("%.2f", res.tc)
                dialogView.findViewById<View>(R.id.row1).visibility = View.GONE
                dialogView.findViewById<View>(R.id.row2).visibility = View.GONE
            }
            TurningResultMode.ROUGHNESS -> {
                l3.text = getString(R.string.res_ra)
                v3.text = String.format("%.2f", res.ra)
                dialogView.findViewById<View>(R.id.row1).visibility = View.GONE
                dialogView.findViewById<View>(R.id.row2).visibility = View.GONE
            }
            TurningResultMode.TAPER -> {
                l3.text = getString(R.string.res_angle)
                v3.text = String.format("%.3f", res.angle)
                dialogView.findViewById<View>(R.id.row1).visibility = View.GONE
                dialogView.findViewById<View>(R.id.row2).visibility = View.GONE
            }
            else -> {}
        }

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        // LOGIKA KOPIOWANIA
        btnCopy?.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)

            val sb = StringBuilder()

            // 1. Dynamiczny nagłówek zależny od trybu
            val header = getString(R.string.calculateResult)

            sb.append(header).append("\n---\n") // Dodajemy linię oddzielającą dla czytelności

            // 2. Pobieranie danych z widocznych rzędów
            if (dialogView.findViewById<View>(R.id.row1).isVisible) {
                sb.append("${l1.text} ${v1.text}\n")
            }
            if (dialogView.findViewById<View>(R.id.row2).isVisible) {
                sb.append("${l2.text} ${v2.text}\n")
            }
            if (dialogView.findViewById<View>(R.id.row3).isVisible) {
                sb.append("${l3.text} ${v3.text}\n")
            }

            // 3. Wysłanie do schowka
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("CNC Result", sb.toString().trim())
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, getString(R.string.params_copied), Toast.LENGTH_SHORT).show()
        }

        dialogView.findViewById<Button>(R.id.btnCloseResults).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun clearInputs() {
        // 1. Odpięcie obserwatora Vc (zapobiega kolorowaniu nowych wpisów)
        val firstEditText = if (edtPanel.isNotEmpty()) edtPanel.getChildAt(0) as? EditText else null
        firstEditText?.let { vcField ->
            vcTextWatcher?.let { watcher -> vcField.removeTextChangedListener(watcher) }
        }
        vcTextWatcher = null

        // 2. Czyszczenie pól tekstowych i przywrócenie białego koloru
        for (i in 0 until edtPanel.childCount) {
            (edtPanel.getChildAt(i) as? EditText)?.apply {
                setText("")
                setTextColor(Color.WHITE)
                isEnabled = true
                alpha = 1.0f
            }
        }

        edtPanel.findViewById<View>(R.id.vc_chips_container)?.let { edtPanel.removeView(it) }
        edtPanel.findViewById<View>(R.id.tv_drill_hint_text)?.let { edtPanel.removeView(it) }

        findViewById<TextInputLayout>(R.id.layoutDiameter).visibility = View.GONE
        findViewById<TextInputLayout>(R.id.layoutModel).visibility = View.GONE
        findViewById<AutoCompleteTextView>(R.id.categoryAutoComplete).setText("", false)
        findViewById<AutoCompleteTextView>(R.id.diameterAutoComplete).setText("", false)
        findViewById<AutoCompleteTextView>(R.id.modelAutoComplete).setText("", false)
        findViewById<TextView>(R.id.tvSelectionSummary).text = getString(R.string.select_tool_hint)

        updateMaterialVisuals(-1)
        toolViewModel.setIsoGroup("")
        toolViewModel.resetSelection()

        //toggleSelectionCard(false)
        btnClear.isEnabled = false
    }

    private fun toggleSelectionCard(expand: Boolean) {
        findViewById<View>(R.id.expandableContent).visibility = if (expand) View.VISIBLE else View.GONE
        findViewById<ImageView>(R.id.ivExpandArrow).rotation = if (expand) 180f else 0f
    }

    private fun updateMaterialVisuals(selectedId: Int) {
        listOf(R.id.btnIsoP, R.id.btnIsoM, R.id.btnIsoK, R.id.btnIsoN, R.id.btnIsoS, R.id.btnIsoH).forEach { id ->
            val btn = findViewById<com.google.android.material.button.MaterialButton>(id)
            btn.strokeWidth = if (id == selectedId) dpToPx(3) else 0
        }
    }

    private fun getTranslatedCategory(polishName: String): String {
        var result = polishName
        val replacements = mapOf(
            "Toczenie mini" to R.string.cat_turning_mini,
            "Wytaczanie" to R.string.cat_boring_op,
            "Toczenie poprzeczne" to R.string.cat_turning_transverse,
            "Toczenie gwintów" to R.string.cat_threading_turning,
            "Gwintownik EG" to R.string.cat_eg_tap,
            "Gwintownik" to R.string.cat_tap,
            "Wygniatak" to R.string.cat_forming_tap,
            "Wytaczadło" to R.string.cat_boring,
            "Płytki" to R.string.cat_inserts,
            "Toczenie" to R.string.cat_turning
        )

        for ((key, resId) in replacements) {
            if (result.contains(key, ignoreCase = true)) {
                result = result.replace(key, getString(resId), ignoreCase = true)
                if (key == "Toczenie mini") break
            }
        }
        return result
    }

    private fun getTranslatedModel(modelName: String): String {
        var result = modelName
        val replacements = mapOf(
            "Lekkie wcinanie" to R.string.model_light_grooving,
            "Wytaczanie" to R.string.model_boring,
            "Wykańczająca" to R.string.model_finishing,
            "Średnia" to R.string.model_medium,
            "Zgrubna" to R.string.model_roughing,
            "Gwintownik" to R.string.model_tap,
            "Kanałki" to R.string.model_grooves,
            "Wiertło" to R.string.model_drill_ref,
            "Stal" to R.string.model_steel
        )

        for ((key, resId) in replacements) {
            // Używamy Regex, aby podmieniać całe słowa, co zapobiega błędom
            // przy podobnych nazwach technicznych
            val regex = Regex(key, RegexOption.IGNORE_CASE)
            if (result.contains(regex)) {
                result = result.replace(regex, getString(resId))
            }
        }
        return result
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    private fun showPremiumRequired() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.window_premium_upgrade, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(true).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<Button>(R.id.btnGoToPremium).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, ActivitySubscription::class.java))
        }
        dialogView.findViewById<Button>(R.id.btnCancelPremium).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showInfo() {

        val isMetric = ClassPrefs().loadPrefInt(this, "calcsys_data") != 1

        val resId = when (selectedOperationIndex) {
            0 -> if (isMetric) R.string.info_8_m else R.string.info_8_inch
            1 -> if (isMetric) R.string.info_9_m else R.string.info_9_inch
            2 -> if (isMetric) R.string.info_13_m else R.string.info_13_inch
            3 -> if (isMetric) R.string.info_10_m else R.string.info_10_inch
            4 -> if (isMetric) R.string.info_11_m else R.string.info_11_inch
            5 -> if (isMetric) R.string.info_14_m else R.string.info_14_inch
            6 -> if (isMetric) R.string.info_15_m else R.string.info_15_inch
            else -> null
        }

        resId?.let { id ->
            val dialogView = LayoutInflater.from(this).inflate(R.layout.window_information_modern, null)
            val content = dialogView.findViewById<TextView>(R.id.infoContent)

            val fullHtmlText = getString(id) +
                    "<br><br><font color='#FFD740'><b>💡 ${getString(R.string.vc_expert_title)}</b></font>" +
                    "<br>• <b>${getString(R.string.vc_expert_min)}</b>" +
                    "<br>• <b>${getString(R.string.vc_expert_opt)}</b>" +
                    "<br>• <b>${getString(R.string.vc_expert_max)}</b>"

            content.text = androidx.core.text.HtmlCompat.fromHtml(fullHtmlText, androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT)

            val dialog = AlertDialog.Builder(this).setView(dialogView).create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialogView.findViewById<Button>(R.id.btnOk).setOnClickListener { dialog.dismiss() }
            dialog.show()
        }
    }
}