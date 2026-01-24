package com.example.mistareasapp.ui.components.Tasks

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mistareasapp.data.tasks.Categoria
import com.example.mistareasapp.data.tasks.Prioridad
import com.example.mistareasapp.data.tasks.Tarea
import com.example.mistareasapp.ui.theme.ColorCard
import com.example.mistareasapp.ui.theme.PrioridadAlta
import com.example.mistareasapp.ui.theme.PrioridadBaja
import com.example.mistareasapp.ui.theme.PrioridadMedia
import com.example.mistareasapp.viewmodel.Tasks.MapasDeTareas
import com.example.mistareasapp.viewmodel.Tasks.TareasViewModel
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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
    modifier: Modifier = Modifier.Companion,
    mapas: MapasDeTareas,
    onTaskToggle: (Tarea, Boolean) -> Unit,
    onEditTask: (Int) -> Unit,
    viewModel: TareasViewModel,
    esVistaCategorias: Boolean
) {
    val estadosSecciones = remember { mutableStateMapOf<String, Boolean>() }
    val listaCategorias by viewModel.todasLasCategorias.collectAsState(initial = emptyList())

    // --- NUEVO: ESTADO PARA EL DIÁLOGO ---
    var tareaAEliminar by remember { mutableStateOf<Tarea?>(null) }
    var mostrarDialogo by remember { mutableStateOf(false) }

    // --- NUEVOS ESTADOS PARA COMPLETAR ---
    var tareaACompletar by remember { mutableStateOf<Tarea?>(null) }
    var mostrarDialogoCompletar by remember { mutableStateOf(false) }

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
                    Text("Eliminar", color = Color.Companion.Red)
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

    if (mostrarDialogoCompletar && tareaACompletar != null) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogoCompletar = false
                tareaACompletar = null
            },
            title = { Text("¿Completar tarea?") },
            text = { Text("¿Quieres marcar como terminada \"${tareaACompletar?.titulo}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    tareaACompletar?.let { viewModel.archivarTarea(it) } // Llama al viewModel
                    mostrarDialogoCompletar = false
                    tareaACompletar = null
                }) {
                    Text("Completar", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoCompletar = false
                    tareaACompletar = null
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraFiltros(
    categorias: List<Categoria>,
    seleccionada: String?,
    onSeleccionar: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Opción para limpiar el filtro
        item {
            FilterChip(
                selected = seleccionada == null,
                onClick = { onSeleccionar(null) },
                label = { Text("Todas") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }

        // Botones para cada categoría
        items(categorias) { cat ->
            FilterChip(
                selected = seleccionada == cat.titulo,
                onClick = { onSeleccionar(cat.titulo) },
                label = { Text(cat.titulo) },
                leadingIcon = {
                    // Aquí usamos tu función obtenerIcono que ya tienes en el proyecto
                    Icon(
                        imageVector = obtenerIcono(cat.icono),
                        contentDescription = null,
                        modifier = Modifier.Companion.size(18.dp)
                    )
                }
            )
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

// --- COMPONENTE: TARJETA DE TAREA INDIVIDUAL ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TareaCard(
    tarea: Tarea,
    categorias: List<Categoria>,
    onTaskToggle: (Tarea, Boolean) -> Unit,
    onDelete: (Tarea) -> Unit,
    onArchive: (Tarea) -> Unit,
    modifier: Modifier = Modifier.Companion
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
        val colorPrioridad = when (tarea.prioridad) {
            Prioridad.ALTA -> PrioridadAlta
            Prioridad.MEDIA -> PrioridadMedia
            Prioridad.BAJA -> PrioridadBaja
        }

        Card(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .height(IntrinsicSize.Min), // Ajusta la altura al contenido
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ColorCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.Companion.fillMaxSize(),
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                // --- 1. BARRA LATERAL IZQUIERDA ---
                Box(
                    modifier = Modifier.Companion
                        .fillMaxHeight()
                        .width(30.dp) // Volvemos al ancho generoso de la imagen original
                        .background(
                            if (tarea.estaCompletada) Color.Companion.Gray.copy(alpha = 0.2f)
                            else colorPrioridad.copy(alpha = 0.30f) // Restauramos la transparencia suave
                        )
                )

                // --- 2. INFORMACIÓN CENTRAL ---
                Column(
                    modifier = Modifier.Companion
                        .weight(1f)
                        .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    // Fila de Título y Repetición
                    Row(verticalAlignment = Alignment.Companion.CenterVertically) {
                        Text(
                            text = tarea.titulo.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Companion.SemiBold,
                            color = if (tarea.estaCompletada) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            textDecoration = if (tarea.estaCompletada) TextDecoration.Companion.LineThrough else null,
                            overflow = TextOverflow.Companion.Ellipsis,
                            modifier = Modifier.Companion.weight(1f, fill = false)
                        )

                        if (tarea.repeticion != "Sin repetición") {
                            Spacer(Modifier.Companion.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                modifier = Modifier.Companion.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(Modifier.Companion.height(4.dp))

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
                    modifier = Modifier.Companion.padding(end = 16.dp)
                ) {
                    Icon(
                        imageVector = obtenerIcono(nombreIcono),
                        contentDescription = null,
                        tint = if (tarea.estaCompletada) Color.Companion.Gray.copy(0.4f) else obtenerColorIcono(
                            nombreIcono
                        ),
                        modifier = Modifier.Companion.size(24.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotonSelectorDato(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.Companion,
    enabled: Boolean = true,
    colorTexto: Color = MaterialTheme.colorScheme.onSurface
) {
    OutlinedCard(
        onClick = {
            println("DEBUG: Botón pulsado")
            onClick()
        },
        modifier = modifier.height(44.dp),
        enabled = enabled, // 2. Lo pasamos al botón real
        // shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Companion.Transparent)
    ) {
        Row(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.Companion.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else Color.Companion.Gray.copy(
                    alpha = 0.5f
                ),
                modifier = Modifier.Companion.size(18.dp)
            )
            Spacer(modifier = Modifier.Companion.width(8.dp))

            // ESTA ES LA LÍNEA CLAVE:
            // Debe usar 'label' directamente. Si usas un remember aquí, se rompe.
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) colorTexto else colorTexto.copy(alpha = 0.5f)
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