# VozCompraReceiver — Receptor de comandos de voz externos

Permite que otra app envíe texto a la Lista de la Compra sin pasar por el reconocedor de voz.

---

## Contrato del broadcast

### Acción

```
com.example.mistareasapp.ACTION_VOZ_COMPRA
```

### Extra de entrada

| Extra  | Tipo   | Obligatorio | Descripción                                   |
|--------|--------|-------------|-----------------------------------------------|
| texto  | String | Sí          | Texto a procesar. Máximo 300 caracteres.      |

### Resultado (broadcast ordenado)

El receptor llama a `setResult(code, data, null)` antes de terminar.
El emisor debe enviar el broadcast con `sendOrderedBroadcast` para recibirlo.

| `resultCode`              | Valor | Significado                          |
|---------------------------|-------|--------------------------------------|
| `Activity.RESULT_OK`      | -1    | Operación completada (añadido o ya existía) |
| `Activity.RESULT_FIRST_USER` | 1  | Error (texto inválido, sin lugares, etc.) |

`resultData` es un String en español natural, listo para leer en voz alta:

| Situación                              | Ejemplo de `resultData`                        |
|----------------------------------------|------------------------------------------------|
| Productos añadidos                     | `"Añadido a Madrid: leche y palomitas"`        |
| Añadidos con duplicados                | `"Añadido a Madrid: leche y palomitas"`        |
| Todo duplicado                         | `"Todo ya estaba en Madrid"`                   |
| Sin lugares configurados               | `"No hay lugares configurados"`                |
| Sin productos reconocidos              | `"No he entendido ningún producto"`            |
| Texto vacío o >300 chars               | `"Texto inválido"`                             |

> **Nota:** si el broadcast llega como normal (no ordenado), `setResult` no tiene
> efecto pero el Toast y el guardado funcionan igualmente.

---

## Permiso requerido (firma)

El receptor exige que la app remitente declare y posea el permiso:

```
com.example.mistareasapp.permission.VOZ_COMANDO
```

Declarado con `protectionLevel="signature"`. Solo apps firmadas con la misma
clave de firma pueden enviarlo.

La app remitente debe incluir en su manifest:

```xml
<uses-permission android:name="com.example.mistareasapp.permission.VOZ_COMANDO" />
```

---

## Ejemplo de uso (app remitente)

```kotlin
val intent = Intent("com.example.mistareasapp.ACTION_VOZ_COMPRA").apply {
    setPackage("com.example.mistareasapp")
    putExtra("texto", "Madrid leche y palomitas")
}
context.sendOrderedBroadcast(intent, null) { _, resultCode, resultData, _, _ ->
    // resultCode == Activity.RESULT_OK  →  éxito
    // resultData == "Añadido a Madrid: leche y palomitas"
    tts.speak(resultData, TextToSpeech.QUEUE_FLUSH, null, null)
}
```

---

## Probar con adb

### Problema con el permiso signature

`adb shell am broadcast` no puede conceder permisos `signature` automáticamente.
El intent llegará pero será rechazado por el sistema con `Permission Denial`.

### Solución para pruebas en debug

**Opción A** — Instalar la app remitente con la **misma debug.keystore** que usa
Android Studio y declarar el permiso en su manifest (ver arriba).

**Opción B** — `adb root` (solo emulador o device con root):

```bash
adb root
adb shell am broadcast \
  -a com.example.mistareasapp.ACTION_VOZ_COMPRA \
  -n com.example.mistareasapp/.ui.screens.shopping.VozCompraReceiver \
  --es texto "Madrid leche y palomitas"
```

**Opción C** — Eliminar temporalmente `android:permission` del `<receiver>` en el
manifest para pruebas, y restaurarlo antes del build de producción.

---

## Ejemplos de texto

| Texto                          | Resultado                                        |
|-------------------------------|--------------------------------------------------|
| `leche`                       | Añade "Leche" al lugar por defecto               |
| `Madrid leche y palomitas`    | Añade "Leche" y "Palomitas" al lugar "Madrid"    |
| `Barcelona agua y pan`        | Añade "Agua" y "Pan" al lugar "Barcelona"        |
