# Análisis de Base de Datos - MyTaskMyHabit

## Resumen General

- **Base de datos:** `tareas_db` (Room)
- **Versión actual:** 31
- **Entidades registradas:** 12
- **DAOs:** 4 (`TareaDao`, `CategoriaDao`, `HabitoDao`, `ListaCompraDao`)
- **TypeConverters:** `Prioridad`, `LocalDate`, `LocalTime`, `FrecuenciaHabito`, `TipoObjetivoHabito`, `CriterioCumplimientoTareas`, `TipoItemCompra`, `TiendaItem`
- **Journal Mode:** TRUNCATE

---

## 1. Segmento TAREAS (`data/tasks/`)

### Tablas

| Tabla | Entidad | Descripción |
|-------|---------|-------------|
| `tareas_table` | `Tarea` | Tareas del usuario |
| `categorias_table` | `Categoria` | Categorías para agrupar tareas |

### Entidad: Tarea

```kotlin
@Entity(tableName = "tareas_table")
data class Tarea(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titulo: String,
    val descripcion: String?,
    val estaCompletada: Boolean = false,
    val prioridad: Prioridad = Prioridad.MEDIA,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaLimite: LocalDate? = null,
    val horaLimite: LocalTime? = null,
    val categoria: String? = null,
    val repeticion: String = "Sin repetición",
    val pendienteClasificar: Boolean = false,
    val repeticionFin: LocalDate? = null,
    val repeticionVeces: Int? = null,
    val repeticionContador: Int = 0
)
```

**Campos destacados:**
- `prioridad`: Enum ALTA / MEDIA / BAJA
- `fechaLimite` + `horaLimite`: Vencimiento con detección automática de tareas vencidas (`estaVencida`)
- `repeticion`: String libre para configurar repetición
- `repeticionFin`, `repeticionVeces`, `repeticionContador`: Control de ciclo de repetición
- `pendienteClasificar`: Flag para tareas creadas por voz que necesitan revisión
- `categoria`: Referencia por nombre (String), sin FK formal

**Lógica de negocio en la entidad:**
- `toComparableDateTime()`: Convierte fecha+hora a `LocalDateTime` para ordenación
- `estaVencida`: Propiedad computada que evalúa si la tarea ha expirado

### Entidad: Categoria

```kotlin
@Entity(tableName = "categorias_table")
data class Categoria(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titulo: String,
    val icono: String = "list",
    val fechaCreacion: Long = System.currentTimeMillis(),
    val activa: Boolean = true
)
```

### Enum: Prioridad

```kotlin
enum class Prioridad(val etiqueta: String) {
    ALTA("Alta"),
    MEDIA("Media"),
    BAJA("Baja")
}
```

### TareaDao - Operaciones disponibles

| Operación | Método | Tipo retorno |
|-----------|--------|--------------|
| Obtener todas | `obtenerTodas()` | `Flow<List<Tarea>>` |
| Insertar una | `insertar(tarea)` | `Long` |
| Insertar varias | `insertAll(tareas)` | - |
| Actualizar | `actualizar(tarea)` | - |
| Eliminar una | `eliminar(tarea)` | - |
| Eliminar todas | `deleteAll()` | - |
| Obtener por ID (Flow) | `obtenerTareaPorId(id)` | `Flow<Tarea?>` |
| Obtener por ID (síncrona) | `obtenerTareaPorIdSincrona(id)` | `Tarea?` |
| Pendientes síncronas | `obtenerTareasPendientesSincronas()` | `List<Tarea>` |

**Ordenación por defecto:** `prioridad DESC, fechaCreacion ASC`

### CategoriaDao - Operaciones disponibles

| Operación | Método | Tipo retorno |
|-----------|--------|--------------|
| Obtener todas | `obtenerTodas()` | `Flow<List<Categoria>>` |
| Insertar | `insertar(categoria)` | - |
| Borrar todas | `borrarTodas()` | - |
| Eliminar una | `eliminar(categoria)` | - |
| Actualizar | `actualizar(categoria)` | - |

### TareaRepository

Singleton con acceso directo al DAO:
- `insertar(context, tarea)`: Inserta una tarea completa
- `insertarSimple(context, texto)`: Inserta tarea rápida desde voz (con descripción "Voz (Error IA)")

---

## 2. Segmento HÁBITOS (`data/habits/`)

### Tablas

