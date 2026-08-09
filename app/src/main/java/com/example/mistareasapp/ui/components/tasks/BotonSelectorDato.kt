package com.example.mistareasapp.ui.components.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * COMPONENTE: BotonSelectorDato
 * * Es un botón personalizado con estilo de tarjeta (OutlinedCard).
 * Se usa en formularios para abrir selectores externos como el de Fecha (DatePicker)
 * o el de Hora (TimePicker).
 *
 * @param label El texto que se muestra (ej: la fecha seleccionada).
 * @param icon El icono a la izquierda del texto.
 * @param onClick La función que se ejecuta al pulsar el botón.
 * @param enabled Si es false, el botón se ve gris y no reacciona al clic.
 * @param colorTexto Permite forzar un color específico para el texto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionNaming") // Esto quita el error de Detekt
@Composable
fun BotonSelectorDato(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colorTexto: Color = MaterialTheme.colorScheme.onSurface
) {
    // 1. ESTRUCTURA EXTERNA: Una tarjeta con borde fino
    OutlinedCard(
        onClick = {
            // Imprimimos en consola para facilitar el rastreo de errores
            println("DEBUG: Botón selector pulsado -> Etiqueta actual: $label")
            onClick()
        },
        modifier = modifier.height(44.dp), // Altura fija para que todos los campos midan igual
        enabled = enabled,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
    ) {
        // 2. DISPOSICIÓN INTERNA: Icono + Espacio + Texto
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // 3. ICONO: Se vuelve gris traslúcido si el botón está desactivado
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Gray.copy(alpha = 0.5f)
                },
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 4. ETIQUETA DE TEXTO:
            // IMPORTANTE: Al no usar 'remember' aquí dentro, el componente reacciona
            // instantáneamente a cualquier cambio que venga desde el ViewModel.
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) colorTexto else colorTexto.copy(alpha = 0.5f)
            )
        }
    }
}