package com.example.mistareasapp.ui.screens.habits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.mistareasapp.iconoAEmoji
import com.example.mistareasapp.iconoEfectivoHabito
import com.example.mistareasapp.viewmodel.Habits.HabitosViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleCalculoGeneral(
    viewModel: HabitosViewModel,
    navController: NavHostController
) {
    val datosCalculos by viewModel.datosCalculos.collectAsState()
    val categorias by viewModel.categoriasHabitos.collectAsState(initial = emptyList())

    val pesoTotal = datosCalculos.sumOf { it.pesoEfectivo }
    val sumaContrib = datosCalculos.sumOf { it.porcentaje.toDouble() * it.pesoEfectivo }
    val porcentajeGeneral = if (pesoTotal > 0) (sumaContrib / pesoTotal * 100).roundToInt() else 0

    val ordenados = datosCalculos.sortedByDescending { it.pesoEfectivo * it.porcentaje }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Desglose del % general") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Fórmula de la media ponderada",
                            fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "% general = Σ(% hábito × peso) ÷ Σpeso\n" +
                            "donde peso = min(días_efectivos, 180) × dificultad",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                // Cabecera tabla
                Row(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text("Hábito", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f))
                    Text("%", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                    Text("Dif.", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(26.dp), textAlign = TextAlign.End)
                    Text("Peso", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                    Text("% s/tot", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
                    Text("% × Peso", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(62.dp), textAlign = TextAlign.End)
                }
                HorizontalDivider()
            }

            items(ordenados) { dato ->
                val peso = dato.pesoEfectivo
                val pctDecimal = dato.porcentaje
                val contribucion = pctDecimal.toDouble() * peso
                val pctSTotal = if (pesoTotal > 0) pctDecimal.toDouble() * peso / pesoTotal * 100 else 0.0
                val icono = iconoAEmoji(iconoEfectivoHabito(dato.habito.categoriaId, dato.habito.icono, categorias))

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("$icono ${dato.habito.nombre}", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface)
                        if (dato.habito.pausado) {
                            Text("Pausado", fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("${(pctDecimal * 100).roundToInt()}%", fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(36.dp), textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("${dato.habito.dificultad}", fontSize = 11.sp,
                        modifier = Modifier.width(26.dp), textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$peso", fontSize = 11.sp,
                        modifier = Modifier.width(40.dp), textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${"%.1f".format(pctSTotal)}pp", fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(52.dp), textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.tertiary)
                    Text("${"%.1f".format(contribucion)}", fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(62.dp), textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary)
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            }

            item {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(thickness = 1.5.dp)
                Spacer(Modifier.height(8.dp))

                // Fila de totales
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TOTAL", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    Text("", modifier = Modifier.width(36.dp))
                    Text("", modifier = Modifier.width(26.dp))
                    Text("$pesoTotal", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                    Text("100pp", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(52.dp), textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.tertiary)
                    Text("${"%.1f".format(sumaContrib)}", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(62.dp), textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.primary)

                }

                Spacer(Modifier.height(16.dp))

                // Resultado final
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Cálculo final", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Text(
                            "${"%.1f".format(sumaContrib)} ÷ $pesoTotal = $porcentajeGeneral%",
                            fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Suma de contribuciones ÷ Peso total = % general",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
