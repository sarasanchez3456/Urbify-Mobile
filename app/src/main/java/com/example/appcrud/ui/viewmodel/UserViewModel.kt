package com.example.appcrud.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appcrud.data.api.RetrofitClient
import com.example.appcrud.data.model.User
import com.example.appcrud.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {
    private val repository = UserRepository(RetrofitClient.apiService)

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        fetchUsers()
    }

    fun fetchUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _users.value = repository.getUsers()
                _error.value = null
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addUser(name: String, email: String) {
        viewModelScope.launch {
            try {
                repository.createUser(User(name = name, email = email))
                fetchUsers()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updateUser(id: String, name: String, email: String) {
        viewModelScope.launch {
            try {
                repository.updateUser(id, User(id = id, name = name, email = email))
                fetchUsers()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteUser(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteUser(id)
                fetchUsers()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}