package com.example.appcrud.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.api.RetrofitClient
import com.example.appcrud.data.model.Item
import com.example.appcrud.data.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ItemViewModel : ViewModel() {
    private val repository = ItemRepository(RetrofitClient.apiService)

    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        fetchItems()
    }

    fun fetchItems() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _items.value = repository.getItems()
                _error.value = null
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addItem(name: String, description: String) {
        viewModelScope.launch {
            try {
                repository.createItem(Item(name = name, description = description))
                fetchItems()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updateItem(id: String, name: String, description: String) {
        viewModelScope.launch {
            try {
                repository.updateItem(id, Item(id = id, name = name, description = description))
                fetchItems()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteItem(id)
                fetchItems()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}