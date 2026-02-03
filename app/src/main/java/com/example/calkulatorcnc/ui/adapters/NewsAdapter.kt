package com.example.calkulatorcnc.ui.adapters

import android.content.Intent
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
import androidx.core.net.toUri

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
        val context = holder.itemView.context

        holder.tvTitle.text = news.title
        holder.tvBody.text = news.content

        holder.tvBody.maxLines = Int.MAX_VALUE

        if (news.date != null) {
            try {
                // Locale.getDefault() automatycznie wybierze format pod niemiecki/polski
                val sdf = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                holder.tvDate.text = sdf.format(news.date)
                holder.tvDate.visibility = View.VISIBLE
            } catch (e: Exception) {
                holder.tvDate.visibility = View.GONE
            }
        } else {
            holder.tvDate.visibility = View.GONE
        }

        // 3. Przycisk "Zobacz w sklepie" (Action Link)
        if (!news.actionLink.isNullOrEmpty()) {
            holder.btnAction.visibility = View.VISIBLE
            // Pobieranie tekstu ze strings.xml (obsłuży "Zobacz w sklepie" i "Im Store ansehen")
            holder.btnAction.text = context.getString(R.string.show_shop)

            holder.btnAction.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, news.actionLink.toUri())
                    context.startActivity(intent)
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
            Glide.with(context)
                .load(news.imageUrl)
                .centerCrop()
                .placeholder(R.drawable.spindle)
                .error(android.R.drawable.stat_notify_error)
                .into(holder.imgNews)
        } else {
            holder.imgNews.visibility = View.GONE
        }
    }

    override fun getItemCount() = newsList.size
}