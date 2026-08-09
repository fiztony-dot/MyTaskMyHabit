# Cálculos de porcentaje de cumplimiento

Este documento describe, de forma unificada, cómo se calcula el porcentaje de
cumplimiento de un hábito en cada pantalla de la app. El objetivo es que el
mismo hábito, en el mismo momento, dé **el mismo resultado** lo mires desde
donde lo mires (salvo que la pantalla muestre explícitamente un periodo
distinto, p. ej. "hoy" vs "histórico").

Toda la lógica vive en `HabitosViewModel.kt`. Las funciones clave son:

- `periodoHistoricoFin(frecuencia)` — fecha de corte del histórico (excluye el periodo en curso).
- `calcularPorcentajeHistorico(habito)` — % histórico de un hábito (0–1), usado en tarjetas y en la pantalla de % general.
- `obtenerDesgloseHistorico(habito)` — desglose completo (periodos evaluados, completados, % por periodo) usado en auditoría y en el PDF.
- `obtenerPieMensual(...)` — datos del pie de la vista mensual.
- `calcularEstadisticas(...)` — rachas y contadores para la pantalla de estadísticas / PDF.

## 1. Principio general: el periodo en curso nunca cuenta en el histórico

`periodoHistoricoFin(frecuencia)` devuelve el último día del periodo
**anterior** al actual:

| Frecuencia | Fin del histórico |
|---|---|
| DIARIA  | ayer |
| SEMANAL | el domingo de la semana anterior (lunes actual − 1 día) |
| MENSUAL | el último día del mes anterior (día 1 del mes actual − 1 día) |

Cualquier cálculo de "% histórico" (tarjetas, badges, % general, auditoría,
PDF) usa este corte. El periodo en curso (hoy, esta semana, este mes) se
muestra siempre como progreso aparte ("X/Y, en curso"), nunca mezclado con el
% histórico.

**Hábitos sin ningún periodo completado:** si `habito.fechaInicio` es
posterior a `periodoHistoricoFin(frecuencia)`, el hábito todavía no tiene
ningún periodo histórico cerrado. En ese caso:
- `calcularPorcentajeHistorico` devuelve `0f`, pero
- la **pantalla de % general** (`datosCalculos`) y la **barra general de
  Vista Hoy** excluyen completamente a estos hábitos del cálculo ponderado
  (no aparecen en la lista ni contribuyen al peso total), para no penalizar
  a un hábito recién creado con un 0% injusto.

**Hábitos pausados:** un hábito pausado tiene un historial real hasta la
fecha de inicio de la pausa. Su contribución al % general **no se elimina
inmediatamente al pausarlo** — se congela en el valor que tenía en ese
momento, pero su **peso decae linealmente** hasta llegar a 0 a los 180 días:

- `calcularPorcentajeHistorico` ya excluye los días pausados vía
  `esFechaPausada(pausas, fecha)`, por lo que devuelve el % correcto de
  los periodos activos anteriores a la pausa.
- `diasVidaEfectivos` también excluye los días pausados, por lo que el
  **peso base** del hábito pausado refleja solo su antigüedad efectiva activa.
- **Peso efectivo con decaimiento** (campo `pesoEfectivo` en `DatoCalculo`):

  ```
  pesoBase       = min(diasVidaEfectivos, 180) × dificultad
  diasPausado    = días transcurridos desde fechaInicioPausa hasta hoy
  factor         = max(0, (180 − diasPausado) / 180)
  pesoEfectivo   = pesoBase × factor          (para hábitos pausados)
  pesoEfectivo   = pesoBase                   (para hábitos activos)
  ```

- A los 180 días de pausa el peso llega a 0 y el hábito deja de contribuir
  al % general (se excluye de `datosCalculos`).
- `datosCalculos` incluye tanto hábitos activos como pausados con al menos
  un periodo histórico cerrado **y peso > 0**.
- La **barra general de Vista Hoy** usa `porcentajeGeneralConPausados`
  (StateFlow calculado a partir de `datosCalculos`), que incluye los
  pausados con su peso decreciente.
- Todas las pantallas que muestran pesos (% de cumplimiento, Desglose,
  Auditoría) usan `dato.pesoEfectivo` — no recalculan el peso localmente.

## 2. Cálculo por tipo de hábito (un periodo individual)

Dentro de un periodo (un día, una semana, un mes), el % de ese periodo se
calcula así:

- **FRECUENCIA + medición BINARIO**: 0% o 100%. Cumplido si
  `progreso >= objetivo` (o `completado == true` para días sueltos).
- **FRECUENCIA + medición PROPORCIONAL_CON_TOPE**: `progreso / objetivo`,
  acotado a `[0, 1]`.
- **FRECUENCIA + medición PROPORCIONAL_SIN_TOPE**: `progreso / objetivo`, sin
  tope superior (puede superar 100% en el periodo individual, aunque el %
  histórico acumulado siempre se topa al 100%).
