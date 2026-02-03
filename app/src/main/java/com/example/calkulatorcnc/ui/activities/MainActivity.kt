package com.example.calkulatorcnc.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isEmpty
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.example.calkulatorcnc.data.preferences.ClassPrefs
import com.example.calkulatorcnc.ui.languages.LanguageManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {

    private lateinit var viewRedDot: View
    private var latestNewsTimestampFromServer: Long = 0
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var updateResultLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        viewRedDot = findViewById(R.id.viewRedDot)
        viewRedDot.visibility = View.GONE
        createViewAEdgetoEdgeForAds()
        migrateToProfessionalLanguageSystem()
        setupClickListeners()
        checkForNewNews()
        analytics = Firebase.analytics
        updateResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode != RESULT_OK) {
                // Tutaj możesz obsłużyć sytuację, gdy użytkownik anulował aktualizację
                // lub wystąpił błąd. Aktualizacja elastyczna spróbuje ponownie później.
            }
        }
        checkForUpdates()
    }

    private fun createViewAEdgetoEdgeForAds() {

        val mainRoot = findViewById<View>(R.id.main)
        val gridLayout = findViewById<android.widget.GridLayout>(R.id.mainGrid)
        val adContainerLayout = findViewById<FrameLayout>(R.id.adContainerLayout)
        val adContainer = findViewById<FrameLayout>(R.id.adContainer)

        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            mainRoot.setPadding(0, 0, 0, 0)
            gridLayout.updatePadding(
                top = systemBars.top + (resources.displayMetrics.density * 12).toInt()
            )
            adContainerLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
                leftMargin = systemBars.left
                rightMargin = systemBars.right
            }
            insets
        }

        adContainer.postDelayed({
            setupAds()
        }, 100)
    }

    private fun setupAds() {
        val adContainer = findViewById<FrameLayout>(R.id.adContainer)
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
                        // OBLICZANIE ROZMIARU ADAPTACYJNEGO
                        val adView = AdView(this)
                        adContainer.addView(adView)

                        val adSize = getAdSize(adContainer)
                        adView.setAdSize(adSize)
                        adView.adUnitId = BuildConfig.ADMOB_BANNER_ID

                        val adRequest = AdRequest.Builder().build()
                        adView.loadAd(adRequest)
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

    private fun setupClickListeners() {
        findViewById<View>(R.id.card_calc).setOnClickListener {
            startActivity(Intent(this, ActivityMilling::class.java))
        }

        findViewById<View>(R.id.card_tools).setOnClickListener {
            startActivity(Intent(this, ActivityTourning::class.java))
        }

        findViewById<View>(R.id.card_fits).setOnClickListener {
            startActivity(Intent(this, ActivityTools::class.java))
        }

        findViewById<View>(R.id.card_other).setOnClickListener {
            startActivity(Intent(this, ActivityTables::class.java))
        }

        findViewById<View>(R.id.card_settings).setOnClickListener {
            startActivity(Intent(this, ActivitySettings::class.java))
        }

        findViewById<View>(R.id.card_notifications).setOnClickListener {
            val pref = ClassPrefs()
            if (latestNewsTimestampFromServer != 0L) {
                pref.savePrefString(this, "last_seen_news_date", latestNewsTimestampFromServer.toString())
            }

            startActivity(Intent(this, ActivityNews::class.java))
        }

        findViewById<View>(R.id.card_no_ads).setOnClickListener {
            startActivity(Intent(this, ActivitySubscription::class.java))
        }

        findViewById<View>(R.id.card_tolerances).setOnClickListener {
            startActivity(Intent(this, ActivityTolerances::class.java))
        }

    }

    private fun migrateToProfessionalLanguageSystem() {
        val pref = ClassPrefs()
        // Sprawdzamy, czy użytkownik ma już nowy system (ISO String)
        val currentIso = pref.loadPrefString(this, "language_iso")

        if (currentIso.isEmpty()) {
            // Jeśli nie ma ISO, sprawdzamy stare dane (Int)
            val oldInt = pref.loadPrefInt(this, "language_data")

            val migratedIso = when (oldInt) {
                0 -> "pl"
                1 -> "en"
                else -> {
                    // Jeśli to zupełnie nowy użytkownik, weź język jego telefonu
                    val deviceLang = resources.configuration.locales[0].language
                    if (LanguageManager.supportedLanguages.any { it.isoCode == deviceLang }) deviceLang else "en"
                }
            }

            // Zapisujemy nowy standard i ustawiamy go w systemie
            pref.savePrefString(this, "language_iso", migratedIso)
            val appLocale = androidx.core.os.LocaleListCompat.forLanguageTags(migratedIso)
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    private fun checkForNewNews() {
        val pref = ClassPrefs()
        val db = FirebaseFirestore.getInstance()

        db.collection("news")
            .whereEqualTo("isVisible", true)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Pobieramy czas najnowszego newsa
                    latestNewsTimestampFromServer =
                        documents.documents[0].getTimestamp("date")?.toDate()?.time ?: 0L

                    // Pobieramy czas ostatnio "widzianego" newsa z pamięci telefonu
                    val lastSeenDate =
                        pref.loadPrefString(this, "last_seen_news_date").toLongOrNull() ?: 0L

                    // POKAZUJEMY kropkę tylko jeśli czas z serwera jest WIĘKSZY niż zapamiętany
                    if (latestNewsTimestampFromServer > lastSeenDate) {
                        viewRedDot.visibility = View.VISIBLE
                    } else {
                        viewRedDot.visibility = View.GONE
                    }
                }
            }
    }

    private fun checkForUpdates() {
        appUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                // Jeśli aktualizacja jest dostępna, zaproponuj ją
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateResultLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                )
            }
        }
    }

    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            // Po pobraniu wyświetlamy SnackBar z prośbą o restart
            showUpdateCompletedSnackbar()
        }
    }

    private fun showUpdateCompletedSnackbar() {
        Snackbar.make(
            findViewById(android.R.id.content),
            getString(R.string.update_downloaded),
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction(getString(R.string.restart_app)) { appUpdateManager.completeUpdate() }
            show()
        }
    }

    override fun onResume() {
        super.onResume()
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, "MainActivity")
            param(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")

            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    showUpdateCompletedSnackbar()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Sprawdzamy, czy manager został zainicjalizowany, aby uniknąć błędu
        if (::appUpdateManager.isInitialized) {
            appUpdateManager.unregisterListener(listener)
        }
    }
}