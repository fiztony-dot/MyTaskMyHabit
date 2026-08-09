# Comparación: Cálculo de Posición del Slider en Flash vs Hábitos

## Slider de Flash (Tarjetas de Estudio)

### Contexto
- **Unidad de navegación**: Días individuales
- **Componente**: `SelectorFechaConProgreso`
- **Archivo**: `PantallaFlashListado.kt`

### Cálculo de la Posición
El slider de Flash navega **día a día**:

1. **Fecha inicial**: `fechaSeleccionada` (obtenida del ViewModel)
2. **Navegación**:
   - **Anterior**: `fechaSeleccionada.minusDays(1)` (retrocede 1 día)
   - **Siguiente**: `fechaSeleccionada.plusDays(1)` (avanza 1 día)
3. **Visualización**: Muestra la fecha exacta en formato "día mes año"
4. **Progreso**: Calcula el progreso del día actual basándose en tarjetas completadas

### Código relevante
```kotlin
SelectorFechaConProgreso(
    fechaSeleccionada = fechaSeleccionada,
    progresoGeneral = progresoGeneral,
    onFechaAnterior = { viewModel.cambiarFecha(fechaSeleccionada.minusDays(1)) },
    onFechaSiguiente = { viewModel.cambiarFecha(fechaSeleccionada.plusDays(1)) }
)
```

---

## Slider de Hábitos

### Contexto
- **Unidad de navegación**: Días individuales (igual que Flash)
- **Componente**: `SelectorFechaConProgreso` (mismo componente reutilizado)
- **Archivo**: `PantallaHabitosListado.kt`

### Cálculo de la Posición
El slider de Hábitos **también navega día a día**, pero muestra el progreso semanal:

1. **Fecha inicial**: `fechaSeleccionada` (obtenida del ViewModel)
2. **Navegación**:
   - **Anterior**: `fechaSeleccionada.minusDays(1)` (retrocede 1 día)
   - **Siguiente**: `fechaSeleccionada.plusDays(1)` (avanza 1 día)
3. **Visualización**: Muestra la fecha exacta del día seleccionado
4. **Progreso**: Calcula el progreso del día actual basándose en hábitos completados
5. **Vista semanal**: La tarjeta de cada hábito muestra una cuadrícula de 7 días (L-D) con el estado de cada día

### Código relevante
```kotlin
SelectorFechaConProgreso(
    fechaSeleccionada = fechaSeleccionada,
    progresoGeneral = progresoGeneral,
    onFechaAnterior = { viewModel.cambiarFecha(fechaSeleccionada.minusDays(1)) },
    onFechaSiguiente = { viewModel.cambiarFecha(fechaSeleccionada.plusDays(1)) }
)
```

### Cálculo de la semana en las tarjetas
```kotlin
val hoy = LocalDate.now()
val lunes = hoy.with(DayOfWeek.MONDAY)  // Lunes de la semana actual

// Se generan 7 días desde el lunes
(0..6).forEach { i ->
    val fecha = lunes.plusDays(i.toLong())
    // ... renderizado del estado del día
}
```

---

## Diferencias Clave

| Aspecto | Flash | Hábitos |
|---------|-------|---------|
| **Navegación del slider** | Día a día | Día a día |
| **Componente usado** | `SelectorFechaConProgreso` | `SelectorFechaConProgreso` (mismo) |
| **Incremento/Decremento** | ±1 día | ±1 día |
| **Visualización en selector** | Fecha individual | Fecha individual |
| **Vista de progreso** | Solo día actual | Día actual en selector + cuadrícula semanal en tarjetas |
| **Base de la semana** | No aplica | Lunes como inicio (`DayOfWeek.MONDAY`) |
| **Rango temporal mostrado** | 1 día | 1 día en selector, 7 días en tarjetas (L-D) |

---

## Conclusión

**Ambos sliders funcionan exactamente igual**: navegan día a día usando `minusDays(1)` y `plusDays(1)`.

La diferencia está en **cómo se presenta la información**:
- **Flash**: Muestra solo el progreso del día seleccionado
- **Hábitos**: Muestra el progreso del día seleccionado en el slider, pero cada tarjeta de hábito despliega una vista semanal completa (7 días) calculada desde el lunes de la semana actual

El componente `SelectorFechaConProgreso` es **compartido y reutilizable** entre ambas pantallas.

