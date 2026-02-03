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

class MainDropdownAdapter(
    context: Context,
    private val layoutResource: Int,
    private val items: Array<String>,
    private val isPremium: Boolean,
    val premiumPositions: List<Int>
) : ArrayAdapter<String>(context, layoutResource, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(layoutResource, parent, false)

        // Próba znalezienia TextView (sprawdza oba możliwe ID)
        val textView = view.findViewById<TextView>(R.id.materialName)
            ?: view.findViewById<TextView>(R.id.spinner_text)

        // Próba znalezienia ikony kłódki (sprawdza oba możliwe ID)
        val lockIcon = view.findViewById<ImageView>(R.id.lockIcon)
            ?: view.findViewById<ImageView>(R.id.lock_icon)

        textView?.text = items[position]

        val isPosPremium = premiumPositions.contains(position)

        if (!isPremium && isPosPremium) {
            // Styl dla opcji zablokowanych
            lockIcon?.visibility = View.VISIBLE
            lockIcon?.setColorFilter(ContextCompat.getColor(context, R.color.gold))
            textView?.alpha = 0.5f
            textView?.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        } else {
            // Styl dla opcji dostępnych
            lockIcon?.visibility = View.GONE
            textView?.alpha = 1.0f
            textView?.setTextColor(ContextCompat.getColor(context, android.R.color.white))
        }

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }
}