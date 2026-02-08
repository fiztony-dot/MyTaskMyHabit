package com.example.mistareasapp.ui.widgets.Tasks

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import com.example.mistareasapp.MainActivity
import com.example.mistareasapp.R

@SuppressLint("RestrictedApi") // Añade esta línea
class TareaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize()
                    .clickable(onClick = actionStartActivity<MainActivity>(
                        parameters = androidx.glance.action.actionParametersOf(
                            androidx.glance.action.ActionParameters.Key<Boolean>("abrirVoz") to true
                        )
                    )),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.widget_icon),
                    contentDescription = "Icono del widget"
                )
                Text(
                    text = "Acceso Rápido",
                    style = TextStyle(color = ColorProvider(Color.Black))
                )
            }
        }
    }
}