package com.example.calkulatorcnc.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ListView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.data.preferences.ClassPrefs
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.entity.Tool
import com.example.calkulatorcnc.ui.adapters.ToolAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ActivityTools : AppCompatActivity() {

    // Widoki - lateinit dla bezpieczeństwa
    private lateinit var btnDelete: AppCompatButton
    private lateinit var btnEdit: AppCompatButton
    private lateinit var btnAdd: AppCompatButton
    private lateinit var btnBack: ImageButton
    private lateinit var listView: ListView

    // Dane i stan
    private var toolList: MutableList<Tool> = mutableListOf()
    private var adapter: ToolAdapter? = null
    private var selectedPosition: Int = -1
    private val toolsLimit = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tools)

        setupInsets()
        initViews()
        loadTools()
        setupAds()

        onBackPressedDispatcher.addCallback(this) {
            saveAndExit()
        }
    }

    private fun initViews() {
        btnDelete = findViewById(R.id.ic_delete)
        btnEdit = findViewById(R.id.ic_edit)
        btnAdd = findViewById(R.id.ic_add)
        btnBack = findViewById(R.id.button_back)
        listView = findViewById(R.id.listView1)

        // Inicjalizacja adaptera raz
        adapter = ToolAdapter(this, toolList)
        listView.adapter = adapter

        btnBack.setOnClickListener { saveAndExit() }

        listView.setOnItemClickListener { _, _, position, _ ->
            selectedPosition = position
            updateButtonStates(true)
        }

        // Długie kliknięcie od razu przechodzi do edycji
        listView.setOnItemLongClickListener { _, _, position, _ ->
            openEditTool(position)
            true
        }

        btnAdd.setOnClickListener {
            if (toolList.size >= toolsLimit) {
                showMessage(getString(R.string.toolsLimit))
            } else {
                val intent = Intent(this, ActivityAddTool::class.java)
                intent.putExtra("editDisabled", false)
                startActivity(intent)
                finish() // Kończymy, bo dane wrócą przez Intent w onCreate/onResume
            }
        }

        btnEdit.setOnClickListener {
            if (selectedPosition != -1) openEditTool(selectedPosition)
        }

        btnDelete.setOnClickListener {
            if (selectedPosition != -1) {
                toolList.removeAt(selectedPosition)
                selectedPosition = -1
                adapter?.notifyDataSetChanged()
                updateButtonStates(false)
                saveJson()
            }
        }

        updateButtonStates(false)
    }

    private fun openEditTool(position: Int) {
        val tool = toolList[position]
        val intent = Intent(this, ActivityAddTool::class.java).apply {
            putExtra("editDisabled", true)
            putExtra("toolName", tool.name)
            putExtra("toolF", tool.f)
            putExtra("toolS", tool.s)
            putExtra("workpiece", tool.workpiece)
            putExtra("notes", tool.notes)
            putExtra("item", position)
        }
        startActivity(intent)
        finish()
    }

    private fun updateButtonStates(isItemSelected: Boolean) {
        val alpha = if (isItemSelected) 1.0f else 0.2f
        btnDelete.isEnabled = isItemSelected
        btnEdit.isEnabled = isItemSelected
        btnDelete.alpha = alpha
        btnEdit.alpha = alpha
    }

    override fun onResume() {
        super.onResume()
        handleIncomingData()
    }

    private fun handleIncomingData() {
        val toolName = intent.getStringExtra("ToolName") ?: return
        if (toolName.isEmpty()) return

        val toolWp = intent.getStringExtra("ToolWorkpiece")
        val toolF = intent.getStringExtra("ToolF")
        val toolS = intent.getStringExtra("ToolS")
        val notes = intent.getStringExtra("ToolNotes")
        val isEdit = intent.getBooleanExtra("editDisabled", false)
        val pos = intent.getIntExtra("item", -1)

        val newTool = Tool(toolWp, toolName, toolF, toolS, notes)

        if (isEdit && pos != -1) {
            toolList[pos] = newTool
        } else {
            toolList.add(newTool)
        }

        // Czyścimy intent, żeby nie dodawać tego samego przy obrocie ekranu
        intent.removeExtra("ToolName")

        adapter?.notifyDataSetChanged()
        saveJson()
    }

    private fun loadTools() {
        val pref = ClassPrefs()
        val data = pref.loadPrefString(this, "toolList")
        if (data.isNotEmpty()) {
            val type = object : TypeToken<MutableList<Tool>>() {}.type
            val loadedList: MutableList<Tool>? = Gson().fromJson(data, type)
            if (loadedList != null) {
                toolList.clear()
                toolList.addAll(loadedList)
                adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun saveJson() {
        val json = Gson().toJson(toolList)
        ClassPrefs().savePrefString(this, "toolList", json)
    }

    private fun saveAndExit() {
        saveJson()
        finish()
    }

    private fun showMessage(str: String) {
        AlertDialog.Builder(this)
            .setMessage(str)
            .setCancelable(false)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
    }

    private fun setupAds() {
        MobileAds.initialize(this)
        val adContainer = findViewById<FrameLayout>(R.id.adContainer)
        val adView = AdView(this).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BuildConfig.ADMOB_BANNER_ID
        }
        adContainer.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}