| Tabla | Entidad | Propósito |
|-------|---------|-----------|
| `habitos` | `Habito` | Definición principal del hábito |
| `habitos_historial` | `HabitoHistorial` | Progreso diario (valor entero/decimal + completado) |
| `habitos_categorias` | `CategoriaHabito` | Categorías con icono, color, orden |
| `habitos_tareas_especificas` | `TareaHabito` | Sub-tareas de un hábito compuesto |
| `tareas_habito_historial` | `TareaHabitoHistorial` | Estado de sub-tareas por fecha |
| `habitos_versiones` | `HabitoVersion` | Versionado de definición (cálculo histórico) |
| `habitos_pausas` | `HabitoPausa` | Períodos de pausa [inicio, fin) |
| `habitos_metricas` | `HabitoMetricas` | Cache de rachas y totales |

### Entidad: Habito

```kotlin
@Entity(tableName = "habitos")
data class Habito(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val descripcion: String? = null,
    val categoriaId: Long = 0,
    val fechaInicio: LocalDate = LocalDate.now(),
    val frecuencia: FrecuenciaHabito = FrecuenciaHabito.DIARIA,
    val tipoObjetivo: TipoObjetivoHabito = TipoObjetivoHabito.FRECUENCIA,
    val vecesPorDia: Int = 1,
    val objetivoValor: Int? = null,
    val unidad: String? = null,
    val esCompuestoPorTareas: Boolean = false,
    val criterioCumplimientoTareas: CriterioCumplimientoTareas = CriterioCumplimientoTareas.TODAS,
    val minimoTareasCumplimiento: Int? = null,
    val objetivoRachaSemanas: Int = 4,
    val recordatoriosActivos: Boolean = false,
    val horaRecordatorio: LocalTime? = null,
    val icono: String = "favorite",
    val colorHex: String = "#FF0000",
    val activo: Boolean = true,
    val fechaModificacion: LocalDate? = null,
    val pausado: Boolean = false,
    val fechaInicioPausa: LocalDate? = null,
    val fechaFinPausa: LocalDate? = null,
    val objetivoPorcentajeDias: Int? = null,
    val diasSemana: String? = null,           // "1,3,5" = lun, mié, vie
    val puedeSuperar100: Boolean = false,      // Legacy
    val tipoMedicion: TipoMedicion = TipoMedicion.PROPORCIONAL_CON_TOPE,
    val dificultad: Int = 3,                   // 1-5, peso para métrica ponderada
    val limiteMaximo: Double? = null,
    val tramosLimite: String? = null,          // JSON de tramos
    val ubeActivo: Boolean = false,
    val orden: Int = 0,
    val archivado: Boolean = false
)
```

### Enums de Hábitos

```kotlin
enum class FrecuenciaHabito {
    DIARIA, SEMANAL, MENSUAL
}

enum class TipoObjetivoHabito {
    FRECUENCIA,
    CUANTITATIVO,
    LIMITE_MAXIMO
}

enum class CriterioCumplimientoTareas {
    TODAS,
    PARCIAL
}

enum class TipoMedicion {
    BINARIO,                  // 0% o 100%
    PROPORCIONAL_CON_TOPE,    // % real, máximo 100%
    PROPORCIONAL_SIN_TOPE     // % real sin límite
}
```

### Entidad: HabitoHistorial

```kotlin
@Entity(
    tableName = "habitos_historial",
    foreignKeys = [ForeignKey(entity = Habito::class, parentColumns = ["id"],
        childColumns = ["habitoId"], onDelete = CASCADE)]
)
data class HabitoHistorial(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitoId: Long,
    val fecha: LocalDate = LocalDate.now(),
    val valorProgreso: Int = 0,
    val completado: Boolean = false,
    val valorProgresoDecimal: Double = 0.0
)
```

### Entidad: CategoriaHabito

```kotlin
@Entity(tableName = "habitos_categorias")
data class CategoriaHabito(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val icono: String = "category",
    val color: String = "#757575",
    val orden: Int = 0
)
```

### Entidad: TareaHabito

```kotlin
@Entity(
    tableName = "habitos_tareas_especificas",
    foreignKeys = [ForeignKey(entity = Habito::class, parentColumns = ["id"],
        childColumns = ["habitoId"], onDelete = CASCADE)]
)
data class TareaHabito(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitoId: Long,
    val nombre: String,
    val descripcion: String? = null,
    val completada: Boolean = false
)
```

### Entidad: TareaHabitoHistorial

