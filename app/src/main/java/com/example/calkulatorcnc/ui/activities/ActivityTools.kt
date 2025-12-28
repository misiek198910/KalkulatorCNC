package com.example.calkulatorcnc.ui.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.billing.SubscriptionManager
import com.example.calkulatorcnc.data.db.AppDatabase
import com.example.calkulatorcnc.entity.Tool
import com.example.calkulatorcnc.ui.adapters.ToolAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ActivityTools : AppCompatActivity() {

    private lateinit var recyclerViewTools: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvToolCount: TextView
    private lateinit var searchView: SearchView

    private lateinit var db: AppDatabase
    private var adapter: ToolAdapter? = null
    private val toolsLimit = 3
    private var adView: AdView? = null
    private var currentToolsCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tools)
        db = AppDatabase.getDatabase(this)
        createViewAEdgetoEdgeForAds()
        initViews()
        setupDataObservation()
        setupSearchLogic()
    }

    private fun createViewAEdgetoEdgeForAds() {
        val mainRoot = findViewById<View>(R.id.main)
        val customHeader = findViewById<LinearLayout>(R.id.customHeader)
        val adContainerLayout = findViewById<FrameLayout>(R.id.adContainerLayout)
        val adContainer = findViewById<FrameLayout>(R.id.adContainer)

        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            mainRoot.setPadding(0, 0, 0, 0)
            customHeader?.updatePadding(top = systemBars.top)

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

    private fun initViews() {
        recyclerViewTools = findViewById(R.id.recyclerViewTools)
        val spanCount = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 2 else 1
        recyclerViewTools.layoutManager = GridLayoutManager(this, spanCount)

        fabAdd = findViewById(R.id.fab_add_tool)
        btnBack = findViewById(R.id.button_back)
        tvToolCount = findViewById(R.id.tvToolCount)
        searchView = findViewById(R.id.searchView)

        // Stylizacja SearchView
        styleSearchView()

        adapter = ToolAdapter(
            mutableListOf(),
            onEdit = { tool -> openEditTool(tool) },
            onDelete = { tool -> confirmDeletion(tool) }
        )
        recyclerViewTools.adapter = adapter

        btnBack.setOnClickListener { finish() }

        fabAdd.setOnClickListener {
            val isPremium = SubscriptionManager.getInstance(this).isPremium.value ?: false
            if (!isPremium && currentToolsCount >= toolsLimit) {
                showPremiumRequiredDialog()
            } else {
                startActivity(Intent(this, ActivityAddTool::class.java).apply {
                    putExtra("editDisabled", false)
                })
            }
        }
    }

    private fun styleSearchView() {
        val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText.setTextColor(Color.WHITE)
        searchEditText.setHintTextColor("#BDBDBD".toColorInt())

        val searchIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchIcon.setColorFilter(Color.WHITE)

        val closeIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeIcon.setColorFilter(Color.WHITE)
    }

    private fun setupDataObservation() {
        // Ważne: Sprawdź czy import to: androidx.lifecycle.Observer
        db.toolDao().getAllTools().observe(this) { list ->
            list?.let {
                currentToolsCount = it.size
                adapter?.updateData(it)
                updateCountDisplay(it.size) // Przekazujemy samą liczbę
            }
        }
    }

    private fun setupSearchLogic() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter?.filter(newText ?: "")
                updateCountDisplay(adapter?.getFilteredCount() ?: 0)
                return true
            }
        })
    }

    private fun updateCountDisplay(count: Int) {
        tvToolCount.text = count.toString()
    }

    private fun openEditTool(tool: Tool) {
        startActivity(Intent(this, ActivityAddTool::class.java).apply {
            putExtra("toolId", tool.id)
            putExtra("editDisabled", true)
            putExtra("toolName", tool.name)
            putExtra("toolF", tool.f)
            putExtra("toolS", tool.s)
            putExtra("workpiece", tool.workpiece)
            putExtra("notes", tool.notes)
        })
    }

    private fun confirmDeletion(tool: Tool) {
        // Room LiveData automatycznie odświeży widok po usunięciu
        lifecycleScope.launch(Dispatchers.IO) {
            db.toolDao().deleteTool(tool)
        }
    }

    private fun setupAds() {
        val adContainer = findViewById<FrameLayout>(R.id.adContainer) ?: return

        SubscriptionManager.getInstance(this).isPremium.observe(this) { isPremium ->
            if (isPremium) {
                // 1. Logika dla użytkownika PREMIUM
                adContainer.visibility = View.GONE
                adContainer.removeAllViews() // Fizycznie usuwamy widoki
                adView?.destroy()
                adView = null
            } else {
                // 2. Logika dla darmowej wersji
                val screenHeightDp = resources.configuration.screenHeightDp
                if (screenHeightDp < 400) {
                    adContainer.visibility = View.GONE
                } else {
                    adContainer.visibility = View.VISIBLE
                    if (adView == null) {
                        // Tworzymy nową reklamę adaptacyjną
                        val newAdView = AdView(this)
                        adContainer.addView(newAdView)

                        val adSize = getAdSize(adContainer)
                        newAdView.setAdSize(adSize)
                        newAdView.adUnitId = BuildConfig.ADMOB_BANNER_ID

                        adView = newAdView
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

    private fun showPremiumRequiredDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.window_premium_upgrade, null)
        val btnGo = dialogView.findViewById<Button>(R.id.btnGoToPremium)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelPremium)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        btnGo.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, ActivitySubscription::class.java))
        }
        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onDestroy() {
        adView?.destroy()
        super.onDestroy()
    }
}