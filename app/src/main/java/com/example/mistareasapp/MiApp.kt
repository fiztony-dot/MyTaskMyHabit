package com.example.mistareasapp

// --- 1. Android Framework Base y Utilidades ---
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import android.widget.Toast
import java.util.Locale

// --- 2. Kotlin Core: Corrutinas y Serialización ---
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable

// --- 3. Networking (Ktor Client) ---
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

// --- 4. AndroidX & Lifecycle (Integración con el SO) ---
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

// --- 5. Jetpack Compose: Navegación ---
import androidx.navigation.*
import androidx.navigation.compose.*

// --- 6. Jetpack Compose: UI, Material Design y Gráficos ---
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset

// --- 7. Jetpack Compose: Runtime y Estado ---
import androidx.compose.runtime.*
import com.example.mistareasapp.core.ai.crearSpeechLauncher

// --- 8. Clases del Proyecto (Local) ---
import com.example.mistareasapp.data.AppDatabase
import com.example.mistareasapp.data.DatabaseBackup
import com.example.mistareasapp.data.tasks.Prioridad
import com.example.mistareasapp.data.tasks.Tarea
import com.example.mistareasapp.ui.screens.habits.PantallaHabitos
import com.example.mistareasapp.ui.screens.habits.PantallaGestionCategoriasHabitos
import com.example.mistareasapp.ui.screens.habits.PantallaPausados
import com.example.mistareasapp.ui.screens.habits.PantallaVistaMensualHabito
import com.example.mistareasapp.ui.screens.tasks.PantallaCrearTarea
import com.example.mistareasapp.ui.screens.tasks.PantallaEditarTarea
import com.example.mistareasapp.ui.screens.tasks.PantallaGestionCategorias
import com.example.mistareasapp.ui.screens.tasks.PantallaListaTareas
import com.example.mistareasapp.ui.theme.MisTareasAppTheme
import com.example.mistareasapp.viewmodel.Tasks.TareasViewModel
import com.example.mistareasapp.viewmodel.Tasks.TareasViewModelFactory
import com.example.mistareasapp.core.notifications.tasks.NotificationHelper
import com.example.mistareasapp.ui.components.tasks.AccionesTopBarTareas
import com.example.mistareasapp.ui.navigation.BarraNavegacion
import com.example.mistareasapp.ui.navigation.BarraNavegacionHabitos
import com.example.mistareasapp.ui.navigation.Rutas
import com.example.mistareasapp.ui.screens.GestionDatosScreen
import com.example.mistareasapp.ui.screens.tasks.PantallaConfiguracion
import com.example.mistareasapp.ui.components.habits.AccionesTopBarHabitos
import com.example.mistareasapp.ui.screens.habits.CrearHabitoScreen
import com.example.mistareasapp.ui.screens.habits.EditarHabitoScreen
import com.example.mistareasapp.ui.screens.habits.PantallaMantenimientoHabitos
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModelFactory

