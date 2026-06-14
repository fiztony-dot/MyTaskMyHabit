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
import com.example.mistareasapp.data.habits.HabitosBackup
import com.example.mistareasapp.data.habits.HabitosImportacion
import com.example.mistareasapp.data.tasks.TareasBackupJson
import com.example.mistareasapp.data.tasks.Prioridad
import com.example.mistareasapp.data.tasks.Tarea
import com.example.mistareasapp.ui.screens.habits.PantallaHabitos
import com.example.mistareasapp.ui.screens.shopping.PantallaGestionCategoriasCompra
import com.example.mistareasapp.ui.screens.shopping.PantallaGestionLugares
import com.example.mistareasapp.ui.screens.shopping.PantallaGestionProductos
import com.example.mistareasapp.ui.screens.shopping.PantallaListaCompra
import com.example.mistareasapp.viewmodel.Shopping.ListaCompraViewModelFactory
import com.example.mistareasapp.viewmodel.Shopping.ListaCompraViewModel
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
import com.example.mistareasapp.ui.screens.habits.PantallaAuditoriaHabitos
import com.example.mistareasapp.ui.screens.habits.PantallaDetalleCalculoGeneral
import com.example.mistareasapp.ui.screens.habits.PantallaCalculosHabitos
import com.example.mistareasapp.ui.screens.habits.PantallaDetalleCalculoHabito
import com.example.mistareasapp.ui.screens.habits.PantallaMantenimientoHabitos
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModelFactory

