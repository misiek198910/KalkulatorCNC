package com.example.calkulatorcnc.ui.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.example.calkulatorcnc.data.preferences.ClassPrefs
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import android.view.ViewGroup

class ActivitySettings : AppCompatActivity() {

    private lateinit var tvCurrentSystem: TextView
    private lateinit var tvCurrentLang: TextView
    private lateinit var adContainer: FrameLayout
    private var adView: AdView? = null
    private val prefs = ClassPrefs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        MobileAds.initialize(this)
        createViewAEdgetoEdgeForAds()
        initViews()
        setupAds()
        updateStatusTexts()

        onBackPressedDispatcher.addCallback(this) { finish() }
    }
    private fun createViewAEdgetoEdgeForAds() {
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        val mainRoot = findViewById<View>(R.id.main)
        val customHeader = findViewById<LinearLayout>(R.id.customHeader)
        val adContainerLayout = findViewById<FrameLayout>(R.id.adContainerLayout)
        val adContainer = findViewById<FrameLayout>(R.id.adContainer)

        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // 1. Tło płynie pod systemowe paski
            mainRoot.setPadding(0, 0, 0, 0)

            // 2. Nagłówek - padding góra (bezpieczna strefa dla zegara)
            customHeader?.updatePadding(top = systemBars.top)

            // 3. Reklama - margines dół i boki (bezpieczna strefa nawigacji)
            adContainerLayout?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
                leftMargin = systemBars.left
                rightMargin = systemBars.right
            }

            insets
        }

        // 4. Inicjalizacja reklamy adaptacyjnej po ułożeniu widoku
        adContainer?.post {
            setupAds()
        }
    }

    private fun initViews() {
        tvCurrentSystem = findViewById(R.id.tvCurrentSystem)
        tvCurrentLang = findViewById(R.id.tvCurrentLang)
        adContainer = findViewById(R.id.adContainer)

        findViewById<View>(R.id.button_back).setOnClickListener { finish() }
        findViewById<View>(R.id.card_system).setOnClickListener { showSystemDialog() }
        findViewById<View>(R.id.card_language).setOnClickListener { showLanguageDialog() }
        findViewById<View>(R.id.card_btn2).setOnClickListener { startActivity(Intent(this, ActivityApps::class.java)) }
        findViewById<View>(R.id.card_btn3).setOnClickListener { openMarketPage(packageName) }
        findViewById<View>(R.id.card_btn4).setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buycoffee.to/mivs/MojaParafia"))
            startActivity(browserIntent)
        }
        findViewById<View>(R.id.card_btn5).setOnClickListener { startActivity(Intent(this, ActivitySubscription::class.java)) }
        findViewById<View>(R.id.card_privacy).setOnClickListener {startActivity(Intent(this,ActivityPrivacyPolicy::class.java))}
    }

    private fun updateStatusTexts() {
        // Pokazywanie aktualnego ustawienia pod główną nazwą kafli
        val sysPos = prefs.loadPrefInt(this, "calcsys_data")
        val sysArray = resources.getStringArray(R.array.spinner1_items)
        tvCurrentSystem.text = sysArray.getOrElse(sysPos) { "" }

        val langPos = prefs.loadPrefInt(this, "language_data")
        val langArray = resources.getStringArray(R.array.spinner2_items)
        tvCurrentLang.text = langArray.getOrElse(langPos) { "" }
    }

    private fun showSystemDialog() {
        val options = resources.getStringArray(R.array.spinner1_items)
        val current = prefs.loadPrefInt(this, "calcsys_data")
        createCustomSelectionDialog(getString(R.string.calc_system), options, current) { which ->
            prefs.savePrefInt(this, "calcsys_data", which)
            updateStatusTexts()
        }
    }

    private fun showLanguageDialog() {
        val options = resources.getStringArray(R.array.spinner2_items)
        val current = prefs.loadPrefInt(this, "language_data")
        createCustomSelectionDialog(getString(R.string.language), options, current) { which ->
            prefs.savePrefInt(this, "language_data", which)
            val iso = if (which == 0) "pl" else "en"
            val appLocale = LocaleListCompat.forLanguageTags(iso)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    private fun createCustomSelectionDialog(title: String, options: Array<String>, selectedIndex: Int, onSelected: (Int) -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_selection_modern, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.dialogTitle).text = title
        val container = dialogView.findViewById<LinearLayout>(R.id.optionsContainer)

        options.forEachIndexed { index, option ->
            val tvOption = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(50)
                )
                text = option
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                setPaddingRelative(dpToPx(16), 0, dpToPx(16), 0)
                textSize = 18f

                        // Stylizacja zaznaczenia
                        if (index == selectedIndex) {
                            setTextColor("#FF9800".toColorInt())
                            setTypeface(null, Typeface.BOLD)
                            setBackgroundResource(R.drawable.bg_search_input)
                        } else {
                            setTextColor(Color.WHITE)
                            setTypeface(null, Typeface.NORMAL)
                            // Standardowe tło z efektem kliknięcia
                            val outValue = TypedValue()
                            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                            setBackgroundResource(outValue.resourceId)
                        }

                setOnClickListener {
                    onSelected(index)
                    dialog.dismiss()
                }
            }
            container.addView(tvOption)

            if (index < options.size - 1) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                    setBackgroundColor("#1AFFFFFF".toColorInt()) // Bardzo delikatna biel
                }
                container.addView(divider)
            }
        }

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun openMarketPage(appId: String) {
        val uri = "market://details?id=$appId".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show()
        }
    }

    private fun setupAds() {

        val adContainer = findViewById<FrameLayout>(R.id.adContainer) ?: return
        val adContainerLayout = findViewById<FrameLayout>(R.id.adContainerLayout) ?: return

        SubscriptionManager.getInstance(this).isPremium.observe(this) { isPremium ->
            if (isPremium) {
                // 1. Logika PREMIUM: Czyścimy kontener i niszczymy reklamę
                adContainerLayout.visibility = View.GONE
                adContainer.removeAllViews()
                adView?.destroy()
                adView = null
            } else {
                // 2. Logika FREE: Sprawdzamy wysokość ekranu (bezpieczeństwo UI)
                val screenHeightDp = resources.configuration.screenHeightDp
                if (screenHeightDp < 400) {
                    adContainerLayout.visibility = View.GONE
                } else {
                    adContainerLayout.visibility = View.VISIBLE

                    // Ładujemy reklamę tylko jeśli jeszcze nie istnieje
                    if (adView == null) {
                        val newAdView = AdView(this).apply {
                            adUnitId = BuildConfig.ADMOB_BANNER_ID
                            // ZAMIANA: Używamy Twojej funkcji getAdSize zamiast AdSize.BANNER
                            setAdSize(getAdSize(adContainer))
                        }

                        adView = newAdView
                        adContainer.removeAllViews() // Czyścimy na wypadek duplikacji
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
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}