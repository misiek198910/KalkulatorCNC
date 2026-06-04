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
import com.example.calkulatorcnc.remote.NewsResponse // Używamy Twojego nowego modelu
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.net.toUri

class NewsAdapter(private val newsList: List<NewsResponse>) :
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

        // 1. Mapowanie podstawowych danych
        holder.tvTitle.text = news.title
        holder.tvBody.text = news.content
        holder.tvBody.maxLines = Int.MAX_VALUE

        // 2. Obsługa daty (String z API -> Format lokalny)
        if (!news.publish_date.isNullOrEmpty()) {
            try {
                // Zakładamy, że serwer zwraca format "yyyy-MM-dd HH:mm:ss"
                val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val formatter = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                val date = parser.parse(news.publish_date)
                if (date != null) {
                    holder.tvDate.text = formatter.format(date)
                    holder.tvDate.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                // Jeśli parsowanie zawiedzie, wyświetlamy surowy tekst lub ukrywamy
                holder.tvDate.text = news.publish_date
                holder.tvDate.visibility = View.VISIBLE
            }
        } else {
            holder.tvDate.visibility = View.GONE
        }

        // 3. Przycisk akcji (Action Link)
        if (!news.action_link.isNullOrEmpty()) {
            holder.btnAction.visibility = View.VISIBLE
            holder.btnAction.text = context.getString(R.string.show_shop)

            holder.btnAction.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, news.action_link.toUri())
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            holder.btnAction.visibility = View.GONE
        }

        // 4. Ładowanie zdjęcia przez Glide (image_url)
        if (!news.image_url.isNullOrEmpty()) {
            holder.imgNews.visibility = View.VISIBLE
            Glide.with(context)
                .load(news.image_url)
                .centerCrop()
                .placeholder(R.drawable.spindle) // Twój placeholder
                .error(android.R.drawable.stat_notify_error)
                .into(holder.imgNews)
        } else {
            holder.imgNews.visibility = View.GONE
        }
    }

    override fun getItemCount() = newsList.size
}