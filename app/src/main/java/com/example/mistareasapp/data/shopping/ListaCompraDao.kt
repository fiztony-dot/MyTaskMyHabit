package com.example.mistareasapp.data.shopping

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ListaCompraDao {

    // --- Lugares ---
    @Query("SELECT * FROM lista_lugares ORDER BY esDefault DESC, nombre ASC")
    fun obtenerLugares(): Flow<List<ListaLugar>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLugar(lugar: ListaLugar): Long

    @Update
    suspend fun actualizarLugar(lugar: ListaLugar)

    @Delete
    suspend fun eliminarLugar(lugar: ListaLugar)

    @Query("SELECT * FROM lista_lugares WHERE esDefault = 1 LIMIT 1")
    suspend fun obtenerLugarDefault(): ListaLugar?

    @Query("SELECT COUNT(*) FROM lista_items WHERE lugarId = :lugarId")
    suspend fun contarItemsPorLugar(lugarId: Long): Int

    @Query("UPDATE lista_lugares SET esDefault = 0")
    suspend fun limpiarDefaultLugares()

    // --- Categorías de producto ---
    @Query("SELECT * FROM lista_categorias_producto ORDER BY orden ASC, nombre ASC")
    fun obtenerCategorias(): Flow<List<ListaCategoriaProducto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarCategoria(categoria: ListaCategoriaProducto): Long

    @Update
    suspend fun actualizarCategoria(categoria: ListaCategoriaProducto)

    @Delete
    suspend fun eliminarCategoria(categoria: ListaCategoriaProducto)

    @Query("SELECT COUNT(*) FROM lista_productos WHERE categoriaId = :categoriaId")
    suspend fun contarProductosPorCategoria(categoriaId: Long): Int

    @Query("SELECT COALESCE(MAX(orden), 0) FROM lista_categorias_producto")
    suspend fun obtenerMaxOrdenCategoria(): Int

    // --- Productos ---
    @Query("SELECT * FROM lista_productos ORDER BY nombre ASC")
    fun obtenerProductos(): Flow<List<ListaProducto>>

    @Query("SELECT * FROM lista_productos WHERE categoriaId = :categoriaId ORDER BY nombre ASC")
    fun obtenerProductosPorCategoria(categoriaId: Long): Flow<List<ListaProducto>>

    @Query("SELECT * FROM lista_productos WHERE nombre LIKE '%' || :busqueda || '%' ORDER BY nombre ASC")
    fun buscarProductos(busqueda: String): Flow<List<ListaProducto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarProducto(producto: ListaProducto): Long

    @Update
    suspend fun actualizarProducto(producto: ListaProducto)

    @Delete
    suspend fun eliminarProducto(producto: ListaProducto)

    @Query("SELECT COUNT(*) FROM lista_items WHERE productoId = :productoId")
    suspend fun contarItemsPorProducto(productoId: Long): Int

    // --- Items ---
    @Query("SELECT * FROM lista_items WHERE lugarId = :lugarId ORDER BY marcadoComprado ASC, fechaCreacion DESC")
    fun obtenerItemsPorLugar(lugarId: Long): Flow<List<ListaItem>>

    @Query("SELECT * FROM lista_items WHERE lugarId = :lugarId AND marcadoComprado = 0 ORDER BY fechaCreacion DESC")
    fun obtenerItemsPendientesPorLugar(lugarId: Long): Flow<List<ListaItem>>

    @Query("SELECT * FROM lista_lugares ORDER BY esDefault DESC, nombre ASC")
    suspend fun obtenerLugaresSync(): List<ListaLugar>

    @Query("SELECT * FROM lista_items WHERE lugarId = :lugarId AND marcadoComprado = 0 ORDER BY fechaCreacion DESC")
    suspend fun obtenerItemsPendientesSync(lugarId: Long): List<ListaItem>

    @Query("SELECT * FROM lista_productos ORDER BY nombre ASC")
    suspend fun obtenerTodosProductosSync(): List<ListaProducto>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarItem(item: ListaItem): Long

    @Update
    suspend fun actualizarItem(item: ListaItem)

    @Delete
    suspend fun eliminarItem(item: ListaItem)

    @Query("UPDATE lista_items SET marcadoComprado = :marcado WHERE id = :itemId")
    suspend fun marcarItem(itemId: Long, marcado: Boolean)

    @Query("UPDATE lista_items SET tipo = :tipo WHERE id = :itemId")
    suspend fun cambiarTipoItem(itemId: Long, tipo: TipoItemCompra)

    @Query("UPDATE lista_items SET tienda = :tienda WHERE id = :itemId")
    suspend fun cambiarTienda(itemId: Long, tienda: TiendaItem)

    @Query("DELETE FROM lista_items WHERE lugarId = :lugarId AND marcadoComprado = 1")
    suspend fun eliminarItemsComprados(lugarId: Long)

    @Query("SELECT COUNT(*) FROM lista_items WHERE lugarId = :lugarId AND marcadoComprado = 0")
    fun contarItemsPendientes(lugarId: Long): Flow<Int>
}
