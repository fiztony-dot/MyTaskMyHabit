# Documentación: Módulo de Tareas

## Objetivo
Describir el funcionamiento actual del módulo de tareas (sin IA remota), sus componentes principales y los flujos operativos.

## Alcance actual
- Crear tareas manualmente.
- Editar tareas existentes.
- Listar tareas por vencimiento y por categoría.
- Crear tareas por voz usando reconocimiento del sistema + parser local.
- Programar/cancelar notificaciones con WorkManager.
- Soporte de widget para acceso rápido.

## Arquitectura funcional

### Capa de datos
- Modelo: `app/src/main/java/com/example/mistareasapp/data/tasks/Tarea.kt`
  - Campos clave: `titulo`, `descripcion`, `prioridad`, `fechaLimite`, `horaLimite`, `categoria`, `repeticion`, `estaCompletada`.
- DAO: `app/src/main/java/com/example/mistareasapp/data/tasks/TareaDao.kt`
  - Lectura reactiva: `obtenerTodas()`.
  - CRUD: `insertar`, `actualizar`, `eliminar`.
  - Soporte notificaciones/boot: `obtenerTareaPorIdSincrona()`, `obtenerTareasPendientesSincronas()`.

### Capa de negocio
- ViewModel: `app/src/main/java/com/example/mistareasapp/viewmodel/Tasks/TareasViewModel.kt`
  - Filtrado por texto y categoría.
  - Clasificación por vencimiento (`vencidas`, `hoy`, `esta semana`, `este mes`, `resto`, `completadas`).
  - Vista por categorías.
  - Reglas de completado y repetición (`completarTarea`).

### Capa UI
- Lista principal: `app/src/main/java/com/example/mistareasapp/ui/screens/tasks/PantallaListaTareas.kt`
  - Tabs: vencimiento/categorías.
  - Buscador y barra de filtros.
- Crear tarea: `app/src/main/java/com/example/mistareasapp/ui/screens/tasks/PantallaCrearTarea.kt`
  - Formulario con fecha, hora, categoría, prioridad y repetición.
- Editar tarea: `app/src/main/java/com/example/mistareasapp/ui/screens/tasks/PantallaEditarTarea.kt`

### Voz (sin IA remota)
- Launcher de voz: `app/src/main/java/com/example/mistareasapp/core/ai/SpeechLauncher.kt`
- Parser local: `app/src/main/java/com/example/mistareasapp/core/voice/VozTaskParser.kt`
  - Soporta expresiones temporales (hoy, mañana, pasado mañana, día semana, franja mañana/tarde/noche, fechas tipo `15/05` y `15 de mayo`, y relativos `en 2 horas`).
  - No depende de API keys externas.

### Notificaciones
- Programación: `app/src/main/java/com/example/mistareasapp/core/notifications/tasks/NotificationHelper.kt`
- Ejecución: `NotificacionWorker.kt`
- Reprogramación al arrancar: `BootReceiver.kt`

### Widget
- Receiver Glance: `app/src/main/java/com/example/mistareasapp/ui/widgets/Tasks/TareaWidgetReceiver.kt`
- Widget clásico (configurable): `app/src/main/java/com/example/mistareasapp/ui/widgets/Tasks/MyAppWidget.kt`

## Flujos principales

### 1) Crear tarea manual
1. Usuario abre `PantallaCrearTarea`.
2. Completa formulario.
3. Se construye `Tarea` y se inserta en Room.
4. Se vuelve a la pantalla anterior.

### 2) Crear tarea por voz
1. Se lanza `RecognizerIntent` desde top bar o widget.
2. `SpeechLauncher` recibe texto reconocido.
3. `VozTaskParser.parse(...)` extrae título/fecha/hora/prioridad.
4. Se inserta la tarea y se muestra resumen por `Toast`.

### 3) Editar tarea
1. Usuario abre `PantallaEditarTarea`.
2. Se cargan datos por `id` desde ViewModel.
3. Usuario guarda cambios.
4. Se actualiza Room.

### 4) Completar tarea
1. `TareasViewModel.completarTarea(...)` marca completada.
2. Cancela works de notificación asociados.
3. Si la tarea tiene repetición, crea la siguiente instancia.

## Navegación y punto de entrada
- Orquestación principal: `app/src/main/java/com/example/mistareasapp/MiApp.kt`
- Inicio configurable en `NavHost(startDestination = ...)`.
- Actualmente puede iniciarse en tareas u hábitos según la constante activa en ese archivo.

## Limitaciones conocidas (estado actual)
- Parser local no cubre lenguaje natural complejo al nivel de un LLM.
- Algunas frases ambiguas pueden no mapear fecha/hora exacta.
- Conviene mantener una batería de frases de regresión para no romper reglas ya soportadas.

## Checklist de validación rápida
- Crear tarea manual con fecha/hora/categoría/prioridad.
- Crear tarea por voz con frases:
  - `cita médica el 15 de mayo a las 10`
  - `pagar alquiler el 15`
  - `enviar informe en 2 horas`
- Completar tarea y verificar cancelación de notificación.
- Reiniciar app/dispositivo y comprobar reprogramación de pendientes.

## Última actualización
2026-05-01

