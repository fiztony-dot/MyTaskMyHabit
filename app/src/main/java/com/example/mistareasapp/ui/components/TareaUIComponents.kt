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
import androidx.compose.material.icons.filled.Repeat // O el icono que hayas elegido
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.mutableStateOf
import com.example.mistareasapp.viewmodel.TareasViewModel
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextOverflow
import kotlin.collections.List // Esto quita el rojo de List<Categoria>
import com.example.mistareasapp.data.Categoria // <--- ESTO ES LO QUE SUELE FALTAR
import androidx.compose.runtime.collectAsState // <--- ESTE ES EL PRINCIPAL
import androidx.compose.runtime.getValue     // Permite usar el 'by'

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

    // Función para inicializar o cambiar todas las secciones
    fun setTodas(abrir: Boolean) {
        val nombres = listOf("Vencidas", "Hoy", "Esta semana", "Este mes", "Más adelante", "Completadas")
        nombres.forEach { estadosSecciones[it] = abrir }
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
                            // APLICAMOS UN OFFSET NEGATIVO AQUÍ
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
@Composable
fun TareaCard(
    tarea: Tarea,
    categorias: List<Categoria>,
    onTaskToggle: (Tarea, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorEstado = if (tarea.estaCompletada) PrioridadCompletada else when (tarea.prioridad) {
        Prioridad.ALTA -> PrioridadAlta
        Prioridad.MEDIA -> PrioridadMedia
        Prioridad.BAJA -> PrioridadBaja
    }

    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorCard),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min).fillMaxWidth()
                .padding(start = 12.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.fillMaxHeight(0.6f).width(4.dp).background(
                    colorEstado,
                    RoundedCornerShape(2.dp)
                )
            )
            Spacer(Modifier.width(8.dp))

            Checkbox(
                checked = tarea.estaCompletada,
                onCheckedChange = { isChecked -> onTaskToggle(tarea, isChecked) },
                colors = CheckboxDefaults.colors(checkedColor = PrioridadCompletada)
            )

            Column(
                modifier = Modifier.weight(1f)
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                // Fila del Título + Icono de repetición
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tarea.titulo.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (tarea.estaCompletada) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        textDecoration = if (tarea.estaCompletada) TextDecoration.LineThrough else null,
                        // 3. Si es muy largo, añade "..." al final
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false) // Para que el icono no se desplace fuera
                    )

                    // MARCA VISUAL DE REPETICIÓN
                    if (tarea.repeticion != "Sin repetición") {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Repetitiva",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }

                val fechaFmt = DateTimeFormatter.ofPattern("dd MMM")
                val textoFecha = tarea.fechaLimite?.format(fechaFmt) ?: "Sin fecha"
                val horaFmt = DateTimeFormatter.ofPattern("HH:mm")
                val textoHora = tarea.horaLimite?.format(horaFmt) ?: ""

                Text(
                    text = "📅 $textoFecha ${if (textoHora.isNotEmpty()) " 🕒 $textoHora" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Obtenemos el icono y el color usando los traductores que ya tienen los 20 tipos
            // 1. Extraemos el nombre de la categoría (si es null, usamos "list")
            // PASO 2: Buscar la categoría de la tarea dentro de la lista de categorías
            val categoriaAsociada = categorias.find { it.titulo == tarea.categoria }

            // PASO 3: Si la encontramos, usamos su icono. Si no, usamos "list"
            val nombreIcono = categoriaAsociada?.icono ?: "list"
            val iconoARenderizar = obtenerIcono(nombreIcono)
            val colorARenderizar = obtenerColorIcono(nombreIcono)

            // ... resto del código del Icon ...
            if (tarea.categoria != null) {
                Icon(
                    imageVector = iconoARenderizar,
                    contentDescription = null,
                    tint = if (tarea.estaCompletada) Color.Gray else colorARenderizar
                )
            }
        }
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