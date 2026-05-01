# Analisis de mensajeria y notificaciones

## 1) Mapa actual (version simple y estructurada)

### 1.1 Componentes principales
- **Permisos y registro**: `app/src/main/AndroidManifest.xml`
- **Programacion de recordatorios**: `app/src/main/java/com/example/mistareasapp/core/notifications/tasks/NotificationHelper.kt`
- **Ejecucion de recordatorios**: `app/src/main/java/com/example/mistareasapp/core/notifications/tasks/NotificacionWorker.kt`
- **Reprogramacion al reiniciar**: `app/src/main/java/com/example/mistareasapp/core/notifications/tasks/BootReceiver.kt`
- **Entrada por voz (crea tareas)**: `app/src/main/java/com/example/mistareasapp/core/ai/SpeechLauncher.kt`
- **Completar tarea (cancelar avisos)**: `app/src/main/java/com/example/mistareasapp/viewmodel/Tasks/TareasViewModel.kt`
- **Creacion manual de tarea**: `app/src/main/java/com/example/mistareasapp/ui/screens/tasks/PantallaCrearTarea.kt`

### 1.2 Flujo A - Cuando se crea una tarea por voz
1. Se reconoce texto en `SpeechLauncher`.
2. Se construye una `Tarea` y se inserta en BD.
3. Se llama a `NotificationHelper.programarNotificacion(...)`.
4. `NotificationHelper` crea dos trabajos en WorkManager:
   - `notif_{id}_principal`
   - `notif_{id}_repeticion`
5. `NotificacionWorker` muestra notificacion cuando llega el momento.
6. Si es repeticion, el propio worker vuelve a programarse.

### 1.3 Flujo B - Cuando se completa una tarea
1. `TareasViewModel.completarTarea(...)` marca la tarea como completada.
2. Se cancelan trabajos por nombre unico:
   - `notif_{id}`
   - `notif_{id}_principal`
   - `notif_{id}_repeticion`
3. Si habia repeticion de tarea de negocio, se crea la siguiente tarea.

### 1.4 Flujo C - Al reiniciar el movil
1. `BootReceiver` recibe `BOOT_COMPLETED`.
2. Lee tareas pendientes desde Room.
3. Reprograma recordatorios con `NotificationHelper` para cada tarea.

### 1.5 Flujo D - Creacion manual desde pantalla
1. `PantallaCrearTarea` guarda tarea con `viewModel.insertar(...)`.
2. No se ve llamada explicita a `NotificationHelper` en esta pantalla.
3. Resultado: segun flujo, puede haber tareas sin recordatorio programado.

---

## 2) Hallazgos (priorizados)

### Critico
- **Programacion con ID no persistido en flujo voz**
  - Archivo: `app/src/main/java/com/example/mistareasapp/core/ai/SpeechLauncher.kt`
  - Reseña simple: la app intenta preparar el aviso demasiado pronto, cuando la tarea todavia no tiene su identificador definitivo.
  - Problema: se programa notificacion usando una `Tarea` creada en memoria (posible `id = 0`) antes de asegurar ID real de Room.
  - Impacto: colisiones de trabajos (`notif_0_*`), sobrescrituras y comportamiento inestable.

### Alto
- **Posible falta de permiso para reprogramar tras reinicio**
  - Archivo: `app/src/main/AndroidManifest.xml`
  - Reseña simple: despues de apagar y encender el movil, la app puede "olvidar" volver a activar los recordatorios.
  - Problema: hay `BootReceiver` para `BOOT_COMPLETED`, pero debe validarse que el permiso requerido para ese evento este correctamente declarado.
  - Impacto: notificaciones no reprogramadas tras reiniciar.

- **Inconsistencia entre flujos de alta de tarea**
  - Archivo: `app/src/main/java/com/example/mistareasapp/ui/screens/tasks/PantallaCrearTarea.kt`
  - Reseña simple: crear una tarea por una via puede activar avisos, y por otra via no; para el usuario parece aleatorio.
  - Problema: crear tarea manual no muestra programacion de recordatorio en el mismo flujo.
  - Impacto: experiencia inconsistente (unas tareas avisan y otras no).

