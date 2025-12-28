package com.example.calkulatorcnc.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.example.calkulatorcnc.data.db.AppDatabase
import com.example.calkulatorcnc.entity.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import getMaterialsList
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import android.view.ViewGroup
import com.example.calkulatorcnc.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

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
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }

    private fun loadDataFromIntent() {
        if (isEditMode) {
            etName.setText(intent.getStringExtra("toolName") ?: "")
            etF.setText(intent.getStringExtra("toolF") ?: "")
            etS.setText(intent.getStringExtra("toolS") ?: "")
            etWorkpiece.setText(intent.getStringExtra("workpiece") ?: "")
            etNotes.setText(intent.getStringExtra("notes") ?: "")
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
        val isPremium = SubscriptionManager.getInstance(this).isPremium.value ?: false
        val materials = getMaterialsList(this)

        val dialogView = LayoutInflater.from(this).inflate(R.layout.window_materials_modern, null)
        val container = dialogView.findViewById<LinearLayout>(R.id.materialsContainer)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        materials.forEach { material ->
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_material, container, false)
            itemView.findViewById<TextView>(R.id.materialName).text = material.name

            itemView.setOnClickListener {
                etWorkpiece.setText(material.name)
                etWorkpiece.isFocusable = false
                dialog.dismiss()
            }
            container.addView(itemView)
        }

        val otherItem = LayoutInflater.from(this).inflate(R.layout.item_material, container, false)
        otherItem.findViewById<TextView>(R.id.materialName).text = getString(R.string.other)
        otherItem.setOnClickListener {
            etWorkpiece.isFocusableInTouchMode = true
            etWorkpiece.setText("")
            etWorkpiece.requestFocus()
            dialog.dismiss()
        }
        container.addView(otherItem)
        dialog.show()
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
}