```kotlin
@Entity(
    tableName = "tareas_habito_historial",
    indices = [
        Index(value = ["tareaId", "fecha"]),
        Index(value = ["habitoId", "fecha"])
    ]
)
data class TareaHabitoHistorial(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tareaId: Long,
    val habitoId: Long,
    val fecha: LocalDate,
    val completada: Boolean
)
```

### Entidad: HabitoVersion

```kotlin
@Entity(
    tableName = "habitos_versiones",
    indices = [Index(value = ["habitoId", "fechaInicio"])]
)
data class HabitoVersion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitoId: Long,
    val fechaInicio: LocalDate,
    val frecuencia: FrecuenciaHabito = FrecuenciaHabito.DIARIA,
    val tipoObjetivo: TipoObjetivoHabito = TipoObjetivoHabito.FRECUENCIA,
    val vecesPorDia: Int = 1,
    val objetivoValor: Int? = null,
    val unidad: String? = null,
    val esCompuestoPorTareas: Boolean = false,
    val minimoTareasCumplimiento: Int? = null,
    val objetivoPorcentajeDias: Int? = null,
    val puedeSuperar100: Boolean = false,
    val diasSemana: String? = null,
    val tipoMedicion: TipoMedicion = TipoMedicion.PROPORCIONAL_CON_TOPE,
    val limiteMaximo: Double? = null,
    val tramosLimite: String? = null
)
```

**Propósito:** Cada vez que cambia la definición de un hábito se crea una nueva versión. Esto permite recalcular el cumplimiento histórico con los parámetros vigentes en cada período.

### Entidad: HabitoPausa

```kotlin
@Entity(
    tableName = "habitos_pausas",
    indices = [Index(value = ["habitoId"])]
)
data class HabitoPausa(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitoId: Long,
    val fechaInicio: LocalDate,
    val fechaFin: LocalDate? = null   // null = pausa activa
)
```

### Entidad: HabitoMetricas

```kotlin
@Entity(
    tableName = "habitos_metricas",
    foreignKeys = [ForeignKey(entity = Habito::class, parentColumns = ["id"],
        childColumns = ["habitoId"], onDelete = CASCADE)],
    indices = [Index("habitoId")]
)
data class HabitoMetricas(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitoId: Long,
    val rachaActual: Int = 0,
    val mejorRacha: Int = 0,
    val totalCompletados: Int = 0
)
```

### Lógica de Límite Máximo (LimiteMaximoHelper)

```kotlin
data class TramoLimite(val desde: Double, val hasta: Double?, val porcentaje: Int)

// Tipos de bebida UBE
enum class TipoBebidaUBE(val label: String, val mlPorUbe: Double) {
    CERVEZA_SIDRA("Cerveza / Sidra", 200.0),
    VINO("Vino", 100.0),
    DESTILADOS("Destilados", 50.0)
}
```

- `calcularPorcentajeLimite(valor, limiteMaximo, tramosJson)`: Calcula % de cumplimiento según tramos
- `mlAUbe(ml, tipo)`: Convierte mililitros a Unidades de Bebida Estándar

### Relaciones (FK con CASCADE)

```
Habito ──1:N──> HabitoHistorial (CASCADE)
Habito ──1:N──> TareaHabito (CASCADE) ──1:N──> TareaHabitoHistorial
Habito ──1:N──> HabitoVersion
Habito ──1:N──> HabitoPausa
Habito ──1:N──> HabitoMetricas (CASCADE)
CategoriaHabito ──referenciada por── Habito.categoriaId (sin FK formal)
```

### HabitoDao - Operaciones principales

| Grupo | Operaciones |
|-------|-------------|
| **Hábitos** | insertar, eliminar, actualizar, obtenerTodos (activos/archivados), actualizarOrden |
| **Historial** | upsertProgreso, obtenerProgresoDiario, obtenerHistorialCompleto, obtenerHistorialEntreFechas (suspend + Flow), eliminarHistorial |
| **Categorías** | insertar, actualizar, eliminar, obtenerCategorias |
| **Tareas** | insertar (una/varias), actualizar, eliminar, obtenerTareasDeHabito, contarTareas, insertarHabitoConTareas (transacción) |
| **Tareas Historial** | upsertTareasHistorial, obtenerPorFechaFlow, eliminarPorFecha |
| **Versiones** | insertarVersion, obtenerVersiones, eliminarVersiones |
| **Pausas** | insertarPausa, obtenerPausas (suspend + Flow), cerrarPausaActiva, eliminarPausas |
| **Observabilidad** | observarCambiosHistorial() (Flow<Int> para trigger de recálculo), obtenerDiasCompletados, obtenerFechasCompletadas |

