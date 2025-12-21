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

class ActivityMilling : AppCompatActivity() {

    private lateinit var mainLayout: ViewGroup
    private lateinit var edtPanel: LinearLayout
    private lateinit var buttonsPanel: LinearLayout
    private lateinit var spinner1: Spinner
    private lateinit var btnCalculate: Button
    private lateinit var btnClear: Button
    private lateinit var btnInfo: Button

    private lateinit var spinnerArray: Array<String>
    private var adapter: ArrayAdapter<String>? = null
    private var watcher: TextWatcher? = null

    // --- POPRAWKA: Deklaracja zmiennej adView ---
    private var adView: AdView? = null

    // Zmienne obliczeniowe
    private var calcSys: Int = 0
    private var f: Double = 0.0
    private var s: Double = 0.0
    private var fzResult: Double = 0.0
    private var vc: Double = 0.0
    private var dc: Double = 0.0
    private var asVal: Double = 0.0
    private var z: Double = 0.0
    private var l: Double = 0.0
    private var fz: Double = 0.0
    private var jump: Double = 0.0
    private var t: Double = 0.0
    private var vf: Double = 0.0
    private var d: Double = 0.0
    private var lb: Double = 0.0
    private var result1: Double = 0.0
    private var result2: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_milling)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupAds() // Wywołanie logiki reklam z subskrypcją
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
        // Inicjalizacja SDK reklam
        MobileAds.initialize(this)

        val adContainer = findViewById<FrameLayout>(R.id.adContainer)
        val subManager = SubscriptionManager.getInstance(this)

        // Obserwacja statusu Premium
        subManager.isPremium.observe(this) { isPremium ->
            if (isPremium) {
                // JEŚLI PREMIUM: Ukryj reklamy
                adContainer.visibility = View.GONE
                adContainer.removeAllViews()
                adView?.destroy()
                adView = null
            } else {
                // JEŚLI NIE PREMIUM: Pokaż reklamy (z uwzględnieniem orientacji)
                val screenHeightDp = resources.configuration.screenHeightDp
                if (screenHeightDp < 400) {
                    adContainer.visibility = View.GONE
                } else {
                    adContainer.visibility = View.VISIBLE
                    // Twórz nową reklamę tylko jeśli jeszcze nie istnieje
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

    // Pozostała część logiki (setupLayout, setupSpinner, updateDynamicInputs, button1_Clicked itd.)
    // pozostaje bez zmian, zgodnie z Twoją poprzednią wersją...

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
        spinnerArray = resources.getStringArray(R.array.miling_spinner1_data)
        adapter = ArrayAdapter(this, R.layout.spinner_item, spinnerArray)
        adapter?.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinner1.adapter = adapter

        spinner1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateDynamicInputs(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateDynamicInputs(position: Int) {
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
            0 -> arrayOf(R.string.VC, R.string.DC, R.string.FZ, R.string.Z)
            1 -> arrayOf(R.string.VC, R.string.DC, R.string.JT)
            4 -> arrayOf(R.string.AS, R.string.T, R.string.VC, R.string.L)
            5 -> arrayOf(R.string.VF, R.string.VC, R.string.Z, R.string.LB)
            6 -> arrayOf(R.string.AS, R.string.VC, R.string.D, R.string.Z, R.string.L)
            7 -> arrayOf(R.string.VF, R.string.VC, R.string.Z, R.string.D)
            2, 3, 8 -> {
                showMessage(getString(R.string.Full_version_2))
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
        fun getValue(index: Int) = tryStringParse(edtPanel.getChildAt(index) as? EditText)

        when (id) {
            0 -> {
                vc = getValue(0); dc = getValue(1); fz = getValue(2); z = getValue(3)
                s = (vc * calcSys) / 3.14 / dc
                f = fz * z * s
                fzResult = 0.0
            }
            1 -> {
                vc = getValue(0); dc = getValue(1); jump = getValue(2)
                s = (vc * calcSys) / 3.14 / dc
                f = jump * s
            }
            4 -> {
                asVal = getValue(0); vc = getValue(1); t = getValue(2); l = getValue(3)
                fzResult = (asVal * t) / (l * vc * calcSys)
            }
            5 -> {
                vf = getValue(0); vc = getValue(1); z = getValue(2); lb = getValue(3)
                fzResult = (vf * lb) / (vc * z * calcSys)
            }
            6 -> {
                asVal = getValue(0); vc = getValue(1); d = getValue(2); z = getValue(3); l = getValue(4)
                fzResult = (asVal * d * 3.14) / (l * vc * z * calcSys)
            }
            7 -> {
                vf = getValue(0); vc = getValue(1); z = getValue(2); d = getValue(3)
                fzResult = (vf * d * 3.14) / (vc * z * calcSys)
            }
        }

        showResultDialog(f.roundToLong().toDouble(), s.roundToLong().toDouble(), fzResult.roundToLong().toDouble())
        zeroVariables()
    }

    private fun showResultDialog(resF: Double, resS: Double, resFZ: Double) {
        val inflater = LayoutInflater.from(this)
        val resultView = inflater.inflate(R.layout.window_calculation_result, null)

        resultView.findViewById<TextView>(R.id.textView1).text = "${getString(R.string.f_colon)} $resF"
        resultView.findViewById<TextView>(R.id.textView2).text = "${getString(R.string.s_colon)} $resS"
        resultView.findViewById<TextView>(R.id.textView3).text = "${getString(R.string.fz_colon)} $resFZ"

        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setView(resultView)
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
        val prefix = if (calcSys == 1000) "info_${item}_m" else "info_${item}_inch"
        val resId = resources.getIdentifier(prefix, "string", packageName)
        val content = if (resId != 0) getString(resId) else "No info available"

        val inflater = LayoutInflater.from(this)
        val infoView = inflater.inflate(R.layout.window_information, null)
        infoView.findViewById<TextView>(R.id.textView1).text = Html.fromHtml(content, Html.FROM_HTML_MODE_COMPACT)

        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setView(infoView)
            .setPositiveButton(getString(R.string.close)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun tryStringParse(edt: EditText?): Double {
        return edt?.text?.toString()?.toDoubleOrNull() ?: 0.0
    }

    private fun zeroVariables() {
        f = 0.0; s = 0.0; fzResult = 0.0; vc = 0.0; dc = 0.0; asVal = 0.0
        z = 0.0; l = 0.0; fz = 0.0; jump = 0.0; t = 0.0; vf = 0.0; d = 0.0; lb = 0.0
        result1 = 0.0; result2 = 0.0
    }

    private fun showMessage(str: String) {
        AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(getString(R.string.Full_version_1))
            .setMessage(str)
            .setPositiveButton("Ok") { dialog, _ -> dialog.dismiss() }
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
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    fun buttonBack_Clicked(view: View?) = finish()
}