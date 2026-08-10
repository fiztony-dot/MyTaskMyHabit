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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LabelOff
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mistareasapp.data.tasks.Categoria
import com.example.mistareasapp.data.tasks.Prioridad
import com.example.mistareasapp.data.tasks.Tarea
import com.example.mistareasapp.ui.components.tasks.BotonSelectorDato
import com.example.mistareasapp.ui.components.tasks.SelectorPrioridad
import com.example.mistareasapp.ui.components.tasks.obtenerColorIcono
import com.example.mistareasapp.ui.components.tasks.obtenerIcono
import com.example.mistareasapp.viewmodel.Tasks.TareasViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.mistareasapp.ui.components.tasks.DateTimePickers


enum class ModoLimiteRepeticion { SIN_LIMITE, HASTA_FECHA, N_VECES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCrearTarea(navController: NavController, viewModel: TareasViewModel = viewModel()) {
    val context = LocalContext.current
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

    // ESTADOS LÍMITE DE REPETICIÓN
    var modoLimite by remember { mutableStateOf(ModoLimiteRepeticion.SIN_LIMITE) }
    var repeticionFin by remember { mutableStateOf<LocalDate?>(null) }
    var repeticionVecesTexto by remember { mutableStateOf("") }
    var showDatePickerFin by remember { mutableStateOf(false) }
    val datePickerFinState = rememberDatePickerState()

    // DIÁLOGOS FECHA/HORA
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState(is24Hour = true)

    // Gestionamos los selectores de Fecha y Hora de vencimiento
    DateTimePickers(
        showDatePicker = showDatePicker,
        showTimePicker = showTimePicker,
        datePickerState = datePickerState,
        timePickerState = timePickerState,
        onDateSelected = { fechaLimite = it },
        onTimeSelected = { horaLimite = it },
        onDismissDate = { showDatePicker = false },
        onDismissTime = { showTimePicker = false }
    )

    // DatePicker para fecha de fin de repetición
    if (showDatePickerFin) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerFin = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerFinState.selectedDateMillis?.let { millis ->
                        repeticionFin = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePickerFin = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePickerFin = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerFinState) }
    }

    Scaffold(
        topBar = {
            CrearTareaTopBar(
                titulo = titulo,
                descripcion = descripcion,
                prioridad = prioridad,
                fechaLimite = fechaLimite,
                horaLimite = horaLimite,
                categoria = categoriaSeleccionada,
                repeticion = repeticionSeleccionada,
                repeticionFin = repeticionFin,
                repeticionVeces = repeticionVecesTexto.trim().toIntOrNull(),
                onBack = { navController.popBackStack() },
                onSave = { nuevaTarea ->
                    scope.launch {
                        viewModel.insertar(nuevaTarea, context)
                        navController.popBackStack()
                    }
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
            // Llamada a la función que pinta la parte de la categoría
            SeccionCategoria(categoriaSeleccionada, listaCategorias, { categoriaSeleccionada = it })

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
                    // Selector de HORA
                    BotonSelectorDato(
                        label = horaLimite?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Hora",
                        icon = Icons.Default.AccessTime,
                        onClick = { showTimePicker = true }, // Quitamos el 'if' de aquí, ahora lo maneja el 'enabled'
                        enabled = fechaLimite != null,       // El botón se desactiva solo si no hay fecha
                        modifier = Modifier.weight(1f),
                        colorTexto = if (fechaLimite != null) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
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

                    // Selector de límite de repetición
                    if (repeticionSeleccionada != opcionesRepeticion[0]) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Límite de repetición", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = modoLimite == ModoLimiteRepeticion.SIN_LIMITE, onClick = { modoLimite = ModoLimiteRepeticion.SIN_LIMITE; repeticionFin = null; repeticionVecesTexto = "" }, label = { Text("Sin límite") })
                            FilterChip(selected = modoLimite == ModoLimiteRepeticion.HASTA_FECHA, onClick = { modoLimite = ModoLimiteRepeticion.HASTA_FECHA; repeticionVecesTexto = "" }, label = { Text("Hasta fecha") })
                            FilterChip(selected = modoLimite == ModoLimiteRepeticion.N_VECES, onClick = { modoLimite = ModoLimiteRepeticion.N_VECES; repeticionFin = null }, label = { Text("N veces") })
                        }
                        when (modoLimite) {
                            ModoLimiteRepeticion.HASTA_FECHA -> {
                                BotonSelectorDato(
                                    label = repeticionFin?.format(DateTimeFormatter.ofPattern("dd/MM/yy")) ?: "Fecha de fin",
                                    icon = Icons.Default.DateRange,
                                    onClick = { showDatePickerFin = true },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            ModoLimiteRepeticion.N_VECES -> {
                                OutlinedTextField(
                                    value = repeticionVecesTexto,
                                    onValueChange = { if (it.all { c -> c.isDigit() }) repeticionVecesTexto = it },
                                    label = { Text("Número de repeticiones") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            else -> {}
                        }
                    }

                    TextButton(
                        onClick = {
                            fechaLimite = null
                            horaLimite = null
                            repeticionSeleccionada = opcionesRepeticion[0]
                            modoLimite = ModoLimiteRepeticion.SIN_LIMITE
                            repeticionFin = null
                            repeticionVecesTexto = ""
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrearTareaTopBar(
    titulo: String,
    descripcion: String,
    prioridad: Prioridad,
    fechaLimite: LocalDate?,
    horaLimite: LocalTime?,
    categoria: Categoria?,
    repeticion: String,
    repeticionFin: LocalDate?,
    repeticionVeces: Int?,
    onBack: () -> Unit,
    onSave: (Tarea) -> Unit
) {
    TopAppBar(
        title = { Text("NUEVA TAREA", fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "Cancelar")
            }
        },
        actions = {
            TextButton(
                enabled = titulo.isNotBlank(),
                onClick = {
                    val tituloFormateado = titulo.trim().replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    }
                    val repFinal = if (fechaLimite == null) "Sin repetición" else repeticion
                    val nuevaTarea = Tarea(
                        titulo = tituloFormateado,
                        descripcion = descripcion.ifBlank { null },
                        prioridad = prioridad,
                        fechaLimite = fechaLimite,
                        horaLimite = if (fechaLimite == null) null else horaLimite,
                        categoria = categoria?.titulo,
                        repeticion = repFinal,
                        repeticionFin = if (repFinal == "Sin repetición") null else repeticionFin,
                        repeticionVeces = if (repFinal == "Sin repetición") null else repeticionVeces
                    )
                    onSave(nuevaTarea)
                }
            ) {
                Text("GUARDAR", fontWeight = FontWeight.ExtraBold)
            }
        }
    )
}


// --- CATEGORÍA (RESTAURADO COMPLETO) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeccionCategoria(
    categoriaSeleccionada: Categoria?,
    listaCategorias: List<Categoria>,
    onCategoriaSelected: (Categoria?) -> Unit
) {
    // El estado del menú ahora vive aquí, solo donde se necesita
    var expandido by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Categoría",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        ExposedDropdownMenuBox(
            expanded = expandido,
            onExpandedChange = { expandido = !expandido }
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
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandido,
                onDismissRequest = { expandido = false }
            ) {
                // Opción: Sin categoría
                DropdownMenuItem(
                    text = { Text("Sin categoría") },
                    onClick = {
                        onCategoriaSelected(null)
                        expandido = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.LabelOff, null, tint = Color.Gray)
                    }
                )
                // Opciones: Lista de categorías de la base de datos
                listaCategorias.filter { it.activa }.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.titulo) },
                        onClick = {
                            onCategoriaSelected(cat)
                            expandido = false
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
}