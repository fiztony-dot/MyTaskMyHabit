package com.example.mistareasapp.ui.components.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mistareasapp.data.tasks.Categoria
import com.example.mistareasapp.data.tasks.Prioridad
import com.example.mistareasapp.data.tasks.Tarea
import com.example.mistareasapp.ui.theme.ColorCard
import com.example.mistareasapp.ui.theme.PrioridadAlta
import com.example.mistareasapp.ui.theme.PrioridadBaja
import com.example.mistareasapp.ui.theme.PrioridadMedia
import java.time.format.DateTimeFormatter

/**
 * COMPONENTE: TareaCard
 * Representa la tarjeta visual de una tarea individual con soporte para gestos (Swipe).
 * * @param tarea Datos de la tarea a mostrar.
 * @param categorias Lista de categorías para obtener iconos y colores.
 * @param onTaskToggle Callback para marcar/desmarcar tarea.
 * @param onDelete Acción al deslizar para eliminar.
 * @param onArchive Acción al deslizar para archivar.
 */
@Suppress("FunctionNaming", "LongMethod", "LongParameterList")
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
    // 1. GESTIÓN DE GESTOS (SWIPE)
    // Define qué ocurre cuando el usuario desliza la tarjeta a izquierda o derecha.
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onDelete(tarea)
                    false // Retorna false para que la tarjeta "rebote" y no desaparezca visualmente antes del diálogo
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onArchive(tarea)
                    false
                }
                else -> false
            }
        }
    )

    // 2. CONTENEDOR DE DESLIZAMIENTO
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.padding(vertical = 4.dp),
        backgroundContent = { DismissBackground(dismissState) }
    ) {

        // 3. LÓGICA DE COLORES SEGÚN PRIORIDAD
        val colorPrioridad = when (tarea.prioridad) {
            Prioridad.ALTA -> PrioridadAlta
            Prioridad.MEDIA -> PrioridadMedia
            Prioridad.BAJA -> PrioridadBaja
        }

        // 4. DISEÑO DE LA TARJETA (CARD)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min), // Mantiene la altura basada en el contenido
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ColorCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // --- BLOQUE A: BARRA INDICADORA DE PRIORIDAD (IZQUIERDA) ---
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(30.dp)
                        .background(
                            if (tarea.estaCompletada) Color.Gray.copy(alpha = 0.2f)
                            else colorPrioridad.copy(alpha = 0.30f)
                        )
                )

                // --- BLOQUE B: INFORMACIÓN DE LA TAREA (CENTRO) ---
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    // Fila 1: Título y estado de repetición
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
                                contentDescription = "Tarea recurrente",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Fila 2: Fecha y Hora límite
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

                // --- BLOQUE C: ICONO DE CATEGORÍA (DERECHA) ---
                val categoriaAsociada = categorias.find { it.titulo == tarea.categoria }
                val nombreIcono = categoriaAsociada?.icono ?: "list"

                Box(
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Icon(
                        // Estas funciones 'obtenerIcono' deben estar accesibles en este scope
                        imageVector = obtenerIcono(nombreIcono),
                        contentDescription = "Categoría: ${tarea.categoria}",
                        tint = if (tarea.estaCompletada) Color.Gray.copy(0.4f) else obtenerColorIcono(nombreIcono),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}