package com.example.mistareasapp.ui.components.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mistareasapp.data.tasks.Categoria
import com.example.mistareasapp.data.tasks.Prioridad
import com.example.mistareasapp.data.tasks.Tarea
import com.example.mistareasapp.ui.theme.PrioridadAlta
import com.example.mistareasapp.ui.theme.PrioridadBaja
import com.example.mistareasapp.ui.theme.PrioridadMedia
import com.example.mistareasapp.viewmodel.Tasks.MapasDeTareas
import com.example.mistareasapp.viewmodel.Tasks.TareasViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.mistareasapp.ui.components.tasks.TareaDialogos
import androidx.compose.ui.platform.LocalContext

// --- COMPONENTE: LISTA PRINCIPAL (CUERPO) ---
/**
 * Gestión de Estados Locales complejos: Maneja un mutableStateMapOf para saber qué secciones ("Hoy", "Vencidas", etc.) están abiertas o cerradas individualmente.
 * Doble Sistema de Diálogos: Incluye toda la lógica y la UI de los cuadros de confirmación para Eliminar (rojo) y para Completar/Archivar (azul/primario).
 * Control Maestro (Mando a distancia): Tiene un LaunchedEffect que escucha al viewModel.todasSeccionesAbiertas para abrir o cerrar todas las categorías de golpe cuando pulsas el botón de la TopBar.
 * Lógica de Secciones Inteligente: Usa una función interna seccion(...) que decide dinámicamente si mostrar el encabezado, cuántas tareas hay, y si debe pintar el contenido basándose en el estado de expansión.
 * Configuración de Scroll (LazyColumn): Define un contentPadding específico para que las tareas no se queden tapadas por la barra de navegación inferior (bottom = 100.dp).
*/

