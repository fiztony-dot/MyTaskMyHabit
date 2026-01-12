package com.example.mistareasapp.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mistareasapp.data.Tarea
import com.example.mistareasapp.data.TareaDao
import com.example.mistareasapp.data.Categoria
import com.example.mistareasapp.data.CategoriaDao
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.* // Importa Flow, StateFlow, combine, etc.
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi // Necesario para flatMapLatest
class TareasViewModelFactory(
    private val tareaDao: TareaDao,
    private val categoriaDao: CategoriaDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TareasViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Aquí le pasamos al ViewModel los DAOs que necesita
            return TareasViewModel(tareaDao, categoriaDao) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}