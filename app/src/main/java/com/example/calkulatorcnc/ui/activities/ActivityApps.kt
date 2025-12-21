package com.example.calkulatorcnc.ui.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
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

class ActivityApps : AppCompatActivity() {
    var toolbar_title: TextView? = null
    var button_back: ImageButton? = null
    var mAdView: AdView? = null

    private class AppItem(val iconResId: Int, val titleResId: Int, val appLink: String?)

    private val appItems: Array<AppItem> = arrayOf<AppItem>(
        AppItem(
            R.drawable.droga_krzyzowa_logo,
            R.string.app_droga_krzyzowa,
            "droga_krzyzowa.droga_krzyzowa"
        ),
        AppItem(
            R.drawable.droga_krzyzowa_plus_logo,
            R.string.app_droga_krzyzowa_plus,
            "mivs.droga_krzyzowa_plus"
        ),
        AppItem(
            R.drawable.kalendarz_liturgiczny_logo,
            R.string.app_kalendarz_liturgiczny,
            "mivs.kalendarz_liturgiczny"
        ),
        AppItem(
            R.drawable.kalendarz_liturgiczny_plus_logo,
            R.string.app_kalendarz_liturgiczny_plus,
            "mivs.kalendarz_liturgiczny_plus"
        ),
        AppItem(R.drawable.kalkulator_cnc_logo, R.string.app_kalkulator_cnc, "kalkulator.cnc"),
        AppItem(
            R.drawable.kalkulator_cnc_plus_logo,
            R.string.app_kalkulator_cnc_plus,
            "kalkulator.cnc.plus"
        ),
        AppItem(R.drawable.ktoz_jak_bog_logo, R.string.app_ktoz_jak_bog, "mivs.ktozjakbog"),
        AppItem(
            R.drawable.ktoz_jak_bog_plus_logo,
            R.string.app_ktoz_jak_bog_plus,
            "mivs.ktozjakbog_plus"
        ),
        AppItem(R.drawable.moj_rozaniec_logo, R.string.app_moj_rozaniec, "mivs.m_j_r_aniec"),
        AppItem(
            R.drawable.moj_rozaniec_plus_logo,
            R.string.app_moj_rozaniec_plus,
            "mivs.m_j_r_aniec_plus"
        ),
        AppItem(
            R.drawable.niewolnik_maryi_logo,
            R.string.app_niewolnik_maryi,
            "mivs.niewolnik_maryi"
        ),
        AppItem(
            R.drawable.niewolnik_maryi_plus_logo,
            R.string.app_niewolnik_maryi_plus,
            "mivs.niewolnik_maryi_plus"
        ),
        AppItem(R.drawable.objawienia_logo, R.string.app_objawienia, "mivs.objawienia"),
        AppItem(
            R.drawable.objawienia_plus_logo,
            R.string.app_objawienia_plus,
            "mivs.objawienia_plus"
        ),
        AppItem(
            R.drawable.rachunek_sumienia_logo,
            R.string.app_rachunek_sumienia,
            "pakiet.rachuneksumienia"
        ),
        AppItem(
            R.drawable.rachunek_sumienia_plus_logo,
            R.string.app_rachunek_sumienia_plus,
            "pakiet.rachuneksumienia_plus"
        )

    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_apps)
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
        toolbar_title!!.setText(R.string.settings_button2)
        button_back!!.setOnClickListener(View.OnClickListener { v: View? -> finish() })

        val container = findViewById<LinearLayout>(R.id.appListContainer)
        container.setGravity(Gravity.CENTER)

        for (item in appItems) {
            val panel = LinearLayout(this)
            panel.setOrientation(LinearLayout.HORIZONTAL)
            panel.setPadding(10, 10, 10, 10)
            panel.setClickable(true)
            val outValue = TypedValue()
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            panel.setBackgroundResource(outValue.resourceId)

            val panelParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            panelParams.setMargins(5, 5, 5, 5)
            panel.setLayoutParams(panelParams)

            val icon = ImageView(this)
            val iconParams = LinearLayout.LayoutParams(100, 100)
            icon.setLayoutParams(iconParams)
            icon.setImageResource(item.iconResId)

            val label = TextView(this)
            val textParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            label.setLayoutParams(textParams)
            label.setText(item.titleResId)
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            label.setGravity(Gravity.CENTER_VERTICAL)
            label.setPadding(16, 0, 0, 0)
            label.setTypeface(null, Typeface.BOLD) // pogrubienie
            label.setTextColor(Color.WHITE)
            label.setBackgroundColor(Color.parseColor("#4A000000"))

            panel.addView(icon)
            panel.addView(label)

            panel.setOnClickListener(View.OnClickListener { v: View? ->
                if (item.appLink != null) {
                    OpenStore(item.appLink)
                }
            })

            container.addView(panel)
        }
    }


    fun OpenStore(packageName: String?) {
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName)
                )
            )
        } catch (e: Exception) //Jeżeli sklep jest niedostepny
        {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)
                )
            )
        }
    }
}