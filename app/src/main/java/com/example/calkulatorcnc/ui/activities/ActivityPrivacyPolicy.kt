package com.example.calkulatorcnc.ui.activities

import android.os.Bundle
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.data.preferences.ClassPrefs

class ActivityPrivacyPolicy : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_privacy_policy)

        // 1. Obsługa Insetów i Nagłówka
        val header = findViewById<android.view.View>(R.id.customHeader)
        val tvTitle = findViewById<TextView>(R.id.header) // Dodaj ID do TextView w XML

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            header.updatePadding(top = systemBars.top)
            insets
        }

        findViewById<ImageButton>(R.id.button_back).setOnClickListener { finish() }

        // Ustawienie tytułu z zasobów (automatycznie wybierze PL lub EN)
        tvTitle.text = getString(R.string.privacy)

        // 2. Logika wyboru pliku HTML
        val webView = findViewById<WebView>(R.id.webViewPrivacy)
        val pref = ClassPrefs()

        // Pobieramy język z Twoich ustawień (0 dla PL, 1 dla EN - zgodnie z Twoim MainActivity)
        val languageData = pref.loadPrefInt(this, "language_data")

        val fileName = if (languageData == 0) {
            "privacy_pl.html"
        } else {
            "privacy_en.html"
        }

        webView.loadUrl("file:///android_asset/$fileName")
    }
}