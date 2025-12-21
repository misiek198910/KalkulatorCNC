package com.example.calkulatorcnc.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.calkulatorcnc.R

class ActivityAddTool : AppCompatActivity() {

    // Widoki - małe litery, lateinit eliminuje sprawdzanie nulli
    private lateinit var mainLayout: ConstraintLayout
    private lateinit var btnAdd: Button
    private lateinit var etWorkpiece: EditText
    private lateinit var etName: EditText
    private lateinit var etF: EditText
    private lateinit var etS: EditText
    private lateinit var etNotes: EditText
    private lateinit var cbWorkpiece: CheckBox
    private lateinit var cbNotes: CheckBox

    // Zmienne pomocnicze
    private var isEditMode: Boolean = false
    private var toolName: String? = null
    private var toolF: String? = null
    private var toolS: String? = null
    private var workpiece: String? = null
    private var notes: String? = null
    private var itemPos: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_tool)

        // Obsługa Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Obsługa przycisku powrotu systemowego
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }

        initViews()
        loadDataFromIntent()
        setupListeners()

        mainLayout.setOnClickListener { hideKeyboard() }
    }

    private fun initViews() {
        mainLayout = findViewById(R.id.main)
        etName = findViewById(R.id.editText1)
        etF = findViewById(R.id.editText2)
        etS = findViewById(R.id.editText3)
        etWorkpiece = findViewById(R.id.editText4)
        etNotes = findViewById(R.id.editText5)
        cbWorkpiece = findViewById(R.id.checkBox1)
        cbNotes = findViewById(R.id.checkBox2)
        btnAdd = findViewById(R.id.button1)
    }

    private fun loadDataFromIntent() {
        isEditMode = intent.getBooleanExtra("editDisabled", false)

        if (isEditMode) {
            toolName = intent.getStringExtra("toolName")
            toolF = intent.getStringExtra("toolF")
            toolS = intent.getStringExtra("toolS")
            workpiece = intent.getStringExtra("workpiece")
            notes = intent.getStringExtra("notes")
            itemPos = intent.getIntExtra("item", 0)

            etName.setText(toolName)
            etF.setText(toolF)
            etS.setText(toolS)
            etWorkpiece.setText(workpiece)
            etNotes.setText(notes)

            // Aktywacja wszystkich pól w trybie edycji
            listOf(etName, etF, etS, etWorkpiece, etNotes).forEach { it.isEnabled = true }
            cbNotes.isEnabled = true
            cbWorkpiece.isEnabled = true
        }

        // Ustawienie początkowego stanu pól na podstawie zawartości
        updateFieldState(etNotes, cbNotes)
        updateFieldState(etWorkpiece, cbWorkpiece)
    }

    private fun updateFieldState(editText: EditText, checkBox: CheckBox) {
        if (editText.text.toString().isEmpty()) {
            editText.alpha = 0.3f
            editText.isEnabled = false
            checkBox.isChecked = false
        } else {
            editText.alpha = 1f
            editText.isEnabled = true
            checkBox.isChecked = true
        }
    }

    private fun setupListeners() {
        cbWorkpiece.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                etWorkpiece.alpha = 1f
                etWorkpiece.isEnabled = true
            } else {
                etWorkpiece.setText("")
                etWorkpiece.alpha = 0.3f
                etWorkpiece.isEnabled = false
            }
        }

        cbNotes.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                etNotes.alpha = 1f
                etNotes.isEnabled = true
            } else {
                handleNotesUncheck()
            }
        }

        btnAdd.setOnClickListener {
            saveAndReturn()
        }
    }

    private fun handleNotesUncheck() {
        if (etNotes.text.toString().isNotEmpty()) {
            AlertDialog.Builder(this)
                .setMessage(getString(R.string.NotesInfo))
                .setCancelable(false)
                .setPositiveButton("OK") { _, _ ->
                    etNotes.setText("")
                    etNotes.alpha = 0.3f
                    etNotes.isEnabled = false
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                    cbNotes.isChecked = true
                }
                .show()
        } else {
            etNotes.alpha = 0.3f
            etNotes.isEnabled = false
        }
    }

    private fun saveAndReturn() {
        val i = Intent(this, ActivityTools::class.java).apply {
            putExtra("ToolName", etName.text.toString())
            putExtra("ToolF", etF.text.toString())
            putExtra("ToolS", etS.text.toString())
            putExtra("ToolWorkpiece", etWorkpiece.text.toString())
            putExtra("ToolNotes", etNotes.text.toString())
            putExtra("editDisabled", isEditMode)
            putExtra("item", itemPos)
        }
        startActivity(i)
        finish()
    }

    fun button_back_Clicked(view: View?) {
        startActivity(Intent(this, ActivityTools::class.java))
        finish()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }
}