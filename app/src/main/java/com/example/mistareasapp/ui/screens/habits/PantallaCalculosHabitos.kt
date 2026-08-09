package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ManageSearch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.mistareasapp.iconoAEmoji
import com.example.mistareasapp.iconoEfectivoHabito
import com.example.mistareasapp.ui.theme.M3CardHabito
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCalculosHabitos(
    viewModel: HabitosViewModel,
    navController: NavHostController
) {
    val datosCalculos by viewModel.datosCalculos.collectAsState()
    val categorias by viewModel.categoriasHabitos.collectAsState(initial = emptyList())

    val pesoTotal = datosCalculos.sumOf { it.pesoEfectivo }
    val sumaContrib = datosCalculos.sumOf { it.porcentaje.toDouble() * it.pesoEfectivo }
    val porcentajeGeneral = if (pesoTotal > 0) (sumaContrib / pesoTotal).toFloat() else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("% de cumplimiento") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("auditoria_habitos?habitoId=-1") }) {
                        Icon(Icons.Default.ManageSearch, contentDescription = "Auditoría")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("¿Cómo se calcula el porcentaje general?",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "Es la media ponderada de todos los hábitos, incluidos los pausados. Un hábito pausado congela su % histórico en el momento de la pausa y sigue contribuyendo con ese valor. Pausar un hábito no altera el % general.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Cabecera de columnas
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hábito", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Cumpl.", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(44.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Peso", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(44.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Aporta", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(52.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
            }

            items(datosCalculos.sortedByDescending { it.pesoEfectivo * it.porcentaje }) { dato ->
                val pesoItem = dato.pesoEfectivo
                val contribucionPp = if (pesoTotal > 0)
                    dato.porcentaje.toDouble() * pesoItem / pesoTotal * 100
                else 0.0

                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        navController.navigate("detalle_calculo/${dato.habito.id}")
                    },
                    colors = CardDefaults.cardColors(containerColor = M3CardHabito)
                ) {
                    val cardTexto = Color(0xFF1A1A1A)
                    val cardTextoSub = Color(0xFF555555)
                    val cardAccent = Color(0xFF4F378B)
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            iconoAEmoji(iconoEfectivoHabito(dato.habito.categoriaId, dato.habito.icono, categorias)),
                            fontSize = 18.sp,
                            modifier = Modifier.size(24.dp).wrapContentSize(Alignment.Center)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                dato.habito.nombre,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = cardTexto
                            )
                            if (dato.habito.pausado) {
                                Text(
                                    "Pausado",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            "${kotlin.math.round(dato.porcentaje * 100).toInt()}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.width(44.dp),
                            color = cardTexto
                        )
                        Text(
                            "$pesoItem",
                            fontSize = 12.sp,
                            modifier = Modifier.width(44.dp),
                            color = cardTextoSub
                        )
                        Text(
                            "${"%.1f".format(contribucionPp)} pp",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(52.dp),
                            color = cardAccent
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Cumplimiento general",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { navController.navigate("detalle_calculo_general") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Ver desglose",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "${kotlin.math.round(porcentajeGeneral * 100)}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "de media ponderada",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        if (pesoTotal > 0) {
                            Text(
                                "Cada hábito contribuye según su cumplimiento, su antigüedad (máx. 180 días) y su dificultad.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
