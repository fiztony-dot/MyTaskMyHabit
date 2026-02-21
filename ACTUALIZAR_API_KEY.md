# ⚠️ URGENTE: Actualizar API Key de Google Gemini

## Problema
Tu API key anterior ha sido **reportada como filtrada/comprometida** por Google Cloud.

```
Error: "Your API key was reported as leaked. Please use another API key."
Status: PERMISSION_DENIED
```

## Solución: Generar Nueva API Key

### Paso 1: Acceder a Google Cloud Console
1. Ve a https://console.cloud.google.com/
2. Inicia sesión con tu cuenta Google

### Paso 2: Eliminar la API key antigua
1. Selecciona tu proyecto
2. Ve a **"APIs & Services"** → **"Credentials"**
3. Busca la clave que empieza con `AIzaSyCcZTsOCkF6dpM...`
4. Haz clic en el botón de **eliminar (papelera)** al lado

### Paso 3: Crear una nueva API Key
1. Haz clic en **"+ CREATE CREDENTIALS"**
2. Selecciona **"API Key"**
3. Se abrirá un diálogo con tu nueva clave
4. **Copia la nueva clave** (se verá algo como `AIzaSy...`)

### Paso 4: Actualizar el código
**Abre:** `app/src/main/java/com/example/mistareasapp/network/IAProcessor.kt`

**Encuentra la línea:**
```kotlin
private const val API_KEY = "TU_NUEVA_API_KEY_AQUI"
```

**Reemplázala con tu nueva clave:**
```kotlin
private const val API_KEY = "AIzaSy[TU_NUEVA_CLAVE_AQUI]"
```

### Paso 5: Prueba la app
1. Ejecuta la app desde Android Studio
2. Abre la sección de Hábitos o Tareas
3. Intenta usar la función de voz
4. Debería funcionar sin errores 403

## ⚡ Verificación

Cuando funcione correctamente, verás en Logcat:
```
RESPUESTA GOOGLE: {"candidates": [{"content": {...}}]}
```

En lugar de:
```
RESPUESTA GOOGLE: {"error": {"code": 403, "message": "Your API key was reported as leaked..."}}
```

## 🔒 Recomendaciones de Seguridad

1. **NO compartas tu nueva API key** en repositorios públicos
2. **Considera usar variables de entorno** para credenciales sensibles
3. **Revisa regularmente** el uso de tu API key en Cloud Console
4. Si ves uso anómalo, **elimina inmediatamente** esa clave y crea una nueva

---

**¿Necesitas ayuda?** Contacta al soporte de Google Cloud o revisa la documentación oficial en https://cloud.google.com/docs/authentication/api-keys