@Suppress("FunctionNaming") // Esto quita el error de Detekt
@Composable
fun CuerpoListaTareas(
    modifier: Modifier = Modifier.Companion,
    mapas: MapasDeTareas,
    onTaskToggle: (Tarea, Boolean) -> Unit,
    onEditTask: (Int) -> Unit,
    viewModel: TareasViewModel,
    esVistaCategorias: Boolean
) {
    val estadosSecciones = remember { mutableStateMapOf<String, Boolean>() }
    val listaCategorias by viewModel.todasLasCategorias.collectAsState(initial = emptyList())

    // 2. ESTADOS DE DIÁLOGOS (¡ESTO ES LO QUE FALTABA!)
    var mostrarDialogo by remember { mutableStateOf(false) }
    var tareaAEliminar by remember { mutableStateOf<Tarea?>(null) }
    var mostrarDialogoCompletar by remember { mutableStateOf(false) }
    var tareaACompletar by remember { mutableStateOf<Tarea?>(null) }
    val context = LocalContext.current

      // Función para inicializar o cambiar todas las secciones
    fun setTodas(abrir: Boolean) {
        val nombres = listOf("Vencidas", "Hoy", "Esta semana", "Este mes", "Más adelante", "Completadas")
        nombres.forEach { estadosSecciones[it] = abrir }
    }

    // --- BLOQUE DE DIÁLOGOS (Llamada corregida) ---
    TareaDialogos(
        mostrarDialogo = mostrarDialogo,
        tareaAEliminar = tareaAEliminar,
        onConfirmEliminar = {
            tareaAEliminar?.let { viewModel.eliminarTarea(it, context) }
            mostrarDialogo = false
            tareaAEliminar = null
        },
        onDismissEliminar = {
            mostrarDialogo = false
            tareaAEliminar = null
        },
        mostrarDialogoCompletar = mostrarDialogoCompletar,
        tareaACompletar = tareaACompletar,
        onConfirmCompletar = {
            tareaACompletar?.let { viewModel.completarTarea(it, context)  }
            mostrarDialogoCompletar = false
            tareaACompletar = null
        },
        onDismissCompletar = {
            mostrarDialogoCompletar = false
            tareaACompletar = null
        }
    )

    // 2. REACCIÓN AL BOTÓN GLOBAL:
    // Este bloque se ejecuta CADA VEZ que el icono de arriba cambia
    LaunchedEffect(viewModel.todasSeccionesAbiertas) {
        // Borramos los estados previos para que el mando maestro tome el control
        estadosSecciones.clear()

        // Ponemos todas las secciones al mismo estado que el botón de arriba
        val secciones =
            listOf("Vencidas", "Hoy", "Esta semana", "Este mes", "Más adelante", "Completadas")
        secciones.forEach { nombre ->
            estadosSecciones[nombre] = viewModel.todasSeccionesAbiertas
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            top = 0.dp,       // <-- Esto hace que el título suba hasta arriba
            start = 16.dp,    // Mantiene el margen a la izquierda
            end = 16.dp,      // Mantiene el margen a la derecha
            bottom = 100.dp   // Deja espacio abajo para que la última tarea no quede tras la barra inferior
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {


        // --- FUNCIÓN INTERNA ---
        fun seccion(titulo: String, lista: List<Tarea>, color: Color) {
            if (lista.isNotEmpty()) {
                val abierto = estadosSecciones.getOrDefault(titulo, true)

                item(key = "header_$titulo") {
                    // Usamos un Box para controlar el espacio sin tocar HeaderSeccionColapsable
                    Box(modifier = Modifier.Companion.padding(top = if (titulo == "Vencidas" || titulo == "Hoy") 0.dp else 12.dp)) {
                        HeaderSeccionColapsable(
                            titulo = titulo,
                            color = color,
                            cantidad = lista.size,
                            abierto = abierto,
                            onToggle = { estadosSecciones[titulo] = !abierto }
                        )
                    }
                }

                if (abierto) {
                    items(lista, key = { "${it.id}_$titulo" }) { tarea ->
                        TareaCard(
                            tarea = tarea,
                            categorias = listaCategorias,
                            onTaskToggle = onTaskToggle,
                            onDelete = { t ->
                                tareaAEliminar = t
                                mostrarDialogo = true
                            },
                            onArchive = { t ->
                                tareaACompletar = t
                                mostrarDialogoCompletar = true
                            },
                            modifier = Modifier.Companion.clickable { onEditTask(tarea.id) }
                        )
                    }
                }
            }
        }

        // Llamadas a las secciones
        seccion("Vencidas", mapas.vencidas, Color(0xFFCF6679))
        seccion("Hoy", mapas.hoy, Color(0xFFFFEB3B))
        seccion("Esta semana", mapas.estaSemana, Color(0xFFFFB74D))
        seccion("Este mes", mapas.esteMes, Color(0xFF64B5F6))
        seccion("Más adelante", mapas.resto, Color(0xFF9E9E9E))

        if (viewModel.mostrarCompletadas && mapas.completadas.isNotEmpty()) {
            item { HorizontalDivider(Modifier.Companion.padding(vertical = 12.dp)) }
            seccion("Completadas", mapas.completadas, Color.Companion.Gray)
        }
    }
}
@Suppress("FunctionNaming") // Esto quita el error de Detekt
@Composable
fun HeaderSeccionColapsable(
    titulo: String,
    color: Color,
    cantidad: Int,
    abierto: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Companion.CenterVertically
    ) {
        // ICONO A LA IZQUIERDA
        Icon(
            imageVector = if (abierto) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = color
        )

        Spacer(modifier = Modifier.Companion.width(8.dp))

        // TEXTO A LA DERECHA
        Text(
            text = "$titulo ($cantidad)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Companion.Bold),
            color = color
        )
    }
}

@Suppress("FunctionNaming") // Esto quita el error de Detekt
@Composable
fun DismissBackground(dismissState: SwipeToDismissBoxState) {
    val color = when (dismissState.dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> Color(0xFFEF5350) // Rojo para borrar
        SwipeToDismissBoxValue.EndToStart -> Color(0xFF66BB6A) // Verde para archivar
        else -> Color.Companion.Transparent
    }

    Box(
        modifier = Modifier.Companion
            .fillMaxSize()
            .background(color, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp),
        contentAlignment = when (dismissState.dismissDirection) {
            SwipeToDismissBoxValue.StartToEnd -> Alignment.Companion.CenterStart
            else -> Alignment.Companion.CenterEnd
        }
    ) {
        val icon = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
            Icons.Default.Delete else Icons.Default.Archive

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Companion.White
        )
    }
}



@Composable
fun HeaderSeccion(titulo: String, color: Color) {
    Text(
        text = titulo.uppercase(),
        style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 1.5.sp),
        color = color.copy(alpha = 0.8f),
        fontWeight = FontWeight.Companion.Bold,
        modifier = Modifier.Companion.padding(top = 16.dp, bottom = 8.dp, start = 8.dp)
    )
}