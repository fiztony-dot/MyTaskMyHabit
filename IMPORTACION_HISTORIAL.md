# Formato de importación de historial de cumplimiento

> Guía para preparar datos de importación de historial de hábitos desde una aplicación externa.
> El formato de destino es la tabla `habitos_historial` de la base de datos Room (`tareas_db`).

---

## Tabla destino: `habitos_historial`

| Columna | Tipo SQL | Descripción |
|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | Generado automáticamente. No incluir. |
| `habitoId` | INTEGER NOT NULL | ID del hábito en la tabla `habitos`. |
| `fecha` | INTEGER NOT NULL | Días desde epoch (LocalDate.toEpochDay()). Ver sección "Fechas". |
| `valorProgreso` | INTEGER NOT NULL | Valor registrado ese día. Ver sección por tipo. |
| `completado` | INTEGER NOT NULL | 0 = no completado, 1 = completado. |

---

## Fechas

Todas las fechas se almacenan como `Long` mediante el TypeConverter de Room (`LocalDate.toEpochDay()`):

```kotlin
// Conversión:
LocalDate.of(2026, 5, 18).toEpochDay()  // → 20592
// Inversa:
LocalDate.ofEpochDay(20592)             // → 2026-05-18
```

En formato CSV/JSON, se recomienda incluir la fecha en formato `YYYY-MM-DD` y convertirla al importar.

---

## Regla de unicidad

La app espera **un único registro por hábito por fecha**. Si existen múltiples, toma el de `id` más alto (`ORDER BY id DESC LIMIT 1`). Al importar, insertar un solo registro por `(habitoId, fecha)`.

---

## Formato por tipo de hábito

### 1. Hábito diario simple (binario)

**Descripción:** hábito que se marca como hecho o no hecho cada día.  
`tipoObjetivo = FRECUENCIA`, `esCompuestoPorTareas = false`, `frecuencia = DIARIA`.

| Campo | Valor |
|---|---|
| `valorProgreso` | 1 si completado, 0 si no |
| `completado` | 1 si completado, 0 si no |

**Ejemplo:**
```csv
habitoId,fecha,valorProgreso,completado
5,20590,1,1   ← día completado
5,20591,0,0   ← día no completado
```

---

### 2. Hábito diario cuantitativo

**Descripción:** se registra un valor numérico por día (ej: vasos de agua, minutos de lectura).  
`tipoObjetivo = CUANTITATIVO`, `frecuencia = DIARIA`.

| Campo | Valor |
|---|---|
| `valorProgreso` | Cantidad registrada ese día (puede ser 0) |
| `completado` | 1 si `valorProgreso >= objetivoValor`, 0 si no |

**Ejemplo** (objetivo: 8 vasos):
```csv
habitoId,fecha,valorProgreso,completado
3,20590,8,1   ← 8/8 vasos → completado
3,20591,5,0   ← 5/8 vasos → no completado
3,20592,0,0   ← sin registro
```

---

### 3. Hábito semanal simple

**Descripción:** hábito con objetivo de N veces por semana.  
`tipoObjetivo = FRECUENCIA`, `frecuencia = SEMANAL`.

Cada día en que el usuario marcó el hábito genera un registro independiente.

| Campo | Valor |
|---|---|
| `valorProgreso` | 1 si ese día fue marcado |
| `completado` | 1 si ese día fue marcado (igual que valorProgreso) |

El objetivo semanal (`vecesPorDia`) se usa para calcular el cumplimiento del periodo en la app; **no se almacena por semana en el historial**. La app agrupa por semana para calcular el porcentaje.

**Ejemplo** (objetivo: 3 veces/semana):
```csv
habitoId,fecha,valorProgreso,completado
7,20589,1,1   ← lunes
7,20591,1,1   ← miércoles
7,20593,1,1   ← viernes
↳ semana con 3/3 → 100%
```

---

### 4. Hábito semanal cuantitativo

**Descripción:** se acumula un valor a lo largo de la semana (ej: 100 min/semana de inglés).  
`tipoObjetivo = CUANTITATIVO`, `frecuencia = SEMANAL`.

Se insertan registros por cada sesión (día), no uno por semana.

| Campo | Valor |
|---|---|
| `valorProgreso` | Valor registrado en esa sesión |
| `completado` | 1 si `valorProgreso >= objetivoValor` en esa entrada; en la práctica para registros parciales es 0 |

