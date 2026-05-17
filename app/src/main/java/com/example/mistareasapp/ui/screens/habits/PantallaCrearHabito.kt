package com.example.mistareasapp.ui.screens.habits

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LabelOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.mistareasapp.data.habits.CategoriaHabito
import com.example.mistareasapp.data.habits.CriterioCumplimientoTareas
import com.example.mistareasapp.data.habits.FrecuenciaHabito
import com.example.mistareasapp.data.habits.Habito
import com.example.mistareasapp.data.habits.TareaHabito
import com.example.mistareasapp.data.habits.TipoObjetivoHabito
import com.example.mistareasapp.ui.components.tasks.obtenerColorIcono
import com.example.mistareasapp.ui.components.tasks.obtenerIcono
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private data class TareaHabitoDraft(
    val id: Int,
    val nombre: String,
    val descripcion: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearHabitoScreen(
    viewModel: HabitosViewModel,
    onGuardar: () -> Unit
) {
    val context = LocalContext.current
    val categorias by viewModel.categoriasHabitos.collectAsState(initial = emptyList())

    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var categoriaSeleccionadaId by remember { mutableStateOf<Long?>(null) }
    val categoriaSeleccionada = categorias.firstOrNull { it.id == categoriaSeleccionadaId }

    var fechaInicio by remember { mutableStateOf(LocalDate.now()) }
    var mostrarDatePicker by remember { mutableStateOf(false) }

    var frecuencia by remember { mutableStateOf(FrecuenciaHabito.DIARIA) }
    var tipoObjetivo by remember { mutableStateOf(TipoObjetivoHabito.FRECUENCIA) }
    var repeticionesPeriodo by remember { mutableStateOf("1") }
    var cantidadObjetivo by remember { mutableStateOf("1") }
    var unidadMedida by remember { mutableStateOf("unidades") }
    var objetivoRachaSemanas by remember { mutableStateOf("4") }

    var recordatoriosActivos by remember { mutableStateOf(false) }
    var horaRecordatorio by remember { mutableStateOf(LocalTime.of(9, 0)) }

    var esCompuestoPorTareas by remember { mutableStateOf(false) }
    var criterioCumplimiento by remember { mutableStateOf(CriterioCumplimientoTareas.TODAS) }
    var minimoTareasTexto by remember { mutableStateOf("1") }
    var nombreNuevaTarea by remember { mutableStateOf("") }
    var descripcionNuevaTarea by remember { mutableStateOf("") }
    var siguienteIdTarea by remember { mutableStateOf(0) }
    val tareasDraft = remember { mutableStateListOf<TareaHabitoDraft>() }

    val guardarHabito = {
        when {
            nombre.isBlank() -> {
                Toast.makeText(context, "El nombre del hábito es obligatorio", Toast.LENGTH_SHORT).show()
            }

            !esCompuestoPorTareas && tipoObjetivo == TipoObjetivoHabito.FRECUENCIA && (repeticionesPeriodo.toIntOrNull() ?: 0) <= 0 -> {
                Toast.makeText(context, "Indica una frecuencia válida", Toast.LENGTH_SHORT).show()
            }

            !esCompuestoPorTareas && tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO && (cantidadObjetivo.toIntOrNull() ?: 0) <= 0 -> {
                Toast.makeText(context, "La cantidad objetivo debe ser mayor que 0", Toast.LENGTH_SHORT).show()
            }

            !esCompuestoPorTareas && tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO && unidadMedida.isBlank() -> {
                Toast.makeText(context, "La unidad de medida es obligatoria", Toast.LENGTH_SHORT).show()
            }

            esCompuestoPorTareas && tareasDraft.isEmpty() -> {
                Toast.makeText(context, "Añade al menos una tarea al hábito", Toast.LENGTH_SHORT).show()
            }

            esCompuestoPorTareas && criterioCumplimiento == CriterioCumplimientoTareas.PARCIAL && (minimoTareasTexto.toIntOrNull() ?: 0) !in 1..tareasDraft.size.coerceAtLeast(1) -> {
                Toast.makeText(context, "El mínimo de tareas debe estar entre 1 y ${tareasDraft.size}", Toast.LENGTH_SHORT).show()
            }

            else -> {
                val minimoTareas = when {
                    !esCompuestoPorTareas -> null
                    criterioCumplimiento == CriterioCumplimientoTareas.TODAS -> tareasDraft.size.coerceAtLeast(1)
                    else -> (minimoTareasTexto.toIntOrNull() ?: 1).coerceIn(1, tareasDraft.size.coerceAtLeast(1))
                }

                val objetivoFrecuencia = if (esCompuestoPorTareas) {
                    minimoTareas ?: 1
                } else {
                    (repeticionesPeriodo.toIntOrNull() ?: 1).coerceAtLeast(1)
                }

                val nuevoHabito = Habito(
                    nombre = nombre.trim(),
                    descripcion = descripcion.trim().ifBlank { null },
                    categoriaId = categoriaSeleccionada?.id ?: 0L,
                    fechaInicio = fechaInicio,
                    frecuencia = frecuencia,
                    tipoObjetivo = if (esCompuestoPorTareas) TipoObjetivoHabito.FRECUENCIA else tipoObjetivo,
                    vecesPorDia = objetivoFrecuencia,
                    objetivoValor = if (!esCompuestoPorTareas && tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO) {
                        cantidadObjetivo.toIntOrNull()
                    } else {
                        null
                    },
                    unidad = if (!esCompuestoPorTareas && tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO) {
                        unidadMedida.trim().ifBlank { null }
                    } else {
                        null
                    },
                    esCompuestoPorTareas = esCompuestoPorTareas,
                    criterioCumplimientoTareas = if (esCompuestoPorTareas) criterioCumplimiento else CriterioCumplimientoTareas.TODAS,
                    minimoTareasCumplimiento = if (esCompuestoPorTareas && criterioCumplimiento == CriterioCumplimientoTareas.PARCIAL) minimoTareas else null,
                    objetivoRachaSemanas = objetivoRachaSemanas.toIntOrNull() ?: 4,
                    recordatoriosActivos = recordatoriosActivos,
                    horaRecordatorio = if (recordatoriosActivos) horaRecordatorio else null,
                    icono = categoriaSeleccionada?.icono ?: "favorite",
                    colorHex = categoriaSeleccionada?.color ?: "#FF0000"
                )

                val tareasPersistir = if (esCompuestoPorTareas) {
                    tareasDraft.map {
                        TareaHabito(
                            habitoId = 0,
                            nombre = it.nombre,
                            descripcion = it.descripcion.ifBlank { null }
                        )
                    }
                } else {
                    emptyList()
                }

                viewModel.insertarHabito(nuevoHabito, tareasPersistir)
                Toast.makeText(context, "Hábito creado", Toast.LENGTH_SHORT).show()
                onGuardar()
            }
        }
    }

    if (mostrarDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = fechaInicio.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        fechaInicio = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    mostrarDatePicker = false
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo hábito") },
                navigationIcon = {
                    IconButton(onClick = onGuardar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = guardarHabito, enabled = nombre.isNotBlank()) {
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SeccionFormulario(
                titulo = "Identidad",
                descripcion = "Nombre, descripción y categoría visual del hábito."
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del hábito*") },
                    placeholder = { Text("Ej: Caminar 30 minutos") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    placeholder = { Text("Qué quieres conseguir con este hábito") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                SelectorCategoriaHabito(
                    categorias = categorias,
                    categoriaSeleccionada = categoriaSeleccionada,
                    onCategoriaSeleccionada = { categoriaSeleccionadaId = it?.id }
                )

                if (categoriaSeleccionada == null) {
                    Text(
                        text = "Si eliges una categoría, el hábito heredará su icono y su color.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SeccionFormulario(
                titulo = "Planificación",
                descripcion = "Define fecha de inicio, periodo y cómo se mide el cumplimiento."
            ) {
                OutlinedButton(
                    onClick = { mostrarDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Inicio: ${fechaInicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
                }

                SelectorTipoObjetivo(
                    tipoObjetivo = tipoObjetivo,
                    onTipoObjetivoSeleccionado = { tipoObjetivo = it },
                    enabled = !esCompuestoPorTareas
                )

                if (esCompuestoPorTareas) {
                    Text(
                        text = "Al componerse por tareas, el cumplimiento se calcula por tareas y no por cantidad.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SelectorFrecuenciaHabito(
                    frecuenciaActual = frecuencia,
                    onFrecuenciaSeleccionada = { frecuencia = it }
                )

                if (!esCompuestoPorTareas && tipoObjetivo == TipoObjetivoHabito.FRECUENCIA) {
                    OutlinedTextField(
                        value = repeticionesPeriodo,
                        onValueChange = { repeticionesPeriodo = it.filter(Char::isDigit) },
                        label = { Text(etiquetaFrecuenciaSimple(frecuencia)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                if (!esCompuestoPorTareas && tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO) {
                    OutlinedTextField(
                        value = cantidadObjetivo,
                        onValueChange = { cantidadObjetivo = it.filter(Char::isDigit) },
                        label = { Text("Cantidad objetivo") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = unidadMedida,
                        onValueChange = { unidadMedida = it },
                        label = { Text("Unidad de medida") },
                        placeholder = { Text("minutos, km, vasos, páginas...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = objetivoRachaSemanas,
                    onValueChange = { objetivoRachaSemanas = it.filter(Char::isDigit) },
                    label = { Text("Objetivo de racha (semanas)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            SeccionFormulario(
                titulo = "Recordatorio",
                descripcion = "Activa un aviso si quieres reforzar el hábito."
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Activar recordatorio", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Puedes definir una hora fija diaria para recordar el hábito.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = recordatoriosActivos,
                        onCheckedChange = { recordatoriosActivos = it }
                    )
                }

                if (recordatoriosActivos) {
                    TimePickerField(
                        hora = horaRecordatorio,
                        onHoraSeleccionada = { horaRecordatorio = it }
                    )
                }
            }

            SeccionFormulario(
                titulo = "Tareas",
                descripcion = "Descompón el hábito en pasos concretos y define su criterio de cumplimiento."
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Este hábito se compone de tareas", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Úsalo cuando quieras marcar el hábito como completado por checklist.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = esCompuestoPorTareas,
                        onCheckedChange = {
                            esCompuestoPorTareas = it
                            if (it && tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO) {
                                tipoObjetivo = TipoObjetivoHabito.FRECUENCIA
                            }
                        }
                    )
                }

                if (esCompuestoPorTareas) {
                    SelectorCriterioCumplimiento(
                        criterio = criterioCumplimiento,
                        onCriterioSeleccionado = { criterioCumplimiento = it }
                    )

                    if (criterioCumplimiento == CriterioCumplimientoTareas.PARCIAL) {
                        OutlinedTextField(
                            value = minimoTareasTexto,
                            onValueChange = { minimoTareasTexto = it.filter(Char::isDigit) },
                            label = { Text("Mínimo de tareas completadas") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Define tareas específicas que componen este hábito",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = nombreNuevaTarea,
                                onValueChange = { nombreNuevaTarea = it },
                                label = { Text("Nombre de la tarea") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = descripcionNuevaTarea,
                                onValueChange = { descripcionNuevaTarea = it },
                                label = { Text("Descripción (opcional)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedButton(
                                onClick = {
                                    val nombreTareaLimpio = nombreNuevaTarea.trim()
                                    if (nombreTareaLimpio.isNotBlank()) {
                                        tareasDraft.add(
                                            TareaHabitoDraft(
                                                id = siguienteIdTarea,
                                                nombre = nombreTareaLimpio,
                                                descripcion = descripcionNuevaTarea.trim()
                                            )
                                        )
                                        siguienteIdTarea += 1
                                        nombreNuevaTarea = ""
                                        descripcionNuevaTarea = ""
                                        if (criterioCumplimiento == CriterioCumplimientoTareas.PARCIAL && minimoTareasTexto.isBlank()) {
                                            minimoTareasTexto = "1"
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Agregar tarea")
                            }
                        }
                    }

                    if (tareasDraft.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            tareasDraft.forEach { tarea ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = (tareasDraft.indexOf(tarea) + 1).toString(),
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tarea.nombre, fontWeight = FontWeight.SemiBold)
                                            if (tarea.descripcion.isNotBlank()) {
                                                Text(
                                                    text = tarea.descripcion,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        IconButton(onClick = {
                                            tareasDraft.remove(tarea)
                                            if (criterioCumplimiento == CriterioCumplimientoTareas.PARCIAL) {
                                                val maximo = tareasDraft.size.coerceAtLeast(1)
                                                val actual = minimoTareasTexto.toIntOrNull() ?: 1
                                                minimoTareasTexto = actual.coerceAtMost(maximo).toString()
                                            }
                                        }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Eliminar tarea",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = guardarHabito,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = nombre.isNotBlank()
            ) {
                Text("Crear hábito")
            }
        }
    }
}

@Composable
private fun SeccionFormulario(
    titulo: String,
    descripcion: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorCategoriaHabito(
    categorias: List<CategoriaHabito>,
    categoriaSeleccionada: CategoriaHabito?,
    onCategoriaSeleccionada: (CategoriaHabito?) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = !expandido }
    ) {
        OutlinedTextField(
            value = categoriaSeleccionada?.nombre ?: "Sin categoría",
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoría") },
            leadingIcon = {
                if (categoriaSeleccionada == null) {
                    Icon(Icons.Default.LabelOff, contentDescription = null, tint = Color.Gray)
                } else {
                    Icon(
                        imageVector = obtenerIcono(categoriaSeleccionada.icono),
                        contentDescription = null,
                        tint = colorCategoria(categoriaSeleccionada)
                    )
                }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            DropdownMenuItem(
                text = {
                    Column {
                        Text("Sin categoría")
                        Text(
                            text = "El hábito usará icono y color por defecto.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                onClick = {
                    onCategoriaSeleccionada(null)
                    expandido = false
                },
                leadingIcon = {
                    Icon(Icons.Default.LabelOff, contentDescription = null, tint = Color.Gray)
                }
            )

            categorias.forEach { categoria ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(categoria.nombre)
                            Text(
                                text = "Icono y color heredados de la categoría",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    onClick = {
                        onCategoriaSeleccionada(categoria)
                        expandido = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = obtenerIcono(categoria.icono),
                            contentDescription = null,
                            tint = colorCategoria(categoria)
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorTipoObjetivo(
    tipoObjetivo: TipoObjetivoHabito,
    onTipoObjetivoSeleccionado: (TipoObjetivoHabito) -> Unit,
    enabled: Boolean
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { if (enabled) expandido = !expandido }
    ) {
        OutlinedTextField(
            value = tipoObjetivo.toLabel(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Tipo de frecuencia") },
            leadingIcon = { Icon(Icons.Default.Repeat, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = enabled && expandido,
            onDismissRequest = { expandido = false }
        ) {
            TipoObjetivoHabito.values().forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion.toLabel()) },
                    onClick = {
                        onTipoObjetivoSeleccionado(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorFrecuenciaHabito(
    frecuenciaActual: FrecuenciaHabito,
    onFrecuenciaSeleccionada: (FrecuenciaHabito) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = !expandido }
    ) {
        OutlinedTextField(
            value = frecuenciaActual.toLabel(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Periodo de tiempo") },
            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            FrecuenciaHabito.values().forEach { frecuencia ->
                DropdownMenuItem(
                    text = { Text(frecuencia.toPeriodoLabel()) },
                    onClick = {
                        onFrecuenciaSeleccionada(frecuencia)
                        expandido = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorCriterioCumplimiento(
    criterio: CriterioCumplimientoTareas,
    onCriterioSeleccionado: (CriterioCumplimientoTareas) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = !expandido }
    ) {
        OutlinedTextField(
            value = criterio.toLabel(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Criterio de cumplimiento") },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) }
        )

        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            CriterioCumplimientoTareas.values().forEach { opcion ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(opcion.toLabel())
                            Text(
                                text = if (opcion == CriterioCumplimientoTareas.TODAS) {
                                    "El hábito solo cuenta si completas todas las tareas."
                                } else {
                                    "Se completa al alcanzar un mínimo de tareas."
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    onClick = {
                        onCriterioSeleccionado(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TimePickerField(
    hora: LocalTime,
    onHoraSeleccionada: (LocalTime) -> Unit
) {
    val context = LocalContext.current

    OutlinedButton(
        onClick = {
            val picker = TimePickerDialog(
                context,
                { _, h, m -> onHoraSeleccionada(LocalTime.of(h, m)) },
                hora.hour,
                hora.minute,
                true
            )
            picker.show()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Hora del recordatorio: ${hora.format(DateTimeFormatter.ofPattern("HH:mm"))}")
    }
}

private fun FrecuenciaHabito.toLabel(): String = when (this) {
    FrecuenciaHabito.DIARIA -> "Diaria"
    FrecuenciaHabito.SEMANAL -> "Semanal"
    FrecuenciaHabito.MENSUAL -> "Mensual"
}

private fun FrecuenciaHabito.toPeriodoLabel(): String = when (this) {
    FrecuenciaHabito.DIARIA -> "Por día"
    FrecuenciaHabito.SEMANAL -> "Por semana"
    FrecuenciaHabito.MENSUAL -> "Por mes"
}

private fun TipoObjetivoHabito.toLabel(): String = when (this) {
    TipoObjetivoHabito.FRECUENCIA -> "Frecuencia"
    TipoObjetivoHabito.CUANTITATIVO -> "Cuantitativo"
}

private fun CriterioCumplimientoTareas.toLabel(): String = when (this) {
    CriterioCumplimientoTareas.TODAS -> "Todas las tareas"
    CriterioCumplimientoTareas.PARCIAL -> "Cumplimiento parcial"
}

private fun etiquetaFrecuenciaSimple(frecuencia: FrecuenciaHabito): String = when (frecuencia) {
    FrecuenciaHabito.DIARIA -> "Veces por día"
    FrecuenciaHabito.SEMANAL -> "Veces por semana"
    FrecuenciaHabito.MENSUAL -> "Veces por mes"
}

private fun colorCategoria(categoria: CategoriaHabito): Color {
    return try {
        Color(categoria.color.toColorInt())
    } catch (_: Exception) {
        obtenerColorIcono(categoria.icono)
    }
}
