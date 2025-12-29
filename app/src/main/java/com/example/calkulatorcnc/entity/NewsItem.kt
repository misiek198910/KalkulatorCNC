package com.example.calkulatorcnc.entity

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.Date

/**
 * Model danych dla wiadomości pobieranych z Firebase Firestore.
 * @IgnoreExtraProperties pozwala uniknąć błędów, jeśli w bazie pojawią się pola
 * nieujęte w tej klasie.
 */
@IgnoreExtraProperties
@Keep
data class NewsItem(
    val title: String = "",
    val content: String = "",
    val date: Date? = null,
    val isVisible: Boolean = true,
    val actionLink: String? = null,
    val imageUrl: String? = null
)