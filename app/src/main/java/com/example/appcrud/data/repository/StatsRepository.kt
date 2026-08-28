package com.example.appcrud.data.repository

import com.example.appcrud.data.api.RetrofitClient
import com.example.appcrud.data.model.Stats

class StatsRepository {

    private val api = RetrofitClient.apiService

    suspend fun getStats(): Stats {
        return api.getStats()
    }
}
