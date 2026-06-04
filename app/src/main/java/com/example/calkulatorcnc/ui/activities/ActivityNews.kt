package com.example.calkulatorcnc.ui.activities

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.example.calkulatorcnc.entity.NewsItem
import com.example.calkulatorcnc.remote.RetrofitClient
import com.example.calkulatorcnc.ui.adapters.NewsAdapter
import com.google.android.gms.ads.*
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class ActivityNews : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private var adView: AdView? = null
    private lateinit var analytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_news)

        setupEdgeToEdge()
        initUI()
        fetchNewsFromApi()
        analytics = Firebase.analytics
    }

    private fun setupEdgeToEdge() {
        val mainRoot = findViewById<View>(R.id.main)
        val customHeader = findViewById<View>(R.id.customHeader)
        val adContainerLayout = findViewById<FrameLayout>(R.id.adContainerLayout)

        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            mainRoot.setPadding(0, 0, 0, 0)
            customHeader.updatePadding(top = systemBars.top)
            adContainerLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom
                leftMargin = systemBars.left
                rightMargin = systemBars.right
            }
            insets
        }
    }

    private fun initUI() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        recyclerView = findViewById(R.id.recyclerViewNews)
        progressBar = findViewById(R.id.progressBarNews)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Inicjalizacja reklam po ułożeniu widoku
        findViewById<FrameLayout>(R.id.adContainer).post { setupAds() }
    }

    private fun fetchNewsFromApi() {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Wywołujemy Twój ParishApiService
                val response = RetrofitClient.instance.getNewsFeed()

                progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    val newsList = response.body() ?: emptyList()

                    if (newsList.isEmpty()) {
                        Toast.makeText(this@ActivityNews, "Brak nowych wiadomości", Toast.LENGTH_SHORT).show()
                    } else {
                        // Pamiętaj o aktualizacji NewsAdapter, aby przyjmował List<NewsResponse>
                        recyclerView.adapter = NewsAdapter(newsList)
                    }
                } else {
                    Toast.makeText(this@ActivityNews, "Błąd serwera: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@ActivityNews, "Błąd połączenia: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupAds() {
        val adContainer = findViewById<FrameLayout>(R.id.adContainer) ?: return
        SubscriptionManager.getInstance(this).isPremium.observe(this) { isPremium ->
            if (isPremium) {
                findViewById<View>(R.id.adContainerLayout).visibility = View.GONE
                adView?.destroy()
                adView = null
            } else {
                if (adView == null) {
                    val newAdView = AdView(this)
                    newAdView.adUnitId = BuildConfig.ADMOB_BANNER_ID
                    newAdView.setAdSize(getAdSize(adContainer))
                    adView = newAdView
                    adContainer.addView(newAdView)
                    newAdView.loadAd(AdRequest.Builder().build())
                }
            }
        }
    }

    private fun getAdSize(container: FrameLayout): AdSize {
        val density = resources.displayMetrics.density
        var width = container.width.toFloat()
        if (width == 0f) width = resources.displayMetrics.widthPixels.toFloat()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, (width / density).toInt())
    }

    override fun onResume() {
        super.onResume()
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, "MainActivity")
            param(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
        }
    }
}