package com.example.calkulatorcnc.ui.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.example.calkulatorcnc.billing.SubscriptionStatus
import com.example.calkulatorcnc.data.db.AppDatabase
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

@SuppressLint("CustomSplashScreen")
class ActivitySplash : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
        private const val AD_TIMEOUT_MS = 8000L
    }

    private var isDatabaseReady = false
    private var isAdLogicFinished = false
    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd = false
    private var isDismissed = false
    private var isConsentStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        initDatabaseAsync()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val subManager = SubscriptionManager.getInstance(applicationContext)
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
    private fun initDatabaseAsync() {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val result = withContext(Dispatchers.IO) {
                    // 1. Inicjalizacja bazy (Tu Room odpala Twoją migrację 7->8)
                    val db = AppDatabase.getDatabase(applicationContext)

                    var count = db.toolNewDao().getToolsCount()

                    // 2. REPARACJA: Jeśli migracja nie skopiowała danych (count == 0)
                    if (count == 0) {
                        //Log.w("DB_INIT", "Tabela pusta! Próba ręcznej naprawy...")

                        // Upewnij się, że nazwa pliku w assets to na pewno "data.db" (lub "database.db")
                        val assetName = "data.db"
                        AppDatabase.copyAssetToTempFile(applicationContext, assetName)

                        val tempDbFile = File(applicationContext.cacheDir, "temp_data.db")
                        if (tempDbFile.exists()) {
                            val tempDbPath = tempDbFile.absolutePath
                            val sdb = db.openHelper.writableDatabase

                            try {
                                // Ręczne kopiowanie poza transakcją Room
                                sdb.execSQL("ATTACH DATABASE '$tempDbPath' AS temp_db")
                                sdb.execSQL("INSERT OR REPLACE INTO cnc_tools SELECT * FROM temp_db.cnc_tools")
                                //Log.d("DB_INIT", "Ręczna reparacja zakończona sukcesem")
                            } catch (e: Exception) {
                                //Log.e("DB_INIT", "Błąd podczas ręcznego kopiowania: ${e.message}")
                                throw e
                            } finally {
                                // Zawsze odłączamy bazę, żeby zwolnić plik
                                try {
                                    sdb.execSQL("DETACH DATABASE temp_db")
                                } catch (e: Exception) {
                                    //Log.e("DB_INIT", "Nie można odłączyć temp_db: ${e.message}")
                                }
                            }
                            count = db.toolNewDao().getToolsCount()
                        }
                    }

                    // 3. Dopiero gdy wszystko gotowe, usuwamy plik tymczasowy
                    AppDatabase.cleanupTempDatabase(applicationContext)

                    count // Zwracamy liczbę narzędzi
                }

                if (result == 0) {
                    Toast.makeText(applicationContext, "Uwaga: Baza narzędzi jest pusta", Toast.LENGTH_LONG).show()
                } else {
                    //Log.d("DB_INIT", "Baza zainicjowana, liczba rekordów: $result")
                }

                isDatabaseReady = true
                tryNavigate()

            } catch (e: Exception) {
                val errorMsg = e.message ?: "Nieznany błąd bazy"
                //Log.e("DB_INIT", "KRYTYCZNY BŁĄD: $errorMsg")
                Toast.makeText(applicationContext, "BŁĄD BAZY: $errorMsg", Toast.LENGTH_LONG).show()

                // Pozwalamy wejść do aplikacji mimo błędu bazy, żeby nie blokować użytkownika
                isDatabaseReady = true
                tryNavigate()
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

    private fun actuallyNavigate() {
        if (isDismissed) return
        isDismissed = true

        if (!isFinishing) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun tryNavigate() {
        // Wchodzimy tylko gdy:
        // 1. Logika reklam się skończyła (reklama zamknięta lub pominięta)
        // 2. Baza danych jest w pełni gotowa
        if (isDatabaseReady && isAdLogicFinished) {
            actuallyNavigate()
        }
    }

    private fun navigateToMainApp() {
        isAdLogicFinished = true
        tryNavigate()
    }
}