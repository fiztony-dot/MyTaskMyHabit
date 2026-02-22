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
    @Query("SELECT * FROM habitos_historial WHERE habitoId = :habitoId AND fecha = :fecha LIMIT 1")
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

    @Query("SELECT * FROM habitos_tareas_especificas WHERE habitoId = :habitoId")
    fun obtenerTareasDeHabito(habitoId: Long): Flow<List<TareaHabito>>

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
}