fun obtenerTitulo(ruta: String?): String {
    return when {
        ruta == Rutas.PantallaTareas.ruta -> "Mis Tareas"
        ruta?.startsWith("habitos") == true -> "Mis Habitos"
        ruta == Rutas.PantallaCrearTarea.ruta -> "Nueva Tarea"
        else -> "Gestión"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisTareasApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- 1. BASE DE DATOS Y VIEWMODELS ---
    val db = AppDatabase.getDatabase(context)
    val factory = TareasViewModelFactory(
        tareaDao = db.tareaDao(),
        categoriaDao = db.categoriaDao()
    )
    val viewModel: TareasViewModel = viewModel(factory = factory)

    val habitosFactory = HabitosViewModelFactory(db.habitoDao())
    val habitosViewModel: HabitosViewModel = viewModel(factory = habitosFactory)

    // --- 2. NAVEGACIÓN ---
    val navController = rememberNavController()
    val listaTareas by viewModel.listaTareas.collectAsState(initial = emptyList())
    val filtroActual by viewModel.categoriaSeleccionada.collectAsState()
    val textoBusqueda by viewModel.textoBusqueda.collectAsStateWithLifecycle()
    val tareasActivas = listaTareas.count { !it.estaCompletada }

    // Para mostrar en Hábitos el % de cumplimiento
    val habitosConProgreso by habitosViewModel.habitosConProgreso.collectAsState(initial = emptyList())
    val totalHabitos = habitosConProgreso.size
    val habitosCompletados = habitosConProgreso.count { it.estaCompletado }
    val progresoGeneral = if (totalHabitos > 0) (habitosCompletados.toFloat() / totalHabitos) else 0f


    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = navBackStackEntry?.destination?.route

    // --- 3. PERMISOS ---
    val permisoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Debes activar las notificaciones en Ajustes", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permisoLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // --- 3B. BACKUP Y RESTORE ---
    var mostrarConfirmacionRestore by remember { mutableStateOf(false) }
    var mostrarInstruccionesPostRestore by remember { mutableStateOf(false) }

    val exportarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            DatabaseBackup.exportDatabase(context, it)
            Toast.makeText(context, "Backup guardado correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    val importarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            DatabaseBackup.importDatabase(context, it)
            mostrarInstruccionesPostRestore = true
        }
    }

     // --- 4. RECONOCIMIENTO DE VOZ ---
    // Recibe los valores de vuelta de la IA

    val speechLauncher = crearSpeechLauncher(
        navController = navController,
        viewModel = viewModel,
        context = context,
        scope = scope
    )

    // Prepara el Intent que abre el reconocimiento de voz de Android.
    // Le indica: que use lenguaje natural (FREE_FORM) y que escuche en español (es-ES)


    val lanzarEscucha = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000)
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    var mostrarMenuPrincipal by remember { mutableStateOf(false) }

    // --- 5. INTERFAZ ---
    MisTareasAppTheme {
        Scaffold(
            topBar = {
                if (rutaActual == Rutas.PantallaTareas.ruta || rutaActual == Rutas.PantallaHabitos.ruta) {
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
                                    // Submenu de Copias de Seguridad
                                    var expandirCopias by remember { mutableStateOf(false) }
                                    Box {
                                        DropdownMenuItem(
                                            text = { Text("Copias de Seguridad") },
                                            onClick = { expandirCopias = !expandirCopias },
                                            leadingIcon = { Icon(Icons.Default.SaveAlt, null) },
                                            trailingIcon = { Icon(Icons.Default.KeyboardArrowRight, null) }
                                        )
                                        DropdownMenu(
                                            expanded = expandirCopias,
                                            onDismissRequest = { expandirCopias = false },
                                            offset = DpOffset(150.dp, 0.dp)
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Guardar Backup") },
                                                onClick = {
                                                    mostrarMenuPrincipal = false
                                                    expandirCopias = false
                                                    exportarLauncher.launch("backup_tareas_${System.currentTimeMillis()}.db")
                                                },
                                                leadingIcon = { Icon(Icons.Default.Backup, null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Restaurar Backup") },
                                                onClick = {
                                                    mostrarMenuPrincipal = false
                                                    expandirCopias = false
                                                    mostrarConfirmacionRestore = true
                                                },
                                                leadingIcon = { Icon(Icons.Default.Restore, null) }
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                    // Items específicos por pestaña
                                    if (rutaActual == Rutas.PantallaTareas.ruta) {
                                        DropdownMenuItem(
                                            text = { Text("Categorías de tareas") },
                                            onClick = {
                                                mostrarMenuPrincipal = false
                                                navController.navigate("categorias")
                                            },
                                            leadingIcon = { Icon(Icons.Default.TableChart, null) }
                                        )
                                    }
                                    if (rutaActual == Rutas.PantallaHabitos.ruta) {
                                        DropdownMenuItem(
                                            text = { Text("Gestión de hábitos") },
                                            onClick = {
                                                mostrarMenuPrincipal = false
                                                navController.navigate("mantenimiento_habitos")
                                            },
                                            leadingIcon = { Icon(Icons.Default.EditNote, null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Hábitos pausados") },
                                            onClick = {
                                                mostrarMenuPrincipal = false
                                                navController.navigate("habitos_pausados")
                                            },
                                            leadingIcon = { Icon(Icons.Default.Pause, null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Categorías de hábitos") },
                                            onClick = {
                                                mostrarMenuPrincipal = false
                                                navController.navigate("categorias_habitos")
                                            },
                                            leadingIcon = { Icon(Icons.Default.TableChart, null) }
                                        )
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Configuración") },
                                        onClick = {
                                            mostrarMenuPrincipal = false
                                            navController.navigate("configuracion")
                                        },
                                        leadingIcon = { Icon(Icons.Default.Settings, null) }
                                    )
                                }
                            }
                        },
                        actions = {
                            if (rutaActual == Rutas.PantallaTareas.ruta) {
                                AccionesTopBarTareas(
                                    viewModel = viewModel,
                                    navController = navController,
                                    onLanzarVoz = { lanzarEscucha() },
                                    textoBusqueda = textoBusqueda,
                                    filtroActual = filtroActual
                                )
                            }
                            if (rutaActual == Rutas.PantallaHabitos.ruta) {
                                AccionesTopBarHabitos(
                                    viewModel = habitosViewModel,
                                    navController = navController,
                                    onLanzarVoz = { lanzarEscucha() },
                                    textoBusqueda = textoBusqueda,
                                    filtroActual = filtroActual
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = {
                val rutasHabitos = listOf(
                    Rutas.HabitosFlash.ruta,
                    Rutas.HabitosListado.ruta,
                    Rutas.HabitosEstadisticas.ruta
                )

                when {
                    rutaActual == Rutas.PantallaTareas.ruta || rutaActual == Rutas.PantallaHabitos.ruta -> {
                        BarraNavegacion(navController, rutaActual)
                    }
                    rutaActual in rutasHabitos -> {
                        BarraNavegacionHabitos(navController, rutaActual)
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                //startDestination = Rutas.PantallaHabitos.ruta,
                startDestination = Rutas.PantallaTareas.ruta,
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            ) {
                composable(Rutas.PantallaTareas.ruta) {
                    val mapasDeTareas by viewModel.tareasClasificadas.collectAsStateWithLifecycle()
                    PantallaListaTareas(
                        navController = navController,
                        viewModel = viewModel,
                        mapas = mapasDeTareas,
                        modifier = Modifier.padding(innerPadding).fillMaxSize()
                    )
                }

                composable(Rutas.PantallaHabitos.ruta) {
                    PantallaHabitos(
                        navController = navController,
                        viewModel = habitosViewModel,
                        modifier = Modifier.padding(innerPadding).fillMaxSize(),
                        progresoGeneral = progresoGeneral
                    )
                }

                composable(Rutas.PantallaCrearTarea.ruta) {
                    PantallaCrearTarea(navController)
                }

                composable(Rutas.PantallaCrearHabito.ruta) {
                    CrearHabitoScreen(
                        viewModel = habitosViewModel,
                        onGuardar = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Rutas.PantallaEditarTarea.ruta,
                    arguments = listOf(navArgument("tareaId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("tareaId") ?: 0
                    PantallaEditarTarea(navController, id, viewModel)
                }

                composable("categorias") {
                    PantallaGestionCategorias(
                        navController,
                        viewModel,
                        modifier = Modifier.padding(innerPadding).fillMaxSize()
                    )
                }

                composable("categorias_habitos") {
                    PantallaGestionCategoriasHabitos(
                        navController,
                        habitosViewModel,
                        modifier = Modifier.padding(innerPadding).fillMaxSize()
                    )
                }

                composable("crear_habito") {
                    CrearHabitoScreen(
                        viewModel = habitosViewModel,
                        onGuardar = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "editar_habito/{habitoId}",
                    arguments = listOf(navArgument("habitoId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getLong("habitoId") ?: return@composable
                    EditarHabitoScreen(
                        habitoId = id,
                        viewModel = habitosViewModel,
                        onGuardar = { navController.popBackStack() }
                    )
                }

                composable("mantenimiento_habitos") {
                    PantallaMantenimientoHabitos(
                        viewModel = habitosViewModel,
                        navController = navController,
                        modifier = Modifier.padding(innerPadding).fillMaxSize()
                    )
                }

                composable("habitos_pausados") {
                    PantallaPausados(
                        viewModel = habitosViewModel,
                        navController = navController
                    )
                }

                composable(
                    route = "vista_mensual/{habitoId}",
                    arguments = listOf(navArgument("habitoId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getLong("habitoId") ?: return@composable
                    PantallaVistaMensualHabito(
                        habitoId = id,
                        viewModel = habitosViewModel,
                        navController = navController
                    )
                }

                composable("ruta_gestion_copias") {
                    GestionDatosScreen(viewModel = viewModel)
                }

                composable("configuracion") {
                    PantallaConfiguracion(
                        navController,
                        viewModel,
                        modifier = Modifier.padding(innerPadding).fillMaxSize()
                    )
                }
            }
        }

        val actividad = LocalContext.current as? MainActivity
        val necesitaVozWidget = actividad?.intent?.getBooleanExtra("abrirVoz", false) ?: false

        if (necesitaVozWidget) {
            LaunchedEffect(Unit) {
                lanzarEscucha()
                actividad?.intent?.removeExtra("abrirVoz")
            }
        }

        // Diálogo de confirmación para restaurar
        if (mostrarConfirmacionRestore) {
            AlertDialog(
                onDismissRequest = { mostrarConfirmacionRestore = false },
                title = { Text("Restaurar Backup") },
                text = { Text("Esto reemplazará los datos actuales con los del backup. ¿Deseas continuar?") },
                confirmButton = {
                    TextButton(onClick = {
                        mostrarConfirmacionRestore = false
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

        // Diálogo de instrucciones post-restore
        if (mostrarInstruccionesPostRestore) {
            AlertDialog(
                onDismissRequest = { mostrarInstruccionesPostRestore = false },
                title = { Text("Restauración completada") },
                text = { Text("Para que los datos se carguen correctamente, por favor cierra la aplicación completamente y vuelve a abrirla.") },
                confirmButton = {
                    TextButton(onClick = { mostrarInstruccionesPostRestore = false }) {
                        Text("ACEPTAR")
                    }
                }
            )
        }
    }
}






