package com.example.mistareasapp

/**
 * DEPRECATED: Este Activity ha sido reemplazado por SpeechLauncher.kt
 * que funciona con el nuevo flujo de API REST.
 * Se mantiene vacío para no romper posibles referencias en AndroidManifest.xml.
 * Se eliminará completamente en una limpieza futura.
 */

import android.os.Bundle
import androidx.activity.ComponentActivity

class VoiceTaskActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish() // No-op: funcionalidad migrada a SpeechLauncher
    }
}
