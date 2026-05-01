# Descripcion de `MiApp.kt`

## Objetivo del archivo
`MiApp.kt` define el composable raiz `MisTareasApp()` y centraliza la orquestacion de la app: inicializacion de datos, navegacion, menu principal, reconocimiento de voz con IA y flujos de backup/restore.

## Responsabilidades principales

### 1) Inicializacion de datos y estado global
- Obtiene la BD con `AppDatabase.getDatabase(context)`.
- Crea `TareasViewModel` usando `TareasViewModelFactory` (DAO de tareas, DAO de categorias y `context`).
- Crea `HabitosViewModel` usando `HabitosViewModelFactory`.
- Observa estados con `collectAsState` / `collectAsStateWithLifecycle`:
  - lista de tareas
  - filtro de categoria
  - texto de busqueda
  - progreso general de habitos

### 2) Navegacion principal
- Crea `navController` con `rememberNavController()`.
- Define `NavHost` con rutas para:
  - tareas (`PantallaListaTareas`)
  - habitos (`PantallaHabitos`)
  - crear/editar tarea
  - categorias de tareas
  - categorias de habitos
  - configuracion
  - gestion de copias
- Actualmente el `startDestination` esta en `Rutas.PantallaHabitos.ruta`.

### 3) Top bar, menus y barras inferiores
- Muestra `TopAppBar` en rutas principales (`tareas` y `habitos`).
- Construye menu desplegable con:
  - **Copias de Seguridad**
    - Guardar backup
    - Restaurar backup (con confirmacion)
  - **Tablas de Referencia**
    - Categorias Tareas
    - Categorias Habitos
  - **Configuracion**
- En la pantalla de tareas, delega acciones en `AccionesTopBarTareas`.
- Cambia la barra inferior segun ruta:
  - `BarraNavegacion` para tareas/habitos raiz
  - `BarraNavegacionHabitos` dentro del modulo habitos

### 4) Permisos y notificaciones
- Solicita permiso de notificaciones en Android 13+ (`POST_NOTIFICATIONS`) via `rememberLauncherForActivityResult`.
- Al crear tarea por voz con IA valida, programa notificacion con `NotificationHelper.programarNotificacion(...)`.

### 5) Reconocimiento de voz + IA
- Lanza microfono con `RecognizerIntent`.
- Determina tipo de entrada segun ruta actual:
  - `TipoEntrada.TAREA`
  - `TipoEntrada.HABITO`
- En flujo de tareas:
  1. llama a `IAProcessor.procesarTexto(...)`
  2. parsea JSON a `IAResultTarea`
  3. construye `Tarea`
  4. inserta en BD y muestra `Toast`
- Si IA falla o devuelve `null`, usa fallback `guardarTareaSimple(...)`.
- Para habitos, actualmente muestra un `Toast` de deteccion (sin creacion automatica completa).

### 6) Backup y restore
- Exporta BD con `DatabaseBackup.exportDatabase(...)`.
- Importa BD con `DatabaseBackup.importDatabase(...)`.
- Muestra dos dialogos:
  - confirmacion antes de restaurar
  - aviso posterior para cerrar y reabrir app

### 7) Integracion con widget
- Lee flag `abrirVoz` desde `MainActivity.intent`.
- Si viene activo, abre escucha por voz automaticamente y limpia el extra.

## Funcion auxiliar destacada
`guardarTareaSimple(texto, viewModel, context)`:
- Crea una tarea basica cuando IA no esta disponible.
- Prioridad por defecto: `Prioridad.MEDIA`.
- Inserta en BD y notifica al usuario con `Toast`.

## Resumen rapido
`MiApp.kt` funciona como "punto de ensamblaje" de la app: conecta BD + ViewModels + navegacion + menus + voz/IA + backup/restore. Es el archivo donde se define el flujo global de la experiencia de usuario.
