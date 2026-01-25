package com.example.mistareasapp.ui.components.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mistareasapp.data.tasks.Prioridad
import com.example.mistareasapp.ui.theme.PrioridadAlta
import com.example.mistareasapp.ui.theme.PrioridadBaja
import com.example.mistareasapp.ui.theme.PrioridadMedia


// --- COMPONENTE: SELECTOR DE PRIORIDAD ---
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionNaming") // Esto quita el error de Detekt
@Composable
fun SelectorPrioridad(
    prioridadSeleccionada: Prioridad,
    onPrioridadCambiada: (Prioridad) -> Unit,
    modifier: Modifier = Modifier.Companion
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Prioridad", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Prioridad.entries.forEach { p ->
                FilterChip(
                    selected = p == prioridadSeleccionada,
                    onClick = { onPrioridadCambiada(p) },
                    label = { Text(p.etiqueta) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when (p) {
                            Prioridad.ALTA -> PrioridadAlta
                            Prioridad.MEDIA -> PrioridadMedia
                            Prioridad.BAJA -> PrioridadBaja
                        },
                        selectedLabelColor = Color.Companion.White
                    )
                )
            }
        }
    }
}