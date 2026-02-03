package com.example.calkulatorcnc.ui.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.google.android.gms.ads.*
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

class ActivityApps : AppCompatActivity() {
    private var toolbarTitle: TextView? = null
    private var buttonBack: ImageButton? = null
    private lateinit var adContainerLayout: FrameLayout
    private lateinit var adContainer: FrameLayout
    private var adView: AdView? = null

    private lateinit var analytics: FirebaseAnalytics
    private class AppItem(val iconResId: Int, val titleResId: Int, val appLink: String?)

    private val appItems: Array<AppItem> = arrayOf(
        AppItem(R.drawable.droga_krzyzowa_logo, R.string.app_droga_krzyzowa, "droga_krzyzowa.droga_krzyzowa"),
        AppItem(R.drawable.kalendarz_liturgiczny_logo, R.string.app_kalendarz_liturgiczny, "mivs.kalendarz_liturgiczny"),
        AppItem(R.drawable.kalkulator_cnc_logo, R.string.app_kalkulator_cnc, "kalkulator.cnc"),
        AppItem(R.drawable.ktoz_jak_bog_logo, R.string.app_ktoz_jak_bog, "mivs.ktozjakbog"),
        AppItem(R.drawable.moj_rozaniec_logo, R.string.app_moj_rozaniec, "mivs.m_j_r_aniec"),
        AppItem(R.drawable.niewolnik_maryi_logo, R.string.app_niewolnik_maryi, "mivs.niewolnik_maryi"),
        AppItem(R.drawable.objawienia_logo, R.string.app_objawienia, "mivs.objawienia"),
        AppItem(R.drawable.rachunek_sumienia_logo, R.string.app_rachunek_sumienia, "pakiet.rachuneksumienia"),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        createViewAEdgetoEdgeForAds()
        setupToolbar()
        setupAppList()
        analytics = Firebase.analytics
    }

    private fun createViewAEdgetoEdgeForAds() {

        enableEdgeToEdge()
        setContentView(R.layout.activity_apps)

        val mainRoot = findViewById<View>(R.id.main)
        val customHeader = findViewById<LinearLayout>(R.id.customHeader)
        adContainerLayout = findViewById(R.id.adContainerLayout)
        adContainer = findViewById(R.id.adContainer)

        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            mainRoot.setPadding(0, 0, 0, 0)
            customHeader?.updatePadding(top = systemBars.top)

            adContainerLayout?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
            }

            insets
        }

        MobileAds.initialize(this)

        SubscriptionManager.getInstance(this).isPremium.observe(this) { isPremium ->
            handleAds(isPremium)

            if (!isPremium) {
                adContainerLayout?.post {
                    setupAds()
                }
            }
        }
    }

    private fun setupToolbar() {
        buttonBack = findViewById(R.id.button_back)
        toolbarTitle = findViewById(R.id.toolbar_title)
        buttonBack?.setOnClickListener { finish() }
    }

    private fun setupAppList() {
        val container = findViewById<LinearLayout>(R.id.appListContainer)
        container.gravity = Gravity.CENTER_HORIZONTAL

        // Obliczanie marginesów w DP
        val margin8 = dpToPx(8)
        val margin12 = dpToPx(12)

        for (item in appItems) {
            // Tworzenie "Szklanego" tła programowo
            val glassBackground = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(16).toFloat()
                // Półprzezroczysty biały (odpowiednik #1AFFFFFF)
                setColor(Color.parseColor("#1AFFFFFF"))
                // Subtelne obramowanie (odpowiednik #33FFFFFF)
                setStroke(dpToPx(1), Color.parseColor("#33FFFFFF"))
            }

            val panel = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                background = glassBackground
                setPadding(margin12, margin12, margin12, margin12)
                isClickable = true
                isFocusable = true

                // Efekt kliknięcia (Ripple)
                val outValue = TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                foreground = ContextCompat.getDrawable(context, outValue.resourceId)

                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(margin8, margin8, margin8, margin8)
                    // Ograniczenie szerokości w trybie Landscape
                    if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                        width = dpToPx(600)
                    }
                }
            }

            val icon = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(50), dpToPx(50))
                setImageResource(item.iconResId)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }

            val label = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
                )
                setText(item.titleResId)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(16), 0, 0, 0)
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
            }

            // Ikona strzałki po prawej (akcent wizualny)
            val arrow = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(24), dpToPx(24))
                setImageResource(R.drawable.ic_search) // Możesz podmienić na strzałkę w prawo
                alpha = 0.5f
            }

            panel.addView(icon)
            panel.addView(label)
            panel.addView(arrow)

            panel.setOnClickListener {
                item.appLink?.let { OpenStore(it) }
            }

            container.addView(panel)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun handleAds(isPremium: Boolean) {
        if (isPremium) {
            adView?.destroy()
            adView = null
            adContainer.removeAllViews()
            adContainerLayout.visibility = View.GONE
        } else {
            if (adView == null) {
                adContainerLayout.visibility = View.VISIBLE
                setupAds()
            }
        }
    }

    private fun setupAds() {
        val adBannerId = BuildConfig.ADMOB_BANNER_ID

        if (adBannerId == "BRAK_ID" || adBannerId.isEmpty()) {
            findViewById<View>(R.id.adContainerLayout)?.visibility = View.GONE
            return
        }

        val adContainerLayout = findViewById<FrameLayout>(R.id.adContainerLayout) ?: return
        val adContainer = findViewById<FrameLayout>(R.id.adContainer) ?: return

        SubscriptionManager.getInstance(this).isPremium.observe(this) { isPremium ->
            if (isPremium) {
                adContainerLayout.visibility = View.GONE
                adContainer.removeAllViews()
                adView?.destroy()
                adView = null
            } else {
                if (resources.configuration.screenHeightDp < 400) {
                    adContainerLayout.visibility = View.GONE
                } else {
                    adContainerLayout.visibility = View.VISIBLE

                    if (adView == null) {
                        val newAdView = AdView(this).apply {
                            adUnitId = adBannerId
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

    fun OpenStore(packageName: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    override fun onPause() { adView?.pause(); super.onPause() }

    override fun onResume() {
        super.onResume();
        adView?.resume()
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, "InneAplikacje")
            param(FirebaseAnalytics.Param.SCREEN_CLASS, "ActivityApps")
        }
    }

    override fun onDestroy() { adView?.destroy(); super.onDestroy() }
}