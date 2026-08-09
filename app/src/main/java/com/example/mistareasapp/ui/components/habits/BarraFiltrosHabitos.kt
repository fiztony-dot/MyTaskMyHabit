package com.example.mistareasapp.ui.components.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mistareasapp.data.habits.CategoriaHabito

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraFiltrosHabitos(
    categorias: List<CategoriaHabito>,
    seleccionada: Long?,
    onSeleccionar: (Long?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
        items(categorias) { cat ->
            FilterChip(
                selected = seleccionada == cat.id,
                onClick = { onSeleccionar(cat.id) },
                label = { Text(cat.nombre) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
