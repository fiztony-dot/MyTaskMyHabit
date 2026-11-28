# VozTareaReceiver — contrato del broadcast

## Componente

```
com.example.mistareasapp.ui.screens.tasks.VozTareaReceiver
```

## Acción

```
<applicationId>.ACTION_VOZ_TAREA
```

Ejemplo con applicationId `com.example.mistareasapp`:
```
com.example.mistareasapp.ACTION_VOZ_TAREA
```

## Extra

| Clave  | Tipo   | Descripción                                      |
|--------|--------|--------------------------------------------------|
| `texto`| String | Texto dictado por el usuario (máx. 300 chars)    |

## Permiso requerido

El emisor debe tener el permiso `<applicationId>.permission.VOZ_COMANDO` (protectionLevel=`signature`). Solo apps firmadas con el mismo keystore pueden enviar este broadcast.

## Resultado (ordered broadcast)

| `resultCode`              | `resultData`                                           | Significado                            |
|---------------------------|--------------------------------------------------------|----------------------------------------|
| `Activity.RESULT_OK` (-1) | `"Tarea creada: <título>, <fecha> a las <hora>"`       | Tarea creada correctamente             |
| `Activity.RESULT_FIRST_USER` (1) | `"No hay conexión"`                           | Sin acceso a internet                  |
| `Activity.RESULT_FIRST_USER` (1) | `"No se pudo crear la tarea"`                 | Error de API genérico                  |
| `Activity.RESULT_FIRST_USER` (1) | `"No se pudo crear la tarea, sin respuesta del servidor"` | Timeout (> 8 s)        |
| `Activity.RESULT_FIRST_USER` (1) | `"No he entendido la tarea"`                  | Texto vacío o > 300 caracteres         |

## Ejemplo de uso (Kotlin)

```kotlin
val intent = Intent("com.example.mistareasapp.ACTION_VOZ_TAREA").apply {
    setPackage("com.example.mistareasapp")
    putExtra("texto", "Llamar a Rafa mañana a las 9")
}
context.sendOrderedBroadcast(intent, null, object : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        val ok = resultCode == Activity.RESULT_OK
        val mensaje = resultData ?: ""
        // mensaje: "Tarea creada: Llamar a Rafa, 29 de noviembre a las 9:00"
    }
}, null, Activity.RESULT_CANCELED, null, null)
```

## Notas

- El receptor usa `goAsync()` + coroutines; responde siempre antes de 8 segundos.
- Mismo permiso signature que `VozCompraReceiver` (`VOZ_COMANDO`).
- El `resultData` está pensado para leerlo por TTS.
