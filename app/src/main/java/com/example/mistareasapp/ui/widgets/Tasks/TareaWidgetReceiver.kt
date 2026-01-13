package com.example.mistareasapp.ui.widgets.Tasks

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class TareaWidgetReceiver : GlanceAppWidgetReceiver() {
    // Aquí le decimos qué widget debe manejar
    override val glanceAppWidget = TareaWidget()
}