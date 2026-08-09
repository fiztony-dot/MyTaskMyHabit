package com.example.mistareasapp.ui.screens.habits

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mistareasapp.EMOJIS_HABITO
import com.example.mistareasapp.data.habits.*
import com.example.mistareasapp.iconoAEmoji
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarHabitoScreen(
    habitoId: Long,
    viewModel: HabitosViewModel,
    onGuardar: () -> Unit
) {
    val context = LocalContext.current
    val todosLosHabitos by viewModel.todosLosHabitos.collectAsState(initial = emptyList())
    val habito = todosLosHabitos.firstOrNull { it.id == habitoId }
    val categorias by viewModel.categoriasHabitos.collectAsState(initial = emptyList())
    val tareasDB by viewModel.obtenerTareasDeHabito(habitoId).collectAsState(initial = emptyList())

    if (habito == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var nombre by remember(habito.id) { mutableStateOf(habito.nombre) }
    var descripcion by remember(habito.id) { mutableStateOf(habito.descripcion ?: "") }
    var categoriaSeleccionadaId by remember(habito.id) { mutableStateOf<Long?>(habito.categoriaId.takeIf { it != 0L }) }
    val categoriaSeleccionada = categorias.firstOrNull { it.id == categoriaSeleccionadaId }

    var fechaInicio by remember(habito.id) { mutableStateOf(habito.fechaInicio) }
    var mostrarDatePicker by remember { mutableStateOf(false) }

    var frecuencia by remember(habito.id) { mutableStateOf(habito.frecuencia) }
    var tipoObjetivo by remember(habito.id) { mutableStateOf(habito.tipoObjetivo) }
    var repeticionesPeriodo by remember(habito.id) { mutableStateOf(habito.vecesPorDia.toString()) }
    var cantidadObjetivo by remember(habito.id) { mutableStateOf((habito.objetivoValor ?: habito.vecesPorDia).toString()) }
    var unidadMedida by remember(habito.id) { mutableStateOf(habito.unidad ?: "") }
    var objetivoRachaSemanas by remember(habito.id) { mutableStateOf((habito.objetivoRachaSemanas ?: 4).toString()) }

    var recordatoriosActivos by remember(habito.id) { mutableStateOf(habito.recordatoriosActivos) }
    var horaRecordatorio by remember(habito.id) { mutableStateOf(habito.horaRecordatorio ?: LocalTime.of(9, 0)) }

    var esCompuestoPorTareas by remember(habito.id) { mutableStateOf(habito.esCompuestoPorTareas) }
    var criterioCumplimiento by remember(habito.id) { mutableStateOf(habito.criterioCumplimientoTareas ?: CriterioCumplimientoTareas.TODAS) }
    var minimoTareasTexto by remember(habito.id) { mutableStateOf((habito.minimoTareasCumplimiento ?: 1).toString()) }

    // Gestión de tareas: separamos las existentes en DB de las nuevas borradores
    val tareasAEliminar = remember { mutableStateListOf<TareaHabito>() }
    val tareasNuevas = remember { mutableStateListOf<TareaHabitoDraft>() }
    var siguienteIdDraft by remember { mutableStateOf(0) }
    var dificultad by remember(habito.id) { mutableStateOf(habito.dificultad) }
    var usarPorcentajeDias by remember(habito.id) { mutableStateOf(habito.objetivoPorcentajeDias != null) }
    var porcentajeDiasTexto by remember(habito.id) { mutableStateOf(habito.objetivoPorcentajeDias?.toString() ?: "60") }
    var diasSemanaSeleccionados by remember(habito.id) {
        mutableStateOf(
            habito.diasSemana?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: emptySet()
        )
    }
    var tipoMedicion by remember(habito.id) { mutableStateOf(habito.tipoMedicion) }
    var limiteMaximoTexto by remember(habito.id) { mutableStateOf(habito.limiteMaximo?.toString() ?: "") }
    var unidadLimite by remember(habito.id) { mutableStateOf(habito.unidad ?: "") }
    var tramosLimiteEdicion by remember(habito.id) {
        mutableStateOf(
            habito.tramosLimite?.let { com.example.mistareasapp.data.habits.parsearTramos(it) }
                ?: if (habito.tipoObjetivo == TipoObjetivoHabito.LIMITE_MAXIMO && (habito.limiteMaximo ?: 0.0) > 0.0)
                    com.example.mistareasapp.data.habits.tramosDefault(habito.limiteMaximo!!)
                else emptyList()
        )
    }
    var ubeActivo by remember(habito.id) { mutableStateOf(habito.ubeActivo) }
    var nombreNuevaTarea by remember { mutableStateOf("") }
    var descripcionNuevaTarea by remember { mutableStateOf("") }
    var mostrarDialogoFechaVigencia by remember { mutableStateOf(false) }
    var habitoPendiente by remember { mutableStateOf<Habito?>(null) }

    val tareasVisibles = tareasDB.filter { it !in tareasAEliminar }
    val totalTareas = tareasVisibles.size + tareasNuevas.size

    val guardar = {
        when {
            nombre.isBlank() -> Toast.makeText(context, "El nombre del hábito es obligatorio", Toast.LENGTH_SHORT).show()
            !esCompuestoPorTareas && tipoObjetivo == TipoObjetivoHabito.FRECUENCIA && (repeticionesPeriodo.toIntOrNull() ?: 0) <= 0 ->
                Toast.makeText(context, "Indica una frecuencia válida", Toast.LENGTH_SHORT).show()
            !esCompuestoPorTareas && tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO && (cantidadObjetivo.toIntOrNull() ?: 0) <= 0 ->
                Toast.makeText(context, "La cantidad objetivo debe ser mayor que 0", Toast.LENGTH_SHORT).show()
            !esCompuestoPorTareas && tipoObjetivo == TipoObjetivoHabito.LIMITE_MAXIMO && (limiteMaximoTexto.toDoubleOrNull() ?: 0.0) <= 0.0 ->
                Toast.makeText(context, "El límite máximo debe ser mayor que 0", Toast.LENGTH_SHORT).show()
            esCompuestoPorTareas && totalTareas == 0 ->
                Toast.makeText(context, "Añade al menos una tarea al hábito", Toast.LENGTH_SHORT).show()
            else -> {
                val minimoTareas = when {
                    !esCompuestoPorTareas -> null
                    criterioCumplimiento == CriterioCumplimientoTareas.TODAS -> totalTareas.coerceAtLeast(1)
                    else -> (minimoTareasTexto.toIntOrNull() ?: 1).coerceIn(1, totalTareas.coerceAtLeast(1))
                }
                val objetivoFrecuencia = if (esCompuestoPorTareas) minimoTareas ?: 1
                else (repeticionesPeriodo.toIntOrNull() ?: 1).coerceAtLeast(1)

                val esPeriodico = frecuencia != FrecuenciaHabito.DIARIA && !esCompuestoPorTareas && tipoObjetivo == TipoObjetivoHabito.FRECUENCIA
                val pctDias = if (esPeriodico && usarPorcentajeDias) porcentajeDiasTexto.toIntOrNull()?.coerceIn(1, 100) else null
                val diasSemanaStr = if (
                    frecuencia == FrecuenciaHabito.DIARIA &&
                    diasSemanaSeleccionados.isNotEmpty() &&
                    diasSemanaSeleccionados.size < 7
                ) diasSemanaSeleccionados.sorted().joinToString(",") else null

                val habitoActualizado = habito.copy(
                    fechaModificacion = LocalDate.now(),
                    nombre = nombre.trim(),
                    descripcion = descripcion.trim().ifBlank { null },
                    categoriaId = categoriaSeleccionada?.id ?: 0L,
                    fechaInicio = fechaInicio,
                    frecuencia = frecuencia,
                    tipoObjetivo = if (esCompuestoPorTareas) TipoObjetivoHabito.FRECUENCIA else tipoObjetivo,
                    vecesPorDia = objetivoFrecuencia,
                    objetivoValor = if (!esCompuestoPorTareas && tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO) cantidadObjetivo.toIntOrNull() else null,
                    unidad = when {
                        !esCompuestoPorTareas && tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO -> unidadMedida.trim().ifBlank { null }
                        !esCompuestoPorTareas && tipoObjetivo == TipoObjetivoHabito.LIMITE_MAXIMO -> unidadLimite.trim().ifBlank { null }
                        else -> null
                    },
                    limiteMaximo = if (!esCompuestoPorTareas && tipoObjetivo == TipoObjetivoHabito.LIMITE_MAXIMO) limiteMaximoTexto.toDoubleOrNull() else null,
                    tramosLimite = if (!esCompuestoPorTareas && tipoObjetivo == TipoObjetivoHabito.LIMITE_MAXIMO && tramosLimiteEdicion.isNotEmpty())
                        com.example.mistareasapp.data.habits.serializarTramos(tramosLimiteEdicion) else null,
                    ubeActivo = tipoObjetivo == TipoObjetivoHabito.LIMITE_MAXIMO && ubeActivo,
                    esCompuestoPorTareas = esCompuestoPorTareas,
                    criterioCumplimientoTareas = if (esCompuestoPorTareas) criterioCumplimiento else CriterioCumplimientoTareas.TODAS,
                    minimoTareasCumplimiento = if (esCompuestoPorTareas && criterioCumplimiento == CriterioCumplimientoTareas.PARCIAL) minimoTareas else null,
                    objetivoRachaSemanas = objetivoRachaSemanas.toIntOrNull() ?: 4,
                    recordatoriosActivos = recordatoriosActivos,
                    horaRecordatorio = if (recordatoriosActivos) horaRecordatorio else null,
                    icono = categoriaSeleccionada?.icono ?: habito.icono,
                    colorHex = categoriaSeleccionada?.color ?: habito.colorHex,
                    objetivoPorcentajeDias = pctDias,
                    diasSemana = diasSemanaStr,
                    tipoMedicion = tipoMedicion,
                    dificultad = dificultad
                )
                if (habito.tieneDefinicionDistintaA(habitoActualizado)) {
                    habitoPendiente = habitoActualizado
                    mostrarDialogoFechaVigencia = true
                } else {
                    viewModel.actualizarHabito(habitoActualizado)
                    tareasAEliminar.forEach { viewModel.eliminarTareaHabito(it) }
                    tareasNuevas.forEach { draft ->
                        viewModel.insertarTareaHabito(TareaHabito(habitoId = habitoId, nombre = draft.nombre, descripcion = draft.descripcion.ifBlank { null }))
                    }
                    Toast.makeText(context, "Hábito actualizado", Toast.LENGTH_SHORT).show()
                    onGuardar()
                }
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
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { mostrarDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (mostrarDialogoFechaVigencia) {
        val hoy = LocalDate.now()
        val vigenciaState = rememberDatePickerState(
            initialSelectedDateMillis = hoy.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = {
                mostrarDialogoFechaVigencia = false
                habitoPendiente = null
            },
            confirmButton = {
                TextButton(onClick = {
                    val pendiente = habitoPendiente
                    if (pendiente != null) {
                        val fechaVigencia = vigenciaState.selectedDateMillis
                            ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                            ?: hoy
                        viewModel.actualizarHabito(pendiente)
                        viewModel.guardarVersionSiCambio(habito, pendiente, fechaVigencia)
                        tareasAEliminar.forEach { viewModel.eliminarTareaHabito(it) }
                        tareasNuevas.forEach { draft ->
                            viewModel.insertarTareaHabito(TareaHabito(habitoId = habitoId, nombre = draft.nombre, descripcion = draft.descripcion.ifBlank { null }))
                        }
                        Toast.makeText(context, "Hábito actualizado", Toast.LENGTH_SHORT).show()
                        onGuardar()
                    }
                    mostrarDialogoFechaVigencia = false
                    habitoPendiente = null
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoFechaVigencia = false
                    habitoPendiente = null
                }) { Text("Cancelar") }
            }
        ) {
            DatePicker(
                state = vigenciaState,
                title = {
                    Text(
                        "¿Desde qué fecha entra en vigor este cambio?",
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                    )
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar hábito") },
                navigationIcon = {
                    IconButton(onClick = onGuardar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = guardar, enabled = nombre.isNotBlank()) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SeccionFormulario("Identidad", "Nombre, descripción y categoría visual del hábito.") {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del hábito*") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                SelectorCategoriaHabito(
                    categorias = categorias,
                    categoriaSeleccionada = categoriaSeleccionada,
                    onCategoriaSeleccionada = { categoriaSeleccionadaId = it?.id }
                )

                SelectorDificultad(
                    dificultad = dificultad,
                    onDificultadSeleccionada = { dificultad = it }
                )
            }

            SeccionFormulario("Planificación", "Define fecha de inicio, periodo y cómo se mide el cumplimiento.") {
                OutlinedButton(onClick = { mostrarDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DateRange, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Inicio: ${fechaInicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
                }
                SelectorTipoObjetivo(tipoObjetivo = tipoObjetivo, onTipoObjetivoSeleccionado = { tipoObjetivo = it }, enabled = !esCompuestoPorTareas)
                SelectorFrecuenciaHabito(frecuenciaActual = frecuencia, onFrecuenciaSeleccionada = { frecuencia = it })

                // Tipo de medición del cumplimiento
                SelectorTipoMedicion(
                    tipoMedicion = tipoMedicion,
                    onTipoSeleccionado = { tipoMedicion = it }
                )

                // Selector de días específicos (solo para hábitos DIARIOS)
                if (frecuencia == FrecuenciaHabito.DIARIA) {
                    SelectorDiasSemana(
                        seleccionados = diasSemanaSeleccionados,
                        onSeleccionCambiada = { diasSemanaSeleccionados = it }
                    )
                }

                if (!esCompuestoPorTareas && tipoObjetivo == TipoObjetivoHabito.FRECUENCIA) {
                    if (frecuencia != FrecuenciaHabito.DIARIA) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Definir objetivo por % de días", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    "Ej: 60% de días del periodo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = usarPorcentajeDias, onCheckedChange = { usarPorcentajeDias = it })
                        }
                    }
                    if (!usarPorcentajeDias || frecuencia == FrecuenciaHabito.DIARIA) {
                        OutlinedTextField(
                            value = repeticionesPeriodo,
                            onValueChange = { repeticionesPeriodo = it.filter(Char::isDigit) },
                            label = { Text(etiquetaFrecuenciaSimple(frecuencia)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    } else {
                        OutlinedTextField(
                            value = porcentajeDiasTexto,
                            onValueChange = { v -> porcentajeDiasTexto = v.filter(Char::isDigit).take(3) },
                            label = { Text("% de días del periodo (1-100)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            suffix = { Text("%") }
                        )
                    }
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (!esCompuestoPorTareas && tipoObjetivo == TipoObjetivoHabito.LIMITE_MAXIMO) {
                    SeccionLimiteMaximo(
                        limiteMaximoTexto = limiteMaximoTexto,
                        onLimiteMaximoChange = { nuevo ->
                            limiteMaximoTexto = nuevo
                            val lim = nuevo.toDoubleOrNull() ?: 0.0
                            if (lim > 0.0 && tramosLimiteEdicion.isEmpty()) {
                                tramosLimiteEdicion = com.example.mistareasapp.data.habits.tramosDefault(lim)
                            }
                        },
                        unidad = unidadLimite,
                        onUnidadChange = { unidadLimite = it },
                        tramos = tramosLimiteEdicion,
                        onTramosChange = { tramosLimiteEdicion = it },
                        ubeActivo = ubeActivo,
                        onUbeActivoChange = { ubeActivo = it }
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

            SeccionFormulario("Tareas", "Descompón el hábito en pasos concretos.") {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Este hábito se compone de tareas", fontWeight = FontWeight.SemiBold)
                    }
                    Switch(checked = esCompuestoPorTareas, onCheckedChange = {
                        esCompuestoPorTareas = it
                        if (it && tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO) tipoObjetivo = TipoObjetivoHabito.FRECUENCIA
                    })
                }

                if (esCompuestoPorTareas) {
                    SelectorCriterioCumplimiento(criterio = criterioCumplimiento, onCriterioSeleccionado = { criterioCumplimiento = it })

                    if (criterioCumplimiento == CriterioCumplimientoTareas.PARCIAL) {
                        OutlinedTextField(
                            value = minimoTareasTexto,
                            onValueChange = { minimoTareasTexto = it.filter(Char::isDigit) },
                            label = { Text("Mínimo de tareas completadas") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    // Tareas existentes
                    if (tareasVisibles.isNotEmpty()) {
                        Text("Tareas actuales", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        tareasVisibles.forEach { tarea ->
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tarea.nombre, fontWeight = FontWeight.SemiBold)
                                        if (!tarea.descripcion.isNullOrBlank()) {
                                            Text(tarea.descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    IconButton(onClick = { tareasAEliminar.add(tarea) }) {
                                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    // Tareas nuevas (borrador)
                    if (tareasNuevas.isNotEmpty()) {
                        Text("Tareas nuevas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        tareasNuevas.forEach { draft ->
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text((tareasVisibles.size + tareasNuevas.indexOf(draft) + 1).toString(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(draft.nombre, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { tareasNuevas.remove(draft) }) {
                                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    // Añadir nueva tarea
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = nombreNuevaTarea, onValueChange = { nombreNuevaTarea = it }, label = { Text("Nombre de la tarea") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = descripcionNuevaTarea, onValueChange = { descripcionNuevaTarea = it }, label = { Text("Descripción (opcional)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedButton(
                                onClick = {
                                    val limpio = nombreNuevaTarea.trim()
                                    if (limpio.isNotBlank()) {
                                        tareasNuevas.add(TareaHabitoDraft(id = siguienteIdDraft, nombre = limpio, descripcion = descripcionNuevaTarea.trim()))
                                        siguienteIdDraft++
                                        nombreNuevaTarea = ""
                                        descripcionNuevaTarea = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Agregar tarea")
                            }
                        }
                    }
                }
            }

            SeccionFormulario("Recordatorio", "Activa un aviso si quieres reforzar el hábito.") {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Activar recordatorio", fontWeight = FontWeight.SemiBold)
                    }
                    Switch(checked = recordatoriosActivos, onCheckedChange = { recordatoriosActivos = it })
                }
                if (recordatoriosActivos) {
                    TimePickerField(hora = horaRecordatorio, onHoraSeleccionada = { horaRecordatorio = it })
                }
            }

            Button(onClick = guardar, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), enabled = nombre.isNotBlank()) {
                Text("Guardar cambios")
            }
        }
    }
}
