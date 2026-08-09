# Estado del módulo de Hábitos — MyTaskMyHabit

---

## Estado rapido (2026-05-31) — actualizado tras iteración 10

### Lo que llevamos

- Modulo de habitos operativo en flujo principal (CRUD, historial, progreso diario/semanal/mensual y estadisticas base).
- UI de Habitos estable en Flash, Listado semanal y vista mensual.
- Filtro por categoria y agrupacion por categoria disponibles en Flash y Listado.
- Historial de tareas por fecha operativo (detalle por tarea y por dia en dialogos).
- Copia/restauracion JSON del modulo de habitos disponible desde menu.
- Logica de cumplimiento documentada en un apartado especifico: `Calculo actual del porcentaje de cumplimiento`.
- Ejecucion de la app verificada en dispositivo fisico por USB.
- **Iteración 10 completada**: porcentaje histórico excluye días pausados; objetivo visible en tarjeta Semana; hoy marcado con color primario consistente; media ponderada por días de vida efectivos en barra general; pantalla "Cálculos" disponible desde menú.

### Lo que queda

- Completar backfill historico del detalle por tarea en fechas antiguas sin filas en `tareas_habito_historial`.
- Valorar extension de `diasSemana` a habitos no diarios (si aplica funcionalmente).
- Implementar ordenacion manual y mejoras UX pendientes; completar bloque de notificaciones/widgets.
- Resolver y estabilizar entorno de emulador (actualmente incidencia de conexion del AVD).

---

## ✅ Implementado

### Estado funcional verificado (2026-05-31)

- Ejecucion correcta en dispositivo fisico por USB.
- Incidencia de conexion en emulador detectada como problema de entorno (no de logica del modulo de habitos).
- Documentacion de calculo de porcentaje de cumplimiento anadida y alineada con la implementacion actual.

### Modelo de datos (Room)

| Entidad | Campos destacados |
|---|---|
| `Habito` | id, nombre, descripcion, icono, colorHex, frecuencia (DIARIA/SEMANAL/MENSUAL), tipoObjetivo (FRECUENCIA/CUANTITATIVO), objetivoValor, unidad, vecesPorDia, esCompuestoPorTareas, minimoTareasCumplimiento, recordatoriosActivos, horaRecordatorio, pausado, fechaInicioPausa, fechaFinPausa, categoriaId, activo, **objetivoPorcentajeDias**, diasSemana, puedeSuperar100 |
| `HabitoHistorial` | id, habitoId, fecha, valorProgreso, completado |
| `TareaHabito` | id, habitoId, nombre, descripcion, completada |
| `TareaHabitoHistorial` | id, tareaId, habitoId, fecha, completada |
| `CategoriaHabito` | id, nombre, colorHex |

- Migraciones Room hasta versión 16
- `HabitoDao` con todas las operaciones CRUD, queries de historial por fecha/rango, deduplicación con `maxByOrNull { it.id }` y `ORDER BY id DESC LIMIT 1`
- Historial de tareas por fecha en `tareas_habito_historial` (lectura/escritura por `habitoId + fecha`)
- Soporte de `diasSemana` para hábitos DIARIOS (ej. lunes/miercoles/viernes)

---

### ViewModel (`HabitosViewModel`)

- **`habitosConProgreso`** — StateFlow para la vista Flash (progreso del día seleccionado)
- **`habitosConHistorialSemanal`** — StateFlow para la vista Listado (historial completo de la semana)
- **`estadisticasHabito`** — StateFlow con racha actual, mejor racha, totales semana/mes/año
- Lógica de periodo capado a hoy (`minOf(finPeriodo, LocalDate.now())`) para evitar conteos futuros
- Deduplicación de entradas duplicadas por fecha
- Fix de race condition en taps rápidos (`toggleHabitoCompleto` re-consulta DB antes de escribir)
- CRUD completo: insertar, actualizar, eliminar hábitos, categorías y tareas
- `pausarHabito` / `despausarHabito` con fecha de inicio/fin de pausa
- `incrementarProgreso`, `toggleHabitoCompleto`, `toggleHabitoCompletoEnFecha`
- `registrarCumplimientoTareas`, `registrarCumplimientoCuantitativo`
- `obtenerEstadoTareasPorFecha` (Flow de detalle por tarea y fecha)
- `obtenerHistorialMes` (Flow reactivo para vista mensual)
- `calcularPieMensualVersionado` (objetivo/progreso mensual con versionado y descuento por pausas)
- `cambiarFecha`, `cambiarVista`

