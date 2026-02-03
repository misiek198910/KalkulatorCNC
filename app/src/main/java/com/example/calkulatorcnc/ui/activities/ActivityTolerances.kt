package com.example.calkulatorcnc.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isEmpty
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.example.calkulatorcnc.data.params.FitType
import com.example.calkulatorcnc.databinding.ActivityTolerancesBinding
import com.example.calkulatorcnc.ui.adapters.MainDropdownAdapter
import com.example.calkulatorcnc.viewModel.TolerancesViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

class ActivityTolerances : AppCompatActivity() {

    private lateinit var binding: ActivityTolerancesBinding
    private val viewModel: TolerancesViewModel by viewModels()
    private var adView: AdView? = null
    private lateinit var analytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTolerancesBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Firebase i Ads
        analytics = Firebase.analytics

        createViewAEdgetoEdgeForAds()
        initUI()
        setupDropdowns()
        setupListeners()
        observeViewModel()
    }
    private fun initUI() {
        // Chowanie klawiatury po kliknięciu w tło
        binding.main.setOnClickListener { hideKeyboard() }

        // LOGIKA STRZAŁKI WSTECZ (Toolbar)
        binding.buttonBack.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            finish()
        }

        // PRZYCISK RESETUJ
        binding.btnReset.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            binding.editTextDiameter.text?.clear()
            viewModel.reset()
        }
    }
    private fun setupDropdowns() {
        val isPremium = SubscriptionManager.getInstance(this).isPremium.value ?: false

        // LISTA OTWORÓW I WAŁKÓW
        val holeOptions = viewModel.availableHoles
        val shaftOptions = viewModel.availableShafts

        // Definicja darmowych tolerancji
        val freeHoles = listOf("H7", "H8")
        val freeShafts = listOf("h6", "h7", "g6")

        // Obliczanie pozycji Premium dla MainDropdownAdapter
        val premiumHolePositions = holeOptions.indices.filter { holeOptions[it] !in freeHoles }
        val premiumShaftPositions = shaftOptions.indices.filter { shaftOptions[it] !in freeShafts }

        // Konfiguracja adapterów z kłódkami
        val holeAdapter = MainDropdownAdapter(
            this,
            R.layout.item_material,
            holeOptions.toTypedArray(),
            isPremium,
            premiumHolePositions
        )

        val shaftAdapter = MainDropdownAdapter(
            this,
            R.layout.item_material,
            shaftOptions.toTypedArray(),
            isPremium,
            premiumShaftPositions
        )

        binding.autoCompleteHole?.setAdapter(holeAdapter)
        binding.autoCompleteShaft?.setAdapter(shaftAdapter)

        // Ustawienie wartości domyślnych (np. H7/h6)
        binding.autoCompleteHole?.setText(viewModel.selectedHole.value, false)
        binding.autoCompleteShaft?.setText(viewModel.selectedShaft.value, false)
    }
    private fun setupListeners() {
        val subManager = SubscriptionManager.getInstance(this)

        binding.editTextDiameter.doAfterTextChanged {
            viewModel.diameterInput.value = it.toString()
            viewModel.calculate()
        }

        binding.autoCompleteHole.setOnItemClickListener { _, _, position, _ ->
            val isPremium = subManager.isPremium.value ?: false
            val holeOptions = viewModel.availableHoles
            val freeHoles = listOf("H7", "H8")

            if (!isPremium && holeOptions[position] !in freeHoles) {
                // Przywróć poprzednią wartość i pokaż okno Premium
                binding.autoCompleteHole?.setText(viewModel.selectedHole.value, false)
                showPremiumRequired()
            } else {
                viewModel.selectedHole.value = holeOptions[position]
                viewModel.calculate()
            }
        }
        binding.btnInfo.setOnClickListener { showInfo() }

        binding.autoCompleteShaft.setOnItemClickListener { _, _, position, _ ->
            val isPremium = subManager.isPremium.value ?: false
            val shaftOptions = viewModel.availableShafts
            val freeShafts = listOf("h6", "h7", "g6")

            if (!isPremium && shaftOptions[position] !in freeShafts) {
                binding.autoCompleteShaft.setText(viewModel.selectedShaft.value, false)
                showPremiumRequired()
            } else {
                viewModel.selectedShaft.value = shaftOptions[position]
                viewModel.calculate()
            }
        }

        binding.btnCopyResults.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)

            // WYMUSZENIE: Zabieramy fokus z pola średnicy i chowamy klawiaturę
            binding.editTextDiameter.clearFocus()
            hideKeyboard()

            val resultString = """
            ${binding.tvFitStatus.text}
            ${binding.tvHoleValues.text}
            ${binding.tvShaftValues.text}
            ${binding.tvClearance?.text}
            """.trimIndent()

            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("CNC Fit Result", resultString)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, getString(R.string.params_copied), Toast.LENGTH_SHORT).show()
        }
    }
    private fun showInfo() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.window_information_modern, null)

        val infoHtml = """
    <b>${getString(R.string.status_clearance)}:</b><br/>
    ${getString(R.string.desc_clearance)}<br/><br/>
    
    <b>${getString(R.string.status_transition)}:</b><br/>
    ${getString(R.string.desc_transition)}<br/><br/>
    
    <b>${getString(R.string.status_interference)}:</b><br/>
    ${getString(R.string.desc_interference)}
""".trimIndent()

