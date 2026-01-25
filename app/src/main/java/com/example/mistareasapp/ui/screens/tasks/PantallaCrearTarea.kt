package com.example.mistareasapp.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LabelOff
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mistareasapp.data.tasks.Categoria
import com.example.mistareasapp.data.tasks.Prioridad
import com.example.mistareasapp.data.tasks.Tarea
import com.example.mistareasapp.data.tasks.TareasDatabase
import com.example.mistareasapp.ui.components.tasks.BotonSelectorDato
import com.example.mistareasapp.ui.components.tasks.SelectorPrioridad
import com.example.mistareasapp.ui.components.tasks.obtenerColorIcono
import com.example.mistareasapp.ui.components.tasks.obtenerIcono
import com.example.mistareasapp.viewmodel.Tasks.TareasViewModel
import com.example.mistareasapp.viewmodel.Tasks.TareasViewModelFactory
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun TimePickerDialog(
    title: String = "Seleccionar Hora",
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = { content() },
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCrearTarea(navController: NavController) {
    val context = LocalContext.current
    val db = TareasDatabase.Companion.getDatabase(context)
    val factory = TareasViewModelFactory(db.tareaDao(), db.categoriaDao())
    val viewModel: TareasViewModel = viewModel(factory = factory)
    val scope = rememberCoroutineScope()

    // ESTADOS FORMULARIO
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var prioridad by remember { mutableStateOf(Prioridad.MEDIA) }
    var fechaLimite by remember { mutableStateOf<LocalDate?>(null) }
    var horaLimite by remember { mutableStateOf<LocalTime?>(null) }

    // ESTADOS CATEGORÍA
    var categoriaSeleccionada by remember { mutableStateOf<Categoria?>(null) }
    var menuCategoriasExpandido by remember { mutableStateOf(false) }
    val listaCategorias by viewModel.todasLasCategorias.collectAsState(initial = emptyList())

    // ESTADOS REPETICIÓN
    val opcionesRepeticion = listOf("Sin repetición", "Una vez al día", "Una vez a la semana", "Una vez al mes", "Una vez al año")
    var repeticionSeleccionada by remember { mutableStateOf(opcionesRepeticion[0]) }
    var menuRepeticionExpandido by remember { mutableStateOf(false) }

    // DIÁLOGOS FECHA/HORA
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState(is24Hour = true)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        fechaLimite = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDatePicker = false
                }) { Text("Cancelar") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    horaLimite = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTimePicker = false
                }) { Text("Cancelar") }
            }
        ) { TimePicker(state = timePickerState) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NUEVA TAREA", fontWeight = FontWeight.Companion.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (titulo.isNotBlank()) {
                                val tituloFormateado = titulo.trim().replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                }
                                val nuevaTarea = Tarea(
                                    titulo = tituloFormateado,
                                    descripcion = descripcion.ifBlank { null },
                                    prioridad = prioridad,
                                    fechaLimite = fechaLimite,
                                    horaLimite = if (fechaLimite == null) null else horaLimite,
                                    categoria = categoriaSeleccionada?.titulo,
                                    repeticion = if (fechaLimite == null) "Sin repetición" else repeticionSeleccionada
                                )
                                scope.launch { viewModel.insertar(nuevaTarea) }
                                navController.popBackStack()
                            }
                        },
                        enabled = titulo.isNotBlank()
                    ) { Text("GUARDAR", fontWeight = FontWeight.Companion.ExtraBold) }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding() // <--- Añade esto para que el teclado no oculte los campos
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Títulos
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("¿Qué hay que hacer?*") },
                    modifier = Modifier.Companion.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.Companion.fillMaxWidth(),
                    minLines = 3
                )
            }

            // --- CATEGORÍA (RESTAURADO COMPLETO) ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Categoría",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Companion.SemiBold
                )
                ExposedDropdownMenuBox(
                    expanded = menuCategoriasExpandido,
                    onExpandedChange = { menuCategoriasExpandido = !menuCategoriasExpandido }
                ) {
                    OutlinedTextField(
                        value = categoriaSeleccionada?.titulo ?: "Sin categoría",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            val nombreIcono = categoriaSeleccionada?.icono ?: "list"
                            Icon(
                                obtenerIcono(nombreIcono),
                                null,
                                tint = obtenerColorIcono(nombreIcono)
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuCategoriasExpandido) },
                        modifier = Modifier.Companion.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = menuCategoriasExpandido,
                        onDismissRequest = { menuCategoriasExpandido = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sin categoría") },
                            onClick = {
                                categoriaSeleccionada = null; menuCategoriasExpandido = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.LabelOff,
                                    null,
                                    tint = Color.Companion.Gray
                                )
                            }
                        )
                        listaCategorias.filter { it.activa }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.titulo) },
                                onClick = {
                                    categoriaSeleccionada = cat; menuCategoriasExpandido = false
                                },
                                leadingIcon = {
                                    Icon(
                                        obtenerIcono(cat.icono),
                                        null,
                                        tint = obtenerColorIcono(cat.icono)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            SelectorPrioridad(
                prioridadSeleccionada = prioridad,
                onPrioridadCambiada = { prioridad = it })

            // --- VENCIMIENTO Y REPETICIÓN ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Vencimiento",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Companion.SemiBold
                )
                Row(
                    modifier = Modifier.Companion.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BotonSelectorDato(
                        label = fechaLimite?.format(DateTimeFormatter.ofPattern("dd/MM/yy"))
                            ?: "Fecha",
                        icon = Icons.Default.DateRange,
                        onClick = { showDatePicker = true },
                        modifier = Modifier.Companion.weight(1f)
                    )
                    BotonSelectorDato(
                        label = horaLimite?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Hora",
                        icon = Icons.Default.AccessTime,
                        onClick = { if (fechaLimite != null) showTimePicker = true },
                        modifier = Modifier.Companion.weight(1f),
                        colorTexto = if (fechaLimite != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.38f
                        )
                    )
                }

                if (fechaLimite != null) {
                    Spacer(modifier = Modifier.Companion.height(8.dp))
                    Text(
                        "Repetición",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Companion.SemiBold
                    )
                    ExposedDropdownMenuBox(
                        expanded = menuRepeticionExpandido,
                        onExpandedChange = { menuRepeticionExpandido = !menuRepeticionExpandido }
                    ) {
                        OutlinedTextField(
                            value = repeticionSeleccionada,
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = { Icon(Icons.Default.Repeat, null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuRepeticionExpandido) },
                            modifier = Modifier.Companion.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = menuRepeticionExpandido,
                            onDismissRequest = { menuRepeticionExpandido = false }
                        ) {
                            opcionesRepeticion.forEach { opcion ->
                                DropdownMenuItem(
                                    text = { Text(opcion) },
                                    onClick = {
                                        repeticionSeleccionada = opcion; menuRepeticionExpandido =
                                        false
                                    }
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            fechaLimite = null
                            horaLimite = null
                            repeticionSeleccionada = opcionesRepeticion[0]
                        },
                        modifier = Modifier.Companion.align(Alignment.Companion.End)
                    ) {
                        Icon(Icons.Default.Clear, null, modifier = Modifier.Companion.size(16.dp))
                        Spacer(Modifier.Companion.width(4.dp))
                        Text("Quitar fecha")
                    }
                }
            }
        }
    }
}