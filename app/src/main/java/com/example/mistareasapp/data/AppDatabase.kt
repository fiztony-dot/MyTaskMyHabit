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
import com.example.mistareasapp.data.habits.TareaHabito
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val MIGRATION_6_7 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Crear tabla de categorías de hábitos
        database.execSQL("ALTER TABLE habitos ADD COLUMN objetivoValor INTEGER DEFAULT NULL")
        database.execSQL("ALTER TABLE habitos ADD COLUMN unidad TEXT DEFAULT NULL")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `habitos_categorias` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `nombre` TEXT NOT NULL, 
                `icono` TEXT NOT NULL, 
                `color` TEXT NOT NULL
            )
        """.trimIndent())

        // 2. Crear tabla principal de hábitos
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `habitos` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `nombre` TEXT NOT NULL, 
                `descripcion` TEXT, 
                `categoriaId` INTEGER NOT NULL, 
                `fechaInicio` INTEGER NOT NULL, 
                `frecuencia` TEXT NOT NULL, 
                `vecesPorDia` INTEGER NOT NULL, 
                `objetivoValor` INTEGER,                -- <--- Añadido
                `unidad` TEXT,                         -- <--- Añadido
                `objetivoRachaSemanas` INTEGER NOT NULL, 
                `recordatoriosActivos` INTEGER NOT NULL, 
                `horaRecordatorio` TEXT,                -- <--- Cambiado de INTEGER a TEXT
                `icono` TEXT NOT NULL, 
                `colorHex` TEXT NOT NULL, 
                `activo` INTEGER NOT NULL
            )
        """.trimIndent())

        // 3. Crear tabla de historial
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

        // 4. Crear tabla de tareas específicas de hábitos
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

@TypeConverters(Converters::class)
@Database(
    entities = [Tarea::class, Categoria::class, Habito::class, HabitoHistorial::class, CategoriaHabito::class, TareaHabito::class],
    version = 8,
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
                    .addMigrations(MIGRATION_6_7) // <--- AÑADE ESTA LÍNEA AQUÍ
                    .addCallback(DatabaseCallback(context))
                    .fallbackToDestructiveMigration() // Se queda como "plan B" por seguridad
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


    /*private class DatabaseCallback(private val context: Context) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val catDao = database.categoriaDao()

                    // Pre-poblamos con nombres de iconos reales de Material Icons
                    catDao.insertar(Categoria(titulo = "Trabajo", icono = "work"))
                    catDao.insertar(Categoria(titulo = "Personal", icono = "person"))
                    catDao.insertar(Categoria(titulo = "Urgente", icono = "warning"))
                    catDao.insertar(Categoria(titulo = "Salud", icono = "favorite"))
                    catDao.insertar(Categoria(titulo = "Hogar", icono = "home"))
                }
            }
        }
    }*/
    // Dentro de AppDatabase.kt -> DatabaseCallback
    private class DatabaseCallback(private val context: Context) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val habitoDao = database.habitoDao()

                    // 1. Insertar Categorías de Hábitos
                    val catSalud = habitoDao.insertarCategoria(CategoriaHabito(nombre = "Salud", icono = "medical_services", color = "#EF5350"))
                    val catDeporte = habitoDao.insertarCategoria(CategoriaHabito(nombre = "Deporte", icono = "fitness_center", color = "#66BB6A"))
                    val catAprendizaje = habitoDao.insertarCategoria(CategoriaHabito(nombre = "Aprendizaje", icono = "school", color = "#42A5F5"))
                    val catBienestar = habitoDao.insertarCategoria(CategoriaHabito(nombre = "Bienestar", icono = "self_improvement", color = "#AB47BC"))
                    val catPersonal = habitoDao.insertarCategoria(CategoriaHabito(nombre = "Personal", icono = "person", color = "#FFA726"))

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