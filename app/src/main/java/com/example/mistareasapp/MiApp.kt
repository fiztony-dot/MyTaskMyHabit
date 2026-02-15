package com.example.mistareasapp

// --- 1. Android Framework Base y Utilidades ---
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import java.util.Locale

// --- 2. Kotlin Core: Corrutinas y Serialización ---
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

// --- 3. Networking (Ktor Client) ---
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

// --- 4. AndroidX & Lifecycle (Integración con el SO) ---
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

// --- 5. Jetpack Compose: Navegación ---
import androidx.navigation.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*

// --- 6. Jetpack Compose: Animaciones ---
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically

// --- 7. Jetpack Compose: UI, Material Design y Gráficos ---
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// --- 8. Jetpack Compose: Runtime y Estado ---
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// --- 9. Clases del Proyecto (Local) ---
import com.example.mistareasapp.data.*
import com.example.mistareasapp.data.TareasDatabase
import com.example.mistareasapp.ui.components.BarraFiltros
import com.example.mistareasapp.ui.screens.*
import com.example.mistareasapp.ui.theme.MisTareasAppTheme
import com.example.mistareasapp.viewmodel.*



//Estructura de la Respuesta de la IA
@Serializable
data class TareaIA(
    val tarea: String,
    val fecha: String?=null,
    val hora: String? = null,
    val prioridad: String? = null
)
//Constantes de Configuración y Credenciales
object DatosIA {
    const val MI_LLAVE = "AIzaSyCcZTsOCkF6dpM-eTZ-DstBsCdGRq_YWcg"
}

