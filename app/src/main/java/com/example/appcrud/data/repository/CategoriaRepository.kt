package com.example.appcrud.data.repository

import com.example.appcrud.data.api.RetrofitClient
import com.example.appcrud.data.model.Categoria

class CategoriaRepository {

    private val api = RetrofitClient.apiService

    suspend fun getCategorias() = api.getCategorias()

    suspend fun createCategoria(categoria: Categoria) { api.createCategoria(categoria) }

    suspend fun updateCategoria(id: Int, categoria: Categoria) { api.updateCategoria(id, categoria) }

    suspend fun deleteCategoria(id: Int) = api.deleteCategoria(id)
}
