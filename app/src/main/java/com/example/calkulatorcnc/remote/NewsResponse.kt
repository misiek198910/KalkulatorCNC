package com.example.calkulatorcnc.remote

import com.google.gson.annotations.SerializedName

@androidx.annotation.Keep
data class NewsResponse(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("image_url") val image_url: String? = null,
    @SerializedName("action_link") val action_link: String? = null,
    @SerializedName("publish_date") val publish_date: String? = null,
    @SerializedName("is_visible") val is_visible: Boolean? = false
)