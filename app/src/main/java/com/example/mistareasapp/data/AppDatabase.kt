package com.example.mistareasapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mistareasapp.data.tasks.Categoria
import com.example.mistareasapp.data.tasks.CategoriaDao
import com.example.mistareasapp.data.Converters
import com.example.mistareasapp.data.tasks.Tarea
import com.example.mistareasapp.data.tasks.TareaDao
import com.example.mistareasapp.data.habits.Habito
import com.example.mistareasapp.data.habits.HabitoHistorial
import com.example.mistareasapp.data.habits.HabitoDao
import com.example.mistareasapp.data.habits.CategoriaHabito
import com.example.mistareasapp.data.habits.FrecuenciaHabito
import com.example.mistareasapp.data.habits.HabitoPausa
import com.example.mistareasapp.data.habits.HabitoVersion
import com.example.mistareasapp.data.habits.TareaHabito
import com.example.mistareasapp.data.habits.TareaHabitoHistorial
import com.example.mistareasapp.data.habits.toVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Migración de versión 5 → 6: Crear tabla de categorías
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Crear tabla de categorías si no existe
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `categorias` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `titulo` TEXT NOT NULL
            )
        """.trimIndent())
    }
}

// Migración de versión 6 → 7: Preparación para hábitos
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Esta es una migración "vacía" para alinear versiones
        // Las tablas de hábitos se crearán en la siguiente migración
        android.util.Log.d("MIGRATION_6_7", "Migración 6→7 completada")
    }
}

// Migración de versión 7 → 8: Crear todas las tablas de hábitos
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Verificar si la tabla habitos existe antes de alterar
        try {
            database.execSQL("ALTER TABLE habitos ADD COLUMN objetivoValor INTEGER DEFAULT NULL")
        } catch (e: Exception) {
            // Si falla es porque ya existe la columna o la tabla no existe
            android.util.Log.d("MIGRATION_6_7", "Columna objetivoValor ya existe o tabla no existe")
        }

        try {
            database.execSQL("ALTER TABLE habitos ADD COLUMN unidad TEXT DEFAULT NULL")
        } catch (e: Exception) {
            android.util.Log.d("MIGRATION_6_7", "Columna unidad ya existe")
        }

        // 2. Crear tabla de categorías de hábitos de forma segura
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `habitos_categorias` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `nombre` TEXT NOT NULL, 
                `icono` TEXT NOT NULL, 
                `color` TEXT NOT NULL
            )
        """.trimIndent())

        // 3. Crear tabla principal de hábitos de forma segura
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `habitos` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `nombre` TEXT NOT NULL, 
                `descripcion` TEXT, 
                `categoriaId` INTEGER NOT NULL, 
                `fechaInicio` INTEGER NOT NULL, 
                `frecuencia` TEXT NOT NULL, 
                `vecesPorDia` INTEGER NOT NULL, 
                `objetivoValor` INTEGER,
                `unidad` TEXT,
                `objetivoRachaSemanas` INTEGER NOT NULL, 
                `recordatoriosActivos` INTEGER NOT NULL, 
                `horaRecordatorio` TEXT,
                `icono` TEXT NOT NULL, 
                `colorHex` TEXT NOT NULL, 
                `activo` INTEGER NOT NULL
            )
        """.trimIndent())

        // 4. Crear tabla de historial
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `habitos_historial` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `habitoId` INTEGER NOT NULL, 
                `fecha` INTEGER NOT NULL, 
                `valorProgreso` INTEGER NOT NULL, 
                `completado` INTEGER NOT NULL, 
                FOREIGN KEY(`habitoId`) REFERENCES `habitos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())

        // 5. Crear tabla de tareas específicas de hábitos
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `habitos_tareas_especificas` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `habitoId` INTEGER NOT NULL, 
                `nombre` TEXT NOT NULL, 
                `descripcion` TEXT, 
                `completada` INTEGER NOT NULL, 
                FOREIGN KEY(`habitoId`) REFERENCES `habitos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """.trimIndent())
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Insertar hábitos de ejemplo SOLO si la tabla está vacía
        val fechaHoy = java.time.LocalDate.now().toEpochDay()

        // Verificar si ya hay hábitos
        val cursor = database.query("SELECT COUNT(*) FROM habitos")
        cursor.moveToFirst()
        val habitosExistentes = cursor.getInt(0)
        cursor.close()

        if (habitosExistentes == 0) {
            // Solo insertamos si no hay hábitos (primera vez)

            // 1. Insertar categorías de hábitos
            database.execSQL("INSERT INTO habitos_categorias (nombre, icono, color) VALUES ('Salud', 'medical_services', '#EF5350')")
            database.execSQL("INSERT INTO habitos_categorias (nombre, icono, color) VALUES ('Deporte', 'fitness_center', '#66BB6A')")
            database.execSQL("INSERT INTO habitos_categorias (nombre, icono, color) VALUES ('Aprendizaje', 'school', '#42A5F5')")
            database.execSQL("INSERT INTO habitos_categorias (nombre, icono, color) VALUES ('Bienestar', 'self_improvement', '#AB47BC')")

            // 2. Insertar hábitos de ejemplo
            database.execSQL("""
                INSERT INTO habitos (nombre, descripcion, categoriaId, fechaInicio, frecuencia, vecesPorDia, objetivoValor, unidad, objetivoRachaSemanas, recordatoriosActivos, horaRecordatorio, icono, colorHex, activo)
                VALUES ('Inglés', 'Practicar inglés', 3, $fechaHoy, 'SEMANAL', 1, 100, 'Minutos', 4, 0, NULL, 'language', '#42A5F5', 1)
            """)

            database.execSQL("""
                INSERT INTO habitos (nombre, descripcion, categoriaId, fechaInicio, frecuencia, vecesPorDia, objetivoValor, unidad, objetivoRachaSemanas, recordatoriosActivos, horaRecordatorio, icono, colorHex, activo)
                VALUES ('Bisoprolol', 'Tomar medicamento', 1, $fechaHoy, 'DIARIA', 1, 1, 'vez', 4, 0, NULL, 'medication', '#EF5350', 1)
            """)

            database.execSQL("""
                INSERT INTO habitos (nombre, descripcion, categoriaId, fechaInicio, frecuencia, vecesPorDia, objetivoValor, unidad, objetivoRachaSemanas, recordatoriosActivos, horaRecordatorio, icono, colorHex, activo)
                VALUES ('Pasos', 'Caminar diariamente', 2, $fechaHoy, 'SEMANAL', 1, 40000, 'Pasos', 4, 0, NULL, 'directions_run', '#66BB6A', 1)
            """)

            database.execSQL("""
                INSERT INTO habitos (nombre, descripcion, categoriaId, fechaInicio, frecuencia, vecesPorDia, objetivoValor, unidad, objetivoRachaSemanas, recordatoriosActivos, horaRecordatorio, icono, colorHex, activo)
                VALUES ('Día sin Alcohol', 'Evitar consumo', 4, $fechaHoy, 'MENSUAL', 1, 18, 'días', 4, 0, NULL, 'no_drinks', '#AB47BC', 1)
            """)
        }
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habitos ADD COLUMN tipoObjetivo TEXT NOT NULL DEFAULT 'FRECUENCIA'")
        database.execSQL("ALTER TABLE habitos ADD COLUMN esCompuestoPorTareas INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE habitos ADD COLUMN criterioCumplimientoTareas TEXT NOT NULL DEFAULT 'TODAS'")
        database.execSQL("ALTER TABLE habitos ADD COLUMN minimoTareasCumplimiento INTEGER DEFAULT NULL")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habitos ADD COLUMN fechaModificacion INTEGER DEFAULT NULL")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habitos ADD COLUMN pausado INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE habitos ADD COLUMN fechaInicioPausa INTEGER DEFAULT NULL")
        database.execSQL("ALTER TABLE habitos ADD COLUMN fechaFinPausa INTEGER DEFAULT NULL")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habitos ADD COLUMN objetivoPorcentajeDias INTEGER DEFAULT NULL")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Días de la semana en que aplica el hábito (solo DIARIA)
        database.execSQL("ALTER TABLE habitos ADD COLUMN diasSemana TEXT DEFAULT NULL")
        // Historial de estado de tareas por fecha
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `tareas_habito_historial` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `tareaId` INTEGER NOT NULL,
                `habitoId` INTEGER NOT NULL,
                `fecha` INTEGER NOT NULL,
                `completada` INTEGER NOT NULL
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_tareas_habito_historial_tareaId_fecha` ON `tareas_habito_historial` (`tareaId`, `fecha`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_tareas_habito_historial_habitoId_fecha` ON `tareas_habito_historial` (`habitoId`, `fecha`)")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habitos ADD COLUMN puedeSuperar100 INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Nueva tabla de versiones de definición de hábito
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `habitos_versiones` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `habitoId` INTEGER NOT NULL,
                `fechaInicio` INTEGER NOT NULL,
                `frecuencia` TEXT NOT NULL,
                `tipoObjetivo` TEXT NOT NULL,
                `vecesPorDia` INTEGER NOT NULL,
                `objetivoValor` INTEGER,
                `unidad` TEXT,
                `esCompuestoPorTareas` INTEGER NOT NULL,
                `minimoTareasCumplimiento` INTEGER,
                `objetivoPorcentajeDias` INTEGER,
                `puedeSuperar100` INTEGER NOT NULL DEFAULT 0,
                `diasSemana` TEXT
            )
        """.trimIndent())
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_habitos_versiones_habitoId_fechaInicio` " +
            "ON `habitos_versiones` (`habitoId`, `fechaInicio`)"
        )
        // Poblar con la definición actual de cada hábito (primera versión = desde fechaInicio del hábito)
        database.execSQL("""
            INSERT INTO `habitos_versiones`
                (`habitoId`, `fechaInicio`, `frecuencia`, `tipoObjetivo`, `vecesPorDia`,
                 `objetivoValor`, `unidad`, `esCompuestoPorTareas`, `minimoTareasCumplimiento`,
                 `objetivoPorcentajeDias`, `puedeSuperar100`, `diasSemana`)
            SELECT `id`, `fechaInicio`, `frecuencia`, `tipoObjetivo`, `vecesPorDia`,
                   `objetivoValor`, `unidad`, `esCompuestoPorTareas`, `minimoTareasCumplimiento`,
                   `objetivoPorcentajeDias`, `puedeSuperar100`, `diasSemana`
            FROM `habitos`
        """.trimIndent())
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Nuevo campo tipoMedicion en habitos
        database.execSQL("ALTER TABLE habitos ADD COLUMN tipoMedicion TEXT NOT NULL DEFAULT 'PROPORCIONAL_CON_TOPE'")
        // Migrar: puedeSuperar100=true → PROPORCIONAL_SIN_TOPE
        database.execSQL("UPDATE habitos SET tipoMedicion = 'PROPORCIONAL_SIN_TOPE' WHERE puedeSuperar100 = 1")
        // Nuevo campo tipoMedicion en versiones
        database.execSQL("ALTER TABLE habitos_versiones ADD COLUMN tipoMedicion TEXT NOT NULL DEFAULT 'PROPORCIONAL_CON_TOPE'")
        database.execSQL("UPDATE habitos_versiones SET tipoMedicion = 'PROPORCIONAL_SIN_TOPE' WHERE puedeSuperar100 = 1")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Nueva tabla para historial completo de periodos pausados
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `habitos_pausas` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `habitoId` INTEGER NOT NULL,
                `fechaInicio` INTEGER NOT NULL,
                `fechaFin` INTEGER
            )
        """.trimIndent())
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_habitos_pausas_habitoId` ON `habitos_pausas` (`habitoId`)"
        )
        // Migrar pausas existentes (campo único) a la nueva tabla
        database.execSQL("""
            INSERT INTO `habitos_pausas` (`habitoId`, `fechaInicio`, `fechaFin`)
            SELECT `id`, `fechaInicioPausa`, `fechaFinPausa`
            FROM `habitos`
            WHERE `fechaInicioPausa` IS NOT NULL
        """.trimIndent())
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habitos ADD COLUMN dificultad INTEGER NOT NULL DEFAULT 3")
        // Pre-cargar dificultades según tabla de referencia
        listOf(
            "Día sin Alcohol" to 3, "Push Up" to 2, "Bisoprolol" to 1,
            "Complementos Nutricionales" to 1, "Imagen" to 2, "Inglés" to 4,
            "Estiramientos" to 4, "Ejercicio" to 5, "Pasos" to 5,
            "Care Nutrition" to 4, "Tai-Chi" to 3, "Natación" to 4
        ).forEach { (nombre, dif) ->
            database.execSQL(
                "UPDATE habitos SET dificultad = $dif WHERE nombre = '${nombre.replace("'", "''")}'"
            )
        }
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Actualizar iconos de categorías por nombre (case-insensitive)
        // Ejercicio → 🤸, Salud → 🩺
        // También cubre las creadas por el importer (nombre en minúsculas)
        listOf("Ejercicio", "ejercicio", "Deporte", "deporte").forEach { nombre ->
            database.execSQL("UPDATE habitos_categorias SET icono = '🤸' WHERE LOWER(nombre) = LOWER('$nombre')")
        }
        listOf("Salud", "salud").forEach { nombre ->
            database.execSQL("UPDATE habitos_categorias SET icono = '🩺' WHERE LOWER(nombre) = LOWER('$nombre')")
        }
        // Bienestar ya tiene 🧘 en iconoAEmoji (self_improvement → 🧘), no cambia
        // Aprendizaje sin cambios
    }
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE habitos_categorias ADD COLUMN orden INTEGER NOT NULL DEFAULT 0")
        // Orden inicial: 1=Aprendizaje, 2=Salud, 3=Bienestar, 4=Ejercicio/Deporte
        database.execSQL("UPDATE habitos_categorias SET orden = 1 WHERE LOWER(nombre) = 'aprendizaje'")
        database.execSQL("UPDATE habitos_categorias SET orden = 2 WHERE LOWER(nombre) = 'salud'")
        database.execSQL("UPDATE habitos_categorias SET orden = 3 WHERE LOWER(nombre) = 'bienestar'")
        database.execSQL("UPDATE habitos_categorias SET orden = 4 WHERE LOWER(nombre) IN ('ejercicio','deporte')")
    }
}

@TypeConverters(Converters::class)
@Database(
    entities = [Tarea::class, Categoria::class, Habito::class, HabitoHistorial::class, CategoriaHabito::class, TareaHabito::class, TareaHabitoHistorial::class, HabitoVersion::class, HabitoPausa::class],
    version = 21,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tareaDao(): TareaDao
    abstract fun categoriaDao(): CategoriaDao // 3. Añadimos el acceso al nuevo DAO
    abstract fun habitoDao(): HabitoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tareas_db"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21)
                    .addCallback(DatabaseCallback(context))
                    .setJournalMode(JournalMode.TRUNCATE)
                    .build()
                INSTANCE = instance
                instance
            }
        }
        fun resetearInstancia() {
            INSTANCE = null
        }
    }

    // Dentro de AppDatabase.kt -> DatabaseCallback
    private class DatabaseCallback(private val context: Context) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val habitoDao = database.habitoDao()

                    // 1. Insertar Categorías de Hábitos
                    habitoDao.insertarCategoria(CategoriaHabito(nombre = "Salud", icono = "medical_services", color = "#EF5350"))
                    habitoDao.insertarCategoria(CategoriaHabito(nombre = "Deporte", icono = "fitness_center", color = "#66BB6A"))
                    habitoDao.insertarCategoria(CategoriaHabito(nombre = "Aprendizaje", icono = "school", color = "#42A5F5"))
                    habitoDao.insertarCategoria(CategoriaHabito(nombre = "Bienestar", icono = "self_improvement", color = "#AB47BC"))
                    habitoDao.insertarCategoria(CategoriaHabito(nombre = "Personal", icono = "person", color = "#FFA726"))

                    // 2. Rellenar Hábitos de la imagen
                    habitoDao.insertarHabito(Habito(
                        nombre = "Ingles",
                        categoriaId = 3, // Aprendizaje
                        frecuencia = FrecuenciaHabito.SEMANAL,
                        objetivoValor = 100,
                        unidad = "Minutos",
                        icono = "language",
                        colorHex = "#42A5F5"
                    ))

                    habitoDao.insertarHabito(Habito(
                        nombre = "Bisoprolol",
                        categoriaId = 1, // Salud
                        frecuencia = FrecuenciaHabito.DIARIA,
                        objetivoValor = 1,
                        unidad = "vez",
                        icono = "medication",
                        colorHex = "#EF5350"
                    ))

                    habitoDao.insertarHabito(Habito(
                        nombre = "Pasos",
                        categoriaId = 2, // Deporte
                        frecuencia = FrecuenciaHabito.SEMANAL,
                        objetivoValor = 40000,
                        unidad = "Pasos",
                        icono = "directions_run",
                        colorHex = "#66BB6A"
                    ))

                    habitoDao.insertarHabito(Habito(
                        nombre = "Día sin Alcohol",
                        categoriaId = 4, // Bienestar
                        frecuencia = FrecuenciaHabito.MENSUAL,
                        objetivoValor = 18, // 60% de 30 días aprox
                        unidad = "días",
                        icono = "no_drinks",
                        colorHex = "#AB47BC"
                    ))
                }
            }
        }
    }
}