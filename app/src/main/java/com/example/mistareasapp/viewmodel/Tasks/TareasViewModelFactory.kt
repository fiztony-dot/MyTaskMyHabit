package com.example.mistareasapp.viewmodel.Tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mistareasapp.data.tasks.CategoriaDao
import com.example.mistareasapp.data.tasks.TareaDao

class TareasViewModelFactory(
    private val tareaDao: TareaDao,
    private val categoriaDao: CategoriaDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TareasViewModelRoom::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Aquí le pasamos al ViewModel los DAOs que necesita
            return TareasViewModelRoom(tareaDao, categoriaDao) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}