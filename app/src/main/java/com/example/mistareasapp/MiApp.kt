package com.example.mistareasapp

// --- 1. Android Framework Base y Utilidades ---
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import java.util.Locale

// --- 2. Kotlin Core: Corrutinas y Serialización ---

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

// --- 6. Jetpack Compose: Animaciones ---

// --- 7. Jetpack Compose: UI, Material Design y Gráficos ---
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

// --- 8. Jetpack Compose: Runtime y Estado ---
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// --- 9. Clases del Proyecto (Local) ---
import com.example.mistareasapp.data.AppDatabase
import com.example.mistareasapp.data.tasks.Prioridad
import com.example.mistareasapp.data.tasks.Tarea
import com.example.mistareasapp.ui.screens.habits.PantallaHabitos
import com.example.mistareasapp.ui.screens.tasks.PantallaCrearTarea
import com.example.mistareasapp.ui.screens.tasks.PantallaEditarTarea
import com.example.mistareasapp.ui.screens.tasks.PantallaGestionCategorias
import com.example.mistareasapp.ui.screens.tasks.PantallaListaTareas
import com.example.mistareasapp.ui.theme.MisTareasAppTheme
import com.example.mistareasapp.viewmodel.Tasks.TareasViewModel
import com.example.mistareasapp.viewmodel.Tasks.TareasViewModelFactory

import com.example.mistareasapp.core.notifications.tasks.NotificationHelper
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import com.example.mistareasapp.ui.components.tasks.AccionesTopBarTareas
import com.example.mistareasapp.ui.navigation.BarraNavegacion
import com.example.mistareasapp.ui.navigation.Rutas
import com.example.mistareasapp.ui.screens.GestionDatosScreen
import com.example.mistareasapp.viewmodel.IAViewModel

// --- 10. Nuevos modelos de IA y tipos de entrada ---
import com.example.mistareasapp.network.IAResultTarea
import com.example.mistareasapp.network.IAResultHabito
import com.example.mistareasapp.network.TipoEntrada
import com.example.mistareasapp.ui.navigation.BarraNavegacionHabitos
import com.example.mistareasapp.ui.screens.habits.PantallaHabitosEstadisticas
import com.example.mistareasapp.ui.screens.habits.PantallaHabitosFlash
import com.example.mistareasapp.ui.screens.habits.PantallaHabitosListado
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModelFactory

fun obtenerTitulo(ruta: String?): String {
    return when {
        ruta == Rutas.PantallaTareas.ruta -> "Mis Tareas"
        ruta?.startsWith("habitos") == true -> "Mis Habitos, Mi Destino"
        ruta == Rutas.PantallaCrearTarea.ruta -> "Nueva Tarea"
        else -> "Gestión"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisTareasApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- 1. PRIMERO CREAMOS LA BASE DE DATOS Y EL VIEWMODEL ---
    val db = AppDatabase.getDatabase(context)
    val factory = TareasViewModelFactory(
        tareaDao = db.tareaDao(), categoriaDao = db.categoriaDao()
    )
    val viewModel: TareasViewModel = viewModel(factory = factory)

    // --- NUEVO: AÑADIMOS EL CEREBRO DE LA IA ---
    val iaViewModel: IAViewModel = viewModel()

    // 3. NUEVO: Factory y ViewModel de Hábitos
    val habitosFactory = HabitosViewModelFactory(db.habitoDao())
    val habitosViewModel: HabitosViewModel = viewModel(factory = habitosFactory)

    // --- 2. AHORA DEFINIMOS LA LISTA Y EL VIGILANTE ---
    val navController = rememberNavController()
    val listaTareas by viewModel.listaTareas.collectAsState(initial = emptyList())
    val filtroActual by viewModel.categoriaSeleccionada.collectAsState()

    LaunchedEffect(listaTareas) {
        Log.d("LOG-NOTIFICACION", "🔔 La lista ha cambiado. Tareas totales: ${listaTareas.size}")

        listaTareas.forEach { tarea ->
            if (!tarea.estaCompletada && tarea.fechaLimite != null) {
                Log.d("LOG-NOTIFICACION", "🔎 Analizando tarea: ${tarea.titulo} (Fecha: ${tarea.fechaLimite})")
                NotificationHelper.programarNotificacion(context, tarea)
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

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            data?.get(0)?.let { textoEscuchado ->

                // 1. Detectamos automáticamente si estamos en Hábitos o Tareas
                val rutaActual = navController.currentBackStackEntry?.destination?.route
                val esWidget = (context as? MainActivity)?.intent?.getBooleanExtra("abrirVoz", false) ?: false

                val tipo = if (esWidget) {
                    TipoEntrada.TAREA // Si es widget, forzamos TAREA para que la IA sepa qué buscar
                } else {
                    if (navController.currentBackStackEntry?.destination?.route == Rutas.PantallaHabitos.ruta) {
                        TipoEntrada.HABITO
                    } else {
                        TipoEntrada.TAREA
                    }
                }

                // 2. Llamada al "cerebro" (IAViewModel)
                iaViewModel.procesarVoz(textoEscuchado, tipo) { resultado ->
                    // --- LOG DE DEPURACIÓN ---
                    Log.d("IA_DEBUG", """
                    TEXTO RECIBIDO: "$textoEscuchado"
                    PANTALLA ACTUAL: $tipo
                    RESULTADO IA: $resultado
                """.trimIndent())
                    // -------------------------
                    when (resultado) {
                        is IAResultTarea -> {
                            // Log específico para los campos de la tarea
                            Log.d("IA_DEBUG", "Distribución: Titulo=${resultado.titulo}, Fecha=${resultado.fecha}, Hora=${resultado.hora}, Prioridad=${resultado.prioridad}")
                            val nuevaTarea = Tarea(
                                id = 0,
                                titulo = resultado.titulo,
                                descripcion = textoEscuchado,
                                prioridad = when(resultado.prioridad.uppercase()) {
                                    "ALTA" -> Prioridad.ALTA
                                    "BAJA" -> Prioridad.BAJA
                                    else -> Prioridad.MEDIA
                                },
                                fechaLimite = resultado.fecha?.let { java.time.LocalDate.parse(it) },
                                horaLimite = resultado.hora?.let { java.time.LocalTime.parse(it) },
                                fechaCreacion = System.currentTimeMillis()
                            )
                            viewModel.insertar(nuevaTarea)
                            NotificationHelper.programarNotificacion(context, nuevaTarea)
                            Toast.makeText(context, "Tarea: ${resultado.titulo}", Toast.LENGTH_SHORT).show()
                        }

                        is IAResultHabito -> {
                            // Preparado para el futuro: aquí conectarás con HabitosViewModel
                            Toast.makeText(context, "Hábito detectado: ${resultado.nombre}", Toast.LENGTH_SHORT).show()
                        }

                        null -> {
                            Toast.makeText(context, "La IA no pudo procesar la frase", Toast.LENGTH_SHORT).show()
                        }
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
    val textoBusqueda by viewModel.textoBusqueda.collectAsStateWithLifecycle()

    MisTareasAppTheme {
        Scaffold(
            topBar = {
                if (rutaActual == Rutas.PantallaTareas.ruta || rutaActual == Rutas.PantallaHabitos.ruta) {
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
                                            text = { Text("Backup & Restore") },
                                            onClick = {
                                                mostrarMenuPrincipal = false
                                                // CAMBIO: Ahora navegamos a la pantalla de gestión
                                                navController.navigate("ruta_gestion_copias")
                                            },
                                            leadingIcon = { Icon(Icons.Default.Backup, null) }
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
                            // En el caso de que estemos en la pestaña TAREAS pintamos los iconos de la TOPBar
                            actions = {
                                if (rutaActual == Rutas.PantallaTareas.ruta) {
                                    AccionesTopBarTareas(
                                        viewModel = viewModel, navController = navController,
                                        onLanzarVoz = { lanzarEscucha() }, textoBusqueda = textoBusqueda,
                                        filtroActual = filtroActual
                                    )
                                }
                            }
                        )
                    }
                }
            },
            bottomBar = {
                // Definimos las rutas del módulo de hábitos
                val rutasHabitos = listOf(
                    Rutas.HabitosFlash.ruta,
                    Rutas.HabitosListado.ruta,
                    Rutas.HabitosEstadisticas.ruta
                )

                when {
                    // Si estamos en tareas o la pantalla principal de hábitos, mostramos la barra principal
                    rutaActual == Rutas.PantallaTareas.ruta || rutaActual == Rutas.PantallaHabitos.ruta -> {
                        BarraNavegacion(navController, rutaActual)
                    }
                    // Si estamos dentro del sub-módulo de hábitos (Flash, Listado, Stats), mostramos la nueva barra
                    rutaActual in rutasHabitos -> {
                        BarraNavegacionHabitos(navController, rutaActual)
                    }
                }
            }
        ) { innerPadding ->
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
                        navController = navController, viewModel = viewModel,
                        mapas = mapasDeTareas,modifier = Modifier.padding(innerPadding).fillMaxSize()
                    )
                }
               composable(Rutas.PantallaHabitos.ruta) {
                   // Para una alineación completa con el patrón de la App, recolectamos el estado aquí
                   val listaHabitos by habitosViewModel.habitosConProgreso.collectAsStateWithLifecycle()

                   PantallaHabitos(
                       navController = navController,
                       viewModel = habitosViewModel,
                       modifier = Modifier.padding(innerPadding).fillMaxSize()
                   )
               }
               /*composable(Rutas.HabitosFlash.ruta) {
                   PantallaHabitosFlash(habitosViewModel, Modifier.padding(innerPadding))
               }
               composable(Rutas.HabitosListado.ruta) {
                   PantallaHabitosListado(habitosViewModel, Modifier.padding(innerPadding))
               }
               composable(Rutas.HabitosEstadisticas.ruta) {
                   PantallaHabitosEstadisticas(habitosViewModel, Modifier.padding(innerPadding))
               }*/
                composable(Rutas.PantallaCrearTarea.ruta) {
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
                    PantallaGestionCategorias(
                        navController,
                        viewModel,
                        modifier = Modifier.padding(innerPadding).fillMaxSize()
                    )
                }
               composable("ruta_gestion_copias") {
                   // Llamamos a la función que está en tu archivo GestionArchivosCopias.kt
                   GestionDatosScreen(viewModel = viewModel)
               }
            }
        }
        // --- AL FINAL DEL COMPOSABLE ---
        val actividad = LocalContext.current as? MainActivity
        val necesitaVozWidget = actividad?.intent?.getBooleanExtra("abrirVoz", false) ?: false

        if (necesitaVozWidget) {
            LaunchedEffect(Unit) {
                lanzarEscucha()
                actividad?.intent?.removeExtra("abrirVoz")
            }
        }
    }
}