fun obtenerTitulo(ruta: String?): String {
    return when {
        ruta == Rutas.PantallaTareas.ruta -> "Mis Tareas"
        ruta?.startsWith("habitos") == true -> "Mis Habitos"
        ruta == Rutas.PantallaCrearTarea.ruta -> "Nueva Tarea"
        ruta == Rutas.PantallaListaCompra.ruta -> "Lista de la Compra"
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

    val listaCompraFactory = ListaCompraViewModelFactory(context.applicationContext as android.app.Application, db.listaCompraDao())
    val listaCompraViewModel: ListaCompraViewModel = viewModel(factory = listaCompraFactory)

    // --- 2. NAVEGACIÓN ---
    val navController = rememberNavController()
    val listaTareas by viewModel.listaTareas.collectAsState(initial = emptyList())
    val filtroActual by viewModel.categoriaSeleccionada.collectAsState()
    val textoBusqueda by viewModel.textoBusqueda.collectAsStateWithLifecycle()
    val tareasActivas = listaTareas.count { !it.estaCompletada }

    // Para mostrar en Hábitos el % de cumplimiento (media ponderada por días de vida efectivos)
    val habitosConProgreso by habitosViewModel.habitosConProgreso.collectAsState(initial = emptyList())
    val pesoTotalHabitos = habitosConProgreso.sumOf { (minOf(it.diasVidaEfectivos, 180) * it.habito.dificultad).toLong() }
    val progresoGeneral = if (pesoTotalHabitos > 0)
        (habitosConProgreso.sumOf { it.porcentajeHistorico.coerceIn(0f, 1f).toDouble() * minOf(it.diasVidaEfectivos, 180) * it.habito.dificultad } / pesoTotalHabitos).toFloat()
    else 0f


    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = navBackStackEntry?.destination?.route

    // Estado de búsqueda y filtro del módulo de hábitos
    val textoBusquedaHabitos by habitosViewModel.textoBusqueda.collectAsState()
    val categoriaFiltroHabitos by habitosViewModel.categoriaFiltro.collectAsState()

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

    // --- 3B. BACKUP Y RESTORE (módulo específico) ---
    var mostrarConfirmacionRestore by remember { mutableStateOf(false) }
    var mostrarInstruccionesPostRestore by remember { mutableStateOf(false) }
    // Indica qué módulo está pendiente de restaurar al confirmar el diálogo
    var restoreEsHabitos by remember { mutableStateOf(false) }

    // Launchers de TAREAS
    val exportarTareasLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { TareasBackupJson.exportar(context, it) } }

    val importarTareasLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { TareasBackupJson.importar(context, it); mostrarInstruccionesPostRestore = true } }

    // Launchers de HÁBITOS
    val exportarHabitosLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { HabitosBackup.exportar(context, it) } }

    val importarHabitosLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { HabitosBackup.importar(context, it); mostrarInstruccionesPostRestore = true } }

    // Helpers para lanzar según el módulo activo
    fun lanzarExportar() {
        if (rutaActual == Rutas.PantallaHabitos.ruta)
            exportarHabitosLauncher.launch("backup_habitos_${System.currentTimeMillis()}.json")
        else
            exportarTareasLauncher.launch("backup_tareas_${System.currentTimeMillis()}.json")
    }
    fun lanzarImportar() {
        if (restoreEsHabitos) importarHabitosLauncher.launch(arrayOf("application/json", "application/octet-stream"))
        else importarTareasLauncher.launch(arrayOf("application/json", "application/octet-stream"))
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
    var mostrarDialogoImportacion by remember { mutableStateOf(false) }
    var resultadoImportacion by remember { mutableStateOf<String?>(null) }

    // Importación: guardamos la URI del primer fichero mientras el usuario elige el segundo
    var uriDefinicionPendiente by remember { mutableStateOf<android.net.Uri?>(null) }

    // Launcher fichero 2: historial → lanza la importación
    val importarHistorialLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uriHist ->
        val uriDef = uriDefinicionPendiente
        uriDefinicionPendiente = null
        if (uriHist != null && uriDef != null) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val resultado = HabitosImportacion.importar(context, uriDef, uriHist)
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    resultadoImportacion = when (resultado) {
                        is HabitosImportacion.Resultado.Exito ->
                            "✅ Importación completada:\n" +
                            "• ${resultado.habitos} hábitos importados\n" +
                            "• ${resultado.registrosHistorial} registros de historial\n\n" +
                            "Cierra y reabre la app para que los cambios surtan efecto."
                        is HabitosImportacion.Resultado.Error ->
                            "❌ Error durante la importación:\n${resultado.mensaje}"
                    }
                }
            }
        }
    }

    // Launcher fichero 1: definición → abre el picker del historial
    val importarDefinicionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uriDef ->
        if (uriDef != null) {
            uriDefinicionPendiente = uriDef
            importarHistorialLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
        }
    }

    // --- 5. INTERFAZ ---
    MisTareasAppTheme {
        Scaffold(
            topBar = {
                if (rutaActual == Rutas.PantallaTareas.ruta || rutaActual == Rutas.PantallaHabitos.ruta || rutaActual == Rutas.PantallaListaCompra.ruta) {
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
                                            val moduloLabel = if (rutaActual == Rutas.PantallaHabitos.ruta) "Hábitos" else "Tareas"
                                            DropdownMenuItem(
                                                text = { Text("Guardar backup de $moduloLabel") },
                                                onClick = {
                                                    mostrarMenuPrincipal = false
                                                    expandirCopias = false
                                                    lanzarExportar()
                                                },
                                                leadingIcon = { Icon(Icons.Default.Backup, null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Restaurar backup de $moduloLabel") },
                                                onClick = {
                                                    mostrarMenuPrincipal = false
                                                    expandirCopias = false
                                                    restoreEsHabitos = (rutaActual == Rutas.PantallaHabitos.ruta)
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
                                        DropdownMenuItem(
                                            text = { Text("% de cumplimiento") },
                                            onClick = {
                                                mostrarMenuPrincipal = false
                                                navController.navigate("calculos_habitos")
                                            },
                                            leadingIcon = { Icon(Icons.Default.Calculate, null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Importar datos") },
                                            onClick = {
                                                mostrarMenuPrincipal = false
                                                mostrarDialogoImportacion = true
                                            },
                                            leadingIcon = { Icon(Icons.Default.FileDownload, null) }
                                        )
                                    }
                                    if (rutaActual == Rutas.PantallaListaCompra.ruta) {
                                        DropdownMenuItem(
                                            text = { Text("Gestionar lugares") },
                                            onClick = {
                                                mostrarMenuPrincipal = false
                                                navController.navigate(Rutas.PantallaGestionLugares.ruta)
                                            },
                                            leadingIcon = { Icon(Icons.Default.LocationOn, null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Gestionar categorías") },
                                            onClick = {
                                                mostrarMenuPrincipal = false
                                                navController.navigate(Rutas.PantallaGestionCategoriasCompra.ruta)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Category, null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Gestionar productos") },
                                            onClick = {
                                                mostrarMenuPrincipal = false
                                                navController.navigate(Rutas.PantallaGestionProductos.ruta)
                                            },
                                            leadingIcon = { Icon(Icons.Default.ShoppingBasket, null) }
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
                                    textoBusqueda = textoBusquedaHabitos,
                                    filtroActual = categoriaFiltroHabitos
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
                    rutaActual == Rutas.PantallaTareas.ruta || rutaActual == Rutas.PantallaHabitos.ruta || rutaActual == Rutas.PantallaListaCompra.ruta -> {
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

                composable("calculos_habitos") {
                    PantallaCalculosHabitos(
                        viewModel = habitosViewModel,
                        navController = navController
                    )
                }

                composable("detalle_calculo_general") {
                    PantallaDetalleCalculoGeneral(
                        viewModel = habitosViewModel,
                        navController = navController
                    )
                }

                composable(
                    route = "auditoria_habitos?habitoId={habitoId}",
                    arguments = listOf(navArgument("habitoId") { type = NavType.LongType; defaultValue = -1L })
                ) { backStackEntry ->
                    val idArg = backStackEntry.arguments?.getLong("habitoId") ?: -1L
                    PantallaAuditoriaHabitos(
                        viewModel = habitosViewModel,
                        navController = navController,
                        habitoIdInicial = if (idArg >= 0) idArg else null
                    )
                }

                composable(
                    route = "detalle_calculo/{habitoId}",
                    arguments = listOf(navArgument("habitoId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getLong("habitoId") ?: return@composable
                    PantallaDetalleCalculoHabito(
                        habitoId = id,
                        viewModel = habitosViewModel,
                        navController = navController
                    )
                }

                composable("configuracion") {
                    PantallaConfiguracion(
                        navController,
                        viewModel,
                        modifier = Modifier.padding(innerPadding).fillMaxSize()
                    )
                }

                composable(Rutas.PantallaListaCompra.ruta) {
                    PantallaListaCompra(
                        navController = navController,
                        vm = listaCompraViewModel,
                        modifier = Modifier.padding(innerPadding).fillMaxSize()
                    )
                }

                composable(Rutas.PantallaGestionLugares.ruta) {
                    PantallaGestionLugares(
                        navController = navController,
                        vm = listaCompraViewModel
                    )
                }

                composable(Rutas.PantallaGestionCategoriasCompra.ruta) {
                    PantallaGestionCategoriasCompra(
                        navController = navController,
                        vm = listaCompraViewModel
                    )
                }

                composable(Rutas.PantallaGestionProductos.ruta) {
                    PantallaGestionProductos(
                        navController = navController,
                        vm = listaCompraViewModel
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
            val moduloNombre = if (restoreEsHabitos) "hábitos" else "tareas"
            AlertDialog(
                onDismissRequest = { mostrarConfirmacionRestore = false },
                title = { Text("Restaurar backup de $moduloNombre") },
                text = { Text("Esto reemplazará todos los datos de $moduloNombre con los del archivo de backup. ¿Deseas continuar?") },
                confirmButton = {
                    TextButton(onClick = {
                        mostrarConfirmacionRestore = false
                        lanzarImportar()
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

        // Diálogo de confirmación de importación
        if (mostrarDialogoImportacion) {
            AlertDialog(
                onDismissRequest = { mostrarDialogoImportacion = false },
                title = { Text("Importar datos de hábitos") },
                text = {
                    Text(
                        "Se pedirán dos ficheros mediante el selector del sistema:\n\n" +
                        "1️⃣ habitos_definicion.json\n" +
                        "2️⃣ habitos_historial.json\n\n" +
                        "⚠️ ATENCIÓN: Se eliminarán todos los hábitos e historial actuales " +
                        "antes de importar. Si falla, los datos anteriores quedan intactos.\n\n" +
                        "¿Deseas continuar?"
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        mostrarDialogoImportacion = false
                        importarDefinicionLauncher.launch(
                            arrayOf("application/json", "application/octet-stream", "*/*")
                        )
                    }) { Text("SELECCIONAR FICHEROS", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogoImportacion = false }) { Text("CANCELAR") }
                }
            )
        }

        // Diálogo de resultado de importación
        resultadoImportacion?.let { mensaje ->
            AlertDialog(
                onDismissRequest = { resultadoImportacion = null },
                title = { Text("Resultado de la importación") },
                text = { Text(mensaje) },
                confirmButton = {
                    TextButton(onClick = { resultadoImportacion = null }) { Text("ACEPTAR") }
                }
            )
        }
    }
}






