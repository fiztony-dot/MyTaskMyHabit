# Cálculo de racha (actual y mejor)

Este documento describe cómo se calcula la racha de un hábito en toda la
aplicación, tras la corrección de la Iteración 43. Antes de esta iteración,
la racha se calculaba contando días/semanas/meses con **algún registro**,
incluyendo el periodo en curso — lo cual era incoherente con el % histórico
(que ya excluía el periodo en curso desde `periodoHistoricoFin`) y premiaba
registros parciales que no llegaban al objetivo.

## 1. Dónde aparece la racha

| Pantalla | Campo |
|---|---|
| Estadísticas del hábito (`PantallaHabitosEstadisticas.kt`) | "Racha Actual" y "Mejor Racha" |
| Informe PDF (`HabitoPdfExporter.kt`, sección 6) | "Racha actual" y "Mejor racha" |

Ambas leen `EstadisticasHabito.rachaActual` / `mejorRacha`, que vienen de la
**misma** función `calcularEstadisticas` en `HabitosViewModel.kt`. No existe
ninguna otra pantalla que calcule la racha por su cuenta.

## 2. Fuente única de verdad: `obtenerDesgloseHistorico`

La racha **no** se calcula de forma independiente. Se deriva de la misma
lista que produce el % histórico (`DesgloseData.cumplidoPorPeriodo`), que ya
usa `HabitosViewModel.obtenerDesgloseHistorico(habito)` — la misma función
que alimenta la pantalla de % de cumplimiento, la auditoría y el informe PDF
(ver `CALCULOS_PORCENTAJE.md`).

`cumplidoPorPeriodo` es una lista cronológica de booleanos, uno por cada
periodo **ya cerrado** (día, semana o mes, según la frecuencia del hábito,
desde `fechaInicio` hasta `periodoHistoricoFin`):

```
cumplidoPorPeriodo[i] = true   si ese periodo llegó a >=100% de su objetivo
cumplidoPorPeriodo[i] = false  en caso contrario
```

El criterio de "100% del objetivo" es el mismo, sea cual sea el tipo de
hábito:

- **FRECUENCIA binario**: `progreso >= objetivo` (✓/✗, no hay términos medios).
- **FRECUENCIA/CUANTITATIVO proporcional**: `progreso / objetivo >= 1.0`
  (con un margen de `1e-9` para evitar falsos negativos por redondeo de
  punto flotante).
- **LIMITE_MAXIMO**: el tramo (`calcularPorcentajeLimite`) del periodo debe
  devolver exactamente `100`.

Esta es la misma lista que ya se usaba para calcular `periodosCompletados` /
`completados` del % histórico — por construcción, **no puede haber
discrepancia** entre "racha rota" y "% histórico bajo": si un periodo no
llegó al 100%, simultáneamente (a) rompe la racha y (b) no suma al conteo
binario de periodos completados que aparece en la sección 5 del informe PDF
("Completados: X de Y") — ver `CALCULOS_PORCENTAJE.md` §9 para la
diferencia entre ese conteo binario y la "suma de cumplimiento equivalente".

## 3. Por qué el periodo en curso nunca cuenta

`obtenerDesgloseHistorico` acota su evaluación a
`periodoHistoricoFin(frecuencia)`:

| Frecuencia | Último periodo evaluado |
|---|---|
| DIARIA  | ayer |
| SEMANAL | la semana anterior a la actual |
| MENSUAL | el mes anterior al actual |

El periodo en curso (hoy, esta semana, este mes) todavía puede cambiar — no
tiene sentido evaluar si "se cumplió" un periodo que no ha terminado. Por
eso `cumplidoPorPeriodo` simplemente no incluye una entrada para el periodo
en curso, y la racha tampoco puede romperse ni alargarse por lo que pase hoy
hasta que ese periodo se cierre (al empezar el siguiente).

## 4. Cómo se calcula la racha a partir de la lista

```kotlin
fun calcularRachaDesdeLista(cumplidoPorPeriodo: List<Boolean>): Pair<Int, Int> {
    // Racha actual: cuántos periodos cumplidos consecutivos hay
    // contando desde el final de la lista (el periodo cerrado más reciente)
    // hacia atrás, parando en el primer periodo no cumplido.
    var actual = 0
    for (i in cumplidoPorPeriodo.indices.reversed()) {
        if (cumplidoPorPeriodo[i]) actual++ else break
    }

    // Mejor racha: la tirada más larga de periodos cumplidos consecutivos
    // en toda la lista (no solo al final).
    var mejor = 0
    var racha = 0
    cumplidoPorPeriodo.forEach { cumplido ->
        if (cumplido) { racha++; if (racha > mejor) mejor = racha } else racha = 0
    }
    return Pair(actual, mejor)
}
```

Notas:
- Si la lista está vacía (el hábito no tiene ningún periodo cerrado todavía,
  p. ej. empezó este mismo mes), tanto la racha actual como la mejor son `0`.
- Los periodos pausados (`HabitoPausa`) ya están excluidos de la lista por
  `obtenerDesgloseHistorico` (no se generan como "periodo activo"), así que
  una pausa no rompe la racha — simplemente no añade entradas a la lista
  durante ese tiempo. Al terminar la pausa, la racha continúa evaluándose
  con normalidad sobre los periodos activos siguientes.
- El versionado de la definición del hábito (`habitos_versiones`) ya está
  aplicado dentro de `obtenerDesgloseHistorico` antes de generar la lista:
  cada periodo se evalúa contra el objetivo vigente en su momento, no contra
  la definición actual.

## 5. Identidad del cálculo en todas las pantallas

`calcularEstadisticas(fechas, habito)` llama a
`obtenerDesgloseHistorico(habito)` una única vez y deriva tanto
`porcentajeHistorico` como `rachaActual`/`mejorRacha` de su resultado. Tanto
`PantallaHabitosEstadisticas` como `generarInformePdf` consumen
`EstadisticasHabito` (directamente o través de `stats.rachaActual` /
`stats.mejorRacha`), por lo que **no hay una segunda fuente de verdad**: el
mismo hábito, en el mismo instante, da exactamente la misma racha actual y
mejor racha en la pantalla de estadísticas y en el informe PDF.
