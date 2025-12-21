package com.example.calkulatorcnc.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.entity.Tool

class ToolAdapter(
    private val context: Context,
    private var toolList: List<Tool>
) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int = toolList.size

    override fun getItem(position: Int): Tool = toolList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    // ViewHolder przechowuje referencje do widoków, by nie szukać ich za każdym razem
    private class ViewHolder(view: View) {
        val tvName: TextView = view.findViewById(R.id.listview_item_textview)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            // Influjemy nowy widok wiersza
            view = inflater.inflate(R.layout.listview_item, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            // Recykling widoku
            view = convertView
            holder = view.tag as ViewHolder
        }

        // Pobieramy dane i przypisujemy do widoku
        val currentTool = getItem(position)
        holder.tvName.text = currentTool.name

        return view
    }

    // Opcjonalna funkcja do aktualizacji danych w liście
    fun updateData(newList: List<Tool>) {
        this.toolList = newList
        notifyDataSetChanged()
    }
}