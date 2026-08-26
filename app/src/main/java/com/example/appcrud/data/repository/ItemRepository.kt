package com.example.appcrud.data.repository

import com.example.appcrud.data.api.ApiService
import com.example.appcrud.data.model.Item

class ItemRepository(private val apiService: ApiService) {
    suspend fun getItems() = apiService.getItems()
    suspend fun getItem(id: String) = apiService.getItem(id)
    suspend fun createItem(item: Item) = apiService.createItem(item)
    suspend fun updateItem(id: String, item: Item) = apiService.updateItem(id, item)
    suspend fun deleteItem(id: String) = apiService.deleteItem(id)
}