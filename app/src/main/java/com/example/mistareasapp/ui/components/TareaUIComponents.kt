package com.example.mistareasapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mistareasapp.data.Prioridad
import com.example.mistareasapp.data.Tarea
import com.example.mistareasapp.ui.theme.ColorCard
import com.example.mistareasapp.ui.theme.PrioridadAlta
import com.example.mistareasapp.ui.theme.PrioridadBaja
import com.example.mistareasapp.ui.theme.PrioridadCompletada
import com.example.mistareasapp.ui.theme.PrioridadMedia
import com.example.mistareasapp.viewmodel.MapasDeTareas
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat // O el icono que hayas elegido
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.mutableStateOf
import com.example.mistareasapp.viewmodel.TareasViewModel
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextOverflow
import kotlin.collections.List // Esto quita el rojo de List<Categoria>
import com.example.mistareasapp.data.Categoria // <--- ESTO ES LO QUE SUELE FALTAR
import androidx.compose.runtime.collectAsState // <--- ESTE ES EL PRINCIPAL
import androidx.compose.runtime.getValue     // Permite usar el 'by'
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.LowPriority
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.AccessTimeFilled
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowDown


// --- COMPONENTE: SELECTOR DE PRIORIDAD ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorPrioridad(
    prioridadSeleccionada: Prioridad,
    onPrioridadCambiada: (Prioridad) -> Unit,
    modifier: Modifier = Modifier.Companion
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Prioridad", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Prioridad.entries.forEach { p ->
                FilterChip(
                    selected = p == prioridadSeleccionada,
                    onClick = { onPrioridadCambiada(p) },
                    label = { Text(p.etiqueta) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when (p) {
                            Prioridad.ALTA -> PrioridadAlta
                            Prioridad.MEDIA -> PrioridadMedia
                            Prioridad.BAJA -> PrioridadBaja
                        },
                        selectedLabelColor = Color.Companion.White
                    )
                )
            }
        }
    }
}

