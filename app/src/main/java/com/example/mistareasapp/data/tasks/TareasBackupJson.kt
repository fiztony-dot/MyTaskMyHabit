package com.example.mistareasapp.data.tasks

import android.content.Context
import android.net.Uri
import android.widget.Toast

/**
 * Backup/restore de Tareas.
 * DEPRECATED: Las tareas ahora viven en la API (Supabase). El backup del módulo
 * de tareas ya no es necesario — los datos están en el servidor.
 * Se mantiene el objeto con métodos no-op para no romper referencias en GestionArchivosCopias.
 */
object TareasBackupJson {

    fun exportar(context: Context, destinationUri: Uri) {
        Toast.makeText(context, "Las tareas están en el servidor — no necesitan backup local", Toast.LENGTH_SHORT).show()
    }

    fun importar(context: Context, sourceUri: Uri) {
        Toast.makeText(context, "Las tareas se gestionan desde el servidor — usa la app para editarlas", Toast.LENGTH_SHORT).show()
    }
}
