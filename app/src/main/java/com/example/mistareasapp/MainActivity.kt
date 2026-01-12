package com.example.mistareasapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Le pasamos una función lambda vacía para satisfacer el requisito del parámetro
            MisTareasApp()
        }
    }
}
