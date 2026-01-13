package com.example.mistareasapp

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider


@SuppressLint("RestrictedApi") // Añade esta línea
class TareaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize()
                    .background(Color.White), // O el color que prefieras
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Acceso Rápido",
                    style = TextStyle(color = ColorProvider(Color.Black))
                )
                // Este botón lanza la actividad de voz que acabamos de terminar
                Button(
                    text = "🎙️ Dictar Tarea",
                    onClick = actionStartActivity<VoiceTaskActivity>()
                )
            }
        }
    }
}