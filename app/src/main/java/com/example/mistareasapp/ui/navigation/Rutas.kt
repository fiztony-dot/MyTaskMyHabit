package com.example.mistareasapp.ui.navigation

sealed class Rutas(val ruta: String) {
    object PantallaTareas : Rutas("tareas")
    object PantallaHabitos : Rutas("habitos")
    object PantallaCrearTarea : Rutas("crear_tarea")

    object PantallaEditarTarea : Rutas("editar_tarea/{tareaId}") {
        fun crearRuta(id: Int) = "editar_tarea/$id"
        }
    object PantallaBackup : Rutas("ruta_gestion_copias")
}