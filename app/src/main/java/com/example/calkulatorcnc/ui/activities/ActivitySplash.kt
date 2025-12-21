package com.example.calkulatorcnc.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.example.calkulatorcnc.billing.SubscriptionStatus
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

@SuppressLint("CustomSplashScreen")
class ActivitySplash : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
        private const val AD_TIMEOUT_MS = 8000L
    }

    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd = false
    private var isDismissed = false
    private var isConsentStarted = false // FLAG: Zapobiega podwójnemu uruchomieniu logiki

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val subManager = SubscriptionManager.getInstance(applicationContext)

        // Obserwujemy status - upewniamy się, że logika odpali się tylko RAZ
        subManager.subscriptionStatus.observe(this) { status ->
            if (isDismissed || isConsentStarted) return@observe

            when (status) {
                SubscriptionStatus.PREMIUM -> {
                    Log.d(TAG, "Użytkownik Premium - pomijam reklamy")
                    navigateToMainApp()
                }
                SubscriptionStatus.NON_PREMIUM -> {
                    isConsentStarted = true // Blokujemy kolejne wywołania przy zmianach statusu
                    setupConsentAndLoadAd()
                }
                else -> {} // Status CHECKING - czekamy
            }
        }
    }

    private fun setupConsentAndLoadAd() {

        val debugSettings = ConsentDebugSettings.Builder(this)
            .addTestDeviceHashedId("B3EEABB8EE11C2BE770B684D95219ECB") // Twój ID z logów
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .build()

        Log.d(TAG, "Sprawdzanie zgód RODO (start)...")
        val params = ConsentRequestParameters.Builder()
            .setConsentDebugSettings(debugSettings)
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(this, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { loadAndShowError ->
                if (loadAndShowError != null) {
                    Log.w(TAG, "UMP: ${loadAndShowError.message}")
                }

                if (consentInformation.canRequestAds()) {
                    initializeMobileAdsAndLoad()
                } else {
                    navigateToMainApp()
                }
            }
        }, { requestConsentError ->
            Log.w(TAG, "UMP Error: ${requestConsentError.message}")
            navigateToMainApp()
        })
    }

    private fun initializeMobileAdsAndLoad() {
        Log.d(TAG, "Inicjalizacja SDK AdMob...")

        // Rejestracja Twojego urządzenia jako testowego (ID z Twoich logów)
        val testDeviceIds = listOf("B3EEABB8EE11C2BE770B684D95219ECB")
        val configuration = RequestConfiguration.Builder()
            .setTestDeviceIds(testDeviceIds)
            .build()
        MobileAds.setRequestConfiguration(configuration)

        MobileAds.initialize(this) { initializationStatus ->
            // Gwarantujemy wykonanie w wątku głównym po pełnej inicjalizacji
            runOnUiThread {
                Log.d(TAG, "SDK AdMob gotowe. Ładowanie reklamy...")
                loadAppOpenAd()
            }
        }
    }

    private fun loadAppOpenAd() {
        val adUnitId = BuildConfig.ADMOB_ADSTART_ID

        Log.d(TAG, "Ładowanie reklamy (HARDCODED ID): $adUnitId")

        val request = AdRequest.Builder().build()
        val timeoutHandler = android.os.Handler(mainLooper)

        val timeoutRunnable = Runnable {
            if (appOpenAd == null && !isDismissed) {
                Log.d(TAG, "Timeout - wchodzę do aplikacji")
                navigateToMainApp()
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable, AD_TIMEOUT_MS)

        AppOpenAd.load(
            this,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    Log.d(TAG, "Reklama App Open załadowana pomyślnie")
                    appOpenAd = ad
                    showAdIfAvailable()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    // Tu zobaczysz dokładny komunikat błędu
                    Log.e(TAG, "Błąd ładowania: ${error.message} (Kod: ${error.code})")
                    navigateToMainApp()
                }
            }
        )
    }

    private fun showAdIfAvailable() {
        val ad = appOpenAd
        if (ad == null || isShowingAd || isDismissed) {
            navigateToMainApp()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Reklama zamknięta")
                navigateToMainApp()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Błąd wyświetlania: ${error.message}")
                navigateToMainApp()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
            }
        }
        ad.show(this)
    }

    private fun navigateToMainApp() {
        if (isDismissed) return
        isDismissed = true

        if (!isFinishing) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}