package com.example.mistareasapp.viewmodel.Shopping

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mistareasapp.data.shopping.ListaCompraDao

class ListaCompraViewModelFactory(
    private val app: Application,
    private val dao: ListaCompraDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListaCompraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ListaCompraViewModel(app, dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
