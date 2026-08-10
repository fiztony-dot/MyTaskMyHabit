package com.example.mistareasapp.viewmodel.Tasks

import com.example.mistareasapp.data.tasks.Tarea

/**
 * Modelos compartidos del módulo Tareas.
 * Usados por las pantallas de Tareas y por TareasViewModel (API).
 */

data class MapasDeTareas(
    val pendientesClasificar: List<Tarea> = emptyList(),
    val vencidas: List<Tarea> = emptyList(),
    val hoy: List<Tarea> = emptyList(),
    val estaSemana: List<Tarea> = emptyList(),
    val esteMes: List<Tarea> = emptyList(),
    val resto: List<Tarea> = emptyList(),
    val completadas: List<Tarea> = emptyList()
)

enum class TipoVista { VENCIMIENTO, CATEGORIAS }