- **CUANTITATIVO**: igual que FRECUENCIA pero `progreso` es la suma de
  `valorProgreso` del periodo y `objetivo` es `objetivoValor ?: vecesPorDia`.
- **LIMITE_MAXIMO**: el % del periodo viene de los tramos (`tramosLimite`,
  función `calcularPorcentajeLimite`). Si no hay tramo que cubra el valor:
  `100` si `valor <= límite`, si no `0`. El límite de un mes parcial (el
  hábito empezó a mitad de mes) se prorratea:
  `límite_prorrateado = límiteBase × díasActivosDelMes / díasTotalesDelMes`.

**Objetivo mensual con `objetivoPorcentajeDias`** (p. ej. "cumplir el 60% de
los días del mes"): el objetivo de días del periodo es

```
objetivo = ceil(díasActivosDelMes × objetivoPorcentajeDias / 100)
```

donde `díasActivosDelMes` son los días del mes en los que el hábito estaba
activo (desde `fechaInicio`, hasta hoy o fin de mes, excluyendo pausas). Este
mismo objetivo (no "días totales del mes") es el denominador correcto en
**todas** las pantallas: badge semanal, pie mensual, calendario del PDF y
resumen estadístico del PDF.

**Versionado**: si la definición del hábito cambió a lo largo del tiempo
(`habitos_versiones`), cada periodo histórico usa la versión vigente en su
fecha de inicio (`versionPara(fecha)`), no la definición actual. Esto aplica
de forma idéntica en `obtenerDesgloseHistorico`, en el calendario del PDF y
en el cálculo de "meses completados en el año".

## 3. Vista Hoy (`PantallaHabitosFlash.kt`)

- **Por tarjeta**: estado binario (verde/blanco/naranja) según si hay
  registro **hoy** y, para LIMITE_MAXIMO, si el acumulado del periodo en
  curso supera o no el límite vigente. El % mostrado en el checkbox
  cuantitativo diario es `valorActual / objetivo` del día.
- **Barra general**: media ponderada de `porcentajeHistorico` (el mismo valor
  que `calcularPorcentajeHistorico`) de todos los hábitos, con peso
  `min(díasVidaEfectivos, 180) × dificultad`. Excluye hábitos sin periodo
  histórico completado (ver §1).

## 4. Vista Semana (`PantallaHabitosListado.kt`)

- **Badge semanal (hábitos SEMANALES)**: `porcentajeSemanal()` —
  completados/objetivo de la semana en curso, con el tipo de medición
  aplicado (`aplicarTipoMedicion`).
- **Badge mensual (hábitos MENSUALES, no LIMITE_MAXIMO)**: acumulado del mes
  en curso (`progresoMesActual`) sobre el objetivo del mes
  (`objetivoDias`, que ya respeta `objetivoPorcentajeDias` — ver §2). Para
  CUANTITATIVO se usa `objetivoVersionado` (el objetivo de valor, no de
  días).
- **Badge mensual (LIMITE_MAXIMO)**: acumulado decimal del mes
  (`progresoMesDecimal`) sobre el límite prorrateado del mes en curso,
  normalizado al límite base antes de mirar los tramos (para que los tramos,
  definidos sobre el límite completo, se apliquen correctamente en meses
  parciales).
- **% histórico de la tarjeta**: idéntico a `calcularPorcentajeHistorico`
  (mismo valor que en Vista Hoy y en la pantalla de % general).
- **Barra general de Vista Hoy**: usa `porcentajeGeneralConPausados`
  (StateFlow del ViewModel calculado a partir de `datosCalculos`), que
  incluye los hábitos pausados con su % y peso congelados.

## 5. Vista Mensual (`PantallaVistaMensualHabito.kt`, `obtenerPieMensual`)

El pie de cumplimiento mensual muestra `progreso / objetivo`:

- DIARIA: días cumplidos del mes hasta hoy / días activos del mes.
- SEMANAL: `semanasActivasMes × vecesPorDia` (cuenta objetivo total de veces).
- MENSUAL: si `objetivoPorcentajeDias != null`, objetivo = días-objetivo del
  mes (igual fórmula que en §2); si no, el objetivo mensual ordinario.
- LIMITE_MAXIMO: acumulado decimal vs límite prorrateado del mes, con
  `pctTramo` calculado igual que en el badge de Vista Semana.

Los círculos de cada día priorizan, en este orden: antes del inicio del
hábito → pausado → valor de límite registrado → completado → valor
cuantitativo → sin marcar.

## 6. Pantalla de % de cumplimiento (`PantallaCalculosHabitos.kt`)

```
% general = Σ(porcentajeHistorico_i × peso_i) / Σ(peso_i)
peso_i    = min(díasVidaEfectivos_i, 180) × dificultad_i
```

`porcentajeHistorico_i` es exactamente `calcularPorcentajeHistorico(habito)`,
el mismo valor que las tarjetas de Vista Hoy/Semana. `díasVidaEfectivos` son
los días activos desde `fechaInicio` hasta `periodoHistoricoFin` (excluyendo
pausas) — por tanto tampoco incluye el periodo en curso.

La lista proviene de `datosCalculos` (StateFlow del ViewModel), que incluye
**tanto hábitos activos como pausados** que tengan al menos un periodo
histórico cerrado. Los hábitos pausados aparecen en la lista con la etiqueta
"Pausado" y contribuyen al % general con su valor y peso congelados en el
momento de la pausa (ver §1 — "Hábitos pausados").

Los hábitos sin ningún periodo histórico completado (ver §1) se excluyen de
la lista y del cálculo: no tendría sentido mostrarles un 0% cuando en
realidad su único periodo todavía no ha terminado de evaluarse.

## 7. Pantalla de auditoría (`PantallaAuditoriaHabitos.kt`, `obtenerAuditoriaPorHabito`)

Lista, periodo a periodo, el detalle de la evaluación: versión vigente,
objetivo, progreso registrado, si estaba pausado, si era el periodo en curso
(`enCurso = fecha/periodo posterior a periodoHistoricoFin`) y el % resultante.
Es la "fuente de verdad" para depurar discrepancias: si el % de una tarjeta no
cuadra con lo esperado, la auditoría debe reproducir, periodo a periodo, el
mismo cálculo que `obtenerDesgloseHistorico` agrega para producir el %
histórico final.

## 8. Informe PDF (`HabitoPdfExporter.kt`, `generarInformePdf`)

- **Calendario mensual**: símbolo `v`/`o`/`·` por día; el pie de cada mes usa
  como denominador el **objetivo real del mes** (versión vigente +
  `objetivoPorcentajeDias` si aplica — misma fórmula que en §2), no los días
  totales del mes ni los días activos sin más.
- **% histórico (sección 5)**: usa `obtenerDesgloseHistorico`, igual que
  auditoría y % general. Esta sección muestra **dos números distintos y
  claramente etiquetados**, porque responden preguntas distintas (ver §9):
  - `Completados (cumplieron el objetivo al 100% o más): X de Y` — conteo
    **binario** (`desglose.periodosAl100`): cada periodo cuenta como 1 si
    llegó a ≥100%, 0 en caso contrario. Es **el mismo criterio que la
    racha** (ver `CALCULOS_RACHA.md`) — nunca deben discrepar.
  - `Suma de cumplimiento mensual (equivalente): X de Y` — solo se muestra
    cuando el hábito NO es de medición binaria (incluye LIMITE_MAXIMO y
    proporcionales). Es la suma de `progreso/objetivo` de cada periodo
    (`desglose.periodosCompletados`), por eso puede tener decimales y no debe
    leerse como "X periodos completados".
- **Resumen estadístico (sección 6)**: "[Periodo] completados" usa
  `periodosAl100Hist`/`periodosTotalHist` — el mismo conteo binario de la
  sección 5, y el mismo criterio que la racha. Cuando el hábito no es binario
  se añade además "Suma de cumplimiento (equiv.)" con `periodosCompletadosHist`
  para no perder la información de cumplimiento parcial. "Mes/semana en
  curso" muestra el progreso real del periodo en curso (no evaluado todavía),
  nunca un booleano "completado/no completado" que no tiene sentido para un
  periodo que aún no ha terminado.

## 9. Discrepancias corregidas en esta iteración (histórico)

- Badge semanal de hábitos MENSUALES con `objetivoPorcentajeDias`: usaba
  `vecesPorDia` (u otro valor no relacionado) como denominador en vez del
  objetivo de días calculado. → corregido para usar `objetivoDias` en todas
  las pantallas.
- PDF: el calendario y el resumen estadístico usaban "días totales del mes"
  o un recuento de registros diarios sueltos como denominador/numerador,
  produciendo porcentajes y totales incoherentes con el resto de la app
  (p. ej. "16/31 = 51 %" en vez de "16/16 = 100 %", o "299 meses" en vez de
  "16 de 17 meses"). → corregido para reutilizar el mismo objetivo versionado
  y el mismo desglose histórico que usan auditoría y % general.
- PDF, sección 5: "Completados: X de Y" no dejaba claro si X era un conteo
  binario o una suma ponderada, y no coincidía con "Meses completados: 16,8
  de 17" de la sección 6 (que sí era la suma ponderada). → ahora se muestran
  ambos números por separado y con etiqueta explícita: el conteo binario
  (`periodosAl100`, mismo criterio que la racha) y, solo si el hábito no es
  binario, la suma de cumplimiento equivalente (`periodosCompletados`). Ver
  también `CALCULOS_RACHA.md`.
- Racha (actual y mejor): se calculaba contando periodos con **cualquier
  registro**, incluyendo el periodo en curso, en vez de periodos **cerrados**
  que llegaron a **100% o más** del objetivo. → corregido para derivarse de
  `obtenerDesgloseHistorico` (la misma lista cronológica que produce el %
  histórico), eliminando así una segunda fuente de verdad. Detalle completo
  en `CALCULOS_RACHA.md`.
