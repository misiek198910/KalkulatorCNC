package com.example.calkulatorcnc.ui.activities

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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.data.preferences.ClassPrefs
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlin.math.roundToLong

class ActivityTourning : AppCompatActivity() {

    // Widoki
    private lateinit var mainLayout: ConstraintLayout
    private lateinit var edtPanel: LinearLayout
    private lateinit var buttonsPanel: LinearLayout
    private lateinit var spinner1: Spinner
    private lateinit var btnCalculate: Button
    private lateinit var btnClear: Button
    private lateinit var btnInfo: Button

    private lateinit var spinnerArray: Array<String>
    private var adapter: ArrayAdapter<String>? = null
    private var watcher: TextWatcher? = null

    // --- DODANA DEKLARACJA ---
    private var adView: AdView? = null

    // Zmienne obliczeniowe
    private var calcSys: Int = 0
    private var f: Double = 0.0
    private var s: Double = 0.0
    private var ap: Double = 0.0
    private var vc: Double = 0.0
    private var fn: Double = 0.0
    private var kc: Double = 0.0
    private var dm: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tourning)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupAds() // Logika reklam zależna od subskrypcji
        setupLayout()
        setupSpinner()

        val pref = ClassPrefs()
        calcSys = if (pref.loadPrefInt(this, "calcsys_data") == 1) 12 else 1000

        mainLayout.setOnClickListener { hideKeyboard() }
    }

    private fun initViews() {
        mainLayout = findViewById(R.id.main)
        spinner1 = findViewById(R.id.spinner1)
        btnCalculate = findViewById(R.id.miling_button1)
        btnClear = findViewById(R.id.miling_button2)
        btnInfo = findViewById(R.id.miling_button3)
        edtPanel = findViewById(R.id.buttons_panel)
        buttonsPanel = findViewById(R.id.layout)
    }

    private fun setupAds() {
        // Inicjalizacja Mobile Ads
        MobileAds.initialize(this)

        val adContainer = findViewById<FrameLayout>(R.id.adContainer)
        val subManager = SubscriptionManager.getInstance(this)

        // Obserwowanie statusu Premium
        subManager.isPremium.observe(this) { isPremium ->
            if (isPremium) {
                // UKRYWANIE REKLAM DLA PREMIUM
                adContainer.visibility = View.GONE
                adContainer.removeAllViews()
                adView?.destroy()
                adView = null
            } else {
                // POKAZYWANIE REKLAM DLA NON-PREMIUM
                val screenHeightDp = resources.configuration.screenHeightDp
                if (screenHeightDp < 400) {
                    adContainer.visibility = View.GONE
                } else {
                    adContainer.visibility = View.VISIBLE
                    if (adContainer.childCount == 0) {
                        val newAdView = AdView(this).apply {
                            setAdSize(AdSize.BANNER)
                            adUnitId = BuildConfig.ADMOB_BANNER_ID
                        }
                        adView = newAdView
                        adContainer.addView(newAdView)
                        newAdView.loadAd(AdRequest.Builder().build())
                    }
                }
            }
        }
    }

    private fun setupLayout() {
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            edtPanel.orientation = LinearLayout.HORIZONTAL
            if (resources.configuration.screenHeightDp < 400) {
                val params = buttonsPanel.layoutParams as ConstraintLayout.LayoutParams
                params.apply {
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    setMargins(0, 0, 0, 0)
                }
                buttonsPanel.layoutParams = params
            }
        } else {
            edtPanel.orientation = LinearLayout.VERTICAL
            edtPanel.minimumWidth = dpToPx(100)
        }
    }

    private fun setupSpinner() {
        spinnerArray = resources.getStringArray(R.array.tourning_spinner1_data)
        adapter = ArrayAdapter(this, R.layout.spinner_item, spinnerArray)
        adapter?.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinner1.adapter = adapter

        spinner1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateInputsForPosition(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateInputsForPosition(position: Int) {
        watcher = object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnClear.isEnabled = !s.isNullOrEmpty()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        }

        edtPanel.removeAllViews()
        btnCalculate.isEnabled = true
        btnClear.isEnabled = false
        btnInfo.isEnabled = true

        val hints = when (position) {
            0 -> arrayOf(R.string.VC, R.string.AP, R.string.Fn, R.string.KC, R.string.DM)
            1 -> arrayOf(R.string.VC, R.string.DM, R.string.Fn)
            2, 3 -> {
                showSimpleMessage(getString(R.string.Full_version_2))
                btnCalculate.isEnabled = false
                btnInfo.isEnabled = false
                emptyArray()
            }
            else -> emptyArray()
        }

        hints.forEachIndexed { index, hintRes ->
            val edt = createEditText(index, watcher)
            edt.hint = getString(hintRes)
            edtPanel.addView(edt)
        }
    }

    fun button1_Clicked(view: View?) {
        val id = spinner1.selectedItemPosition
        fun getVal(index: Int) = tryStringParse(edtPanel.getChildAt(index) as? EditText)

        when (id) {
            0 -> {
                vc = getVal(0); ap = getVal(1); fn = getVal(2); kc = getVal(3); dm = getVal(4)
                s = (vc * calcSys) / 3.14 / dm
                val powerCalc = vc * ap * fn * kc
                val constantDivisor = 600.0 * 3.0
                f = powerCalc / constantDivisor
            }
            1 -> {
                vc = getVal(0); dm = getVal(1); fn = getVal(2)
                s = (vc * calcSys) / 3.14 / dm
                f = fn * s
            }
        }

        showResult(f.roundToLong().toDouble(), s.roundToLong().toDouble())
        zeroVariables()
    }

    private fun showResult(resF: Double, resS: Double) {
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.window_calculation_result, null)

        v.findViewById<TextView>(R.id.textView1).text = "${getString(R.string.f_colon)} $resF"
        v.findViewById<TextView>(R.id.textView2).text = "${getString(R.string.s_colon)} $resS"
        v.findViewById<TextView>(R.id.textView3).text = "${getString(R.string.fz_colon)} 0.0"

        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setView(v)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.close)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    fun button2_Clicked(view: View?) {
        btnClear.isEnabled = false
        for (i in 0 until edtPanel.childCount) {
            (edtPanel.getChildAt(i) as? EditText)?.setText("")
        }
    }

    fun button3_Clicked(view: View?) {
        val item = spinner1.selectedItemPosition
        val infoIndex = item + 8
        val prefix = if (calcSys == 1000) "info_${infoIndex}_m" else "info_${infoIndex}_inch"

        val resId = resources.getIdentifier(prefix, "string", packageName)
        val content = if (resId != 0) getString(resId) else "No info"

        val inflater = LayoutInflater.from(this)
        val infoV = inflater.inflate(R.layout.window_information, null)
        infoV.findViewById<TextView>(R.id.textView1).text = Html.fromHtml(content, Html.FROM_HTML_MODE_COMPACT)

        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setView(infoV)
            .setPositiveButton(getString(R.string.close)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun tryStringParse(edt: EditText?): Double {
        return edt?.text?.toString()?.toDoubleOrNull() ?: 0.0
    }

    private fun zeroVariables() {
        f = 0.0; s = 0.0; ap = 0.0; vc = 0.0; fn = 0.0; kc = 0.0; dm = 0.0
    }

    private fun showSimpleMessage(str: String) {
        AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(getString(R.string.Full_version_1))
            .setMessage(str)
            .setPositiveButton("Ok") { d, _ -> d.dismiss() }
            .show()
    }

    private fun createEditText(index: Int, tw: TextWatcher?): EditText {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val params = if (isLandscape) {
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
        } else {
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        params.setMargins(10, 5, 10, 5)

        return EditText(this).apply {
            textSize = 20f
            setTextColor(Color.BLACK)
            setHintTextColor(Color.parseColor("#C0C0C0"))
            tag = index
            gravity = Gravity.CENTER
            setPadding(10, 15, 10, 15)
            setBackgroundResource(R.drawable.edittext_style)
            layoutParams = params
            inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_CLASS_NUMBER
            addTextChangedListener(tw)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    fun buttonBack_Clicked(view: View?) = finish()
}