package com.example.mistareasapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {
    @Query("SELECT * FROM categorias_table ORDER BY titulo ASC")
    fun obtenerTodas(): Flow<List<Categoria>>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertar(categoria: Categoria)

    @Query("DELETE FROM categorias_table")
    suspend fun borrarTodas()

    @Delete
    suspend fun eliminar(categoria: Categoria)

    @Update
    suspend fun actualizar(categoria: Categoria) // Permite guardar cambios en icono
}