package com.example.mistareasapp

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class TareaWidgetReceiver : GlanceAppWidgetReceiver() {
    // Aquí le decimos qué widget debe manejar
    override val glanceAppWidget = TareaWidget()
}