// Ustawienie tekstu w oknie informacyjnym (window_information_modern.xml)
        dialogView.findViewById<TextView>(R.id.infoContent).text =
            androidx.core.text.HtmlCompat.fromHtml(infoHtml, androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT)

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.btnOk).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    private fun observeViewModel() {
        viewModel.fitResult.observe(this) { res ->
            if (res != null) {
                binding.resultCard.visibility = View.VISIBLE

                // Pobieranie sformatowanych tekstów z zasobów
                binding.tvHoleValues.text = getString(R.string.fit_hole_label, res.holeMin, res.holeMax)
                binding.tvShaftValues.text = getString(R.string.fit_shaft_label, res.shaftMin, res.shaftMax)

                // Dynamiczny wybór etykiety: Luz vs Wcisk
                val labelRes = if (res.minClearance >= 0) {
                    R.string.fit_clearance_label
                } else {
                    R.string.fit_interference_label
                }
                binding.tvClearance.text = getString(labelRes, res.minClearance, res.maxClearance)

                // Ustawianie statusu i kolorów na podstawie typu pasowania
                when (res.type) {
                    FitType.CLEARANCE -> {
                        binding.tvFitStatus.text = getString(R.string.status_clearance)
                        binding.tvFitStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
                    }
                    FitType.TRANSITION -> {
                        binding.tvFitStatus.text = getString(R.string.status_transition)
                        binding.tvFitStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
                    }
                    FitType.INTERFERENCE -> {
                        binding.tvFitStatus.text = getString(R.string.status_interference)
                        binding.tvFitStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                    }
                }
            } else {
                binding.resultCard.visibility = View.GONE
            }
        }
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
    private fun createViewAEdgetoEdgeForAds() {
        val mainRoot = binding.main
        val customHeader = binding.customHeader
        val adContainerLayout = binding.adContainerLayout
        val adContainer = binding.adContainer

        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density

            // Tło wypełnia cały ekran
            mainRoot.setPadding(0, 0, 0, 0)

            // 1. GÓRA: Padding dla kontenera nagłówka (jak w Milling)
            customHeader?.updatePadding(top = systemBars.top)

            // 2. DÓŁ: Margines dla reklamy (zapewnia odstęp od paska nawigacji)
            adContainerLayout?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
                leftMargin = 0
                rightMargin = 0
            }
            insets
        }
        adContainer?.post { setupAds() }
    }
    private fun setupAds() {
        val adContainer = binding.adContainer
        val subManager = SubscriptionManager.getInstance(this)

        subManager.isPremium.observe(this) { isPremium ->
            if (isPremium) {
                adContainer.visibility = View.GONE
                adContainer.removeAllViews()
            } else {
                val screenHeightDp = resources.configuration.screenHeightDp
                if (screenHeightDp < 400) {
                    adContainer.visibility = View.GONE
                } else {
                    adContainer.visibility = View.VISIBLE
                    if (adContainer.isEmpty()) {
                        val adView = AdView(this)
                        this.adView = adView
                        adContainer.addView(adView)
                        val adSize = getAdSize(adContainer)
                        adView.setAdSize(adSize)
                        adView.adUnitId = BuildConfig.ADMOB_BANNER_ID
                        adView.loadAd(AdRequest.Builder().build())
                    }
                }
            }
        }
    }
    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
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

    override fun onPause() { adView?.pause(); super.onPause() }
    override fun onResume() {
        super.onResume(); adView?.resume()
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, "Pasowanie")
            param(FirebaseAnalytics.Param.SCREEN_CLASS, "ActivityTolerances")
        }
    }
    override fun onDestroy() { adView?.destroy(); super.onDestroy() }
}