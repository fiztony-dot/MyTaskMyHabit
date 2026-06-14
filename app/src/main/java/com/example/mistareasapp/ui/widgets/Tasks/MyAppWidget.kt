package com.example.mistareasapp.ui.widgets.Tasks

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.net.toUri
import com.example.mistareasapp.MainActivity
import com.example.mistareasapp.R

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [MyAppWidgetConfigureActivity]
 */
class MyAppWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, MyAppWidget::class.java))
            if (ids.isNotEmpty()) onUpdate(context, manager, ids)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) = Unit

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.my_app_widget)

    val abrirVozIntent = Intent(context, MainActivity::class.java).apply {
        putExtra("abrirVoz", true)
        // URI única por instancia para evitar PendingIntent compartidos entre widgets.
        data = "mistareasapp://widget/tareas/$appWidgetId".toUri()
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        appWidgetId,
        abrirVozIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    views.setOnClickPendingIntent(R.id.widget_tareas_root, pendingIntent)
    views.setOnClickPendingIntent(R.id.widget_tareas_mic, pendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}