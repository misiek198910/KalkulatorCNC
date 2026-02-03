package com.example.calkulatorcnc.ui.activities

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.example.calkulatorcnc.data.db.AppDatabase
import com.example.calkulatorcnc.entity.Tool
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import getMaterialsList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.toColorInt
import getIsoGroupForMaterial
import androidx.core.graphics.drawable.toDrawable

class ActivityAddTool : AppCompatActivity() {
    private var adView: AdView? = null
    private var toolId: Int = 0
    private lateinit var mainLayout: ConstraintLayout
    private lateinit var btnAdd: Button
    private lateinit var btnClose: Button
    private lateinit var etWorkpiece: EditText
    private lateinit var etName: EditText
    private lateinit var etF: EditText
    private lateinit var etS: EditText
    private lateinit var etNotes: EditText
    private var isEditMode: Boolean = false
    private lateinit var analytics: FirebaseAnalytics

    private fun updateMaterialUI(materialName: String?) {
        val isoGroup = getIsoGroupForMaterial(this, materialName)
        val color = when (isoGroup) {
            "P" -> "#0091EA".toColorInt()
            "M" -> "#FFEA00".toColorInt()
            "K" -> "#D50000".toColorInt()
            "N" -> "#00C853".toColorInt()
            "S" -> "#FF6D00".toColorInt()
            "H" -> "#9E9E9E".toColorInt()
            else -> android.graphics.Color.TRANSPARENT
        }

        if (color != android.graphics.Color.TRANSPARENT) {
            etWorkpiece.background = getIsoStripDrawable(color)
            etWorkpiece.setPadding((20 * resources.displayMetrics.density).toInt(), 0, 0, 0)
        } else {
            // Powrót do standardowego stylu
            etWorkpiece.setBackgroundResource(R.drawable.edittext_style)
            etWorkpiece.setPadding((12 * resources.displayMetrics.density).toInt(), 0, 0, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        createViewAEdgetoEdgeForAds()
        MobileAds.initialize(this)
        initViews()

        toolId = intent.getIntExtra("toolId", 0)
        isEditMode = intent.getBooleanExtra("editDisabled", false)

        loadDataFromIntent()
        setupListeners()
        setupMaterialSelection()
        analytics = Firebase.analytics
        mainLayout.setOnClickListener { hideKeyboard() }
    }

    private fun createViewAEdgetoEdgeForAds() {
        setContentView(R.layout.activity_add_tool)

        val mainRoot = findViewById<View>(R.id.main)
        val toolScrollView = findViewById<ScrollView>(R.id.toolScrollView)
        val adContainerLayout = findViewById<FrameLayout>(R.id.adContainerLayout)
        val adContainer = findViewById<FrameLayout>(R.id.adContainer)

        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            mainRoot.setPadding(0, 0, 0, 0)

            toolScrollView?.updatePadding(
                top = systemBars.top + (resources.displayMetrics.density * 8).toInt()
            )

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

    private fun setupAds() {
        val adContainer = findViewById<FrameLayout>(R.id.adContainer) ?: return
        val adContainerLayout = findViewById<FrameLayout>(R.id.adContainerLayout)

        SubscriptionManager.getInstance(this).isPremium.observe(this) { isPremium ->
            if (isPremium) {

                adContainerLayout?.visibility = View.GONE
                adContainer.removeAllViews()
                adView?.destroy()
                adView = null
            } else {

                val screenHeightDp = resources.configuration.screenHeightDp
                if (screenHeightDp < 400) {
                    adContainerLayout?.visibility = View.GONE
                } else {
                    adContainerLayout?.visibility = View.VISIBLE

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

    private fun initViews() {
        mainLayout = findViewById(R.id.main)
        etName = findViewById(R.id.editText1)
        etF = findViewById(R.id.editText2)
        etS = findViewById(R.id.editText3)
        etWorkpiece = findViewById(R.id.editText4)
        etNotes = findViewById(R.id.editText5)
        btnAdd = findViewById(R.id.button1)
        btnClose = findViewById(R.id.button2)

        applyChromebookFix(etName)
        applyChromebookFix(etF)
        applyChromebookFix(etS)
        applyChromebookFix(etWorkpiece) // Tu zachowamy ostrożność ze względu na dialog
        applyChromebookFix(etNotes)

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }

    private fun loadDataFromIntent() {
        if (isEditMode) {
            etName.setText(intent.getStringExtra("toolName") ?: "")
            etF.setText(intent.getStringExtra("toolF") ?: "")
            etS.setText(intent.getStringExtra("toolS") ?: "")
            etNotes.setText(intent.getStringExtra("notes") ?: "")

            val workpiece = intent.getStringExtra("workpiece") ?: ""
            etWorkpiece.setText(workpiece)
            updateMaterialUI(workpiece)
        }
    }

    private fun setupListeners() {
        btnAdd.setOnClickListener { saveAndReturn() }
        btnClose.setOnClickListener { finish() }
    }

    private fun setupMaterialSelection() {
        // Pole materiału jest zawsze klikalne, aby otworzyć dialog
        etWorkpiece.isFocusable = false
        etWorkpiece.setOnClickListener {
            showMaterialSelectionDialog()
        }
    }

    private fun showMaterialSelectionDialog() {
        val materials = getMaterialsList(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.window_materials_modern, null)
        val container = dialogView.findViewById<LinearLayout>(R.id.materialsContainer)

        // Zapewniamy pełną przezroczystość kontenera pod kafelkami
        container.setBackgroundColor(Color.TRANSPARENT)

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Obsługa przycisku zamknij
        dialogView.findViewById<Button>(R.id.btnCancelMaterials)?.setOnClickListener {
            dialog.dismiss()
        }

        materials.forEach { material ->
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_material, container, false)
            val tvMaterial = itemView.findViewById<TextView>(R.id.materialName)

            tvMaterial.text = "${material.name} (${material.isoGroup})"
            tvMaterial.setTextColor(android.graphics.Color.WHITE)

            val color = when(material.isoGroup) {
                "P" -> "#0091EA".toColorInt()
                "M" -> "#FFEA00".toColorInt()
                "K" -> "#D50000".toColorInt()
                "N" -> "#00C853".toColorInt()
                "S" -> "#FF6D00".toColorInt()
                "H" -> "#9E9E9E".toColorInt()
                else -> Color.TRANSPARENT
            }

            // Nakładamy przezroczysty styl z paskiem
            itemView.background = getIsoStripDrawable(color)
            tvMaterial.setPadding((20 * resources.displayMetrics.density).toInt(), 0, 0, 0)

            itemView.setOnClickListener {
                etWorkpiece.setText(material.name)
                updateMaterialUI(material.name)
                dialog.dismiss()
            }
            container.addView(itemView)
        }
        dialog.show()
    }

    private fun getIsoStripDrawable(isoColor: Int): android.graphics.drawable.Drawable {
        val density = resources.displayMetrics.density
        val stripWidth = (5 * density).toInt()

        val background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_search_input)
            ?: Color.TRANSPARENT.toDrawable()

        // 2. WARSTWA PASKA ISO
        val strip = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(isoColor)
            cornerRadii = floatArrayOf(8 * density, 8 * density, 0f, 0f, 0f, 0f, 8 * density, 8 * density)
        }

        val layers = arrayOf(background, strip)
        val layerDrawable = android.graphics.drawable.LayerDrawable(layers)

        // Ustawiamy pasek ISO na lewej krawędzi
        layerDrawable.setLayerWidth(1, stripWidth)
        layerDrawable.setLayerGravity(1, android.view.Gravity.START)

        return layerDrawable
    }

    private fun saveAndReturn() {
        val name = etName.text.toString().trim()

        // Automatyczne ustawienie materiału na "Inne", jeśli pole jest puste
        var workpieceValue = etWorkpiece.text.toString().trim()
        if (workpieceValue.isEmpty()) {
            workpieceValue = getString(R.string.other)
        }

        if (name.isEmpty()) {
            Toast.makeText(this, "Wpisz nazwę narzędzia", Toast.LENGTH_SHORT).show()
            return
        }

        val newTool = Tool(
            id = toolId,
            name = name,
            workpiece = workpieceValue,
            f = etF.text.toString().trim(),
            s = etS.text.toString().trim(),
            notes = etNotes.text.toString().trim()
        )

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@ActivityAddTool)
            if (isEditMode && toolId != 0) {
                db.toolDao().updateTool(newTool)
            } else {
                db.toolDao().insertTool(newTool)
            }
            withContext(Dispatchers.Main) { finish() }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    private fun applyChromebookFix(editText: EditText) {
        editText.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            elevation = (resources.displayMetrics.density * 2)

            setOnTouchListener { v, event ->
                if (event.action == android.view.MotionEvent.ACTION_UP) {
                    if (v.isFocusable) {
                        // Dla normalnych pól tekstowych wywołujemy klawiaturę
                        v.requestFocus()
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                        // Zwracamy false, by system mógł dokończyć obsługę zdarzenia (np. kursor)
                        return@setOnTouchListener false
                    } else {
                        // Dla etWorkpiece (które ma isFocusable = false w setupMaterialSelection)
                        v.performClick()
                        // KLUCZOWA ZMIANA: Zwracamy true, aby skonsumować zdarzenie i zapobiec
                        // ponownemu otwarciu dialogu przez systemowy OnClickListener
                        return@setOnTouchListener true
                    }
                }
                false
            }
        }
    }


    override fun onResume() {
        super.onResume()
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, "Dodaj Narzędzie")
            param(FirebaseAnalytics.Param.SCREEN_CLASS, "ActivityAddTool")
        }
    }
}