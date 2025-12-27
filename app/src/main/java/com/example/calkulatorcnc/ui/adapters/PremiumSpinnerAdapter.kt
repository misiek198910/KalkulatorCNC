package com.example.calkulatorcnc.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.calkulatorcnc.R

class PremiumSpinnerAdapter(
    context: Context,
    private val mainResource: Int, // Layout dla zamkniętego spinnera
    private val items: Array<String>,
    private val isPremium: Boolean,
    private val premiumPositions: List<Int>
) : ArrayAdapter<String>(context, mainResource, items) {

    private var dropDownResource: Int = mainResource

    // Ta metoda nadpisuje standardowe ustawianie layoutu listy rozwijanej
    override fun setDropDownViewResource(resource: Int) {
        super.setDropDownViewResource(resource)
        this.dropDownResource = resource
    }

    // Widok ZAMKNIĘTEGO spinnera (tutaj zazwyczaj NIE chcemy kłódki)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(mainResource, parent, false)
        val textView = view.findViewById<TextView>(R.id.spinner_text)

        textView.text = items[position]
        textView.alpha = 1.0f // Zawsze pełna widoczność dla wybranego elementu

        // Ukrywamy kłódkę w widoku zamkniętym, nawet jeśli to opcja premium
        view.findViewById<ImageView>(R.id.lock_icon)?.visibility = View.GONE

        return view
    }

    // Widok LISTY ROZWIJANEJ (tutaj kłódki są kluczowe)
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = LayoutInflater.from(context).inflate(dropDownResource, parent, false)

        val textView = view.findViewById<TextView>(R.id.spinner_text)
        val lockIcon = view.findViewById<ImageView>(R.id.lock_icon)

        textView.text = items[position]

        val isPosPremium = premiumPositions.contains(position)

        if (!isPremium && isPosPremium) {
            lockIcon?.visibility = View.VISIBLE
            lockIcon?.setColorFilter(ContextCompat.getColor(context, R.color.gold))
            textView.alpha = 0.5f // Poszarzenie tekstu
            textView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        } else {
            lockIcon?.visibility = View.GONE
            textView.alpha = 1.0f
            textView.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            lockIcon?.clearColorFilter()
        }

        return view
    }
}