### Medio
- **Estrategia de cancelacion no totalmente uniforme**
  - Archivos:
    - `app/src/main/java/com/example/mistareasapp/core/notifications/tasks/NotificationHelper.kt`
    - `app/src/main/java/com/example/mistareasapp/core/notifications/tasks/NotificacionWorker.kt`
    - `app/src/main/java/com/example/mistareasapp/viewmodel/Tasks/TareasViewModel.kt`
  - Reseña simple: a veces se usan reglas distintas para quitar avisos, y eso puede dejar recordatorios activos por error.
  - Problema: se mezclan cancelaciones por nombre unico y por tags con convenciones distintas.
  - Impacto: riesgo de dejar trabajos huerfanos en escenarios borde.

- **Doble solicitud de permiso de notificaciones**
  - Archivos:
    - `app/src/main/java/com/example/mistareasapp/MainActivity.kt`
    - `app/src/main/java/com/example/mistareasapp/MiApp.kt`
  - Reseña simple: la app pide el mismo permiso dos veces desde lugares distintos y puede confundir al usuario.
  - Problema: el mismo permiso se solicita desde dos sitios.
  - Impacto: UX confusa y mas complejidad de mantenimiento.

### Bajo
- **Permisos de alarmas exactas sin uso directo de AlarmManager**
  - Archivo: `app/src/main/AndroidManifest.xml`
  - Reseña simple: se piden mas permisos de los que realmente se usan, lo que complica la app sin aportar beneficio claro.
  - Problema: la estrategia actual usa WorkManager, no alarmas exactas directas.
  - Impacto: permisos extra que complican revisiones y soporte.

---

## 3) Recomendaciones

### 3.1 Arquitectura
- Centralizar la logica de programacion/cancelacion en un solo servicio de dominio (por ejemplo `TaskReminderScheduler`).
- Evitar que UI o flujos de voz programen directamente sin pasar por el mismo punto unico.

### 3.2 Consistencia de ID
- Programar notificaciones solo cuando exista ID real de BD.
- En insercion, recuperar entidad persistida (o ID insertado) y usar ese valor para nombres/tags.

### 3.3 Convenciones de WorkManager
- Definir una convencion unica:
  - Nombre unico: `notif_task_{id}_{tipo}`
  - Tag base: `notif_task_{id}`
- Cancelar siempre por tag base mas, si aplica, por nombre unico de cada tipo.

### 3.4 Permisos y arranque
- Revisar y dejar solo permisos necesarios.
- Verificar cadena completa de reprogramacion al reinicio en dispositivo real.

### 3.5 Cobertura funcional
- Alinear alta manual, edicion y voz para que todas sigan la misma regla de recordatorios.
- Documentar comportamiento esperado cuando no hay fecha/hora.

---

## 4) Plan de accion propuesto

## Fase 0 - Definicion (0.5 dia)
- Definir comportamiento oficial de recordatorios:
  - cuando programar,
  - cuando cancelar,
  - que pasa sin fecha/hora,
  - reglas de repeticion.
- Entregable: mini documento de reglas funcionales.

## Fase 1 - Estabilizacion tecnica (1-2 dias)
- Corregir flujo voz para usar ID persistido real.
- Unificar convenciones de `uniqueWorkName` y `tags`.
- Unificar punto de peticion de permiso de notificaciones.
- Entregable: recordatorios estables sin colisiones.

## Fase 2 - Consistencia de flujos (1 dia)
- Asegurar que crear/editar/completar tarea usa la misma capa de scheduling.
- Revisar reprogramacion tras boot y corregir permisos/eventos necesarios.
- Entregable: todos los caminos de tarea tienen comportamiento homogeneo.

## Fase 3 - Pruebas y observabilidad (1 dia)
- Añadir pruebas (unitarias/integracion) para:
  - alta manual,
  - alta por voz,
  - completar/cancelar,
  - reinicio.
- Mejorar logs de diagnostico en scheduling/cancelacion.
- Entregable: checklist de regresion y trazabilidad en logs.

## Fase 4 - Limpieza y evolucion (0.5-1 dia)
- Eliminar permisos o codigo no utilizados.
- Preparar extension a habitos reutilizando la misma capa de recordatorios.
- Entregable: base limpia y reutilizable para tareas y habitos.

---

## 5) Prioridad sugerida (orden de ejecucion)
1. Corregir ID persistido en flujo voz.
2. Unificar convenciones WorkManager (nombres/tags/cancelacion).
3. Alinear alta manual con scheduling.
4. Revisar boot + permisos reales requeridos.
5. Unificar solicitud de permisos y limpiar manifiesto.
6. Añadir pruebas de regresion.
