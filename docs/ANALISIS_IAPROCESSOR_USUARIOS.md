# IAProcessor.kt - ¿Quién lo utiliza?

## 📍 ¿QUÉ HACE IAProcessor?

`IAProcessor.kt` es un **singleton que procesa texto con IA (Google Gemini)** para extraer información estructurada (tareas y hábitos).

### Funcionalidad principal:
```kotlin
suspend fun procesarTexto(textoEscuchado: String, tipo: TipoEntrada): String?
```

**Entrada:** Texto de voz natural (ej: "Pagar la factura mañana a las 5")
**Salida:** JSON estructurado (ej: `{"titulo": "Pagar la factura", "fecha": "2026-03-09", "hora": "17:00"}`)

---

## 👥 QUIÉN LO UTILIZA

### **1. `SpeechLauncher.kt`** ⭐ PRINCIPAL
**Ubicación:** `core/ai/SpeechLauncher.kt`

**¿Qué hace?**
- Lanza el reconocimiento de voz del SO Android
- Captura el texto hablado
- **Llama a `IAProcessor.procesarTexto()`** para procesarlo
- Crea una tarea o hábito según el resultado

**Quién lo usa:**
- **`MiApp.kt`** → Función `crearSpeechLauncher()` (línea ~74)

---

### **2. `IAViewModel.kt`** 📋 SECUNDARIO
**Ubicación:** `viewmodel/IAViewModel.kt`

**¿Qué hace?**
- ViewModel auxiliar para procesar voz
- Abstrae la lógica de transformación de JSON a modelos
- Incluye validaciones adicionales

**Estado:** Parece ser **NO UTILIZADO ACTUALMENTE** en la app

---

### **3. `MiApp.kt`** (Indirectamente)
**Ubicación:** `MiApp.kt`

**¿Cómo se usa?**
- Importa `IAProcessor` (línea 74)
- Lo utiliza a través de `SpeechLauncher` (no directamente)

**Flujo:**
```
Usuario hace clic en micrófono
    ↓
MiApp llama crearSpeechLauncher()
    ↓
SpeechLauncher captura voz
    ↓
SpeechLauncher llama IAProcessor.procesarTexto()
    ↓
Se crea tarea/hábito automáticamente
```

---

## 🔗 CADENA DE LLAMADAS

```
MiApp.kt
  └─ crearSpeechLauncher() [línea ~74]
      └─ SpeechLauncher.kt
          └─ IAProcessor.procesarTexto() [línea ~57]
              ├─ Llamada a Google Gemini API
              └─ Retorna JSON
```

---

## 📊 RESUMEN

| Archivo | Usa IAProcessor | Tipo de uso | Estado |
|---------|-----------------|------------|--------|
| **SpeechLauncher.kt** | ✅ SÍ | Directo: procesa voz | ⭐ ACTIVO |
| **IAViewModel.kt** | ✅ SÍ | Directo: procesa voz | ⚠️ NO USADO |
| **MiApp.kt** | ✅ SÍ | Indirecto: vía SpeechLauncher | ⭐ ACTIVO |
| **NetworkClient.kt** | ❌ NO | N/A | ❌ DUPLICADO |

---

## 🎯 CONCLUSIÓN

**`IAProcessor` es utilizado por:**
1. **`SpeechLauncher.kt`** (Principal) → Procesa voz capturada y crea tareas/hábitos
2. **`MiApp.kt`** (Indirecto) → A través de SpeechLauncher

**Flujo activo:**
```
Usuario presiona micrófono → SpeechLauncher captura voz → IAProcessor extrae datos con IA → Se crea Tarea/Hábito
```

