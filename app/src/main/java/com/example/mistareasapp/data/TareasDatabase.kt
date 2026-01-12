// Archivo: TareasDatabase.kt

package com.example.mistareasapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@TypeConverters(Converters::class)
@Database(
    entities = [Tarea::class, Categoria::class], // 1. Añadimos la entidad Categoria
    version = 5,
    exportSchema = false
)
abstract class TareasDatabase : RoomDatabase() {

    abstract fun tareaDao(): TareaDao
    abstract fun categoriaDao(): CategoriaDao // 3. Añadimos el acceso al nuevo DAO

    companion object {
        @Volatile
        private var INSTANCE: TareasDatabase? = null

        fun getDatabase(context: Context): TareasDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TareasDatabase::class.java,
                    "tareas_db"
                )
                    .addCallback(DatabaseCallback(context))
                    // Usamos fallback para que Room cree las tablas nuevas automáticamente
                    // Esto evita tener que escribir migraciones SQL complejas en desarrollo
                    .fallbackToDestructiveMigration()
                    .setJournalMode(JournalMode.TRUNCATE) // <--- AÑADE ESTA LÍNEA
                    .build()
                INSTANCE = instance
                instance
            }
        }
        fun resetearInstancia() {
            INSTANCE = null
        }
    }


    private class DatabaseCallback(private val context: Context) : Callback() {
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
    }
}