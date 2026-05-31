// Ubicación: data/habits/HabitoDao.kt
package com.example.mistareasapp.data.habits

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitoDao {

    // --- GESTIÓN DE HÁBITOS ---
    @Query("SELECT * FROM habitos ORDER BY id DESC")
    fun obtenerTodosLosHabitos(): Flow<List<Habito>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarHabito(habito: Habito): Long

    @Delete
    suspend fun eliminarHabito(habito: Habito)

    @Update
    suspend fun actualizarHabito(habito: Habito)

    // Obtiene el historial de un hábito para un día concreto
    @Query("SELECT * FROM habitos_historial WHERE habitoId = :habitoId AND fecha = :fecha ORDER BY id DESC LIMIT 1")
    suspend fun obtenerProgresoDiario(habitoId: Long, fecha: LocalDate): HabitoHistorial?

    // Inserta o actualiza el progreso de un día (ej: sumar un vaso de agua)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgreso(historial: HabitoHistorial)

    // Obtiene todos los registros de un hábito para estadísticas/gráficas
    @Query("SELECT * FROM habitos_historial WHERE habitoId = :habitoId ORDER BY fecha ASC")
    fun obtenerHistorialCompleto(habitoId: Long): Flow<List<HabitoHistorial>>

    // Elimina el historial si se borra el hábito (Cascada manual si no se usa ForeignKey)
    @Query("DELETE FROM habitos_historial WHERE habitoId = :habitoId")
    suspend fun eliminarHistorialDeHabito(habitoId: Long)

    // --- GESTIÓN DE CATEGORÍAS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarCategoria(categoria: CategoriaHabito)

    @Update
    suspend fun actualizarCategoria(categoria: CategoriaHabito)

    @Delete
    suspend fun eliminarCategoria(categoria: CategoriaHabito)

    @Query("SELECT * FROM habitos_categorias")
    fun obtenerCategorias(): Flow<List<CategoriaHabito>>

    // --- GESTIÓN DE TAREAS ESPECÍFICAS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTareaHabito(tarea: TareaHabito)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTareasHabito(tareas: List<TareaHabito>)

    @Update
    suspend fun actualizarTareaHabito(tarea: TareaHabito)

    @Delete
    suspend fun eliminarTareaHabito(tarea: TareaHabito)

    @Query("SELECT * FROM habitos_tareas_especificas WHERE habitoId = :habitoId ORDER BY id ASC")
    fun obtenerTareasDeHabito(habitoId: Long): Flow<List<TareaHabito>>

    @Transaction
    suspend fun insertarHabitoConTareas(habito: Habito, tareas: List<TareaHabito>): Long {
        val habitoId = insertarHabito(habito)
        if (tareas.isNotEmpty()) {
            insertarTareasHabito(tareas.map { it.copy(habitoId = habitoId) })
        }
        return habitoId
    }

    // En HabitoDao.kt añade o actualiza:

    @Query("""
    SELECT * FROM habitos 
    WHERE activo = 1 
    ORDER BY categoriaId ASC
""")
    fun obtenerHabitosActivos(): Flow<List<Habito>>

    // Para las estadísticas rápidas
    @Query("SELECT COUNT(*) FROM habitos_historial WHERE habitoId = :habitoId AND completado = 1")
    fun obtenerDiasCompletados(habitoId: Long): Flow<Int>

    // Para calcular la racha (obtiene las fechas de cumplimiento ordenadas)
    @Query("SELECT fecha FROM habitos_historial WHERE habitoId = :habitoId AND completado = 1 ORDER BY fecha DESC")
    fun obtenerFechasCompletadas(habitoId: Long): Flow<List<LocalDate>>

    // Para la vista semanal: historial de un hábito entre dos fechas (suspend)
    @Query("SELECT * FROM habitos_historial WHERE habitoId = :habitoId AND fecha BETWEEN :inicio AND :fin ORDER BY fecha ASC")
    suspend fun obtenerHistorialEntreFechas(habitoId: Long, inicio: LocalDate, fin: LocalDate): List<HabitoHistorial>

    // Para la vista mensual: reactivo como Flow
    @Query("SELECT * FROM habitos_historial WHERE habitoId = :habitoId AND fecha BETWEEN :inicio AND :fin ORDER BY fecha ASC")
    fun obtenerHistorialEntreFechasFlow(habitoId: Long, inicio: LocalDate, fin: LocalDate): Flow<List<HabitoHistorial>>

    // --- HISTORIAL POR TAREA Y FECHA ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTareasHistorial(historial: List<TareaHabitoHistorial>)

    @Query("SELECT * FROM tareas_habito_historial WHERE habitoId = :habitoId AND fecha = :fecha")
    fun obtenerTareasHistorialPorFechaFlow(habitoId: Long, fecha: LocalDate): Flow<List<TareaHabitoHistorial>>

    @Query("DELETE FROM tareas_habito_historial WHERE habitoId = :habitoId AND fecha = :fecha")
    suspend fun eliminarTareasHistorialPorFecha(habitoId: Long, fecha: LocalDate)

    @Query("DELETE FROM tareas_habito_historial WHERE habitoId = :habitoId")
    suspend fun eliminarTareasHistorialDeHabito(habitoId: Long)
}