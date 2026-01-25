package com.example.mistareasapp.ui.components.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mistareasapp.data.tasks.Categoria

/**
 * COMPONENTE: BarraFiltros
 * Permite al usuario filtrar la lista de tareas mediante una fila horizontal de chips.
 * * @param categorias Lista de categorías disponibles en la app.
 * @param seleccionada El título de la categoría actualmente filtrada (null para "Todas").
 * @param onSeleccionar Callback que se dispara al cambiar el filtro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionNaming") // Esto quita el error de Detekt
@Composable
fun BarraFiltros(
    categorias: List<Categoria>,
    seleccionada: String?,
    onSeleccionar: (String?) -> Unit
) {
    // 1. CONTENEDOR DESLIZABLE HORIZONTAL
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // 2. BLOQUE: OPCIÓN "TODAS" (LIMPIAR FILTROS)
        item {
            FilterChip(
                selected = seleccionada == null,
                onClick = { onSeleccionar(null) },
                label = { Text("Todas") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }

        // 3. BLOQUE: LISTA DINÁMICA DE CATEGORÍAS
        items(categorias) { cat ->
            FilterChip(
                selected = seleccionada == cat.titulo,
                onClick = { onSeleccionar(cat.titulo) },
                label = { Text(cat.titulo) },
                leadingIcon = {
                    // Nota: Asegúrate de que 'obtenerIcono' sea accesible desde este archivo
                    Icon(
                        imageVector = obtenerIcono(cat.icono),
                        contentDescription = "Filtro ${cat.titulo}",
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}