// --- COMPONENTE: LISTA PRINCIPAL (CUERPO) ---
@Composable
fun CuerpoListaTareas(
    modifier: Modifier = Modifier,
    mapas: MapasDeTareas,
    onTaskToggle: (Tarea, Boolean) -> Unit,
    onEditTask: (Int) -> Unit,
    viewModel: TareasViewModel,
    esVistaCategorias: Boolean // <-- Añadimos este parámetro
) {
    // CAMBIO CLAVE: En lugar de 6 variables, usamos un Mapa que las controla todas
    val estadosSecciones = remember { mutableStateMapOf<String, Boolean>() }

    val listaCategorias by viewModel.todasLasCategorias.collectAsState(initial = emptyList())

    // --- NUEVO: ESTADO PARA EL DIÁLOGO ---
    var tareaAEliminar by remember { mutableStateOf<Tarea?>(null) }
    var mostrarDialogo by remember { mutableStateOf(false) }

    // Función para inicializar o cambiar todas las secciones
    fun setTodas(abrir: Boolean) {
        val nombres = listOf("Vencidas", "Hoy", "Esta semana", "Este mes", "Más adelante", "Completadas")
        nombres.forEach { estadosSecciones[it] = abrir }
    }

    // --- BLOQUE DEL DIÁLOGO ---
    if (mostrarDialogo && tareaAEliminar != null) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogo = false
                tareaAEliminar = null
            },
            title = { Text("¿Eliminar tarea?") },
            text = { Text("¿Estás seguro de que quieres borrar \"${tareaAEliminar?.titulo}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    tareaAEliminar?.let { viewModel.eliminarTarea(it) }
                    mostrarDialogo = false
                    tareaAEliminar = null
                }) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogo = false
                    tareaAEliminar = null
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // 2. REACCIÓN AL BOTÓN GLOBAL:
    // Este bloque se ejecuta CADA VEZ que el icono de arriba cambia
    LaunchedEffect(viewModel.todasSeccionesAbiertas) {
        // Borramos los estados previos para que el mando maestro tome el control
        estadosSecciones.clear()

        // Ponemos todas las secciones al mismo estado que el botón de arriba
        val secciones = listOf("Vencidas", "Hoy", "Esta semana", "Este mes", "Más adelante", "Completadas")
        secciones.forEach { nombre ->
            estadosSecciones[nombre] = viewModel.todasSeccionesAbiertas
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {


        // --- FUNCIÓN INTERNA ---
        fun seccion(titulo: String, lista: List<Tarea>, color: Color) {
            if (lista.isNotEmpty()) {
                // Si no está en el mapa, por defecto la ponemos abierta (true)
                val abierto = estadosSecciones.getOrDefault(titulo, true)

                item(key = "header_$titulo") {
                    HeaderSeccionColapsable(
                        titulo = titulo,
                        color = color,
                        cantidad = lista.size,
                        abierto = abierto,
                        onToggle = { estadosSecciones[titulo] = !abierto } // Sigue funcionando individual
                    )
                }

                if (abierto) {
                    items(lista, key = { "${it.id}_$titulo" }) { tarea ->
                        TareaCard(
                            tarea = tarea,
                            categorias = listaCategorias,
                            onTaskToggle = onTaskToggle,
                            // AÑADIMOS ESTAS DOS LÍNEAS:
                            onDelete = { t ->
                                tareaAEliminar = t
                                mostrarDialogo = true // Activamos el diálogo
                            },
                            onArchive = { t -> viewModel.archivarTarea(t) },
                            modifier = Modifier
                                .padding(top = 0.dp)
                                .clickable { onEditTask(tarea.id) }
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
            item { HorizontalDivider(Modifier.padding(vertical = 12.dp)) }
            seccion("Completadas", mapas.completadas, Color.Gray)
        }
    }
}
@Composable
fun HeaderSeccionColapsable(
    titulo: String,
    color: Color,
    cantidad: Int,
    abierto: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ICONO A LA IZQUIERDA
        Icon(
            imageVector = if (abierto) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = color
        )

        Spacer(modifier = Modifier.width(8.dp))

        // TEXTO A LA DERECHA
        Text(
            text = "$titulo ($cantidad)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}
// --- COMPONENTE: TARJETA DE TAREA INDIVIDUAL ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TareaCard(
    tarea: Tarea,
    categorias: List<Categoria>,
    onTaskToggle: (Tarea, Boolean) -> Unit,
    onDelete: (Tarea) -> Unit,
    onArchive: (Tarea) -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onDelete(tarea)
                    false // Rebote para diálogo
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onArchive(tarea)
                    false // Rebote para marcar completada
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.padding(vertical = 4.dp),
        backgroundContent = { DismissBackground(dismissState) }
    ) {
        // Colores y Iconos de Prioridad (Mantenemos tus variables de color)
        val (colorPrioridad, iconoPrioridad) = when (tarea.prioridad) {
            Prioridad.ALTA -> PrioridadAlta to Icons.Default.PriorityHigh
            Prioridad.MEDIA -> PrioridadMedia to Icons.Default.Remove
            Prioridad.BAJA -> PrioridadBaja to Icons.Default.KeyboardArrowDown
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min), // Ajusta la altura al contenido
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ColorCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {

                // --- AQUI ESTÁ EL CAMBIO: BARRA LATERAL ---
                val (colorPrioridad, iconoPrioridad) = when (tarea.prioridad) {
                    Prioridad.ALTA -> PrioridadAlta to Icons.Rounded.Whatshot
                    Prioridad.MEDIA -> PrioridadMedia to Icons.Rounded.AccessTimeFilled
                    Prioridad.BAJA -> PrioridadBaja to Icons.Rounded.KeyboardDoubleArrowDown
                }
                // --- 1. BARRA DE PRIORIDAD IZQUIERDA ---
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(38.dp) // Ancho fijo para que no baile
                        .background(
                            if (tarea.estaCompletada) Color.Gray.copy(alpha = 0.2f)
                            else colorPrioridad.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (tarea.estaCompletada) Icons.Default.CheckCircle else iconoPrioridad,
                        contentDescription = null,
                        tint = if (tarea.estaCompletada) Color.Gray else colorPrioridad,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // --- 2. INFORMACIÓN CENTRAL ---
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    // Fila de Título y Repetición
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tarea.titulo.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (tarea.estaCompletada) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            textDecoration = if (tarea.estaCompletada) TextDecoration.LineThrough else null,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (tarea.repeticion != "Sin repetición") {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Fila de Fecha y Hora (Tu lógica original)
                    val fechaFmt = DateTimeFormatter.ofPattern("dd MMM")
                    val textoFecha = tarea.fechaLimite?.format(fechaFmt) ?: "Sin fecha"
                    val horaFmt = DateTimeFormatter.ofPattern("HH:mm")
                    val textoHora = tarea.horaLimite?.format(horaFmt) ?: ""

                    Text(
                        text = "📅 $textoFecha ${if (textoHora.isNotEmpty()) "  🕒 $textoHora" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // --- 3. ICONO DE CATEGORÍA (DERECHA) ---
                val categoriaAsociada = categorias.find { it.titulo == tarea.categoria }
                val nombreIcono = categoriaAsociada?.icono ?: "list"

                Box(
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Icon(
                        imageVector = obtenerIcono(nombreIcono),
                        contentDescription = null,
                        tint = if (tarea.estaCompletada) Color.Gray.copy(0.4f) else obtenerColorIcono(nombreIcono),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DismissBackground(dismissState: SwipeToDismissBoxState) {
    val color = when (dismissState.dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> Color(0xFFEF5350) // Rojo para borrar
        SwipeToDismissBoxValue.EndToStart -> Color(0xFF66BB6A) // Verde para archivar
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp),
        contentAlignment = when (dismissState.dismissDirection) {
            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
            else -> Alignment.CenterEnd
        }
    ) {
        val icon = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
            Icons.Default.Delete else Icons.Default.Archive

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotonSelectorDato(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colorTexto: Color = MaterialTheme.colorScheme.onSurface
) {
    OutlinedCard(
        onClick = {
            println("DEBUG: Botón pulsado")
            onClick()
        },
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            // ESTA ES LA LÍNEA CLAVE:
            // Debe usar 'label' directamente. Si usas un remember aquí, se rompe.
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = colorTexto
            )
        }
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