package com.example.mistareasapp.ui.navigation

sealed class Rutas(val ruta: String) {
    object PantallaTareas : Rutas("tareas")
    object PantallaCrearTarea : Rutas("crear_tarea")

    object PantallaEditarTarea : Rutas("editar_tarea/{tareaId}") {
        fun crearRuta(id: Int) = "editar_tarea/$id"
        }
    object PantallaBackup : Rutas("ruta_gestion_copias")

    // Módulo Hábitos (Infraestructura nueva)
    object HabitosFlash : Rutas("habitos_flash")
    object HabitosListado : Rutas("habitos_listado")
    object HabitosEstadisticas : Rutas("habitos_estadisticas")
    // Ruta "padre" para entrar al módulo
    object PantallaHabitos : Rutas("habitos_flash")
    object PantallaGestionCategorias : Rutas("categorias")
    object PantallaGestionCategoriasHabitos : Rutas("categorias_habitos")
    object PantallaConfiguracion : Rutas("configuracion")
    object PantallaCrearHabito : Rutas("crear_habito")




}