---

### Pantallas UI

#### Vista Flash (`PantallaHabitosFlash`)
- Selector de fecha con barra de progreso general del día
- Lista de hábitos activos con card compacta (emoji + nombre + frecuencia/progreso)
- **Checkbox 3 estados** (mismo patrón en ambas vistas):
  - 🟢 Verde pastel — completado o con progreso válido
  - 🟠 Naranja pastel — hábito de tareas con cumplimiento parcial
  - ⬜ Blanco — sin progreso
- Contenido del checkbox según tipo:
  - Diario simple → ✓ o vacío
  - Cuantitativo → `X%` del periodo
  - Por tareas + cumplido → ✓
  - Por tareas + parcial → `X/Y tareas`
  - No diario (semanal/mensual) → `X/Y días`
- Barra de progreso para hábitos cuantitativos
- Navegación a editar hábito
- **Diálogo de tareas**: inicializa todas las tareas sin marcar para fechas distintas a hoy
- **Diálogo de tareas**: si no hay historial en esa fecha, inicializa sin marcar
- **Diálogo de tareas**: carga detalle guardado por fecha (`TareaHabitoHistorial`) y permite editar días pasados
- **Diálogo cuantitativo**: input manual + botones rápidos (+/- escala automática según objetivo) + barra de progreso

#### Vista Listado / Semanal (`PantallaHabitosListado`)
- Selector de semana (navegación anterior/siguiente) con barra de progreso semanal general
- Card por hábito con grid de 7 días (lunes → domingo)
- **Cuadrados del grid** con mismo esquema de 3 colores que Flash:
  - Verde pastel si `completado = true`
  - Verde pastel si cuantitativo no-diario y `valorProgreso > 0` (cualquier registro cuenta)
  - Naranja pastel si tareas con cumplimiento parcial
  - Blanco si pasado sin registrar
  - Gris claro si fecha futura
- Contenido del cuadrado: ✓ si completado, `X%` si cuantitativo con progreso, `X/Y` si tareas parcial, número del día en el resto
- Badge de % semanal (verde ≥80%, naranja ≥50%, rojo <50%)
- Botones por card: ✏️ Editar — 📅 Vista mensual — ⏸ Pausar/▶ Reanudar
- Diálogo de pausa/reanudación con DatePicker
- Clic en día pasado abre diálogo de tareas o cuantitativo según tipo, o toggle directo

#### Vista Mensual (`PantallaVistaMensual`)
- Calendario mensual reactivo con estado por día

#### Gestión de hábitos (`PantallaMantenimientoHabitos`)
- Lista de hábitos con editar/eliminar
- Confirmación obligatoria antes de eliminar (con aviso de borrado de historial)

#### Hábitos pausados (`PantallaPausados`)
- Vista dedicada de hábitos pausados
- Reanudación con DatePicker

#### Pantalla Crear Hábito (`PantallaCrearHabito`)
- Formulario completo: nombre, descripción, icono (emoji), color, frecuencia, tipo objetivo, unidad, veces por día, tareas específicas, categoría
- Toggle para definir objetivo como **% de días del periodo** (`objetivoPorcentajeDias`)

#### Pantalla Editar Hábito (`PantallaEditarHabito`)
- Mismas opciones que Crear, precargadas con los valores actuales
- Soporte para `objetivoPorcentajeDias`

#### Estadísticas (`PantallaEstadisticasHabitos`)
- Racha actual y mejor racha
- Completados en semana / mes / año con % respecto a días transcurridos

---

## 📊 Cálculo actual del porcentaje de cumplimiento

> Referencia unificada de como se calcula y muestra el % en la implementacion actual.

### Regla base por tipo de hábito

- **Simple (check diario)**: porcentaje binario para el dia evaluado (`0%` o `100%`).
- **Cuantitativo**: porcentaje segun `valorProgreso` frente al objetivo configurado del periodo aplicable.
- **Por tareas**: porcentaje segun tareas completadas frente al total/minimo requerido; puede quedar en estado parcial.
- **No diario (semanal/mensual)**: porcentaje por dias cumplidos (`X/Y dias`) dentro del periodo.

### Vista Flash (`PantallaHabitosFlash`)

- **Progreso general del dia**: `habitosCompletados / totalHabitos * 100`.
- En app se obtiene desde `habitosConProgreso`:
  - `totalHabitos = habitosConProgreso.size`
  - `habitosCompletados = habitosConProgreso.count { it.estaCompletado }`
