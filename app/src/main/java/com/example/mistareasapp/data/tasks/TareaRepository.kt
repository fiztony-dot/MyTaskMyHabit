package com.example.mistareasapp.data.Tasks

import android.content.Context

object TareaRepository {

    private fun dao(context: Context) =
        TareasDatabase.getDatabase(context).tareaDao()

    suspend fun insertar(context: Context, tarea: Tarea) {
        dao(context).insertar(tarea)
    }

    suspend fun insertarSimple(context: Context, texto: String) {
        dao(context).insertar(
            Tarea(
                titulo = texto.replaceFirstChar { it.uppercase() },
                descripcion = "Voz (Error IA)"
            )
        )
    }
}
