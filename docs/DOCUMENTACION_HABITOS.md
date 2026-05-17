# Documentación funcional: módulo de Hábitos

## Objetivo
Este documento describe **cómo funciona hoy** la parte de hábitos, qué archivos participan en cada paso y qué partes están operativas.

## Qué está operativo ahora
- Navegación principal de hábitos con 3 pestañas: **Flash**, **Hábitos (listado)** y **Estadísticas**.
- Carga de hábitos desde Room y combinación con progreso diario.
- Marcado de progreso diario (toggle y suma).
- Selector de fecha compartido en Flash/Listado.
- Creación de hábito desde formulario (`CrearHabitoScreen`).
- Gestión de categorías de hábitos (alta, edición nombre/icono, borrado).

## Mapa rápido (proceso -> archivo)

### Entrada y navegación
- Orquestación general de app: `app/src/main/java/com/example/mistareasapp/MiApp.kt`
- Pantalla contenedora de hábitos (tabs): `app/src/main/java/com/example/mistareasapp/ui/screens/habits/PantallaHabitos.kt`

### Estado y lógica
- ViewModel principal: `app/src/main/java/com/example/mistareasapp/viewmodel/Habits/HabitosViewModel.kt`
  - Mantiene `vistaActual` (FLASH/LISTADO/ESTADISTICAS)
  - Mantiene `fechaSeleccionada`
  - Expone `habitosConProgreso`

### Persistencia (Room)
- DAO de hábitos: `app/src/main/java/com/example/mistareasapp/data/habits/HabitoDao.kt`
- Base de datos y migraciones: `app/src/main/java/com/example/mistareasapp/data/AppDatabase.kt`

### Pantallas de trabajo
- Flash: `app/src/main/java/com/example/mistareasapp/ui/screens/habits/PantallaHabitosFlash.kt`
- Listado: `app/src/main/java/com/example/mistareasapp/ui/screens/habits/PantallaHabitosListado.kt`
- Estadísticas: `app/src/main/java/com/example/mistareasapp/ui/screens/habits/PantallaHabitosEstadisticas.kt`
- Crear hábito: `app/src/main/java/com/example/mistareasapp/ui/screens/habits/PantallaCrearHabito.kt`
- Categorías: `app/src/main/java/com/example/mistareasapp/ui/screens/habits/PantallaGestionCategoriasHabitos.kt`

## Flujo funcional (paso a paso)

### 1) Abrir pestaña Hábitos
1. `MiApp.kt` navega a `Rutas.PantallaHabitos.ruta`.
2. `PantallaHabitos.kt` pinta tabs y decide qué subpantalla mostrar según `viewModel.vistaActual`.
3. `HabitosViewModel` ya tiene `habitosConProgreso` activo y la fecha actual seleccionada.

### 2) Ver hábitos del día (Flash)
1. `PantallaHabitosFlash.kt` observa `fechaSeleccionada` y `habitosConProgreso`.
2. Muestra `SelectorFechaConProgreso` (flechas día anterior/siguiente).
3. Para cada hábito, pinta estado y progreso del día.
4. Al pulsar acción:
   - Hábito simple: `toggleHabitoCompleto(...)`
   - Hábito cuantitativo: `incrementarProgreso(...)`
5. El ViewModel hace `upsertProgreso` en Room.
6. La UI se recompone automáticamente con el nuevo estado.

### 3) Ver listado semanal (pestaña Hábitos)
1. `PantallaHabitosListado.kt` usa el mismo flujo `habitosConProgreso`.
2. Muestra selector de fecha superior.
3. Pinta tarjetas por hábito con una vista semanal visual.
4. Sirve como vista de consulta más amplia (no solo flash diario).

### 4) Ver pestaña Estadísticas
1. `PantallaHabitosEstadisticas.kt` muestra una pantalla de métricas visuales.
2. Actualmente combina datos reales con valores de demostración para algunos bloques.
3. Se considera funcional para navegación/UX, pero no está cerrada al 100% como analítica final.

### 5) Crear un hábito
1. Desde UI se abre `CrearHabitoScreen` (`PantallaCrearHabito.kt`).
2. Usuario rellena nombre, descripción, frecuencia, objetivos, recordatorio, icono y color.
3. Al guardar:
   - Se crea objeto `Habito`
   - Se inserta con `viewModel.insertarHabito(...)`
4. Se vuelve a la pantalla anterior y el hábito aparece en los listados.

### 6) Gestionar categorías de hábitos
1. Abrir `PantallaGestionCategoriasHabitos.kt`.
2. Funciones disponibles:
   - Alta de categoría
   - Renombrado
   - Cambio de icono
   - Borrado
3. Todo persiste por `HabitosViewModel` -> `HabitoDao`.

## Qué hace cada capa (sin detalle técnico excesivo)
- **UI** (`ui/screens/habits/*`): pinta pantallas e invoca acciones.
- **ViewModel** (`HabitosViewModel.kt`): decide estado y reglas de negocio de progreso.
- **Room/DAO** (`HabitoDao.kt`): guarda y recupera hábitos/progreso/categorías.
- **DB/Migraciones** (`AppDatabase.kt`): asegura estructura y datos base.

## Estado real (resumen)

### Operativo
- Navegación entre tabs.
- Carga y render de hábitos desde BD.
- Progreso diario en Flash.
- Cambio de fecha y recálculo.
- Creación de hábitos.
- Gestión de categorías.

### Parcial / en evolución
- Estadísticas avanzadas (algunas secciones aún son orientativas).
- Vista semanal del listado: visualmente funcional, con margen para mejorar exactitud analítica.

## Datos iniciales y migración
- La BD de hábitos está integrada en versión `9`.
- `AppDatabase.kt` incluye migraciones de hábitos y carga de datos iniciales en escenarios de primera ejecución.

## Riesgos/observaciones actuales
- Hay comentarios y logs de depuración en varios archivos que convendría limpiar al cerrar la versión.
- Conviene validar en dispositivo real la coherencia entre:
  - progreso diario,
  - vista semanal,
  - y estadísticas.

## Checklist funcional rápido
- [ ] Se abre pestaña Hábitos y cambian tabs sin error.
- [ ] En Flash, marcar/desmarcar actualiza el estado al instante.
- [ ] Cambiar fecha con flechas actualiza datos mostrados.
- [ ] Crear hábito nuevo lo añade al listado.
- [ ] CRUD de categorías funciona (alta/editar icono/borrar).

## Última actualización
2026-05-01
