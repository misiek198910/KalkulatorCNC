package com.example.calkulatorcnc.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.calkulatorcnc.BuildConfig
import com.example.calkulatorcnc.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

class ActivityInformation : AppCompatActivity() {
    var mAdView: AdView? = null
    var toolbar_title: TextView? = null
    var button_back: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_information)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        MobileAds.initialize(this)

        val adContainer = findViewById<FrameLayout>(R.id.adContainer)
        val mAdView = AdView(this)
        mAdView.setAdSize(AdSize.BANNER)
        mAdView.setAdUnitId(BuildConfig.ADMOB_BANNER_ID)
        adContainer.addView(mAdView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        button_back = findViewById<ImageButton>(R.id.button_back)
        toolbar_title = findViewById<TextView>(R.id.toolbar_title)
        toolbar_title!!.setText(R.string.settings_button1)
        button_back!!.setOnClickListener(View.OnClickListener { v: View? -> finish() })
    }

    fun button_blog_Clicked(view: View?) {
        val link = "https://ubzj0c.webwave.dev/blog"

        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        startActivity(browserIntent)
    }
}