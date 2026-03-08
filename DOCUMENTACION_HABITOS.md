# 📊 Documentación Completa: Funcionalidad de Hábitos

**Proyecto**: MyTaskMyHabit  
**Fecha**: 2026-03-07  
**Versión BD**: 9

---

## 📑 Índice

1. [Visión General](#visión-general)
2. [Arquitectura de Datos](#arquitectura-de-datos)
3. [Modelos de Datos](#modelos-de-datos)
4. [Capa de Acceso a Datos (DAO)](#capa-de-acceso-a-datos-dao)
5. [ViewModel y Lógica de Negocio](#viewmodel-y-lógica-de-negocio)
6. [Interfaz de Usuario](#interfaz-de-usuario)
7. [Navegación](#navegación)
8. [Datos de Ejemplo](#datos-de-ejemplo)
9. [Flujo de Funcionamiento](#flujo-de-funcionamiento)
10. [Estado Actual y Pendientes](#estado-actual-y-pendientes)

---

## 🎯 Visión General

La funcionalidad de **Hábitos** permite a los usuarios:
- Crear y gestionar hábitos personales con seguimiento diario/semanal/mensual
- Visualizar el progreso en tres vistas diferentes (Flash, Listado, Estadísticas)
- Categorizar hábitos (Salud, Deporte, Aprendizaje, Bienestar, Personal)
- Definir objetivos cuantitativos (ej: 100 minutos de inglés/semana)
- Hacer seguimiento del cumplimiento diario con indicadores visuales
- Ver porcentaje de cumplimiento general

---

## 🗄️ Arquitectura de Datos

### Tablas de Base de Datos (Room)

```
┌─────────────────────┐
│  habitos_categorias │ ◄─┐
└─────────────────────┘   │
                          │
┌─────────────────────┐   │ FK
│      habitos        │ ──┘
└─────────────────────┘
         │ 1
         │
         │ N
         ▼
┌─────────────────────┐
│  habitos_historial  │  (Progreso diario)
└─────────────────────┘

┌─────────────────────┐
│ habitos_tareas_     │  (Tareas específicas del hábito)
│   especificas       │
└─────────────────────┘
```

### Versión de Base de Datos

- **Versión actual**: 9
- **Migraciones implementadas**: 5→6, 6→7, 7→8, 8→9
- **Migración 8→9**: Creó las tablas de hábitos y añadió datos de ejemplo

---

## 📦 Modelos de Datos

### 1. **Habito** (Entidad Principal)

**Archivo**: `data/habits/Habito.kt`

```kotlin
@Entity(tableName = "habitos")
data class Habito(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,                        // Ej: "Inglés"
    val descripcion: String? = null,           // Ej: "Practicar inglés"
    val categoriaId: Long = 0,                 // FK a habitos_categorias
    val fechaInicio: LocalDate = LocalDate.now(),
    val frecuencia: FrecuenciaHabito = FrecuenciaHabito.DIARIA,
    val vecesPorDia: Int = 1,                  // Ej: 3 veces al día
    val objetivoValor: Int? = null,            // Ej: 100 (minutos)
    val unidad: String? = null,                // Ej: "Minutos", "Pasos", "días"
    val objetivoRachaSemanas: Int = 4,         // Racha objetivo
    val recordatoriosActivos: Boolean = false,
    val horaRecordatorio: LocalTime? = null,
    val icono: String = "favorite",            // Nombre del icono Material
    val colorHex: String = "#FF0000",          // Color en hexadecimal
    val activo: Boolean = true                 // Si está activo o archivado
)
```

**Características clave**:
- Soporta hábitos **cuantitativos** (`objetivoValor` + `unidad`) y **de frecuencia** (`vecesPorDia`)
- Personalización visual con `icono` y `colorHex`
- Recordatorios programables

---

### 2. **CategoriaHabito**

**Archivo**: `data/habits/CategoriaHabito.kt`

```kotlin
@Entity(tableName = "habitos_categorias")
data class CategoriaHabito(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,      // Ej: "Salud", "Deporte"
    val icono: String = "category",
    val color: String = "#757575"
)
```

**Categorías predefinidas**:
1. Salud (`medical_services`, `#EF5350`)
2. Deporte (`fitness_center`, `#66BB6A`)
3. Aprendizaje (`school`, `#42A5F5`)
4. Bienestar (`self_improvement`, `#AB47BC`)
5. Personal (`person`, `#FFA726`)

---

### 3. **FrecuenciaHabito** (Enum)

**Archivo**: `data/habits/FrecuenciaHabito.kt`

```kotlin
enum class FrecuenciaHabito {
    DIARIA,    // Se debe cumplir cada día
    SEMANAL,   // Objetivo semanal
    MENSUAL    // Objetivo mensual
}
```

---

### 4. **HabitoHistorial** (Progreso Diario)

**Archivo**: `data/habits/HabitoHistorial.kt`

```kotlin
@Entity(
    tableName = "habitos_historial",
    foreignKeys = [
        ForeignKey(
            entity = Habito::class,
            parentColumns = ["id"],
            childColumns = ["habitoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HabitoHistorial(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitoId: Long,                    // FK a habitos
    val fecha: LocalDate = LocalDate.now(), // Día del registro
    val valorProgreso: Int = 0,            // Ej: 50 (minutos hechos)
    val completado: Boolean = false        // ¿Objetivo cumplido ese día?
)
```

**Función**:
- Guarda el progreso de cada hábito **por día**
- Permite calcular rachas y estadísticas
- `valorProgreso`: valor acumulado ese día
- `completado`: si alcanzó el objetivo

---

### 5. **TareaHabito** (Tareas Específicas)

**Archivo**: `data/habits/TareaHabito.kt`

```kotlin
@Entity(
    tableName = "habitos_tareas_especificas",
    foreignKeys = [...]
)
data class TareaHabito(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitoId: Long,
    val nombre: String,
    val descripcion: String? = null,
    val completada: Boolean = false
)
```

**Uso**: Para hábitos que se descomponen en sub-tareas (ej: "Limpiar casa" → "Cocina", "Baño")

---

### 6. **HabitoMetricas** (Estadísticas)

**Archivo**: `data/habits/HabitoMetricas.kt`

```kotlin
@Entity(tableName = "habitos_metricas")
data class HabitoMetricas(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitoId: Long,
    val rachaActual: Int = 0,      // Días consecutivos cumplidos
    val mejorRacha: Int = 0,       // Récord personal
    val totalCompletados: Int = 0  // Total de días cumplidos
)
```

**Estado**: Definido pero **no implementado aún** en UI/ViewModel

---

## 🔌 Capa de Acceso a Datos (DAO)

**Archivo**: `data/habits/HabitoDao.kt`

### Operaciones Principales

```kotlin
@Dao
interface HabitoDao {
    // --- HÁBITOS ---
    @Query("SELECT * FROM habitos ORDER BY id DESC")
    fun obtenerTodosLosHabitos(): Flow<List<Habito>>

    @Query("SELECT * FROM habitos WHERE activo = 1 ORDER BY categoriaId ASC")
    fun obtenerHabitosActivos(): Flow<List<Habito>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarHabito(habito: Habito): Long

    @Update
    suspend fun actualizarHabito(habito: Habito)

    @Delete
    suspend fun eliminarHabito(habito: Habito)

    // --- PROGRESO DIARIO ---
    @Query("SELECT * FROM habitos_historial WHERE habitoId = :habitoId AND fecha = :fecha LIMIT 1")
    suspend fun obtenerProgresoDiario(habitoId: Long, fecha: LocalDate): HabitoHistorial?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgreso(historial: HabitoHistorial)

    @Query("SELECT * FROM habitos_historial WHERE habitoId = :habitoId ORDER BY fecha ASC")
    fun obtenerHistorialCompleto(habitoId: Long): Flow<List<HabitoHistorial>>

    @Query("DELETE FROM habitos_historial WHERE habitoId = :habitoId")
    suspend fun eliminarHistorialDeHabito(habitoId: Long)

    // --- CATEGORÍAS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarCategoria(categoria: CategoriaHabito)

    @Update
    suspend fun actualizarCategoria(categoria: CategoriaHabito)

    @Delete
    suspend fun eliminarCategoria(categoria: CategoriaHabito)

    @Query("SELECT * FROM habitos_categorias")
    fun obtenerCategorias(): Flow<List<CategoriaHabito>>

    // --- TAREAS ESPECÍFICAS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTareaHabito(tarea: TareaHabito)

    @Query("SELECT * FROM habitos_tareas_especificas WHERE habitoId = :habitoId")
    fun obtenerTareasDeHabito(habitoId: Long): Flow<List<TareaHabito>>

    // --- ESTADÍSTICAS ---
    @Query("SELECT COUNT(*) FROM habitos_historial WHERE habitoId = :habitoId AND completado = 1")
    fun obtenerDiasCompletados(habitoId: Long): Flow<Int>

    @Query("SELECT fecha FROM habitos_historial WHERE habitoId = :habitoId AND completado = 1 ORDER BY fecha DESC")
    fun obtenerFechasCompletadas(habitoId: Long): Flow<List<LocalDate>>
}
```

**Total de métodos**: 17 funciones

---

## 🎛️ ViewModel y Lógica de Negocio

### HabitosViewModel

**Archivo**: `viewmodel/Habits/HabitosViewModel.kt`

#### Estado Observable

```kotlin
class HabitosViewModel(private val habitoDao: HabitoDao) : ViewModel() {
    
    // Tipo de vista activa (Flash, Listado, Estadísticas)
    var vistaActual by mutableStateOf(TipoVistaHabitos.FLASH)

    // Todos los hábitos
    val todosLosHabitos: Flow<List<Habito>> = habitoDao.obtenerTodosLosHabitos()

    // Categorías
    val categoriasHabitos: Flow<List<CategoriaHabito>> = habitoDao.obtenerCategorias()

    // Fecha seleccionada por el usuario
    private val _fechaSeleccionada = MutableStateFlow(LocalDate.now())
    val fechaSeleccionada = _fechaSeleccionada.asStateFlow()

    // Hábitos + Progreso del día seleccionado (combinados)
    val habitosConProgreso: StateFlow<List<HabitoConProgreso>> = combine(
        todosLosHabitos,
        _fechaSeleccionada
    ) { listaHabitos, fecha ->
        listaHabitos.map { habito ->
            val progreso = habitoDao.obtenerProgresoDiario(habito.id, fecha)
            HabitoConProgreso(habito, progreso)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

#### Métodos Principales

```kotlin
// Navegación entre vistas
fun cambiarVista(nuevaVista: TipoVistaHabitos)

// CRUD de hábitos
fun insertarHabito(habito: Habito)
fun actualizarHabito(habito: Habito)
fun eliminarHabito(habito: Habito)

// CRUD de categorías
fun insertarCategoriaHabito(categoria: CategoriaHabito)
fun actualizarCategoriaHabito(categoria: CategoriaHabito)
fun eliminarCategoriaHabito(categoria: CategoriaHabito)

// Progreso diario
fun cambiarFecha(nuevaFecha: LocalDate)
fun incrementarProgreso(habito: Habito, progresoActual: HabitoHistorial?)
fun toggleHabitoCompleto(habito: Habito, progresoActual: HabitoHistorial?)
```

---

### HabitoConProgreso (Data Class)

**Archivo**: `viewmodel/Habits/HabitoConProgreso.kt`

```kotlin
data class HabitoConProgreso(
    val habito: Habito,
    val progreso: HabitoHistorial? = null
) {
    val estaCompletado: Boolean = progreso?.completado ?: false
    val valorActual: Int = progreso?.valorProgreso ?: 0
}
```

**Función**: Combina un hábito con su progreso del día actual para facilitar el renderizado en UI

---

## 🖼️ Interfaz de Usuario

### Estructura de Navegación

```
PantallaHabitos (Contenedor principal)
├── Tab: Flash       → PantallaHabitosFlash
├── Tab: Hábitos     → PantallaHabitosListado
└── Tab: Estadísticas → PantallaHabitosEstadisticas
```

---

### 1. **PantallaHabitos** (Contenedor)

**Archivo**: `ui/screens/habits/PantallaHabitos.kt`

```kotlin
@Composable
fun PantallaHabitos(
    navController: NavHostController,
    viewModel: HabitosViewModel,
    modifier: Modifier = Modifier,
    progresoGeneral: Float = 0f
)
```

**Componentes**:
- `SecondaryTabRow` con 3 tabs
- Renderizado condicional según `viewModel.vistaActual`

---

### 2. **PantallaHabitosFlash** (Vista Rápida)

**Archivo**: `ui/screens/habits/PantallaHabitosFlash.kt`

**Características**:
- Selector de fecha con flechas `◀ 7 de marzo ▶`
- Indicador de cumplimiento: `85%` (en la esquina superior derecha)
- Lista vertical de hábitos del día con:
  - Icono y nombre del hábito
  - Progreso con barra visual
  - Botón `+` para incrementar (hábitos cuantitativos)
  - Checkbox para marcar como completado (hábitos de frecuencia)

**Componentes principales**:

```kotlin
@Composable
fun PantallaHabitosFlash(
    viewModel: HabitosViewModel,
    progresoGeneral: Float = 0f,
    modifier: Modifier = Modifier
)

@Composable
fun HabitoFlashItem(item: HabitoConProgreso, viewModel: HabitosViewModel)
```

**Cálculo de progreso**:
```kotlin
val totalGoal = habito.objetivoValor ?: habito.vecesPorDia
val porcentaje = (valorActual.toFloat() / totalGoal).coerceIn(0f, 1f)
```

---

### 3. **PantallaHabitosListado** (Vista Semanal)

**Archivo**: `ui/screens/habits/PantallaHabitosListado.kt`

**Características**:
- Selector de semana con navegación
- Cuadrícula semanal (L M X J V S D) mostrando estado de cada día
- FloatingActionButton para crear nuevo hábito
- Tarjetas con información detallada de cada hábito

**Componentes**:

```kotlin
@Composable
fun PantallaHabitosListado(viewModel: HabitosViewModel, modifier: Modifier = Modifier)

@Composable
fun HabitoListadoCard(item: HabitoConProgreso)

@Composable
fun SelectorSemana()  // Selector de semana con flechas

@Composable
fun CuadriculaSemana(...)  // Grid de 7 días
```

**Estado actual**: 
- ✅ UI implementada
- ⚠️ Lógica de cuadrícula semanal pendiente de conectar con datos reales

---

### 4. **PantallaHabitosEstadisticas**

**Archivo**: `ui/screens/habits/PantallaHabitosEstadisticas.kt`

**Estado actual**: 
- ✅ Estructura básica creada
- ⚠️ Gráficas y métricas pendientes de implementar

---

### 5. **PantallaGestionCategoriasHabitos**

**Archivo**: `ui/screens/habits/PantallaGestionCategoriasHabitos.kt`

**Funcionalidad**:
- CRUD completo de categorías de hábitos
- Diálogo de creación/edición con:
  - Nombre de categoría
  - Selector de icono (lista extensa)
  - Selector de color

**Estado actual**: ✅ Completamente implementado

---

## 🧭 Navegación

### Integración en MiApp.kt

```kotlin
// Navegación principal
composable(Rutas.PantallaHabitos.ruta) {
    PantallaHabitos(
        navController = navController,
        viewModel = habitosViewModel,
        modifier = Modifier.padding(innerPadding).fillMaxSize(),
        progresoGeneral = progresoGeneral
    )
}

// Gestión de categorías
composable("categorias_habitos") {
    PantallaGestionCategoriasHabitos(
        navController,
        habitosViewModel,
        modifier = Modifier.padding(innerPadding).fillMaxSize()
    )
}
```

### Barra de Navegación

**Archivo**: `ui/navigation/BarraNavegacion.kt`

```kotlin
BarraNavegacion(navController, rutaActual)
```

**Opciones**:
- Tareas
- **Hábitos** ← Acceso principal

### Menú Principal (TopAppBar)

```kotlin
DropdownMenu {
    // Submenu: Tablas de Referencia
    DropdownMenuItem(text = "Categorias Hábitos") {
        navController.navigate("categorias_habitos")
    }
}
```

---

## 📊 Datos de Ejemplo

### Hábitos Precargados (Migración 8→9)

La app crea automáticamente 4 hábitos de ejemplo en la primera instalación:

```kotlin
1. Inglés
   - Categoría: Aprendizaje (azul)
   - Tipo: Semanal
   - Objetivo: 100 Minutos/semana
   - Icono: language

2. Bisoprolol
   - Categoría: Salud (rojo)
   - Tipo: Diaria
   - Objetivo: 1 vez/día
   - Icono: medication

3. Pasos
   - Categoría: Deporte (verde)
   - Tipo: Semanal
   - Objetivo: 40,000 Pasos/semana
   - Icono: directions_run

4. Día sin Alcohol
   - Categoría: Bienestar (morado)
   - Tipo: Mensual
   - Objetivo: 18 días/mes
   - Icono: no_drinks
```

### Código de inserción (AppDatabase.kt)

```kotlin
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Verifica si la tabla está vacía
        val cursor = database.query("SELECT COUNT(*) FROM habitos")
        cursor.moveToFirst()
        val habitosExistentes = cursor.getInt(0)
        cursor.close()

        if (habitosExistentes == 0) {
            // Inserta categorías
            database.execSQL("INSERT INTO habitos_categorias ...")
            
            // Inserta hábitos de ejemplo
            database.execSQL("INSERT INTO habitos ...")
        }
    }
}
```

---

## ⚙️ Flujo de Funcionamiento

### 1. **Carga Inicial**

```
1. MainActivity lanza MisTareasApp()
2. MisTareasApp crea HabitosViewModel
3. HabitosViewModel observa habitoDao.obtenerTodosLosHabitos()
4. Room devuelve los hábitos desde SQLite
5. ViewModel combina hábitos + progreso del día actual
6. habitosConProgreso Flow emite la lista
7. UI se reenderiza automáticamente
```

### 2. **Marcar Hábito como Completado**

```
Usuario hace click en checkbox/botón
    ↓
PantallaHabitosFlash llama viewModel.toggleHabitoCompleto()
    ↓
ViewModel calcula nuevo estado
    ↓
ViewModel llama habitoDao.upsertProgreso()
    ↓
Room inserta/actualiza en habitos_historial
    ↓
Flow de habitosConProgreso emite nuevo valor
    ↓
UI se actualiza automáticamente (Compose recomposición)
```

### 3. **Cambiar de Fecha**

```
Usuario hace click en flecha ◀ o ▶
    ↓
viewModel.cambiarFecha(nuevaFecha)
    ↓
_fechaSeleccionada.value = nuevaFecha
    ↓
combine() en habitosConProgreso detecta cambio
    ↓
Consulta habitoDao.obtenerProgresoDiario() para nueva fecha
    ↓
UI muestra progreso del nuevo día
```

---

## 📋 Estado Actual y Pendientes

### ✅ Implementado

| Componente | Estado | Notas |
|------------|--------|-------|
| Modelos de datos | ✅ | Todos los entities creados |
| Base de datos | ✅ | Tablas creadas, migraciones OK |
| DAO | ✅ | 17 métodos implementados |
| ViewModel | ✅ | Flujos reactivos funcionando |
| Vista Flash | ✅ | Funcional con progreso diario |
| Vista Listado | ✅ | UI creada, pendiente lógica semanal |
| Vista Estadísticas | ⚠️ | Esqueleto creado |
| Gestión Categorías | ✅ | CRUD completo |
| Navegación | ✅ | Integrado en app principal |
| Datos de ejemplo | ✅ | 4 hábitos + 5 categorías |

---

### ⚠️ Funcionalidades Pendientes

#### Alta Prioridad

1. **Creación de Hábitos** ❌
   - Pantalla de formulario
   - Validaciones
   - Guardado en BD

2. **Edición de Hábitos** ❌
   - Pantalla de edición
   - Actualización de valores

3. **Lógica Semanal en Vista Listado** ⚠️
   - Conectar cuadrícula con datos reales
   - Calcular progreso semanal

4. **Estadísticas** ⚠️
   - Gráficas de progreso
   - Cálculo de rachas
   - Métricas de cumplimiento

#### Media Prioridad

5. **Recordatorios** ❌
   - Notificaciones programadas
   - WorkManager integration

6. **Tareas Específicas** ❌
   - UI para gestionar sub-tareas
   - Checkboxes individuales

7. **Archivado de Hábitos** ❌
   - Filtro activo/archivado
   - Opción de archivar

8. **Integración con IA** ❌
   - Crear hábito desde voz
   - Parseo de texto a estructura

#### Baja Prioridad

9. **Exportar/Importar** ⚠️
   - Incluir hábitos en backup
   - Restore de hábitos

10. **Widgets** ❌
    - Widget de progreso diario
    - Quick actions

---

### 🐛 Problemas Conocidos

1. **Cuadrícula semanal no muestra datos reales**
   - Vista Listado renderiza UI pero no consulta historial
   
2. **Falta índice en Foreign Keys**
   - Warning de Room sobre performance
   - Recomendación: Añadir `indices = [Index("habitoId")]`

3. **HabitoMetricas definido pero no usado**
   - Tabla creada pero sin lógica de cálculo
   - ViewModel no la consulta

---

### 🎯 Próximos Pasos Recomendados

**Sprint 1** (Funcionalidad básica):
1. Implementar pantalla de creación de hábitos
2. Implementar pantalla de edición
3. Conectar cuadrícula semanal con datos

**Sprint 2** (Estadísticas):
1. Implementar cálculo de rachas
2. Crear gráficas de progreso
3. Usar tabla HabitoMetricas

**Sprint 3** (Features avanzados):
1. Sistema de recordatorios
2. Tareas específicas
3. Integración con IA de voz

---

## 📝 Notas Técnicas

### Dependencias Requeridas

```gradle
// Room
implementation "androidx.room:room-runtime:2.x.x"
implementation "androidx.room:room-ktx:2.x.x"
ksp "androidx.room:room-compiler:2.x.x"

// Compose
implementation "androidx.compose.material3:material3"
implementation "androidx.lifecycle:lifecycle-viewmodel-compose"

// Flows
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android"
```

### Convenciones de Código

- **Nombres de tablas**: `habitos`, `habitos_categorias`, `habitos_historial`
- **Nombrado de archivos**: PascalCase para clases, camelCase para funciones
- **Package structure**: `data/habits/`, `viewmodel/Habits/`, `ui/screens/habits/`
- **Logs de debug**: Tag `HABITOS_VM`, `HABITOS_DEBUG`

### Performance

- ✅ Uso de `Flow` para observación reactiva
- ✅ `StateFlow` con `WhileSubscribed(5000)` para lifecycle awareness
- ✅ Foreign Keys con CASCADE para integridad referencial
- ⚠️ Faltan índices en algunas columnas FK (Room warning)

---

## 📞 Información de Contacto

**Desarrollador**: [Tu nombre]  
**Última actualización**: 2026-03-07  
**Versión del documento**: 1.0

---

## 🔗 Archivos Relacionados

```
Modelos:
├── data/habits/Habito.kt
├── data/habits/CategoriaHabito.kt
├── data/habits/FrecuenciaHabito.kt
├── data/habits/HabitoHistorial.kt
├── data/habits/HabitoMetricas.kt
└── data/habits/TareaHabito.kt

DAO:
└── data/habits/HabitoDao.kt

ViewModels:
├── viewmodel/Habits/HabitosViewModel.kt
├── viewmodel/Habits/HabitosViewModelFactory.kt
└── viewmodel/Habits/HabitoConProgreso.kt

Pantallas:
├── ui/screens/habits/PantallaHabitos.kt
├── ui/screens/habits/PantallaHabitosFlash.kt
├── ui/screens/habits/PantallaHabitosListado.kt
├── ui/screens/habits/PantallaHabitosEstadisticas.kt
└── ui/screens/habits/PantallaGestionCategoriasHabitos.kt

Componentes:
└── ui/components/habits/SelectorFechaConProgreso.kt  ← NUEVO

Base de Datos:
└── data/AppDatabase.kt (MIGRATION_8_9)

Navegación:
├── ui/navigation/BarraNavegacion.kt
└── MiApp.kt (routing)
```

---

**FIN DEL DOCUMENTO**


