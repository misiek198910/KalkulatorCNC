package com.example.calkulatorcnc.remote

import androidx.annotation.Keep
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @Keep
    @GET("news")
    suspend fun getNewsFeed(): Response<List<NewsResponse>>
}