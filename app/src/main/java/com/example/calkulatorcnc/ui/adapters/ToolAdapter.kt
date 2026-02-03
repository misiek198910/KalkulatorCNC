package com.example.calkulatorcnc.ui.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.entity.Tool
import getIsoGroupForMaterial // Import funkcji zabezpieczającej z MaterialParam.kt

class ToolAdapter(
    private var tools: List<Tool>, // Lista obecnie wyświetlana
    private val onEdit: (Tool) -> Unit,
    private val onDelete: (Tool) -> Unit
) : RecyclerView.Adapter<ToolAdapter.ToolViewHolder>() {

    // Kopia zapasowa wszystkich narzędzi do filtrowania
    private var allTools: List<Tool> = tools

    class ToolViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvToolName)
        val tvWorkpiece: TextView = view.findViewById(R.id.tvWorkpiece)
        val tvS: TextView = view.findViewById(R.id.tvS)
        val tvF: TextView = view.findViewById(R.id.tvF)
        val tvNotes: TextView = view.findViewById(R.id.tvNotes)
        val expandableLayout: LinearLayout = view.findViewById(R.id.expandableLayout)
        val btnCopy: ImageButton = view.findViewById(R.id.btnCopy)
        val btnExpand: ImageButton = view.findViewById(R.id.btnExpand)
        val btnMenu: ImageButton = view.findViewById(R.id.btnMenu)
    }

    // --- FUNKCJE OBSŁUGUJĄCE ACTIVITY ---

    fun updateData(newTools: List<Tool>) {
        this.allTools = newTools
        this.tools = newTools
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        tools = if (query.isEmpty()) {
            allTools
        } else {
            allTools.filter {
                it.name.contains(query, ignoreCase = true) ||
                        (it.workpiece?.contains(query, ignoreCase = true) ?: false)
            }
        }
        notifyDataSetChanged()
    }

    fun getFilteredCount(): Int = tools.size

    // --- STANDARDOWE METODY ADAPTERA ---

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tool_card, parent, false)
        return ToolViewHolder(view)
    }

    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        val tool = tools[position]
        val context = holder.itemView.context

        holder.tvName.text = tool.name
        holder.tvWorkpiece.text = tool.workpiece ?: ""
        holder.tvS.text = "S: ${tool.s ?: "---"}"
        holder.tvF.text = "F: ${tool.f ?: "---"}"

        // --- NOWA LOGIKA ISO (ZABEZPIECZENIE DLA STARYCH DANYCH) ---
        // Funkcja sprawdza grupę ISO na podstawie tekstowej nazwy materiału
        val isoGroup = getIsoGroupForMaterial(context, tool.workpiece)
        val isoColor = when (isoGroup) {
            "P" -> context.getColor(R.color.iso_p_blue)
            "M" -> context.getColor(R.color.iso_m_yellow)
            "K" -> context.getColor(R.color.iso_k_red)
            "N" -> context.getColor(R.color.iso_n_green)
            "S" -> context.getColor(R.color.iso_s_orange)
            "H" -> context.getColor(R.color.iso_h_grey)
            else -> Color.TRANSPARENT // Dla starych lub nieznanych materiałów
        }

        if (isoColor != Color.TRANSPARENT) {
            // Stylizacja etykiety materiału jako kolorowy "badge"
            holder.tvWorkpiece.setBackgroundColor(isoColor)
            // Czarny tekst dla żółtej grupy M, biały dla reszty
            holder.tvWorkpiece.setTextColor(if (isoGroup == "M") Color.BLACK else Color.WHITE)
            holder.tvWorkpiece.setPadding(16, 4, 16, 4)
        } else {
            // Reset stylu dla materiałów "Inne" lub starych wpisów bez dopasowania
            holder.tvWorkpiece.setBackgroundResource(0)
            holder.tvWorkpiece.setTextColor(Color.parseColor("#BDBDBD"))
            holder.tvWorkpiece.setPadding(0, 0, 0, 0)
        }

        // Logika tekstu notatek
        if (tool.notes.isNullOrBlank()) {
            holder.tvNotes.text = context.getString(R.string.no_notes_available)
            holder.tvNotes.alpha = 0.5f
        } else {
            holder.tvNotes.text = tool.notes
            holder.tvNotes.alpha = 1.0f
        }

        // Logika rozwijania
        holder.btnExpand.visibility = View.VISIBLE
        holder.btnExpand.setOnClickListener {
            val isExpanded = holder.expandableLayout.visibility == View.VISIBLE
            TransitionManager.beginDelayedTransition(holder.itemView as ViewGroup, AutoTransition())

            if (isExpanded) {
                holder.expandableLayout.visibility = View.GONE
                holder.btnExpand.setImageResource(R.drawable.ic_expand_more)
            } else {
                holder.expandableLayout.visibility = View.VISIBLE
                holder.btnExpand.setImageResource(R.drawable.ic_expand_less)
            }
        }

        // Logika kopiowania
        holder.btnCopy.setOnClickListener {
            val textToCopy = context.getString(
                R.string.copy_tool_template,
                tool.name,
                tool.workpiece ?: "---",
                tool.s ?: "---",
                tool.f ?: "---"
            )
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(context.getString(R.string.clipboard_label), textToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, context.getString(R.string.params_copied), Toast.LENGTH_SHORT).show()
        }

        // Menu kontekstowe
        holder.btnMenu.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_tool_item, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit -> { onEdit(tool); true }
                    R.id.action_delete -> { onDelete(tool); true }
                    else -> false
                }
            }
            popup.show()
        }
    }

    override fun getItemCount(): Int = tools.size
}