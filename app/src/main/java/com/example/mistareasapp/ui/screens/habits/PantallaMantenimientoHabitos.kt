package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.navigation.NavHostController
import com.example.mistareasapp.data.habits.CategoriaHabito
import com.example.mistareasapp.data.habits.Habito
import com.example.mistareasapp.data.habits.TipoObjetivoHabito
import com.example.mistareasapp.iconoAEmoji
import com.example.mistareasapp.iconoEfectivoHabito
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMantenimientoHabitos(
    viewModel: HabitosViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val habitos by viewModel.todosLosHabitos.collectAsState(initial = emptyList())
    val categorias by viewModel.categoriasHabitos.collectAsState(initial = emptyList())
    var habitoAEliminar by remember { mutableStateOf<Habito?>(null) }

    // Agrupar hábitos por categoría respetando el orden definido (campo `orden` ASC, luego id ASC)
    val categoriasOrdenadas = categorias.sortedWith(compareBy({ it.orden }, { it.id }))
    val habitosPorCategoria: Map<Long?, List<Habito>> = habitos.groupBy { it.categoriaId }
    // Categorías que tienen hábitos + hábitos sin categoría al final
    val gruposOrdenados: List<Pair<CategoriaHabito?, List<Habito>>> = buildList {
        categoriasOrdenadas.forEach { cat ->
            val lista = habitosPorCategoria[cat.id]
            if (!lista.isNullOrEmpty()) add(cat to lista)
        }
        val sinCategoria = habitosPorCategoria[null]
        if (!sinCategoria.isNullOrEmpty()) add(null to sinCategoria)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de hábitos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        if (habitos.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No hay hábitos creados", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                gruposOrdenados.forEach { (categoria, listaHabitos) ->
                    // Cabecera de categoría
                    item(key = "cat_${categoria?.id ?: -1}") {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            if (categoria != null) {
                                val colorCat = try { Color(categoria.color.toColorInt()) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                                Text(
                                    iconoAEmoji(categoria.icono),
                                    fontSize = 16.sp
                                )
                                Text(
                                    categoria.nombre,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = colorCat
                                )
                            } else {
                                Text("Sin categoría", fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("(${listaHabitos.size})", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(6.dp))
                    }

                    // Hábitos de esta categoría
                    listaHabitos.forEach { habito ->
                        item(key = habito.id) {
                            HabitoMantenimientoItem(
                                habito = habito,
                                categorias = categorias,
                                onEditar = { navController.navigate("editar_habito/${habito.id}") },
                                onEliminar = { habitoAEliminar = habito },
                                onMoverArriba = { viewModel.moverHabitoArriba(habito, listaHabitos) },
                                onMoverAbajo = { viewModel.moverHabitoAbajo(habito, listaHabitos) },
                                puedeSubir = listaHabitos.indexOf(habito) > 0,
                                puedeBajar = listaHabitos.indexOf(habito) < listaHabitos.lastIndex
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    habitoAEliminar?.let { habito ->
        AlertDialog(
            onDismissRequest = { habitoAEliminar = null },
            title = { Text("Eliminar hábito") },
            text = {
                Text("¿Seguro que quieres eliminar \"${habito.nombre}\"? Se eliminará también todo su historial y no se podrá recuperar.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminarHabito(habito)
                        habitoAEliminar = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { habitoAEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun HabitoMantenimientoItem(
    habito: Habito,
    categorias: List<CategoriaHabito>,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onMoverArriba: () -> Unit = {},
    onMoverAbajo: () -> Unit = {},
    puedeSubir: Boolean = false,
    puedeBajar: Boolean = false
) {
    val colorHabito = try {
        Color(habito.colorHex.toColorInt())
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val tipoLabel = when {
        habito.esCompuestoPorTareas -> "Por tareas"
        habito.tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO -> "Cuantitativo · ${habito.objetivoValor} ${habito.unidad ?: ""}"
        habito.tipoObjetivo == TipoObjetivoHabito.LIMITE_MAXIMO -> {
            val lim = habito.limiteMaximo?.toBigDecimal()?.stripTrailingZeros()?.toPlainString() ?: "?"
            val unid = habito.unidad?.let { " $it" } ?: ""
            val per = when (habito.frecuencia) { com.example.mistareasapp.data.habits.FrecuenciaHabito.DIARIA -> "día"; com.example.mistareasapp.data.habits.FrecuenciaHabito.SEMANAL -> "sem"; com.example.mistareasapp.data.habits.FrecuenciaHabito.MENSUAL -> "mes" }
            "Límite: $lim$unid/$per"
        }
        else -> "Frecuencia · ${habito.vecesPorDia}x/${habito.frecuencia.name.lowercase()}"
    }

    val fmtFecha = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono efectivo (el de la categoría si existe, igual que en el resto de vistas)
            Surface(shape = CircleShape, color = colorHabito.copy(alpha = 0.15f), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = iconoAEmoji(iconoEfectivoHabito(habito.categoriaId, habito.icono, categorias)),
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(habito.nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(tipoLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val fechaTexto = if (habito.fechaModificacion != null) {
                    "Modificado: ${habito.fechaModificacion.format(fmtFecha)}"
                } else {
                    "Desde: ${habito.fechaInicio.format(fmtFecha)}"
                }
                Text(fechaTexto, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column {
                IconButton(onClick = onMoverArriba, enabled = puedeSubir, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Subir", tint = if (puedeSubir) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onMoverAbajo, enabled = puedeBajar, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Bajar", tint = if (puedeBajar) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(16.dp))
                }
            }
            IconButton(onClick = onEditar) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
