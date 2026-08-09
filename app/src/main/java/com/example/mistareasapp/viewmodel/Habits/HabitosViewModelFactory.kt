package com.example.mistareasapp.viewmodel.Habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mistareasapp.data.habits.HabitoDao

class HabitosViewModelFactory(private val habitoDao: HabitoDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HabitosViewModel(habitoDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}