**Ejemplo** (objetivo: 100 min/semana):
```csv
habitoId,fecha,valorProgreso,completado
2,20589,34,0   ← lunes, 34 min registrados (< 100)
2,20591,40,0   ← miércoles, 40 min
2,20593,30,0   ← viernes, 30 min
↳ total semana = 104 min → 104% si puedeSuperar100, o 100% si no
```

> **Nota:** si el usuario registra toda la semana en un solo día, puede insertarse un único registro con el total semanal y `completado = 1` si supera el objetivo.

---

### 5. Hábito mensual simple

**Descripción:** hábito con objetivo de N días por mes (o `objetivoPorcentajeDias`%).  
`tipoObjetivo = FRECUENCIA`, `frecuencia = MENSUAL`.

Un registro por cada día marcado durante el mes.

| Campo | Valor |
|---|---|
| `valorProgreso` | 1 si ese día fue marcado |
| `completado` | 1 si ese día fue marcado |

**Ejemplo** (objetivo: 60% de días del mes = 19 días en mayo):
```csv
habitoId,fecha,valorProgreso,completado
9,20563,1,1   ← 1 may
9,20564,1,1   ← 2 may
...
9,20581,1,1   ← 19 may
↳ 19/19 días objetivo → 100%
```

---

### 6. Hábito por tareas (compuesto)

**Descripción:** el cumplimiento del hábito se basa en marcar un conjunto de tareas.  
`esCompuestoPorTareas = true`.

| Campo | Valor |
|---|---|
| `valorProgreso` | Número de tareas marcadas ese día |
| `completado` | 1 si `valorProgreso >= minimoTareasCumplimiento` (o todas si criterio = TODAS) |

Adicionalmente, el detalle de qué tareas se marcaron se guarda en `tareas_habito_historial`:

**Tabla `tareas_habito_historial`:**
| Columna | Tipo | Descripción |
|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | Auto |
| `tareaId` | INTEGER | ID de la tarea en `habitos_tareas_especificas` |
| `habitoId` | INTEGER | ID del hábito |
| `fecha` | INTEGER | Epoch day de la fecha |
| `completada` | INTEGER | 0 o 1 |

**Ejemplo** (hábito con 4 tareas, mínimo 3):
```csv
-- habitos_historial:
habitoId,fecha,valorProgreso,completado
11,20590,3,1   ← 3/4 tareas → cumple mínimo

-- tareas_habito_historial:
tareaId,habitoId,fecha,completada
101,11,20590,1
102,11,20590,1
103,11,20590,1
104,11,20590,0
```

---

## Consideraciones de importación

### Versionado por fecha de definición

La app **no implementa actualmente versionado retroactivo** de la definición del hábito. Los parámetros actuales (`vecesPorDia`, `objetivoValor`, `objetivoPorcentajeDias`, `puedeSuperar100`) se aplican a todo el historial.

Si el hábito cambió de definición en algún punto (ej: el objetivo pasó de 60 min/semana a 100 min/semana), los cálculos históricos usarán el valor actual (100 min). Tener esto en cuenta al preparar datos históricos para que los porcentajes sean coherentes.

### Deduplicación

Si se cargan varios registros con la misma `(habitoId, fecha)`, la app tomará el de mayor `id`. Recomendación: insertar un único registro por par.

### Fechas futuras

No se deben insertar registros con `fecha > hoy`. La app filtra fechas futuras en los cálculos de porcentaje.

### Hábitos pausados

Los hábitos con `pausado = 1` se excluyen de las vistas activas. Si se importa historial de un hábito actualmente pausado, los datos existen en DB pero no se muestran hasta que se reactive.

### Orden de inserción recomendado

```
1. Insertar categorías en habitos_categorias (si no existen)
2. Insertar hábitos en habitos
3. Insertar tareas en habitos_tareas_especificas (si esCompuestoPorTareas)
4. Insertar historial en habitos_historial (un registro por habitoId+fecha)
5. Insertar historial de tareas en tareas_habito_historial (si aplica)
```

---

## Ejemplo de payload JSON para importación

```json
{
  "habitos_historial": [
    { "habitoId": 5, "fecha": 20590, "valorProgreso": 1, "completado": 1 },
    { "habitoId": 5, "fecha": 20591, "valorProgreso": 0, "completado": 0 },
    { "habitoId": 2, "fecha": 20589, "valorProgreso": 34, "completado": 0 },
    { "habitoId": 2, "fecha": 20591, "valorProgreso": 66, "completado": 1 }
  ],
  "tareas_habito_historial": [
    { "tareaId": 101, "habitoId": 11, "fecha": 20590, "completada": 1 },
    { "tareaId": 102, "habitoId": 11, "fecha": 20590, "completada": 0 }
  ]
}
```