- **Por card**:
  - Simple diario: check/no check.
  - Cuantitativo: `%` del objetivo del periodo.
  - Por tareas: `X/Y tareas` cuando es parcial; check si cumple minimo.
  - No diario: `X/Y dias` del periodo.

### Vista Listado / Semanal (`PantallaHabitosListado`)

- El badge semanal muestra el `%` de cumplimiento del habito en la semana visible.
- Semaforo visual del badge:
  - Verde: `>= 80%`
  - Naranja: `>= 50%`
  - Rojo: `< 50%`
- En el grid diario:
  - Verde si `completado = true`.
  - Verde en cuantitativo no diario si `valorProgreso > 0`.
  - Naranja si tareas parciales.
  - Blanco si pasado sin registro.
  - Gris si fecha futura.

### Estadísticas (`PantallaEstadisticasHabitos`)

- `% cumplimiento periodo = completados / dias transcurridos * 100`.
- Se evita contar futuro capando el periodo a hoy (`minOf(finPeriodo, LocalDate.now())`).
- Incluye racha actual y mejor racha como metricas complementarias.

### Vista Mensual (`PantallaVistaMensual`)

- El calendario refleja estado por dia con base en historial reactivo.
- Cuando se expresa porcentaje mensual agregado, se interpreta como dias cumplidos sobre dias transcurridos del mes (sin incluir fechas futuras).

### Casos importantes y limitaciones actuales

- Fechas futuras no deben penalizar el porcentaje.
- En habitos por tareas, el estado parcial se refleja visualmente y en `%`, pero el check completo depende del `minimoTareasCumplimiento`.
- **Limitacion conocida**: el detalle por tarea y fecha existe, pero para periodos anteriores a su guardado en `tareas_habito_historial` solo queda el agregado (`valorProgreso`).

---

### Diseño visual
- Cards color `#ADADAD` (gris neutro) sobre fondo oscuro `#0D0C12`
- Texto oscuro en cards: `#1A1A1A` (principal), `#555555` (secundario), `#444444` (iconos)
- Emojis sin fondo Surface
- Cards: `RoundedCornerShape(20.dp)`, elevación 2dp
- Checkboxes/cuadrados: `RoundedCornerShape(8.dp)`, borde 1.5dp
- Colores semánticos coherentes: verde `#A8D5A2` / `#7CB87A`, naranja `#FFCB87` / `#FFAA50`

---

## 🔲 Pendiente / Por implementar

### Entorno / QA
- [ ] Estabilizar el emulador (arranque/conexion AVD) para pruebas regulares sin depender solo de dispositivo fisico

### Funcionalidad core
- [ ] **Backfill del detalle histórico por tarea** — el detalle en `tareas_habito_historial` existe desde su incorporación (migración 13→14), pero los días antiguos sin filas detalladas no pueden reconstruirse automáticamente solo con `valorProgreso`
- [ ] **Extender días específicos a hábitos no diarios (si aplica a producto)** — actualmente el filtro por `diasSemana` se usa en hábitos DIARIOS
- [ ] **Ordenación manual de hábitos** — drag & drop para reordenar la lista

### Notificaciones y recordatorios
- [ ] Motor de recordatorios de hábitos (programación + disparo de notificación) usando `recordatoriosActivos` y `horaRecordatorio`
- [ ] Notificación de racha en riesgo (no has registrado el hábito hoy)
- [ ] Resumen diario push (cuántos hábitos completados del día)

### Widgets
- [ ] Widget de pantalla de inicio con progreso del día (hábitos completados / total)
- [ ] Widget de racha de un hábito concreto

### Estadísticas avanzadas
- [ ] Gráfica de barras semanal/mensual por hábito
- [ ] Gráfica de línea de racha histórica
- [ ] Heatmap anual estilo GitHub (365 días)
- [ ] Comparativa entre periodos

### Datos y configuración
- [ ] Exportar historial a CSV (JSON de hábitos ya implementado)
- [ ] Importar/restaurar completo multipaquete (hoy existe restore JSON específico de hábitos)
- [ ] Sincronización en la nube (Google Drive / Firebase)

### UX menor
- [ ] Animación al marcar hábito como completado (confetti / pulso)
- [ ] Swipe para marcar/desmarcar hábito en Flash sin abrir diálogo
