package com.example.mistareasapp.ui.screens

import androidx.activity.compose.BackHandler
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mistareasapp.ui.components.BotonSelectorDato
import com.example.mistareasapp.ui.components.SelectorPrioridad
import com.example.mistareasapp.data.Tarea
import com.example.mistareasapp.data.Prioridad
import com.example.mistareasapp.data.Categoria
import com.example.mistareasapp.viewmodel.TareasViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.material3.rememberDatePickerState
import com.example.mistareasapp.ui.components.obtenerIcono
import com.example.mistareasapp.ui.components.obtenerColorIcono

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEditarTarea(navController: NavController, tareaId: Int, viewModel: TareasViewModel) {
    // 1. Estados de los campos de la tarea
     var fechaCreacion by remember { mutableStateOf(0L) }

    // NUEVOS ESTADOS PARA REPETICIÓN
    val opcionesRepeticion = listOf("Sin repetición", "Una vez al día", "Una vez a la semana", "Una vez al mes", "Una vez al año")
    var repeticionSeleccionada by remember { mutableStateOf(opcionesRepeticion[0]) }
    var menuRepeticionExpandido by remember { mutableStateOf(false) }

    // 2. Estados para Categoría
    var categoriaSeleccionada by remember { mutableStateOf<Categoria?>(null) }
    var menuExpandido by remember { mutableStateOf(false) }

    // 3. Datos desde el ViewModel
    val listaCategorias by viewModel.todasLasCategorias.collectAsState(initial = emptyList())
    val tareaDb by viewModel.obtenerTareaPorId(tareaId).collectAsState(initial = null)
    var nombre by remember(tareaDb) { mutableStateOf(tareaDb?.titulo ?: "") }
    var descripcion by remember(tareaDb) { mutableStateOf(tareaDb?.descripcion ?: "") }
    var prioridad by remember(tareaDb) { mutableStateOf(tareaDb?.prioridad ?: Prioridad.MEDIA) }
    var estaCompletada by remember(tareaDb) { mutableStateOf(tareaDb?.estaCompletada ?: false) }
    var fechaLimite by remember(tareaDb) { mutableStateOf(tareaDb?.fechaLimite) }
    var horaLimite by remember(tareaDb) { mutableStateOf(tareaDb?.horaLimite) }
    var tareaOriginal by remember { mutableStateOf<Tarea?>(null) }

    // 4. Estados de UI
    var mostrarDialogoAlerta by remember { mutableStateOf(false) }
    var mostrarCalendario by remember { mutableStateOf(false) }
    val contexto = LocalContext.current
    // Busca esta línea y cámbiala:

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = remember(tareaOriginal) {
            fechaLimite?.atStartOfDay(java.time.ZoneId.systemDefault())
                ?.toInstant()
                ?.toEpochMilli()
        }
    )


    // Carga inicial de la tarea
    // Dentro de LaunchedEffect(tareaDb?.id) en PantallaEditarTarea.kt
    LaunchedEffect(tareaDb) {
        tareaDb?.let { tareaEncontrada ->
            // Solo inicializamos si es la primera vez que entramos (tareaOriginal es null)
            if (tareaOriginal == null) {
                // 1. Guardamos la referencia original para detectar cambios
                tareaOriginal = tareaEncontrada

                // 2. Campos fijos
                fechaCreacion = tareaEncontrada.fechaCreacion
                repeticionSeleccionada = tareaEncontrada.repeticion

                // 3. Lógica de búsqueda para la Categoría
                // Buscamos el objeto Categoria que coincida con el nombre guardado en la tarea
                categoriaSeleccionada = listaCategorias.find { it.titulo == tareaEncontrada.categoria }

                // LOG DE CONTROL: Esto confirmará en consola que los datos han llegado
                println("DEBUG: LaunchedEffect completado. Fecha encontrada: ${tareaEncontrada.fechaLimite}")
            }
        }
    }

    val tieneCambios = tareaOriginal != null && (
            nombre != tareaOriginal?.titulo ||
                    descripcion != (tareaOriginal?.descripcion ?: "") ||
                    prioridad != tareaOriginal?.prioridad ||
                    estaCompletada != tareaOriginal?.estaCompletada ||
                    fechaLimite != tareaOriginal?.fechaLimite ||
                    horaLimite != tareaOriginal?.horaLimite ||
                    repeticionSeleccionada != tareaOriginal?.repeticion || // COMPARACIÓN REPETICIÓN
                    categoriaSeleccionada?.titulo != tareaOriginal?.categoria
            )

    val mostrarRelojNativo = {
        val calendario = java.util.Calendar.getInstance()
        val horaActual = horaLimite?.hour ?: calendario.get(java.util.Calendar.HOUR_OF_DAY)
        val minutoActual = horaLimite?.minute ?: calendario.get(java.util.Calendar.MINUTE)

        TimePickerDialog(contexto, { _, hora, minuto ->
            horaLimite = LocalTime.of(hora, minuto)
        }, horaActual, minutoActual, true).show()
    }

    BackHandler(enabled = tieneCambios) {
        mostrarDialogoAlerta = true
    }

    // --- DIÁLOGOS ---
    if (mostrarDialogoAlerta) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoAlerta = false },
            confirmButton = {
                TextButton(onClick = { navController.popBackStack() }) { Text("Descartar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoAlerta = false }) { Text("Cancelar") }
            },
            title = { Text("¿Descartar cambios?") },
            text = { Text("Si sales ahora, perderás las modificaciones realizadas.") }
        )
    }

    if (mostrarCalendario) {
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    // 1. Obtenemos la selección del calendario
                    datePickerState.selectedDateMillis?.let { millis ->
                        // 2. 🔥 ACTUALIZAMOS la variable que usa el botón para pintar el texto
                        fechaLimite = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    mostrarCalendario = false
                }) { Text("Aceptar") }
            },
            // ... (resto del código del diálogo)
        ) {
            DatePicker(state = datePickerState)
        }
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EDITAR TAREA", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (tieneCambios) mostrarDialogoAlerta = true else navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Cambiamos el icono circular por un TextButton para unificar con "Nueva Tarea"
                    TextButton(
                        onClick = {
                            val tareaEditada = Tarea(
                                id = tareaId,
                                titulo = nombre,
                                descripcion = descripcion.ifBlank { null },
                                estaCompletada = estaCompletada,
                                prioridad = prioridad,
                                fechaCreacion = fechaCreacion,
                                fechaLimite = fechaLimite,
                                horaLimite = if (fechaLimite == null) null else horaLimite,
                                categoria = categoriaSeleccionada?.titulo,
                                // GUARDAMOS REPETICIÓN
                                repeticion = if (fechaLimite == null) "Sin repetición" else repeticionSeleccionada
                            )
                            viewModel.actualizar(tareaEditada)
                            navController.popBackStack()
                        },
                        enabled = nombre.isNotBlank()
                    ) {
                        Text(
                            text = "GUARDAR",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            // Usamos un color que resalte o el color primario
                            color = if (nombre.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .imePadding() // <--- Añade esto para que el teclado no oculte los campos
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Textos
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            // 2. Categoría
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Categoría", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(
                    expanded = menuExpandido,
                    onExpandedChange = { menuExpandido = !menuExpandido }
                ) {
                    OutlinedTextField(
                        value = categoriaSeleccionada?.titulo ?: "Sin categoría",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            val iconoAMostrar = categoriaSeleccionada?.icono ?: "list"
                            Icon(
                                imageVector = obtenerIcono(iconoAMostrar),
                                contentDescription = null,
                                tint = obtenerColorIcono(iconoAMostrar)
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpandido) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = menuExpandido,
                        onDismissRequest = { menuExpandido = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sin categoría") },
                            onClick = { categoriaSeleccionada = null; menuExpandido = false },
                            leadingIcon = { Icon(Icons.Default.LabelOff, null) }
                        )
                        listaCategorias.filter { it.activa }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.titulo) },
                                onClick = { categoriaSeleccionada = cat; menuExpandido = false },
                                leadingIcon = {
                                    Icon(
                                        imageVector = obtenerIcono(cat.icono),
                                        contentDescription = null,
                                        tint = obtenerColorIcono(cat.icono)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 3. Prioridad
            SelectorPrioridad(prioridadSeleccionada = prioridad, onPrioridadCambiada = { prioridad = it })

            // 4. Estado completada
            /*Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tarea completada", style = MaterialTheme.typography.titleMedium)
                Switch(checked = estaCompletada, onCheckedChange = { estaCompletada = it })
            }*/

            // 5. Vencimiento y Repetición
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Vencimiento", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                // --- FILA DE BOTONES (Igual que en Nueva Tarea) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Selector de Fecha
                    val textoFecha = fechaLimite?.format(DateTimeFormatter.ofPattern("dd/MM/yy")) ?: "Fecha"
                    BotonSelectorDato(
                        label = textoFecha,
                        icon = Icons.Default.DateRange,
                        onClick = { mostrarCalendario = true },
                        modifier = Modifier.weight(1f)
                    )

                    // Selector de Hora
                    val textoHora = horaLimite?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Hora"
                    BotonSelectorDato(
                        label = textoHora,
                        icon = Icons.Default.AccessTime,
                        onClick = { if (fechaLimite != null) mostrarRelojNativo() },
                        modifier = Modifier.weight(1f),
                        enabled = fechaLimite != null
                    )
                }

                // --- FILA DE TEXTOS "QUITAR" (Justo debajo) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Quitar Fecha
                    if (fechaLimite != null) {
                        TextButton(
                            onClick = {
                                fechaLimite = null
                                horaLimite = null
                                repeticionSeleccionada = opcionesRepeticion[0]
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Quitar fecha", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        // Espaciador para mantener la hora a la derecha si no hay fecha
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Quitar Hora
                    if (horaLimite != null) {
                        TextButton(
                            onClick = { horaLimite = null },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Quitar hora", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // --- SELECTOR DE REPETICIÓN (Solo si hay fecha) ---
                if (fechaLimite != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Repetición", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = menuRepeticionExpandido,
                            onDismissRequest = { menuRepeticionExpandido = false }
                        ) {
                            opcionesRepeticion.forEach { opcion ->
                                DropdownMenuItem(
                                    text = { Text(opcion) },
                                    onClick = {
                                        repeticionSeleccionada = opcion
                                        menuRepeticionExpandido = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



