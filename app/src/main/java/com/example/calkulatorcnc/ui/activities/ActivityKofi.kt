package com.example.calkulatorcnc.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.calkulatorcnc.R

class ActivityKofi : AppCompatActivity() {
    var textViewKofi: TextView? = null
    var main: ConstraintLayout? = null
    var button_back: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_kofi)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        button_back = findViewById<ImageButton>(R.id.button_back)
        button_back!!.setOnClickListener(View.OnClickListener { v: View? ->
            finish()
        })

        val btn = findViewById<ImageView>(R.id.kofi_button)
        btn.setOnClickListener(View.OnClickListener { v: View? ->
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/michals"))
            startActivity(browserIntent)
        })

        textViewKofi = findViewById<TextView>(R.id.textView16)
        textViewKofi!!.setOnClickListener(View.OnClickListener { v: View? ->
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/michals"))
            startActivity(browserIntent)
        })
    }
}