---

## 3. Segmento SHOPPING (`data/shopping/`)

### Tablas

| Tabla | Entidad | Propósito |
|-------|---------|-----------|
| `lista_lugares` | `ListaLugar` | Ubicaciones de compra |
| `lista_categorias_producto` | `ListaCategoriaProducto` | Categorías de producto con color y orden |
| `lista_productos` | `ListaProducto` | Catálogo maestro de productos (~240 pre-cargados) |
| `lista_items` | `ListaItem` | Items añadidos a una lista de compra |

### Entidad: ListaLugar

```kotlin
@Entity(tableName = "lista_lugares")
data class ListaLugar(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val esDefault: Boolean = false
)
```

**Datos pre-cargados:** Barcelona (default), Madrid, Menorca

### Entidad: ListaCategoriaProducto

```kotlin
@Entity(tableName = "lista_categorias_producto")
data class ListaCategoriaProducto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val colorHex: String,
    val orden: Int = 0
)
```

**14 categorías pre-cargadas:**
1. Lácteos (#FF9800)
2. Bebidas (#2196F3)
3. Frutas (#4CAF50)
4. Verduras (#8BC34A)
5. Carnes y Pescados (#F44336)
6. Panadería (#FF5722)
7. Congelados (#00BCD4)
8. Pastas y Arroces (#FFC107)
9. Conservas (#795548)
10. Limpieza (#9C27B0)
11. Higiene (#E91E63)
12. Desayuno y Cereales (#FF6F00)
13. Snacks (#9E9E9E)
14. Otros (#607D8B)

### Entidad: ListaProducto

```kotlin
@Entity(
    tableName = "lista_productos",
    foreignKeys = [ForeignKey(entity = ListaCategoriaProducto::class,
        parentColumns = ["id"], childColumns = ["categoriaId"], onDelete = SET_NULL)],
    indices = [Index(value = ["categoriaId"])]
)
data class ListaProducto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val categoriaId: Long?,
    val aliases: String = "[]"   // JSON array para búsqueda flexible
)
```

### Entidad: ListaItem

```kotlin
@Entity(
    tableName = "lista_items",
    foreignKeys = [
        ForeignKey(entity = ListaProducto::class, parentColumns = ["id"],
            childColumns = ["productoId"], onDelete = CASCADE),
        ForeignKey(entity = ListaLugar::class, parentColumns = ["id"],
            childColumns = ["lugarId"], onDelete = CASCADE)
    ],
    indices = [Index(value = ["productoId"]), Index(value = ["lugarId"])]
)
data class ListaItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productoId: Long,
    val lugarId: Long,
    val cantidad: String = "1",
    val unidad: String = "",
    val marcadoComprado: Boolean = false,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val tipo: TipoItemCompra = TipoItemCompra.URGENTE,
    val tienda: TiendaItem = TiendaItem.NINGUNA
)
```

### Enums de Shopping

```kotlin
enum class TipoItemCompra {
    URGENTE, PLANIFICADO
}

enum class TiendaItem {
    NINGUNA, MERCADONA, ASIATICA
}
```

### Relaciones (FK formales)

```
ListaCategoriaProducto ──1:N──> ListaProducto (ON DELETE SET NULL)
ListaProducto ──1:N──> ListaItem (ON DELETE CASCADE)
ListaLugar ──1:N──> ListaItem (ON DELETE CASCADE)
```

### ListaCompraDao - Operaciones principales

| Grupo | Operaciones |
|-------|-------------|
| **Lugares** | obtenerLugares (Flow), insertar, actualizar, eliminar, obtenerDefault, contarItems, limpiarDefault |
| **Categorías** | obtenerCategorias (Flow), insertar, actualizar, eliminar, contarProductos, obtenerMaxOrden |
| **Productos** | obtenerProductos (Flow), obtenerPorCategoria, buscarProductos (LIKE), insertar, actualizar, eliminar, contarItems |
| **Items** | obtenerPorLugar (Flow, todos/pendientes), insertar, actualizar, eliminar, marcar comprado, cambiarTipo, cambiarTienda, eliminarComprados, contarPendientes |
| **Sync** | obtenerLugaresSync, obtenerItemsPendientesSync, obtenerTodosProductosSync |

---

## 4. Historial de Migraciones

| Versión | Cambio principal |
|---------|-----------------|
| 5 → 6 | Crear tabla `categorias` (tasks) |
| 6 → 7 | Preparación para hábitos (vacía) |
| 7 → 8 | Crear tablas de hábitos (habitos, historial, categorías, tareas) |
| 8 → 9 | Insertar hábitos y categorías de ejemplo |
| 9 → 10 | Añadir tipoObjetivo, esCompuestoPorTareas, criterioCumplimiento |
| 10 → 11 | Añadir fechaModificacion a habitos |
| 11 → 12 | Añadir pausado, fechaInicioPausa, fechaFinPausa |
| 12 → 13 | Añadir objetivoPorcentajeDias |
| 13 → 14 | Añadir diasSemana + crear tabla tareas_habito_historial |
| 14 → 15 | Añadir puedeSuperar100 |
| 15 → 16 | Crear tabla habitos_versiones + poblar con datos actuales |
| 16 → 17 | Añadir tipoMedicion (migrar desde puedeSuperar100) |
| 17 → 18 | Crear tabla habitos_pausas + migrar pausas existentes |
| 18 → 19 | Añadir dificultad (1-5) |
| 19 → 20 | Actualizar iconos de categorías (emoji) |
| 20 → 21 | Añadir orden a habitos_categorias |
| 21 → 22 | Crear tablas de shopping + pre-cargar datos |
| 22 → 23 | Añadir limiteMaximo, tramosLimite, ubeActivo, valorProgresoDecimal |
| 23 → 24 | Añadir orden a habitos |
| 24 → 25 | Añadir tipo (URGENTE/PLANIFICADO) a lista_items |
| 25 → 26 | Añadir archivado a habitos |
| 26 → 27 | Añadir pendienteClasificar a tareas_table |
| 27 → 28 | Añadir esMercadona a lista_items |
| 28 → 29 | Añadir tienda a lista_items + migrar esMercadona |
| 29 → 30 | Recrear lista_items con esquema limpio (sin esMercadona) |
| 30 → 31 | Añadir repeticionFin, repeticionVeces, repeticionContador a tareas_table |

---

## 5. Diagrama de Relaciones

```
┌─────────────────────────────────────────────────────────────────┐
│                        TAREAS                                   │
├─────────────────────────────────────────────────────────────────┤
│  categorias_table ───(string ref)───> tareas_table              │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        HÁBITOS                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  habitos_categorias ───(id ref)───> habitos                     │
│                                        │                        │
│                                        ├──> habitos_historial   │
│                                        ├──> habitos_tareas_esp. │
│                                        │       └──> tareas_hab._hist. │
│                                        ├──> habitos_versiones   │
│                                        ├──> habitos_pausas      │
│                                        └──> habitos_metricas    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        SHOPPING                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  lista_categorias_producto ──(FK SET NULL)──> lista_productos   │
│                                                    │            │
│                                              (FK CASCADE)       │
│                                                    ▼            │
│  lista_lugares ──────────(FK CASCADE)──────> lista_items        │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. Observaciones y Recomendaciones

### Puntos fuertes

- Separación clara por dominio en paquetes (`tasks/`, `habits/`, `shopping/`)
- Migraciones incrementales bien documentadas (v5 → v31)
- Versionado de definiciones de hábitos: diseño sofisticado para estadísticas históricas precisas
- FK con CASCADE donde aplica, especialmente en shopping y habits
- Pre-carga de datos maestros en migraciones (categorías, productos)
- Backup de seguridad automático antes de migraciones

### Puntos a mejorar

1. **Tarea↔Categoría (tasks):** La referencia es un `String` libre, no una FK. Si se renombra una categoría, las tareas quedan huérfanas. Se podría migrar a `categoriaId: Int?` con FK.

2. **Habito.categoriaId sin FK en la entidad:** Room no la valida en compile time. Se podría agregar un `@ForeignKey` explícito.

3. **HabitoMetricas no está en el array de entities de @Database:** Room no crea la tabla automáticamente. Necesitará una migración para crearla o añadirla al array de entities.

4. **Paquete inconsistente en TareaRepository:** El package dice `com.example.mistareasapp.data.Tasks` (T mayúscula) pero el directorio es `tasks`. Puede dar problemas en builds de Linux/CI.

5. **`exportSchema = false`:** Se pierde el historial JSON de schemas. Activarlo permitiría validar migraciones con `MigrationTestHelper`.

6. **Migración 20→21 definida pero falta en cadena numérica:** Revisar que el orden de registro en `.addMigrations()` sea consistente.