@Composable
fun MiBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(tonalElevation = 0.dp) {
        NavigationBarItem(
            label = { Text("Tareas") },
            icon = { Icon(Icons.Filled.List, contentDescription = null) },
            selected = currentRoute == Rutas.PantallaTareas.ruta,
            onClick = {
                navController.navigate(Rutas.PantallaTareas.ruta) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            label = { Text("Hábitos") },
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
            selected = currentRoute == Rutas.PantallaHabitos.ruta,
            onClick = {
                navController.navigate(Rutas.PantallaHabitos.ruta) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

fun obtenerTitulo(ruta: String?): String {
    return when (ruta) {
        Rutas.PantallaTareas.ruta -> "Mis Tareas"
        Rutas.PantallaHabitos.ruta -> "Mis Hábitos"
        Rutas.PantallaCrearTarea.ruta -> "Nueva Tarea"
        else -> "Gestión de Tareas"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisTareasApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- 1. PRIMERO CREAMOS LA BASE DE DATOS Y EL VIEWMODEL ---
    // (Esto tiene que ir arriba para que 'listaTareas' lo pueda usar)
    val db = TareasDatabase.getDatabase(context)
    val factory = TareasViewModelFactory(
        tareaDao = db.tareaDao(),
        categoriaDao = db.categoriaDao()
    )
    val viewModel: TareasViewModel = viewModel(factory = factory)

    // --- 2. AHORA DEFINIMOS LA LISTA Y EL VIGILANTE ---
    // (Ya no darán rojo porque el viewModel ya existe arriba)
    val navController = rememberNavController()
    val listaTareas by viewModel.listaTareas.collectAsState(initial = emptyList())

    LaunchedEffect(listaTareas) {
        Log.d("LOG-NOTIFICACION", "🔔 La lista ha cambiado. Tareas totales: ${listaTareas.size}")

        listaTareas.forEach { tarea ->
            if (!tarea.estaCompletada && tarea.fechaLimite != null) {
                Log.d("LOG-NOTIFICACION", "🔎 Analizando tarea: ${tarea.titulo} (Fecha: ${tarea.fechaLimite})")
                programarNotificacion(context, tarea)
            }
        }
    }


    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = navBackStackEntry?.destination?.route

    // --- 1. AQUÍ PEDIMOS EL PERMISO PARA ANDROID 13+ ---
    val permisoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("LOG", "Permiso de notificaciones concedido")
        } else {
            Toast.makeText(context, "Debes activar las notificaciones en Ajustes", Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permisoLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    // Este es tu nuevo motor de IA, mucho más fiable
    val client = remember { HttpClient(OkHttp) }
    val apiKey = DatosIA.MI_LLAVE
    
    // Properly dispose HttpClient when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            client.close()
        }
    }


    // Este lanzador abre el selector de archivos para guardar
    val exportarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            // 1. Exportamos y reseteamos la instancia global (INSTANCE = null)
            DatabaseBackup.exportDatabase(context, it)

            // 2. OBTENEMOS LA NUEVA DB (Esto crea una conexión fresca)
            val nuevaDb = TareasDatabase.getDatabase(context)

            // 3. ACTUALIZAMOS LOS DAOS DEL VIEWMODEL
            // Necesitamos una función en el ViewModel que acepte los nuevos DAOs
            viewModel.actualizarDaos(nuevaDb.tareaDao(), nuevaDb.categoriaDao())

            Toast.makeText(context, "Copia guardada y base de datos reconectada", Toast.LENGTH_SHORT).show()
        }
    }
    // Este lanzador abre el explorador para elegir un archivo existente
    var mostrarInstruccionesPostRestore by remember { mutableStateOf(false) }

    // Modificamos el lanzador de importar para que al terminar active este diálogo
    val importarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            DatabaseBackup.importDatabase(context, it)
            mostrarInstruccionesPostRestore = true // Activamos el aviso de "ahora reinicia"
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        data?.get(0)?.let { textoEscuchado ->
            scope.launch {
                try {
                    // 1. Obtenemos la fecha de hoy para darle contexto a la IA
                    val sdfHoy = java.text.SimpleDateFormat("EEEE dd/MM/yyyy", java.util.Locale("es", "ES"))
                    val urlCorrecta = "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash-lite:generateContent?key=$apiKey"


                    // 1. Obtenemos fecha y hora LOCAL del dispositivo
                    val ahora = java.time.LocalDateTime.now()
                    val fechaHoy = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").format(ahora)
                    val horaHoy = java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(ahora)
                    val anyoHoy = java.time.format.DateTimeFormatter.ofPattern("yyyy").format(ahora)

                    // 3. LOGS PARA VER QUÉ ESTÁ PASANDO (Copia esto justo después de donde obtienes la respuesta de la IA)
                    android.util.Log.d("LOG", "=== DATOS ENVIADOS ===")
                    android.util.Log.d("LOG", "Texto escuchado: $textoEscuchado")
                    android.util.Log.d("LOG", "Fecha Hoy: $fechaHoy | Hora Hoy: $horaHoy")

                    val response = client.post(urlCorrecta) {
                        contentType(ContentType.Application.Json)
                        setBody(buildJsonObject {
                            putJsonArray("contents") {
                                addJsonObject {
                                    putJsonArray("parts") {
                                        addJsonObject {
                                            put("text", """
                                                Eres un experto en extracción de datos.
                                                CONTEXTO: Hoy es $fechaHoy, la hora actual es $horaHoy y el año es $anyoHoy.
                                            
                                                TAREA: Extraer la información de: "$textoEscuchado"
                                            
                                                INSTRUCCIONES CRÍTICAS:
                                                1. "fecha": 
                                                   - Si dice "mañana", usa exactamente: ${java.time.LocalDate.now().plusDays(1)}.
                                                   - Si menciona día/mes, usa el año $anyoHoy y devuélvelo como YYYY-MM-DD.
                                                   - Si dice "en X minutos/horas" o solo indica una hora, la fecha es $fechaHoy.
                                                   - Solo si es una tarea sin ninguna referencia de tiempo, pon null.
                                                2. "hora":
                                                   - Si dice "en X minutos", calcula la hora sumando a $horaHoy.
                                                   - Si no especifica hora, pon null.
                                                   - Formato HH:mm (24h).
                                                3. JSON (estrictamente numérico):
                                                {
                                                  "tarea": "acción sin palabras temporales",
                                                  "fecha": "YYYY-MM-DD o null",
                                                  "hora": "HH:mm o null",
                                                  "prioridad": "ALTA|MEDIA|BAJA"
                                                }
                                            
                                                EJEMPLOS:
                                                - "Mañana a las 5": {"fecha": "${java.time.LocalDate.now().plusDays(1)}", "hora": "17:00"}
                                                - "En 2 min": {"fecha": "$fechaHoy", "hora": "Calcula según $horaHoy"}
                                            """.trimIndent())

                                        }
                                    }
                                }
                            }
                        }.toString())
                    }

                    if (response.status.isSuccess()) {
                        val responseBody = response.bodyAsText()

                        val jsonElement = Json.parseToJsonElement(responseBody)
                        val textoIA = jsonElement.jsonObject["candidates"]
                            ?.jsonArray?.get(0)
                            ?.jsonObject?.get("content")
                            ?.jsonObject?.get("parts")
                            ?.jsonArray?.get(0)
                            ?.jsonObject?.get("text")
                            ?.jsonPrimitive?.content ?: ""

                        val jsonLimpio = textoIA.replace("```json", "").replace("```", "").trim()

                        // Asegúrate de que tu data class TareaIA tenga: val hora: String?
                        val objetoTarea = Json.decodeFromString<TareaIA>(jsonLimpio)

                        // --- AÑADE ESTOS LOGS AQUÍ ---
                        android.util.Log.d("LOG_IA", "1. JSON RECIBIDO DE GEMINI: $jsonLimpio")
                        android.util.Log.d("LOG_IA", "2. OBJETO DESERIALIZADO: Tarea=${objetoTarea.tarea}, Fecha=${objetoTarea.fecha}, Hora=${objetoTarea.hora}")
                        // -----------------------------
                        withContext(Dispatchers.Main) {
                            try {
                                // 1. Formateo del título
                                val tituloFormateado = objetoTarea.tarea.trim().replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
                                }

                                // 2. Procesamos la fecha (acepta null, "null" o dd/mm/yyyy)
                                val fechaConvertida = try {
                                    val fechaTexto = objetoTarea.fecha?.trim()

                                    if (fechaTexto.isNullOrBlank() || fechaTexto == "null") {
                                        null
                                    } else {
                                        // 1. Detectamos el formato (Gemini usa '-' y nosotros '/')
                                        val fechaParseada = if (fechaTexto.contains("-")) {
                                            // Formato YYYY-MM-DD (El que viene de Gemini)
                                            java.time.LocalDate.parse(fechaTexto)
                                        } else {
                                            // Formato DD/MM/YYYY (El que tenías antes por si acaso)
                                            val p = fechaTexto.split("/")
                                            if (p.size == 3) {
                                                val dia = p[0].toInt()
                                                val mes = p[1].toInt()
                                                val anioRaw = p[2].toInt()
                                                val anio = if (anioRaw < 100) anioRaw + 2000 else anioRaw
                                                java.time.LocalDate.of(anio, mes, dia)
                                            } else {
                                                null
                                            }
                                        }

                                        // 2. Validación final contra "Alucinaciones"
                                        val hoy = java.time.LocalDate.now()
                                        if (fechaParseada != null && fechaParseada.isBefore(hoy)) {
                                            android.util.Log.w("LOG", "⚠️ IA envió fecha pasada: $fechaParseada. Ajustando a hoy.")
                                            hoy
                                        } else {
                                            fechaParseada
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("LOG", "❌ Error fatal parseando fecha: ${objetoTarea.fecha}")
                                    java.time.LocalDate.now()
                                }

                                // 3. Procesamos la hora (acepta null, "null" o HH:mm)
                                val horaConvertida = try {
                                    val horaTexto = objetoTarea.hora?.trim()
                                    if (!horaTexto.isNullOrBlank() && horaTexto != "null") {
                                        // Soporta formatos como "8:05" convirtiéndolos a "08:05" si fuera necesario
                                        val horaLimpia = if (horaTexto.contains(":") && horaTexto.indexOf(":") == 1) "0$horaTexto" else horaTexto
                                        java.time.LocalTime.parse(horaLimpia)
                                    } else null
                                } catch (e: Exception) {
                                    android.util.Log.e("LOG", "Error en hora: ${objetoTarea.hora}")
                                    null
                                }

                                // ... después de calcular horaConvertida ...

                                var horaFinal = horaConvertida

                                // PARCHE: Si el usuario dijo "en X minutos" y la IA ha devuelto algo muy lejano (más de 3 horas)
                                // es que la IA ha alucinado. Vamos a intentar corregirlo localmente.
                                val texto = textoEscuchado.lowercase()
                                if (texto.contains("en ") && (texto.contains("minuto") || texto.contains("min"))) {
                                    val minutos = texto.filter { it.isDigit() }.toIntOrNull() ?: 2 // por defecto 2 si no lee el número
                                    if (minutos < 60) {
                                        // Si la IA dio una hora que está a más de 1 hora de diferencia de "ahora + minutos"
                                        val calculoLocal = java.time.LocalTime.now().plusMinutes(minutos.toLong())
                                        horaFinal = calculoLocal
                                        android.util.Log.d("LOG", "🕒 Corrección local: Sumados $minutos min. Hora: $horaFinal")
                                    }
                                }

                                val prioridadConvertida = when (objetoTarea.prioridad?.uppercase()) {
                                    "ALTA" -> Prioridad.ALTA
                                    "BAJA" -> Prioridad.BAJA
                                    else -> Prioridad.MEDIA
                                }

                                val nuevaTarea = Tarea(
                                    id = 0,
                                    titulo = tituloFormateado,
                                    descripcion = textoEscuchado,
                                    estaCompletada = false,
                                    prioridad = prioridadConvertida,
                                    fechaCreacion = System.currentTimeMillis(),
                                    fechaLimite = fechaConvertida,
                                    horaLimite = horaFinal
                                )

                                // --- ESTO ES LO QUE QUEREMOS VER EN EL LOGCAT ---
                                android.util.Log.d("LOG", "Ejecutando guardado para: ${nuevaTarea.titulo}")

                                viewModel.insertar(nuevaTarea)
                                programarNotificacion(context, nuevaTarea)

                                Toast.makeText(context, "Tarea guardada: $tituloFormateado", Toast.LENGTH_SHORT).show()

                            } catch (e: Exception) {
                                // Si algo falla catastróficamente, lo veremos aquí
                                android.util.Log.e("LOG", "Error crítico en el bloque Main: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LOG", "Error: ${e.message}")
                    // --- ESTO ES LO QUE TIENES QUE AÑADIR PARA QUE NO FALLE ---
                    withContext(Dispatchers.Main) {
                        val tareaSimple = Tarea(
                            titulo = textoEscuchado.replaceFirstChar { it.uppercase() },
                            descripcion = "Voz (Sin IA por error de red)",
                            prioridad = Prioridad.MEDIA
                        )
                        viewModel.insertar(tareaSimple)
                        Toast.makeText(context, "Guardado simple (Error de conexión)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val lanzarEscucha = {
        // 1. Preparamos el Intent
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            // Añade esto para que la ventana de voz no tarde tanto en cerrarse al terminar
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000)
        }

        try {
            // 2. IMPORTANTE: Intentar limpiar el foco antes de lanzar
            // A veces ayuda a que el sistema no crea que el micro sigue en uso
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    var mostrarMenuPrincipal by remember { mutableStateOf(false) }
    var mostrarConfirmacionRestore by remember { mutableStateOf(false) }

    val tareasActivas = listaTareas.count { !it.estaCompletada }

    MisTareasAppTheme { // Si sigue en rojo, asegúrate de que el import de arriba sea correcto
        Scaffold(
            topBar = {
                if (rutaActual == Rutas.PantallaTareas.ruta || rutaActual == Rutas.PantallaHabitos.ruta) {
                    val filtroActual by viewModel.categoriaSeleccionada.collectAsState()
                    val listaCategoriasUI by viewModel.todasLasCategorias.collectAsState(initial = emptyList())

                    // Usamos Column para que la TopBar y el Filtro convivan verticalmente
                    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                        TopAppBar(
                            title = {
                                val tituloBase = obtenerTitulo(rutaActual).uppercase()
                                val tituloFinal = if (tareasActivas > 0) "$tituloBase ($tareasActivas)" else tituloBase
                                Text(text = tituloFinal, fontWeight = FontWeight.Bold)
                            },
                            navigationIcon = {
                                Box {
                                    IconButton(onClick = { mostrarMenuPrincipal = true }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menú")
                                    }
                                    DropdownMenu(
                                        expanded = mostrarMenuPrincipal,
                                        onDismissRequest = { mostrarMenuPrincipal = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Backup") },
                                            onClick = {
                                                mostrarMenuPrincipal = false
                                                exportarLauncher.launch("backup_tareas_${System.currentTimeMillis()}.db")
                                            },
                                            leadingIcon = { Icon(Icons.Default.Backup, null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Restore") },
                                            onClick = {
                                                mostrarMenuPrincipal = false
                                                mostrarConfirmacionRestore = true
                                            },
                                            leadingIcon = { Icon(Icons.Default.Restore, null) }
                                        )
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = { Text("Tablas de Referencia") },
                                            onClick = {
                                                mostrarMenuPrincipal = false
                                                navController.navigate("categorias")
                                            },
                                            leadingIcon = { Icon(Icons.Default.TableChart, null) }
                                        )
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = { Text("Mostrar completadas") },
                                            onClick = {
                                                viewModel.cambiarVisibilidadCompletadas(!viewModel.mostrarCompletadas)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.CheckCircle, null,
                                                    tint = if (viewModel.mostrarCompletadas) MaterialTheme.colorScheme.primary else Color.Transparent
                                                )
                                            }
                                        )
                                    }
                                }
                            },
                            actions = {
                                if (rutaActual == Rutas.PantallaTareas.ruta) {
                                    // --- 1. NUEVO BOTÓN DE FILTRO ---
                                    IconButton(onClick = { viewModel.mostrarBarraFiltro = !viewModel.mostrarBarraFiltro }) {
                                        Icon(
                                            imageVector = if (filtroActual == null) Icons.Default.FilterList else Icons.Default.FilterListOff,
                                            contentDescription = "Filtrar",
                                            tint = if (filtroActual != null) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                        )
                                    }

                                    // --- 2. BOTÓN MAESTRO EXPANDIR/COLAPSAR ---
                                    IconButton(onClick = {
                                        val nuevoEstado = !viewModel.todasSeccionesAbiertas
                                        viewModel.cambiarEstadoGlobalSecciones(nuevoEstado)
                                    }) {
                                        Icon(
                                            imageVector = if (viewModel.todasSeccionesAbiertas) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                                            contentDescription = "Contraer/Expandir todo"
                                        )
                                    }

                                    // --- 3. BOTÓN AÑADIR (Con tu lógica original) ---
                                    var expandedAdd by remember { mutableStateOf(false) }
                                    val rotation by animateFloatAsState(
                                        targetValue = if (expandedAdd) 45f else 0f,
                                        animationSpec = tween(durationMillis = 200),
                                        label = "rotation"
                                    )
                                    Box {
                                        IconButton(onClick = { expandedAdd = true }) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = "Nueva tarea",
                                                    tint = Color.Black,
                                                    modifier = Modifier
                                                        .padding(8.dp)
                                                        .graphicsLayer { rotationZ = rotation }
                                                )
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = expandedAdd,
                                            onDismissRequest = { expandedAdd = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("🗣️ Por voz") },
                                                onClick = { expandedAdd = false; lanzarEscucha() }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("📝 Escribir") },
                                                onClick = { expandedAdd = false; navController.navigate(Rutas.PantallaCrearTarea.ruta) }
                                            )
                                        }
                                    }
                                }
                            }
                        )

                        // --- BARRA DE FILTROS ANIMADA ---
                        AnimatedVisibility(
                            visible = viewModel.mostrarBarraFiltro && rutaActual == Rutas.PantallaTareas.ruta,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            BarraFiltros(
                                categorias = listaCategoriasUI,
                                seleccionada = filtroActual,
                                onSeleccionar = { viewModel.filtrarPor(it) }
                            )
                        }
                    }
                }
            }
            ,
            bottomBar = { if (rutaActual != Rutas.PantallaCrearTarea.ruta) MiBottomBar(navController) }
        ) { innerPadding ->
            // (Mantén aquí tus diálogos de seguridad: mostrarConfirmacionRestore, etc.)
            if (mostrarConfirmacionRestore) {
                AlertDialog(
                    onDismissRequest = { mostrarConfirmacionRestore = false },
                    title = { Text("¿Restaurar copia de seguridad?") },
                    text = { Text("Esto borrará las tareas actuales y las reemplazará por las de la copia. ¿Deseas continuar?") },
                    confirmButton = {
                        TextButton(onClick = {
                            mostrarConfirmacionRestore = false
                            // Esto es lo que realmente abre el buscador de archivos
                            importarLauncher.launch(arrayOf("application/octet-stream", "application/x-sqlite3"))
                        }) {
                            Text("RESTAURAR", color = Color.Red)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { mostrarConfirmacionRestore = false }) {
                            Text("CANCELAR")
                        }
                    }
                )
            }

            if (mostrarInstruccionesPostRestore) {
                AlertDialog(
                    onDismissRequest = { mostrarInstruccionesPostRestore = false },
                    title = { Text("Restauración completada") },
                    text = { Text("Para que los datos se carguen correctamente, por favor cierra la aplicación por completo y vuelve a abrirla.") },
                    confirmButton = {
                        Button(onClick = { mostrarInstruccionesPostRestore = false }) {
                            Text("ENTENDIDO")
                        }
                    }
                )
            }

            NavHost(
                navController = navController,
                startDestination = Rutas.PantallaTareas.ruta,
                // CLAVE: El NavHost ocupa TODO. No le pongas padding(innerPadding) aquí.
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            ) {
                composable(Rutas.PantallaTareas.ruta) {
                    // Recolectamos los datos (esto lo tienes bien)
                    val mapasDeTareas by viewModel.tareasClasificadas.collectAsStateWithLifecycle()

                    PantallaListaTareas(
                        navController = navController,
                        viewModel = viewModel,
                        mapas = mapasDeTareas, // <--- ¡ESTA ES LA LÍNEA QUE FALTA!
                        modifier = Modifier.padding(innerPadding).fillMaxSize()
                    )
                }

                composable(Rutas.PantallaHabitos.ruta) {
                    PantallaHabitos(navController, viewModel, modifier = Modifier.padding(innerPadding).fillMaxSize())
                }

                composable(Rutas.PantallaCrearTarea.ruta) {
                    // Esta pantalla tiene su propio Scaffold, no necesita que le pases el padding del padre
                    PantallaCrearTarea(navController)
                }

                composable(
                    route = Rutas.PantallaEditarTarea.ruta,
                    arguments = listOf(navArgument("tareaId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("tareaId") ?: 0
                    PantallaEditarTarea(navController, id, viewModel)
                }

                composable("categorias") {
                    PantallaGestionCategorias(navController, viewModel, modifier = Modifier.padding(innerPadding).fillMaxSize())
                }
            }
        }
    }
}
fun programarNotificacion(context: android.content.Context, tarea: Tarea) {
    val fecha = tarea.fechaLimite ?: return
    val hora = tarea.horaLimite ?: java.time.LocalTime.of(9, 0)
    val fechaHoraLimite = java.time.LocalDateTime.of(fecha, hora)
    val ahora = java.time.LocalDateTime.now()

    val delayBase = java.time.Duration.between(ahora, fechaHoraLimite).toMillis()

    // 1. DETERMINAMOS EL INTERVALO DE REPETICIÓN SEGÚN TU SOLICITUD
    val tiempoRepeticion = when (tarea.prioridad) {
        Prioridad.ALTA -> 60 * 60 * 1000L           // 60 minutos
        /*Prioridad.ALTA -> 5 * 60 * 1000L           // 60 minutos*/
        Prioridad.MEDIA -> 24 * 60 * 60 * 1000L      // 24 horas
        Prioridad.BAJA -> 3 * 24 * 60 * 60 * 1000L  // 3 días
        else -> 0L
    }

    if (delayBase > 0) {
        // A. AVISO PRINCIPAL (A la hora de la tarea)
        programarTareaEnWorkManager(context, tarea, delayBase, "principal")
        android.util.Log.d("LOG-NOTIFICACION", "✅ ALARMA PRINCIPAL: '${tarea.titulo}' a las $hora")

        // B. AVISO DE REPETICIÓN (Según prioridad)
        if (tiempoRepeticion > 0) {
            programarTareaEnWorkManager(context, tarea, delayBase + tiempoRepeticion, "repeticion")
            val info = when(tarea.prioridad) {
                Prioridad.ALTA -> "60 min"
                Prioridad.MEDIA -> "24 horas"
                Prioridad.BAJA -> "3 días"
                else -> ""
            }
            android.util.Log.d("LOG-NOTIFICACION", "➕ REPETICIÓN PROGRAMADA (cada $info) para: '${tarea.titulo}'")
        }

    } else {
        android.util.Log.d("LOG-NOTIFICACION", "❌ NO PROGRAMADA: '${tarea.titulo}' ya pasó.")
    }
}

// Esta es la función que "empaqueta" el código que preguntaste antes
private fun programarTareaEnWorkManager(
    context: android.content.Context,
    tarea: Tarea,
    delayMs: Long,
    tipo: String
) {
    val data = androidx.work.workDataOf(
        "titulo" to tarea.titulo,
        "id_tarea" to tarea.id
    )

    val request = androidx.work.OneTimeWorkRequestBuilder<NotificacionWorker>()
        .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
        .setInputData(data)
        .addTag("notif_${tarea.id}_$tipo") // Tag: notif_ID_principal o notif_ID_repeticion
        .build()

    androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
        "notif_${tarea.id}_$tipo",
        androidx.work.ExistingWorkPolicy.REPLACE,
        request
    )
}

private suspend fun guardarTareaSimple(texto: String, viewModel: TareasViewModel, context: android.content.Context) {
    withContext(Dispatchers.Main) {
        val tareaBasica = Tarea(
            titulo = texto.replaceFirstChar { it.uppercase() },
            descripcion = "Voz (IA no disponible)",
            prioridad = Prioridad.MEDIA
        )
        viewModel.insertar(tareaBasica)
        Toast.makeText(context, "Guardado simple (IA falló)", Toast.LENGTH_SHORT).show()
    }
}