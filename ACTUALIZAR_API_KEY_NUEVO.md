# 🔑 Cómo Actualizar la API Key de Gemini (Forma SEGURA)

## ⚠️ IMPORTANTE: Nunca subas la API key a Git

La API key ahora se guarda en `local.properties`, que **NO se sube a Git** automáticamente.

---

## 📋 Pasos para generar y configurar una nueva API key

### 1. Ir a Google AI Studio
👉 [https://aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey)

### 2. Crear una nueva API key
- Haz clic en **"Create API Key"**
- Selecciona un proyecto existente o crea uno nuevo
- La nueva clave aparecerá (empieza con `AIzaSy...`)
- **Copia la clave inmediatamente** (solo se muestra una vez)

### 3. Configurar restricciones de seguridad (MUY IMPORTANTE)

Para evitar que Google detecte la clave como filtrada:

1. Haz clic en la clave recién creada
2. Ve a **"Edit API key"** > **"Set an application restriction"**
3. Selecciona **"Android apps"**
4. Haz clic en **"Add an item"**
5. Ingresa:
   - **Package name**: `com.example.mistareasapp`
   - **SHA-1 certificate fingerprint**: Obtén la huella desde Android Studio:
     ```
     Terminal > gradlew signingReport
     ```
     Copia el `SHA-1` que aparece en la sección `debug`

6. En **"API restrictions"**:
   - Selecciona **"Restrict key"**
   - Marca solo: `Generative Language API`

7. Guarda los cambios

---

### 4. Actualizar la API key en el proyecto

1. Abre el archivo `local.properties` (en la raíz del proyecto)
2. Busca la línea:
   ```properties
   GEMINI_API_KEY=PLACEHOLDER_CAMBIAR_POR_TU_NUEVA_API_KEY
   ```
3. Reemplázala con tu nueva clave:
   ```properties
   GEMINI_API_KEY=AIzaSy_TU_NUEVA_CLAVE_AQUI
   ```
4. Guarda el archivo

### 5. Limpiar y reconstruir el proyecto

En Android Studio:
- **Build** > **Clean Project**
- **Build** > **Rebuild Project**

O desde la terminal:
```bash
./gradlew clean build
```

---

## ✅ Verificación

Ejecuta la app y prueba el reconocimiento de voz. Si todo está bien configurado, deberías ver en el Logcat:
```
🎤 ENTRADA: texto='...', tipo=TAREA
✅ RESULTADO DE IA: ...
```

---

## 🔒 Seguridad

- ✅ `local.properties` está en `.gitignore` (NO se sube a Git)
- ✅ La API key NO está en el código fuente
- ✅ BuildConfig la inyecta en tiempo de compilación
- ✅ Las restricciones de Android evitan uso no autorizado

---

## 🚨 Si la clave se filtra otra vez

Si Google detecta la clave como filtrada:

1. **Elimina la clave comprometida** desde Google AI Studio
2. Revisa que `local.properties` esté en `.gitignore`
3. Revisa el historial de Git: busca commits que puedan contener la clave
4. Si encuentras la clave en Git, limpia el historial (contacta a soporte si necesitas ayuda)
5. Crea una nueva API key siguiendo estos pasos desde el principio

---

## 📝 Notas

- Cada desarrollador debe tener su propia API key en su `local.properties` local
- Nunca compartas tu API key por chat, email o repositorios públicos
- Revisa periódicamente el uso de la API en [Google Cloud Console](https://console.cloud.google.com/)

