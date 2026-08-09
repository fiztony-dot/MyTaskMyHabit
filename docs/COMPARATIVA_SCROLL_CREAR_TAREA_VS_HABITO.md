# Comparativa: pantalla crear tarea vs crear habito (foco en scroll)

## Objetivo
Comparar como estan montadas `PantallaCrearTarea` y `CrearHabitoScreen`, centrando el analisis en el comportamiento de scroll y en los puntos que pueden causar diferencias visibles.

## 1) Estructura general de cada pantalla

### Crear tarea
Archivo: `app/src/main/java/com/example/mistareasapp/ui/screens/tasks/PantallaCrearTarea.kt`

- Usa `Scaffold` con `TopAppBar`.
- El contenido principal es una sola `Column` con:
  - `fillMaxSize()`
  - `padding(innerPadding)` (padding del Scaffold)
  - `imePadding()`
  - `verticalScroll(rememberScrollState())`
  - `padding(16.dp)`
- Tiene `verticalArrangement = Arrangement.spacedBy(24.dp)` para separar bloques de formulario.

### Crear habito
Archivo: `app/src/main/java/com/example/mistareasapp/ui/screens/habits/PantallaCrearHabito.kt`

- Usa `Scaffold` con `TopAppBar`.
- Dentro del `Scaffold` hay un `Box(fillMaxSize + padding(padding))`.
- Dentro del `Box` hay una `Column` con:
  - `fillMaxSize()`
  - `verticalScroll(rememberScrollState())`
  - `padding(16.dp)`
  - `imePadding()`
- La separacion vertical se hace mayormente con `Spacer(...)` manuales.

## 2) Comparativa concreta de scroll

## Igualdades
- Ambas usan `verticalScroll(rememberScrollState())` en una `Column` principal.
- Ambas estan en `Scaffold`, por lo que dependen del `innerPadding`/`padding` del contenido.
- Ambas intentan adaptarse al teclado con `imePadding()`.

## Diferencias que impactan
- **Wrapper adicional en habitos**: habitos tiene `Box` extra entre `Scaffold` y `Column`; tareas no.
- **Orden de modifiers**:
  - Tareas: `imePadding()` antes de `verticalScroll()` y luego `padding(16.dp)`.
  - Habitos: `verticalScroll()` antes de `padding(16.dp)` y `imePadding()` al final.
- **Espaciado de contenido**:
  - Tareas: espaciado global consistente (`Arrangement.spacedBy`).
  - Habitos: mezcla de muchos `Spacer`, mas propenso a diferencias visuales.

## 3) Riesgos tipicos de que "parezca que no hace scroll"

- El contenido real no supera el alto visible en ciertos dispositivos/densidades.
- El teclado reduce viewport y, segun el orden de modifiers, la sensacion de scroll cambia.
- Tener dos rutas a la misma pantalla (`Rutas.PantallaCrearHabito.ruta` y `"crear_habito"` en `MiApp.kt`) puede confundir pruebas y percepcion de cambios.

## 4) Conclusiones practicas

- El patron de `PantallaCrearTarea` es mas simple y estable para formularios largos: `Scaffold -> Column scrollable` sin capa intermedia.
- `CrearHabitoScreen` funciona, pero su estructura actual (`Scaffold -> Box -> Column`) y el orden de modifiers puede generar comportamiento menos predecible.
- Si se busca homogeneidad y menos sorpresas con scroll/teclado, conviene alinear habitos con el patron de tareas.

## 5) Referencias

- `app/src/main/java/com/example/mistareasapp/ui/screens/tasks/PantallaCrearTarea.kt`
- `app/src/main/java/com/example/mistareasapp/ui/screens/habits/PantallaCrearHabito.kt`
- `app/src/main/java/com/example/mistareasapp/MiApp.kt`

