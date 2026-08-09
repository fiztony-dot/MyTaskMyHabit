package com.example.mistareasapp.ui.widgets.shopping

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.example.mistareasapp.R
import com.example.mistareasapp.ui.screens.shopping.VozCompraActivity

@SuppressLint("RestrictedApi")
class ListaCompraWidget : GlanceAppWidget() {

    companion object {
        private val COLOR_BG = ColorProvider(androidx.compose.ui.graphics.Color(0xFF0D0C12))
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(COLOR_BG)
                    .clickable(actionStartActivity<VozCompraActivity>()),
                contentAlignment = Alignment.Center
            ) {
                // Icono de micrófono centrado y grande
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_mic),
                    contentDescription = "Añadir por voz a la lista de la compra",
                    modifier = GlanceModifier.size(40.dp)
                )
                // Icono de carrito pequeño en esquina inferior derecha
                Box(
                    modifier = GlanceModifier.fillMaxSize().padding(bottom = 4.dp, end = 4.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_compra_preview),
                        contentDescription = null,
                        modifier = GlanceModifier.size(14.dp)
                    )
                }
            }
        }
    }
}
