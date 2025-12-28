package com.example.calkulatorcnc.ui.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.calkulatorcnc.R
import com.example.calkulatorcnc.entity.NewsItem
import java.text.SimpleDateFormat
import java.util.Locale

class NewsAdapter(private val newsList: List<NewsItem>) :
    RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvNewsDate)
        val tvTitle: TextView = view.findViewById(R.id.tvNewsTitle)
        val tvBody: TextView = view.findViewById(R.id.tvNewsBody)
        val btnAction: Button = view.findViewById(R.id.btnNewsAction)
        val imgNews: ImageView = view.findViewById(R.id.imgNews)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val news = newsList[position]

        // 1. Tytuł i treść
        holder.tvTitle.text = news.title
        holder.tvBody.text = news.content

        // 2. Formatowanie daty (Polski format: np. 28 gru 2025)
        if (news.date != null) {
            try {
                val sdf = SimpleDateFormat("d MMM yyyy", Locale("pl", "PL"))
                holder.tvDate.text = sdf.format(news.date)
                holder.tvDate.visibility = View.VISIBLE
            } catch (e: Exception) {
                holder.tvDate.visibility = View.GONE
            }
        } else {
            holder.tvDate.visibility = View.GONE
        }

        // 3. Obsługa przycisku akcji (link zewnętrzny)
        if (!news.actionLink.isNullOrEmpty()) {
            holder.btnAction.visibility = View.VISIBLE
            holder.btnAction.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(news.actionLink))
                    holder.itemView.context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            holder.btnAction.visibility = View.GONE
        }

        // 4. Ładowanie zdjęcia przez Glide
        if (!news.imageUrl.isNullOrEmpty()) {
            holder.imgNews.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(news.imageUrl)
                .centerCrop() // Dopasowanie zdjęcia do ramki
                .placeholder(R.drawable.spindle) // Twoja ikona jako placeholder
                .error(android.R.drawable.stat_notify_error)
                .into(holder.imgNews)
        } else {
            holder.imgNews.visibility = View.GONE
        }
    }

    override fun getItemCount() = newsList.size
}