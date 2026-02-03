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
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.example.calkulatorcnc.data.db.AppDatabase
import com.example.calkulatorcnc.data.preferences.ClassPrefs
import com.example.calkulatorcnc.data.repository.ToolRepository
import com.example.calkulatorcnc.viewModel.ToolViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import kotlinx.coroutines.launch
import kotlin.math.roundToLong
import androidx.core.view.isVisible
import com.example.calkulatorcnc.ui.adapters.MainDropdownAdapter
import com.google.android.material.textfield.TextInputLayout

data class CalculationResult(
    val f: Double = 0.0,
    val s: Double = 0.0,
    val fz: Double = 0.0,
    val center: Double = 0.0,
    val rm: Double = 0.0,
    val hv: Double = 0.0,
    val hb: Double = 0.0,
    val hrc: Double = 0.0,
    val isHardnessMode: Boolean = false,
    val isCenterMode: Boolean = false,
    val isSawMode: Boolean = false
)

class ActivityMilling : AppCompatActivity() {
    private lateinit var edtPanel: LinearLayout
    private lateinit var mainDropdown: AutoCompleteTextView
    private var selectedOperationIndex: Int = 0
    private lateinit var btnClear: androidx.appcompat.widget.AppCompatImageButton
    private var adView: AdView? = null
    private var calcSys: Int = 1000
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var toolViewModel: ToolViewModel
    private var vcTextWatcher: TextWatcher? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_milling)

        createViewAEdgetoEdgeForAds()
        initUI()
        setupAds()
        setupSpinner()
        analytics = Firebase.analytics

        calcSys = if (ClassPrefs().loadPrefInt(this, "calcsys_data") == 1) 12 else 1000

        val dao = AppDatabase.getDatabase(this).toolNewDao()
        val repository = ToolRepository(dao)
        toolViewModel = ToolViewModel(repository)

        setupToolCascade()
        observeToolData()

        findViewById<LinearLayout>(R.id.collapsibleHeader).setOnClickListener {
            val content = findViewById<View>(R.id.expandableContent)
            val isVisible = content.isVisible
            toggleSelectionCard(!isVisible)
        }
    }

    private fun createViewAEdgetoEdgeForAds() {
        val mainRoot = findViewById<View>(R.id.main)
        val customHeader = findViewById<LinearLayout>(R.id.customHeader)
        val bottomBar = findViewById<LinearLayout>(R.id.bottom_bar)
        val adContainerLayout = findViewById<FrameLayout>(R.id.adContainerLayout)
        val adContainer = findViewById<FrameLayout>(R.id.adContainer)

        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density

            mainRoot.setPadding(0, 0, 0, 0)

            // 1. Góra (Notch / Status Bar)
            customHeader?.updatePadding(top = systemBars.top)

            val isChromebook = android.os.Build.MODEL.contains("sdk_gpc", ignoreCase = true)
            val bottomExtraFix = if (isChromebook) (32 * density).toInt() else 0

            // 2. KLUCZOWA ZMIANA: Kontener dolny ma tylko boczny padding
            // Nie dajemy tu paddingu dolnego systemBars.bottom, bo przejmie to margines reklamy
            bottomBar?.updatePadding(
                left = systemBars.left + (10 * density).toInt(),
                right = systemBars.right + (8 * density).toInt(),
                bottom = bottomExtraFix // tylko fix dla Chromebooka
            )

            // 3. Reklama przejmuje odpowiedzialność za odstęp od dołu ekranu
            // To jest rozwiązanie z Twojej "idealnej" metody - margin działa najlepiej
            adContainerLayout?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                // Tutaj ustawiamy bezpieczny odstęp od paska nawigacji
                bottomMargin = systemBars.bottom
                leftMargin = 0
                rightMargin = 0
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
        findViewById<androidx.appcompat.widget.AppCompatImageButton>(R.id.button_back).setOnClickListener {
            finish()
        }

        findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.milingbutton1).setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            hideKeyboard()
            calculate()
        }

        btnClear.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            clearInputs()
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
                if (screenHeightDp < 400) {
                    adContainerLayout?.visibility = View.GONE
                    adContainer.visibility = View.GONE
                } else {
                    adContainerLayout?.visibility = View.VISIBLE
                    adContainer.visibility = View.VISIBLE
                    if (adView == null) {
                        val newAdView = AdView(this).apply {
                            adUnitId = BuildConfig.ADMOB_BANNER_ID
                            setAdSize(getAdSize(adContainer))
                        }
                        adView = newAdView
                        adContainer.removeAllViews()
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
        val operationsArray = resources.getStringArray(R.array.miling_spinner1_data)
        val premiumPositions = listOf(6, 7, 8)

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
                resetToolCascade()
                setupToolCascade()
            }
        }
    }

    private fun updateDynamicInputs(position: Int) {
        val isPremium = SubscriptionManager.getInstance(this).isPremium.value ?: false
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val showToolSelection = (position == 0 || position == 1 || position == 7)
        findViewById<View>(R.id.toolSelectionCard).visibility =
            if (showToolSelection) View.VISIBLE else View.GONE

        edtPanel.visibility = View.VISIBLE

        if (!isPremium && (position in 6..8)) {
            edtPanel.removeAllViews()
            findViewById<Button>(R.id.milingbutton1).isEnabled = false
            showPremiumRequired()
            return
        }

        findViewById<Button>(R.id.milingbutton1).isEnabled = true
        edtPanel.removeAllViews()
        edtPanel.orientation = LinearLayout.VERTICAL

        val layoutCategory = findViewById<TextInputLayout>(R.id.layoutCategory)
        val layoutDiameter = findViewById<TextInputLayout>(R.id.layoutDiameter)

        if (position == 7) {
            layoutCategory.hint = getString(R.string.step1_system)
            layoutDiameter.hint = getString(R.string.step2_radius)
        } else {
            layoutCategory.hint = getString(R.string.step1_category)
            layoutDiameter.hint = getString(R.string.step2_diameter)
        }

        val hints = when (position) {
            0 -> arrayOf(R.string.VC, R.string.DC, R.string.FZ, R.string.Z)
            1 -> arrayOf(R.string.VC, R.string.DC, R.string.JT)
            2 -> arrayOf(R.string.AS, R.string.T, R.string.VC, R.string.L)
            3 -> arrayOf(R.string.VF, R.string.VC, R.string.Z, R.string.LB)
            4 -> arrayOf(R.string.AS, R.string.VC, R.string.D, R.string.Z, R.string.L)
            5 -> arrayOf(R.string.VF, R.string.VC, R.string.Z, R.string.D)
            6 -> arrayOf(R.string.HD, R.string.TD, R.string.F)
            7 -> arrayOf(R.string.VC, R.string.DM, R.string.Fn)
            8 -> arrayOf(R.string.PointA, R.string.PointB)
            9 -> arrayOf(
                R.string.hardness_rm,
                R.string.hardness_hv,
                R.string.hardness_hb,
                R.string.hardness_hrc
            )

            else -> emptyArray()
        }

        hints.forEachIndexed { index, hintRes ->
            val isLast = index == hints.size - 1
            edtPanel.addView(createEditText(index, getString(hintRes), isLast))
        }
    }

    private fun createEditText(index: Int, hintText: String, isLast: Boolean) =
        EditText(this).apply {
            val isLandscape =
                resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

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
            setHintTextColor(
                androidx.core.content.ContextCompat.getColor(
                    context,
                    R.color.textColor_hint
                )
            )
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

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }

    private fun setupToolCascade() {
        val isPremium = SubscriptionManager.getInstance(this).isPremium.value ?: false

        val materialButtons = mapOf(
            R.id.btnIsoP to "P", R.id.btnIsoM to "M",
            R.id.btnIsoK to "K", R.id.btnIsoN to "N",
            R.id.btnIsoS to "S", R.id.btnIsoH to "H"
        )

        materialButtons.forEach { (id, group) ->
            findViewById<View>(id)?.setOnClickListener {
                clearInputs()
                toolViewModel.setIsoGroup(group)
                updateMaterialVisuals(id)
                findViewById<View>(R.id.layoutDiameter).apply {
                    visibility = View.GONE
                    alpha = 1.0f
                }
                findViewById<View>(R.id.layoutModel).visibility = View.GONE
                findViewById<AutoCompleteTextView>(R.id.categoryAutoComplete).setText("", false)
                findViewById<AutoCompleteTextView>(R.id.diameterAutoComplete).setText("", false)
                findViewById<AutoCompleteTextView>(R.id.modelAutoComplete).setText("", false)
                it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                toolViewModel.resetSelection()
            }
        }

        val categoryView = findViewById<AutoCompleteTextView>(R.id.categoryAutoComplete)
        var originalCategories = listOf<String>()

        lifecycleScope.launch {
            toolViewModel.categories.collect { categories ->
                val techKeywords = when (selectedOperationIndex) {
                    0 -> listOf("Frez", "Wiert", "Rozwiert", "Pogłębia", "Wielofunkcyjne")
                    1 -> listOf("Gwint", "Wygniat", "EG", "UNC", "UNF")
                    7 -> listOf("Wytacz", "Wielofunkcyjne")
                    else -> emptyList()
                }

                val filtered = if (techKeywords.isEmpty()) {
                    emptyList()
                } else {
                    categories.filter { category ->
                        val matchesKeyword =
                            techKeywords.any { key -> category.contains(key, ignoreCase = true) }
                        val isNotTurning =
                            if (selectedOperationIndex == 1 || selectedOperationIndex == 7) {
                                !category.contains(
                                    "Toczenie",
                                    ignoreCase = true
                                ) && !category.contains("Płytki", ignoreCase = true)
                            } else {
                                true
                            }
                        matchesKeyword && isNotTurning
                    }.sorted()
                }

                originalCategories = filtered
                val translatedCategories = filtered.map { getTranslatedCategory(it) }
                //lista blokad kategori dla premium
                val premiumKeywords = listOf("Wielofunkcyjne", "FreeTurn")
                val toolPremiumPositions = filtered.indices.filter { i ->
                    premiumKeywords.any { key -> filtered[i].contains(key, ignoreCase = true) }
                }

                val adapter = MainDropdownAdapter(
                    this@ActivityMilling,
                    R.layout.spinner_dropdown_item,
                    translatedCategories.toTypedArray(),
                    isPremium,
                    toolPremiumPositions
                )
                categoryView.setAdapter(adapter)
            }
        }

        categoryView.setOnItemClickListener { _, _, position, _ ->
            val adapter = categoryView.adapter as MainDropdownAdapter
            if (!isPremium && adapter.premiumPositions.contains(position)) {
                categoryView.setText("", false)
                showPremiumRequired()
            } else {
                val selectedPolishName = originalCategories[position]

                // Resetujemy stan pola średnicy i pokazujemy je
                findViewById<AutoCompleteTextView>(R.id.diameterAutoComplete).setText("", false)
                findViewById<View>(R.id.layoutDiameter).apply {
                    visibility = View.VISIBLE
                    alpha = 1.0f
                }
                findViewById<View>(R.id.layoutModel).visibility = View.GONE

                toolViewModel.setCategory(selectedPolishName)
            }
        }

        // --- STEP 2: ŚREDNICA ---
        val diameterView = findViewById<AutoCompleteTextView>(R.id.diameterAutoComplete)
        val layoutDiameter = findViewById<TextInputLayout>(R.id.layoutDiameter)

        val handleListClick = {
            val adapter = diameterView.adapter
            val currentMaterial = toolViewModel.selectedMaterialGroup.value ?: ""
            val currentCategory = toolViewModel.selectedCategory.value ?: ""

            if ((adapter == null || adapter.count == 0) && currentCategory.isNotEmpty() && currentMaterial.isNotEmpty()) {
                diameterView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                Toast.makeText(
                    this,
                    getString(R.string.error_no_tool_for_material),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                diameterView.showDropDown()
            }
        }

        diameterView.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                handleListClick()
                v.performClick()
            }
            true
        }
        layoutDiameter.setEndIconOnClickListener { handleListClick() }

        lifecycleScope.launch {
            toolViewModel.availableDiameters.collect { diameters ->
                val currentMaterial = toolViewModel.selectedMaterialGroup.value ?: ""
                val currentCategory = toolViewModel.selectedCategory.value ?: ""

                // 1. Jeśli brak średnic, ale wybrany komplet (Kategoria + Materiał)
                if (diameters.isEmpty() && currentCategory.isNotEmpty() && currentMaterial.isNotEmpty()) {
                    diameterView.setAdapter(null)
                    layoutDiameter.alpha = 0.5f
                } else if (diameters.isNotEmpty()) {
                    layoutDiameter.alpha = 1.0f
                    layoutDiameter.visibility = View.VISIBLE

                    val diameterStrings = diameters.map { it.toString() }
                    val isPrecisionTool = currentCategory.contains("Gwint", ignoreCase = true) ||
                            currentCategory.contains("Wygniat", ignoreCase = true) ||
                            currentCategory.contains("EG", ignoreCase = true) ||
                            currentCategory.contains("Rozwiert", ignoreCase = true)
                    val isHssDrill = currentCategory.contains("Wiert", ignoreCase = true) &&
                            currentCategory.contains("HSS", ignoreCase = true)
                    val isVhmTool = currentCategory.contains("VHM", ignoreCase = true)

                    val diameterPremiumPositions = diameters.indices.filter { i ->
                        when {
                            currentCategory.contains(
                                "Głowica",
                                ignoreCase = true
                            ) -> diameters[i] >= 63.0

                            isPrecisionTool -> diameters[i] >= 10.0
                            isHssDrill || isVhmTool -> diameters[i] >= 10.0
                            else -> diameters[i] > 20.0
                        }
                    }

                    val adapter = MainDropdownAdapter(
                        this@ActivityMilling,
                        R.layout.spinner_dropdown_item,
                        diameterStrings.toTypedArray(),
                        SubscriptionManager.getInstance(this@ActivityMilling).isPremium.value
                            ?: false,
                        diameterPremiumPositions
                    )
                    diameterView.setAdapter(adapter)
                }
            }
        }

        diameterView.setOnItemClickListener { parent, _, position, _ ->
            val adapter = diameterView.adapter as MainDropdownAdapter
            if (!isPremium && adapter.premiumPositions.contains(position)) {
                diameterView.setText("", false)
                showPremiumRequired()
            } else {
                val selectedStr = adapter.getItem(position).toString()
                val selected = selectedStr.toDoubleOrNull() ?: return@setOnItemClickListener
                toolViewModel.setDiameter(selected)
                findViewById<View>(R.id.layoutModel).visibility = View.VISIBLE
                findViewById<AutoCompleteTextView>(R.id.modelAutoComplete).setText("", false)
            }
        }

        // --- STEP 3: MODEL ---
        val modelView = findViewById<AutoCompleteTextView>(R.id.modelAutoComplete)
        lifecycleScope.launch {
            toolViewModel.availableModels.collect { models ->
                val sortedModels = models.sortedWith(compareBy { model ->
                    val match = Regex("(\\d+)xD").find(model)
                    match?.groupValues?.get(1)?.toInt() ?: 0
                })

                val currentCategory = toolViewModel.selectedCategory.value ?: ""
                val isDrill = currentCategory.contains("Wiert", ignoreCase = true)
                val modelPremiumPositions = if (isDrill) {
                    emptyList<Int>()
                } else {
                    sortedModels.indices.filter { i ->
                        val match = Regex("(\\d+)xD").find(sortedModels[i])
                        val depth = match?.groupValues?.get(1)?.toInt() ?: 0
                        depth > 5
                    }
                }

                val adapter = MainDropdownAdapter(
                    this@ActivityMilling,
                    R.layout.spinner_dropdown_item,
                    sortedModels.toTypedArray(),
                    isPremium,
                    modelPremiumPositions
                )
                modelView.setAdapter(adapter)
            }
        }

        modelView.setOnItemClickListener { parent, _, position, _ ->
            val adapter = modelView.adapter as MainDropdownAdapter
            if (!isPremium && adapter.premiumPositions.contains(position)) {
                modelView.setText("", false)
                showPremiumRequired()
            } else {
                val selected = adapter.getItem(position) as String
                toolViewModel.setModel(selected)
                toggleSelectionCard(false)
            }
        }
    }

    private fun resetToolCascade() {
        findViewById<View>(R.id.layoutDiameter).visibility = View.GONE
        findViewById<View>(R.id.layoutModel).visibility = View.GONE
        findViewById<AutoCompleteTextView>(R.id.categoryAutoComplete).setText("", false)
        findViewById<AutoCompleteTextView>(R.id.diameterAutoComplete).setText("", false)
        findViewById<AutoCompleteTextView>(R.id.modelAutoComplete).setText("", false)
        findViewById<TextView>(R.id.tvSelectionSummary).text = getString(R.string.select_tool_hint)
        edtPanel.findViewById<View>(R.id.vc_chips_container)?.let { edtPanel.removeView(it) }
    }

    private fun observeToolData() {
        lifecycleScope.launch {
            toolViewModel.toolParameters.collect { tool ->
                tool?.let { currentTool ->
                    val modelAutoComplete =
                        findViewById<AutoCompleteTextView>(R.id.modelAutoComplete)
                    if (modelAutoComplete.text.isNotEmpty()) {
                        edtPanel.isVisible = true
                        val editTexts = (0 until edtPanel.childCount)
                            .map { edtPanel.getChildAt(it) }
                            .filterIsInstance<EditText>()

                        if (editTexts.size >= 3) {
                            val vcEditText = editTexts[0]
                            val diameterEditText = editTexts[1]
                            val feedEditText = editTexts[2]
                            val isBoringMode = selectedOperationIndex == 7

                            // --- RESET POPRZEDNICH USTAWIEŃ ---
                            vcTextWatcher?.let { vcEditText.removeTextChangedListener(it) }
                            edtPanel.findViewById<View>(R.id.vc_chips_container)
                                ?.let { edtPanel.removeView(it) }
                            edtPanel.findViewById<View>(R.id.tv_drill_hint_text)
                                ?.let { edtPanel.removeView(it) }

                            val rawMin = currentTool.vcMin
                            val rawMax = currentTool.vcMax
                            val minRounded = Math.round(rawMin).toInt()
                            val maxRounded = Math.round(rawMax).toInt()
                            val optRounded = Math.round((rawMin + rawMax) / 2).toInt()

                            // 1. LOGIKA KOLORÓW
                            vcTextWatcher = object : TextWatcher {
                                override fun afterTextChanged(s: Editable?) {
                                    val input = s.toString().toDoubleOrNull() ?: 0.0
                                    applyVcColor(vcEditText, input, rawMin, rawMax)
                                }

                                override fun beforeTextChanged(
                                    s: CharSequence?,
                                    q: Int,
                                    w: Int,
                                    e: Int
                                ) {
                                }

                                override fun onTextChanged(
                                    s: CharSequence?,
                                    q: Int,
                                    w: Int,
                                    e: Int
                                ) {
                                }
                            }

                            // 2. WPISYWANIE WARTOŚCI STARTOWEJ
                            vcEditText.setText(optRounded.toString())
                            vcEditText.addTextChangedListener(vcTextWatcher)
                            applyVcColor(vcEditText, optRounded.toDouble(), rawMin, rawMax)

                            // 3. PRZYCISKI CHIPS (MIN / OPT / MAX)
                            val chipsLayout = LinearLayout(this@ActivityMilling).apply {
                                id = R.id.vc_chips_container
                                orientation = LinearLayout.HORIZONTAL
                                gravity = Gravity.CENTER
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply { setMargins(0, 0, 0, dpToPx(8)) }
                            }

                            val fastSettings = listOf(
                                Triple(getString(R.string.label_min), minRounded, "#FFD740"),
                                Triple(getString(R.string.label_opt), optRounded, "#4CAF50"),
                                Triple(getString(R.string.label_max), maxRounded, "#FF5252")
                            )

                            fastSettings.forEach { (label, value, colorHex) ->
                                val chip = Button(
                                    this@ActivityMilling,
                                    null,
                                    0,
                                    com.google.android.material.R.style.Widget_MaterialComponents_Button_TextButton
                                ).apply {
                                    text = label
                                    textSize = 13f
                                    setTypeface(null, android.graphics.Typeface.BOLD)
                                    setTextColor(Color.parseColor(colorHex))
                                    layoutParams = LinearLayout.LayoutParams(dpToPx(80), dpToPx(40))
                                    setOnClickListener {
                                        it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                                        vcEditText.setText(value.toString())
                                    }
                                }
                                chipsLayout.addView(chip)
                            }
                            edtPanel.addView(chipsLayout, 1)

                            // 4. AUTOMATYCZNE WPISYWANIE ŚREDNICY I POSUWU
                            if (isBoringMode) {
                                diameterEditText.setText("") // Wytaczanie wymaga ręcznego wpisania DM
                            } else {
                                diameterEditText.setText(currentTool.diameter.toString())
                            }

                            val isTap =
                                currentTool.toolCategory.contains("Gwint", ignoreCase = true) ||
                                        currentTool.toolCategory.contains(
                                            "Wygniat",
                                            ignoreCase = true
                                        ) ||
                                        currentTool.toolCategory.contains("EG", ignoreCase = true)

                            feedEditText.setText(currentTool.feedStep.toString())

                            if (isTap) {
                                feedEditText.isEnabled = false
                                feedEditText.alpha = 0.6f
                            } else {
                                feedEditText.isEnabled = true
                                feedEditText.alpha = 1.0f
                            }

                            if (editTexts.size >= 4) {
                                val zEditText = editTexts[3]
                                val isDrill =
                                    currentTool.toolCategory.contains("Wiert", ignoreCase = true)

                                if (isDrill) {
                                    // Dla wierteł wymuszamy 1, bo baza podaje fn (posuw na obrót)
                                    zEditText.setText("1")
                                    zEditText.isEnabled =
                                        false // Opcjonalnie: blokujemy pole, by użytkownik nie wpisał 2
                                    zEditText.alpha = 0.7f      // Wizualne oznaczenie blokady
                                } else {
                                    // Dla frezów wpisujemy realną liczbę ostrzy z bazy
                                    zEditText.setText(currentTool.flutesCount.toString())
                                    zEditText.isEnabled = true
                                    zEditText.alpha = 1.0f
                                }
                            }

                            // 5. PODPOWIEDŹ WIERTŁA POD GWINT
                            if (isTap) {
                                val drillSize = if (currentTool.toolCategory.contains(
                                        "Wygniat",
                                        ignoreCase = true
                                    )
                                ) {
                                    currentTool.diameter - (0.5 * currentTool.feedStep)
                                } else {
                                    currentTool.diameter - currentTool.feedStep
                                }
                                val tvDrillHint = TextView(this@ActivityMilling).apply {
                                    id = R.id.tv_drill_hint_text
                                    text = "${getString(R.string.drill_hint_label)} Ø${
                                        String.format(
                                            "%.2f",
                                            drillSize
                                        )
                                    }"
                                    setTextColor(Color.parseColor("#FFD740"))
                                    textSize = 14f
                                    setTypeface(null, android.graphics.Typeface.BOLD)
                                    gravity = Gravity.CENTER
                                    setPadding(0, dpToPx(4), 0, dpToPx(8))
                                }
                                edtPanel.addView(tvDrillHint)
                            }

                            // 6. AKTUALIZACJA PASKA PODSUMOWANIA
                            val labelSummary = if (isBoringMode) "rε" else "Ø"
                            findViewById<TextView>(R.id.tvSelectionSummary).text =
                                "${currentTool.materialGroup} | ${getTranslatedCategory(currentTool.toolCategory)} | $labelSummary: ${currentTool.diameter}"
                        }
                        Toast.makeText(
                            this@ActivityMilling,
                            getString(R.string.parameters_load),
                            Toast.LENGTH_SHORT
                        ).show()
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

    private fun toggleSelectionCard(expand: Boolean) {
        val content = findViewById<View>(R.id.expandableContent)
        val arrow = findViewById<ImageView>(R.id.ivExpandArrow)
        content.visibility = if (expand) View.VISIBLE else View.GONE
        arrow.rotation = if (expand) 180f else 0f
    }

    private fun updateMaterialVisuals(selectedId: Int) {
        val ids = listOf(
            R.id.btnIsoP,
            R.id.btnIsoM,
            R.id.btnIsoK,
            R.id.btnIsoN,
            R.id.btnIsoS,
            R.id.btnIsoH
        )
        ids.forEach { id ->
            // Musimy rzutować na MaterialButton, aby mieć dostęp do właściwości stroke
            val btn = findViewById<com.google.android.material.button.MaterialButton>(id)
            if (btn != null) {
                if (id == selectedId) {
                    btn.strokeWidth = dpToPx(3) // Grubość obwódki dla zaznaczonego
                    btn.setStrokeColorResource(R.color.white) // Kolor obwódki
                } else {
                    btn.strokeWidth = 0 // Brak obwódki dla reszty
                }
            }
        }
    }

    private fun calculate() {
        val editTexts = (0 until edtPanel.childCount).map { edtPanel.getChildAt(it) }
            .filterIsInstance<EditText>()
        if (editTexts.isEmpty()) return

        val currentCalcSys = if (ClassPrefs().loadPrefInt(this, "calcsys_data") == 1) 12 else 1000

        val vals = editTexts.map { it.text.toString().toDoubleOrNull() ?: 0.0 }

        if (selectedOperationIndex != 8 && selectedOperationIndex != 9 && vals.any { it <= 0.0 }) {
            Toast.makeText(this, getString(R.string.error2), Toast.LENGTH_SHORT).show()
            return
        }

        val result = try {
            when (selectedOperationIndex) {
                // Frezowanie
                0 -> {
                    val s = (vals[0] * currentCalcSys) / (Math.PI * vals[1])
                    val f = vals[2] * vals[3] * s
                    CalculationResult(f = f, s = s)
                }
                // Gwintowanie
                1 -> {
                    val s = (vals[0] * currentCalcSys) / (Math.PI * vals[1])
                    val f = vals[2] * s
                    CalculationResult(f = f, s = s)
                }
                // Frezowanie tarczowe / Piły (tryb fz)
                2 -> CalculationResult(
                    fz = (vals[0] * vals[1]) / (vals[3] * vals[2] * currentCalcSys),
                    isSawMode = true
                )

                3 -> CalculationResult(
                    fz = (vals[0] * vals[3]) / (vals[1] * vals[2] * currentCalcSys),
                    isSawMode = true
                )

                4 -> CalculationResult(
                    fz = (vals[0] * vals[2] * Math.PI) / (vals[4] * vals[1] * vals[3] * currentCalcSys),
                    isSawMode = true
                )

                5 -> CalculationResult(
                    fz = (vals[0] * vals[3] * Math.PI) / (vals[1] * vals[2] * currentCalcSys),
                    isSawMode = true
                )
                // Kompensacja promienia
                6 -> CalculationResult(f = vals[2] * ((vals[0] - vals[1]) / vals[0]))
                // Wytaczanie
                7 -> {
                    val s = (vals[0] * currentCalcSys) / (Math.PI * vals[1])
                    val f = vals[2] * s
                    CalculationResult(f = f, s = s)
                }
                // Środek punktów
                8 -> CalculationResult(center = (vals[0] + vals[1]) / 2, isCenterMode = true)
                9 -> {
                    val filledCount = vals.count { it > 0.0 }

                    if (filledCount == 0) {
                        Toast.makeText(this, getString(R.string.error2), Toast.LENGTH_SHORT).show()
                        return // PRZERYWA funkcję calculate() - okno się nie pokaże
                    }

                    if (filledCount > 1) {
                        Toast.makeText(
                            this,
                            getString(R.string.error_only_one_value),
                            Toast.LENGTH_LONG
                        ).show()
                        return // PRZERYWA funkcję calculate() - okno się nie pokaże
                    }

                    // Jeśli doszliśmy tutaj, oznacza to, że wypełnione jest dokładnie jedno pole
                    val inputIndex = vals.indexOfFirst { it > 0.0 }
                    val res = HardnessConverter.calculate(inputIndex, vals[inputIndex])

                    if (res != null) {
                        // Zwracamy wynik do zmiennej 'result' i idziemy do showResultDialog
                        CalculationResult(
                            rm = res.rm, hv = res.hv, hb = res.hb, hrc = res.hrc,
                            isHardnessMode = true
                        )
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.error_out_of_range),
                            Toast.LENGTH_SHORT
                        ).show()
                        return // PRZERYWA funkcję calculate()
                    }
                }

                else -> CalculationResult()
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(R.string.calculation_error, e.message ?: "NaN"),
                Toast.LENGTH_SHORT
            ).show()
            CalculationResult()
        }

        // 2. OSTRZEŻENIA VC (Działają tylko gdy narzędzie jest załadowane z bazy)
        val currentTool = toolViewModel.toolParameters.value
        if (currentTool != null) {
            if (vals[0] > currentTool.vcMax) {
                Toast.makeText(this, getString(R.string.vc_warn_high), Toast.LENGTH_LONG).show()
            } else if (vals[0] < currentTool.vcMin) {
                Toast.makeText(this, getString(R.string.vc_info_low), Toast.LENGTH_SHORT).show()
            }
        }

        showResultDialog(result)
    }

    object HardnessConverter {
        data class Entry(val rm: Double, val hv: Double, val hb: Double, val hrc: Double)

        private val table = listOf(
            Entry(255.0, 80.0, 76.0, 0.0), Entry(320.0, 100.0, 95.0, 0.0),
            Entry(480.0, 150.0, 143.0, 0.0), Entry(640.0, 200.0, 190.0, 13.2),
            Entry(800.0, 250.0, 238.0, 22.2), Entry(970.0, 300.0, 285.0, 29.8),
            Entry(1130.0, 350.0, 333.0, 35.5), Entry(1290.0, 400.0, 380.0, 41.3),
            Entry(1450.0, 450.0, 428.0, 45.3), Entry(1620.0, 500.0, 475.0, 49.1),
            Entry(1930.0, 600.0, 570.0, 55.2), Entry(2200.0, 700.0, 665.0, 59.7)
        )

        fun calculate(typeIndex: Int, value: Double): Entry? {
            val sortedTable = table.sortedBy { getValByIndex(it, typeIndex) }
            if (value < getValByIndex(sortedTable.first(), typeIndex) ||
                value > getValByIndex(sortedTable.last(), typeIndex)
            ) return null

            for (i in 0 until sortedTable.size - 1) {
                val low = sortedTable[i]
                val high = sortedTable[i + 1]
                val vLow = getValByIndex(low, typeIndex)
                val vHigh = getValByIndex(high, typeIndex)

                if (value >= vLow && value <= vHigh) {
                    val t = (value - vLow) / (vHigh - vLow)
                    return Entry(
                        low.rm + t * (high.rm - low.rm), low.hv + t * (high.hv - low.hv),
                        low.hb + t * (high.hb - low.hb), low.hrc + t * (high.hrc - low.hrc)
                    )
                }
            }
            return null
        }

        private fun getValByIndex(e: Entry, index: Int) = when (index) {
            0 -> e.rm; 1 -> e.hv; 2 -> e.hb; else -> e.hrc
        }
    }

    private fun showResultDialog(res: CalculationResult) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.window_calculation_modern, null)

        // Inicjalizacja widoków
        val label1 = dialogView.findViewById<TextView>(R.id.label1)
        val value1 = dialogView.findViewById<TextView>(R.id.value1)
        val label2 = dialogView.findViewById<TextView>(R.id.label2)
        val value2 = dialogView.findViewById<TextView>(R.id.value2)
        val label3 = dialogView.findViewById<TextView>(R.id.label3)
        val value3 = dialogView.findViewById<TextView>(R.id.value3)
        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseResults)
        val btnCopy = dialogView.findViewById<ImageButton>(R.id.btnCopyResults)

        // Sprawdzenie systemu jednostek (0 = mm, 1 = cale)
        val isInch = ClassPrefs().loadPrefInt(this, "calcsys_data") == 1
        val locale = java.util.Locale.US // Używamy kropki jako separatora

        when {
            res.isHardnessMode -> {
                label1.text = getString(R.string.hardness_rm)
                value1.text = String.format("%.0f N/mm²", res.rm)

                label2.text = getString(R.string.hardness_hv)
                value2.text = String.format("%.0f HV", res.hv)

                label3.text = getString(R.string.hardness_hb)
                value3.text = String.format("%.0f HB", res.hb)

                // Jeśli masz row4 w XML, użyj go, jeśli nie - możesz dopisać HRC do row3 lub użyć Toast
                // Zakładając, że masz 3 rzędy, HRC można pokazać jako label3 "HB / HRC"
                if (res.hrc > 0) {
                    label3.text = "HB / HRC"
                    value3.text = String.format("%.0f / %.1f", res.hb, res.hrc)
                }
            }

            res.isCenterMode -> {
                dialogView.findViewById<View>(R.id.row1).visibility = View.GONE
                dialogView.findViewById<View>(R.id.row2).visibility = View.GONE
                dialogView.findViewById<View>(R.id.separator).visibility = View.GONE
                label3.text = getString(R.string.center_colon)
                value3.text = String.format(locale, "%.3f", res.center)
            }

            res.isSawMode -> {
                dialogView.findViewById<View>(R.id.row1).visibility = View.GONE
                dialogView.findViewById<View>(R.id.row2).visibility = View.GONE
                dialogView.findViewById<View>(R.id.separator).visibility = View.GONE
                label3.text = getString(R.string.fz_colon)
                value3.text = String.format(locale, "%.4f", res.fz)
            }

            else -> {
                // ZAMIANA: Używamy przetłumaczonych stringów z jednostkami
                label1.text = getString(if (isInch) R.string.res_f_inch else R.string.res_f_m)
                label2.text = getString(if (isInch) R.string.res_n_inch else R.string.res_n_m)

                // FORMATOWANIE POSUWU (F):
                // Dla cali 2 miejsca po przecinku, dla metrycznych 1 miejsce
                value1.text = if (isInch) String.format(locale, "%.2f", res.f)
                else String.format(locale, "%.1f", res.f)

                // FORMATOWANIE OBROTÓW (n): Zawsze liczba całkowita
                value2.text = String.format(locale, "%.0f", res.s)

                if (res.fz > 0 && selectedOperationIndex != 0) {
                    label3.text = getString(R.string.fz_colon)
                    value3.text = String.format(locale, "%.3f", res.fz)
                } else {
                    dialogView.findViewById<View>(R.id.row3).visibility = View.GONE
                    dialogView.findViewById<View>(R.id.separator).visibility = View.GONE
                }
            }
        }

        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(true).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // LOGIKA KOPIOWANIA
        btnCopy?.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)

            val sb = StringBuilder()

            // 1. Dynamiczny nagłówek zależny od trybu
            val header = if (res.isHardnessMode) {
                getString(R.string.hardness_res_title) // "Wyniki konwersji"
            } else {
                // Użyj istniejącego stringa nagłówka, np. "WYNIKI OBLICZEŃ"
                // Jeśli nie masz go w strings.xml, możesz wpisać go tu lub dodać nowy zasób
                getString(R.string.calculateResult)
            }

            sb.append(header).append("\n---\n") // Dodajemy linię oddzielającą dla czytelności

            // 2. Pobieranie danych z widocznych rzędów
            if (dialogView.findViewById<View>(R.id.row1).isVisible) {
                sb.append("${label1.text} ${value1.text}\n")
            }
            if (dialogView.findViewById<View>(R.id.row2).isVisible) {
                sb.append("${label2.text} ${value2.text}\n")
            }
            if (dialogView.findViewById<View>(R.id.row3).isVisible) {
                sb.append("${label3.text} ${value3.text}\n")
            }

            // 3. Wysłanie do schowka
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("CNC Result", sb.toString().trim())
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, getString(R.string.params_copied), Toast.LENGTH_SHORT).show()
        }
        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showPremiumRequired() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.window_premium_upgrade, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(true).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<Button>(R.id.btnGoToPremium).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, ActivitySubscription::class.java))
        }
        dialogView.findViewById<Button>(R.id.btnCancelPremium)
            .setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun clearInputs() {
        // 1. Usuwamy obserwatora Vc
        val firstEditText =
            if (edtPanel.childCount > 0) edtPanel.getChildAt(0) as? EditText else null
        firstEditText?.let { vcField ->
            vcTextWatcher?.let { watcher -> vcField.removeTextChangedListener(watcher) }
        }
        vcTextWatcher = null

        // 2. Czyścimy panel przycisków i pola
        for (i in 0 until edtPanel.childCount) {
            (edtPanel.getChildAt(i) as? EditText)?.apply {
                setText("")
                setTextColor(Color.WHITE)
            }
        }

        edtPanel.findViewById<View>(R.id.vc_chips_container)?.let { edtPanel.removeView(it) }

        findViewById<TextInputLayout>(R.id.layoutDiameter).apply {
            visibility = View.GONE
            alpha = 1.0f
        }
        findViewById<TextInputLayout>(R.id.layoutModel).visibility = View.GONE
        findViewById<AutoCompleteTextView>(R.id.diameterAutoComplete).setText("", false)
        findViewById<AutoCompleteTextView>(R.id.modelAutoComplete).setText("", false)
        findViewById<AutoCompleteTextView>(R.id.categoryAutoComplete).setText("", false)
        edtPanel.findViewById<View>(R.id.tv_drill_hint_text)?.let { edtPanel.removeView(it) }
        findViewById<TextView>(R.id.tvSelectionSummary).text = getString(R.string.select_tool_hint)

        updateMaterialVisuals(-1)
        toolViewModel.setIsoGroup("")
        toolViewModel.resetSelection()

        //toggleSelectionCard(false)
        btnClear.isEnabled = false
    }

    private fun showInfo() {
        val resId = getInfoResourceId(selectedOperationIndex, calcSys == 1000)
        if (resId != 0) {
            val dialogView =
                LayoutInflater.from(this).inflate(R.layout.window_information_modern, null)

            val fullHtmlText = if (selectedOperationIndex == 9) {
                getString(resId)
            } else {
                getString(resId) + "<br><br><font color='#FFD740'><b>💡 ${getString(R.string.vc_expert_title)}</b></font><br>• <b>${
                    getString(
                        R.string.vc_expert_min
                    )
                }</b><br>• <b>${getString(R.string.vc_expert_opt)}</b><br>• <b>${getString(R.string.vc_expert_max)}</b>"
            }

            dialogView.findViewById<TextView>(R.id.infoContent).text =
                androidx.core.text.HtmlCompat.fromHtml(
                    fullHtmlText,
                    androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
                )

            val dialog = AlertDialog.Builder(this).setView(dialogView).create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialogView.findViewById<Button>(R.id.btnOk).setOnClickListener { dialog.dismiss() }
            dialog.show()
        }
    }

    private fun getInfoResourceId(pos: Int, isMetric: Boolean): Int {
        return when (pos) {
            0 -> if (isMetric) R.string.info_0_m else R.string.info_0_inch
            1 -> if (isMetric) R.string.info_13_m else R.string.info_13_inch
            2 -> if (isMetric) R.string.info_4_m else R.string.info_4_inch
            3 -> if (isMetric) R.string.info_5_m else R.string.info_5_inch
            4 -> if (isMetric) R.string.info_6_m else R.string.info_6_inch
            5 -> if (isMetric) R.string.info_7_m else R.string.info_7_inch
            6 -> if (isMetric) R.string.info_2_m else R.string.info_2_inch
            7 -> if (isMetric) R.string.info_3_m else R.string.info_3_inch
            8 -> if (isMetric) R.string.info_12_m else R.string.info_12_inch
            9 -> R.string.info_hardness
            else -> 0
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun getTranslatedCategory(polishName: String): String {
        var result = polishName
        val replacements = mapOf(
            "Gwintownik EG" to R.string.cat_eg_tap, "Gwintownik" to R.string.cat_tap,
            "Wygniatak" to R.string.cat_forming_tap, "Rozwiertak" to R.string.cat_reamer,
            "Wiertło" to R.string.cat_drill, "Wiert" to R.string.cat_drill,
            "Frez" to R.string.cat_mill, "Wytaczadło" to R.string.cat_boring,
            "Pogłębiacz" to R.string.cat_sink, "Narzędzia wielofunkcyjne" to R.string.cat_multi
        )
        for ((key, resId) in replacements) {
            if (result.contains(key, ignoreCase = true)) {
                result = result.replace(key, getString(resId), ignoreCase = true)
                break
            }
        }
        return result
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onPause() {
        adView?.pause(); super.onPause()
    }

    override fun onResume() {
        super.onResume(); adView?.resume()
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, "Frezowanie")
            param(FirebaseAnalytics.Param.SCREEN_CLASS, "ActivityMilling")
        }
    }

    override fun onDestroy() {
        adView?.destroy(); super.onDestroy()
    }
}