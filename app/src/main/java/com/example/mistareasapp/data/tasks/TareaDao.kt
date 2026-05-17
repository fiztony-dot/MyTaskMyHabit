package com.example.mistareasapp.data.tasks

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TareaDao {

    // ➡️ OBTENER TODAS LAS TAREAS
    @Query("SELECT * FROM tareas_table ORDER BY prioridad DESC, fechaCreacion ASC")
    fun obtenerTodas(): Flow<List<Tarea>>

    // ➕ INSERTAR UNA TAREA
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(tarea: Tarea): Long

    // ➕ INSERTAR VARIAS TAREAS
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tareas: List<Tarea>)

    // 🔄 ACTUALIZAR UNA TAREA
    @Update
    suspend fun actualizar(tarea: Tarea)

    // 🗑️ ELIMINAR UNA TAREA
    @Delete
    suspend fun eliminar(tarea: Tarea)

    // 🗑️ ELIMINAR TODAS LAS TAREAS
    @Query("DELETE FROM tareas_table")
    suspend fun deleteAll()

    @Query("SELECT * FROM tareas_table WHERE id = :id")
    fun obtenerTareaPorId(id: Int): Flow<Tarea?> // Añade la palabra Flow

    // ➡️ AÑADE ESTA PARA EL WORKER (Sin Flow y sin suspend)
    @Query("SELECT * FROM tareas_table WHERE id = :id")
    fun obtenerTareaPorIdSincrona(id: Int): Tarea?
    // ... otros campos de TareaDao ...

    // ➡️ AÑADE ESTO: Para reprogramar alarmas al iniciar el móvil
    @Query("SELECT * FROM tareas_table WHERE estaCompletada = 0")
    fun obtenerTareasPendientesSincronas(): List<Tarea>


}