package com.example.mistareasapp.data.habits

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Detalle de un periodo individual para la tabla del PDF (sección 5). */
data class PeriodoDetalleCalculo(
    val etiqueta: String,
    val progresoStr: String,
    val objetivoStr: String,
    val pctEfectivo: Double   // valor exacto (0-100+) usado en el cálculo de la media, sin truncar
)

private val FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es"))
private const val PAGE_W = 595   // A4 width  at 72 dpi
private const val PAGE_H = 842   // A4 height at 72 dpi
private const val MARGIN = 40f
private const val LINE_H = 14f
private const val CAL_ROW_H = 26f   // alto de fila del calendario — deja espacio entre número y símbolo

private val MESES_ES = listOf(
    "Enero","Febrero","Marzo","Abril","Mayo","Junio",
    "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
)

// ─── Paint presets ────────────────────────────────────────────────────────────
private fun titlePaint() = Paint().apply { textSize = 16f; color = Color.rgb(30, 30, 30); isFakeBoldText = true }
private fun sectionPaint() = Paint().apply { textSize = 12f; color = Color.rgb(50, 80, 180); isFakeBoldText = true }
private fun normalPaint() = Paint().apply { textSize = 10f; color = Color.rgb(50, 50, 50) }
private fun smallPaint()  = Paint().apply { textSize = 9f;  color = Color.rgb(100, 100, 100) }
private fun linePaint()   = Paint().apply { color = Color.rgb(200, 200, 200); strokeWidth = 0.5f }
private fun calGridPaint() = Paint().apply { color = Color.rgb(225, 225, 225); strokeWidth = 0.5f }
private fun calDayPaint() = Paint().apply { textSize = 9.5f; color = Color.rgb(60, 60, 60) }

data class PdfInformeData(
    val habito: Habito,
    val categoriaNombre: String?,
    val versiones: List<HabitoVersion>,             // filtradas para mostrar en sección 2
    val versionesCalculo: List<HabitoVersion>,       // todas, para calcular objetivos por mes
    val pausas: List<HabitoPausa>,
    val historialCompleto: List<HabitoHistorial>,   // ordenado ASC
    val porcentajeHistorico: Float,                 // 0-1
    val rachaActual: Int,
    val mejorRacha: Int,
    val periodosCompletadosHist: Float,             // suma de cumplimiento equivalente (puede tener decimales)
    val periodosTotalHist: Int,                     // periodos totales evaluados en el histórico
    val periodosAl100Hist: Int,                     // conteo BINARIO de periodos al 100% (mismo criterio que la racha)
    val esBinarioHist: Boolean,                     // true si el cumplimiento de periodo es 0/100 exacto
    val mesEnCursoProgreso: Int,
    val mesEnCursoObjetivo: Int,
    val anioCompletados: Int,
    val anioTotal: Int,
    val desgloseTexto: String,                      // texto libre con el desglose del cálculo
    val detallesPorPeriodo: List<PeriodoDetalleCalculo> = emptyList(), // tabla periodo → prog/obj/% para sección 5
    val mesEnCursoNombre: String = "",              // "Julio 2026"
    val anioRangoTexto: String = ""                 // "ene–jun" para hábitos MENSUALES
)

/**
 * Genera un PDF de informe del hábito en el directorio cache y devuelve el URI compartible.
 */
fun generarPdfHabito(context: Context, data: PdfInformeData): android.net.Uri {
    val doc = PdfDocument()
    val state = PageState(doc)

    state.newPage()

    // ── Título ──────────────────────────────────────────────────────────────
    state.drawText(data.habito.nombre, titlePaint())
    state.nl()
    if (data.categoriaNombre != null) {
        state.drawText("Categoría: ${data.categoriaNombre}", smallPaint())
        state.nl()
    }
    state.drawHLine()
    state.nl()

    // ── 1. Definición actual ─────────────────────────────────────────────────
    state.drawSection("1. Definición actual")
    val h = data.habito
    val tipoTexto = when {
        h.esCompuestoPorTareas -> "Por tareas"
        h.tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO -> "Cuantitativo — objetivo ${h.objetivoValor ?: h.vecesPorDia} ${h.unidad ?: ""}"
        h.tipoObjetivo == TipoObjetivoHabito.LIMITE_MAXIMO -> {
            val lim = h.limiteMaximo?.toBigDecimal()?.stripTrailingZeros()?.toPlainString() ?: "?"
            "Límite máximo — $lim ${h.unidad ?: ""} / ${h.frecuencia.name.lowercase()}"
        }
        h.objetivoPorcentajeDias != null ->
            "${h.objetivoPorcentajeDias}% de días del mes (≈${diasAproxPct(h.objetivoPorcentajeDias!!)} días en meses de 30)"
        else -> "Frecuencia — ${h.vecesPorDia}x/${h.frecuencia.name.lowercase()}"
    }
    state.drawKV("Tipo", tipoTexto)
    state.drawKV("Frecuencia", h.frecuencia.name.replaceFirstChar { it.uppercase() })
    state.drawKV("Unidad", h.unidad ?: "—")
    state.drawKV("Medición", h.tipoMedicion.name.lowercase().replace('_', ' '))
    state.drawKV("Dificultad", "${h.dificultad} / 5")
    state.drawKV("Inicio", h.fechaInicio.format(FMT))
    if (h.tramosLimite != null) {
        val tramos = parsearTramos(h.tramosLimite)
        val tramosTexto = tramos.joinToString("  |  ") { t ->
            val hasta = if (t.hasta != null) t.hasta.toBigDecimal().stripTrailingZeros().toPlainString() else "inf"
            "${t.desde.toBigDecimal().stripTrailingZeros().toPlainString()}-$hasta -> ${t.porcentaje}%"
        }
        state.drawKV("Tramos", tramosTexto)
    }
    state.nl()

    // ── 2. Historial de cambios de definición ────────────────────────────────
    state.drawSection("2. Historial de cambios de definición")
    if (data.versiones.isEmpty()) {
        state.drawText("Sin cambios de definición registrados.", normalPaint()); state.nl()
    } else {
        data.versiones.forEachIndexed { idx, v ->
            val obj = when {
                v.tipoObjetivo == TipoObjetivoHabito.LIMITE_MAXIMO ->
                    "Lim. ${v.limiteMaximo?.toBigDecimal()?.stripTrailingZeros()?.toPlainString() ?: "?"} ${v.unidad ?: ""}"
                v.tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO ->
                    "${v.objetivoValor ?: v.vecesPorDia} ${v.unidad ?: ""}"
                v.objetivoPorcentajeDias != null ->
                    "${v.objetivoPorcentajeDias}% de dias del mes (aprox ${diasAproxPct(v.objetivoPorcentajeDias!!)} dias en meses de 30)"
                else -> "${v.vecesPorDia}x/${v.frecuencia.name.lowercase()}"
            }
            state.drawText("  ${idx + 1}. Desde ${v.fechaInicio.format(FMT)}  -  $obj", normalPaint(), wrapWidth = (PAGE_W - 2 * MARGIN).toInt())
        }
    }
    state.nl()

    // ── 3. Periodos pausados ─────────────────────────────────────────────────
    state.drawSection("3. Periodos pausados")
    if (data.pausas.isEmpty()) {
        state.drawText("Sin periodos de pausa.", normalPaint()); state.nl()
    } else {
        data.pausas.forEach { p ->
            val fin = if (p.fechaFin != null) p.fechaFin.format(FMT) else "en curso"
            state.drawText("  - ${p.fechaInicio.format(FMT)}  ->  $fin", normalPaint())
            state.nl()
        }
    }
    state.nl()

    // ── 4. Resumen global de días cumplidos ─────────────────────────────────
    state.drawSection("4. Resumen global de días cumplidos")
    val resumen = calcularResumenDias(h.fechaInicio, data.historialCompleto, data.pausas, data.versionesCalculo, h)
    if (resumen.totalObjetivo > 0) {
        val totalPct = resumen.totalCumplidos * 100 / resumen.totalObjetivo
        state.drawKV("Total histórico", "${resumen.totalCumplidos} de ${resumen.totalObjetivo} ${resumen.labelObjetivo} ($totalPct%)")
    }
    if (resumen.anioObjetivo > 0) {
        val anioPct = resumen.anioCumplidos * 100 / resumen.anioObjetivo
        val rangoStr = if (resumen.mesRangoTexto.isNotEmpty()) " (${resumen.mesRangoTexto})" else ""
        state.drawKV("Año ${resumen.anioActual}", "${resumen.anioCumplidos} de ${resumen.anioObjetivo} ${resumen.labelObjetivo}$rangoStr ($anioPct%)")
    }
    state.nl()

    // ── 5. Cumplimiento — calendario mensual ────────────────────────────────
    state.drawSection("5. Cumplimiento mensual")
    val esLimiteMaximo = h.tipoObjetivo == TipoObjetivoHabito.LIMITE_MAXIMO
    val isCuant = h.tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO
    state.drawCalendarioMeses(h.fechaInicio, data.historialCompleto, data.pausas, data.versionesCalculo, h, esLimiteMaximo, isCuant)

    // ── 6. Porcentaje histórico ──────────────────────────────────────────────
    state.drawSection("6. Porcentaje de cumplimiento histórico")
    state.drawKV("Resultado", "${(data.porcentajeHistorico * 100).toInt()}%")
    state.nl()
    state.drawText(data.desgloseTexto, smallPaint(), wrapWidth = (PAGE_W - 2 * MARGIN).toInt())
    state.nl()

    // Tabla de detalle por periodo
    if (data.detallesPorPeriodo.isNotEmpty()) {
        state.drawText("Desglose por periodo:", normalPaint().apply { isFakeBoldText = true })
        val colEtq = 80f; val colProg = 70f; val colArrow = 20f
        val tblPaint = smallPaint()
        val tblBold  = smallPaint().apply { isFakeBoldText = true }
        data.detallesPorPeriodo.forEach { det ->
            val fraccion = "${det.progresoStr} / ${det.objetivoStr}"
            // Mostrar con 1 decimal si no es entero exacto (para que la suma cuadre con la media final)
            val pctStr = if (det.pctEfectivo == kotlin.math.floor(det.pctEfectivo))
                "${det.pctEfectivo.toInt()}%" else "%.1f%%".format(det.pctEfectivo)
            state.checkPageBreakPublic(LINE_H + 2f)
            state.drawTableRow(det.etiqueta, fraccion, "→", pctStr, colEtq, colProg, colArrow, tblPaint, tblBold)
        }
        state.nl()
    }

    // ── 7. Resumen estadístico ───────────────────────────────────────────────
    state.drawSection("7. Resumen estadístico")
    val labelPer = when (h.frecuencia) {
        FrecuenciaHabito.DIARIA   -> "días"
        FrecuenciaHabito.SEMANAL  -> "semanas"
        FrecuenciaHabito.MENSUAL  -> "meses"
    }
    state.drawKV("Racha actual", "${data.rachaActual} $labelPer")
    state.drawKV("Mejor racha", "${data.mejorRacha} $labelPer")
    // Conteo binario (cumplió 100% sí/no) — mismo criterio que la racha
    state.drawKV("${labelPer.replaceFirstChar { it.uppercase() }} completados", "${data.periodosAl100Hist} de ${data.periodosTotalHist}")
    if (!data.esBinarioHist) {
        // Suma de cumplimiento equivalente — distinta pregunta, puede tener decimales
        state.drawKV("Suma de cumplimiento (equiv.)", "%.1f de %d".format(data.periodosCompletadosHist, data.periodosTotalHist))
    }
    when (h.frecuencia) {
        FrecuenciaHabito.MENSUAL -> {
            val mesLabel = if (data.mesEnCursoNombre.isNotEmpty()) "Mes en curso (${data.mesEnCursoNombre})" else "Mes en curso"
            state.drawKV(mesLabel, formatearMesEnCurso(data.mesEnCursoProgreso, data.mesEnCursoObjetivo, "días objetivo"))
            val rangoStr = if (data.anioRangoTexto.isNotEmpty()) " (${data.anioRangoTexto})" else ""
            val anioYear = java.time.LocalDate.now().year
            state.drawKV("Año $anioYear", "${data.anioCompletados} de ${data.anioTotal} meses completados$rangoStr")
        }
        FrecuenciaHabito.SEMANAL -> {
            val mesLabel = if (data.mesEnCursoNombre.isNotEmpty()) "Semana en curso (${data.mesEnCursoNombre})" else "Semana en curso"
            state.drawKV(mesLabel, formatearMesEnCurso(data.mesEnCursoProgreso, data.mesEnCursoObjetivo, "días objetivo"))
            state.drawKV("Año ${java.time.LocalDate.now().year}", "${data.anioCompletados} de ${data.anioTotal} semanas")
        }
        FrecuenciaHabito.DIARIA -> {
            val mesLabel = if (data.mesEnCursoNombre.isNotEmpty()) "Mes en curso (${data.mesEnCursoNombre})" else "Mes en curso"
            state.drawKV(mesLabel, formatearMesEnCurso(data.mesEnCursoProgreso, data.mesEnCursoObjetivo, "días objetivo"))
            state.drawKV("Año ${java.time.LocalDate.now().year}", "${data.anioCompletados} de ${data.anioTotal} días")
        }
    }
    state.nl()

    state.finalizePage()
    val file = File(context.cacheDir, "informe_${h.nombre.replace(' ', '_')}_${LocalDate.now()}.pdf")
    file.outputStream().use { doc.writeTo(it) }
    doc.close()

    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun diasAproxPct(pct: Int): Int = kotlin.math.ceil(30 * pct / 100.0).toInt()

private fun formatearMesEnCurso(progreso: Int, objetivo: Int, labelUnidad: String): String {
    return if (objetivo > 0 && progreso > objetivo) {
        val pct = progreso * 100 / objetivo
        "$progreso de $objetivo $labelUnidad ($pct% — superado)"
    } else {
        "$progreso de $objetivo $labelUnidad"
    }
}

private data class ResumenDias(
    val totalCumplidos: Int,
    val totalObjetivo: Int,
    val labelObjetivo: String,   // "días objetivo" o "días activos" (para límite máximo)
    val anioCumplidos: Int,
    val anioObjetivo: Int,
    val anioActual: Int,
    val mesRangoTexto: String    // "Ene–Jun"
)

private fun calcularResumenDias(
    fechaInicio: LocalDate,
    historial: List<HabitoHistorial>,
    pausas: List<HabitoPausa>,
    versiones: List<HabitoVersion>,
    habito: Habito
): ResumenDias {
    val hoy = LocalDate.now()
    val mesInicio = YearMonth.from(fechaInicio)
    val mesFin = YearMonth.from(hoy).minusMonths(1)
    val anioActual = hoy.year
    val esLimiteMaximo = habito.tipoObjetivo == TipoObjetivoHabito.LIMITE_MAXIMO
    val isCuant = habito.tipoObjetivo == TipoObjetivoHabito.CUANTITATIVO

    if (mesInicio.isAfter(mesFin)) return ResumenDias(0, 0, "días objetivo", 0, 0, anioActual, "")

    val histPorFecha = historial
        .groupBy { it.fecha }
        .mapValues { e -> e.value.maxByOrNull { it.id }!! }

    fun esFechaPausada(fecha: LocalDate) = pausas.any { p ->
        !fecha.isBefore(p.fechaInicio) && (if (p.fechaFin != null) fecha.isBefore(p.fechaFin) else true)
    }
    fun versionPara(fecha: LocalDate) =
        versiones.lastOrNull { !it.fechaInicio.isAfter(fecha) } ?: versiones.firstOrNull()

    var totalCumplidos = 0
    var totalObjetivo = 0
    var anioCumplidos = 0
    var anioObjetivo = 0
    var primerMesAnio: YearMonth? = null
    var ultimoMesAnio: YearMonth? = null

    var mes = mesInicio
    while (!mes.isAfter(mesFin)) {
        val d1Mes = mes.atDay(1)
        val d2Mes = mes.atEndOfMonth()
        val d1 = if (d1Mes.isBefore(fechaInicio)) fechaInicio else d1Mes

        val diasActivosMes = (0..ChronoUnit.DAYS.between(d1, d2Mes).toInt())
            .count { i -> !esFechaPausada(d1.plusDays(i.toLong())) }

        val vMes = versionPara(d1)
        val objetivoMes: Int = when {
            esLimiteMaximo -> diasActivosMes
            vMes?.objetivoPorcentajeDias != null ->
                kotlin.math.ceil(diasActivosMes * vMes.objetivoPorcentajeDias!! / 100.0).toInt()
            isCuant -> vMes?.objetivoValor ?: vMes?.vecesPorDia ?: habito.vecesPorDia
            else -> vMes?.vecesPorDia ?: habito.vecesPorDia
        }

        var completadosMes = 0
        for (diaN in 1..mes.lengthOfMonth()) {
            val fecha = mes.atDay(diaN)
            if (fecha.isBefore(fechaInicio) || esFechaPausada(fecha)) continue
            val reg = histPorFecha[fecha]
            val cumplido = when {
                esLimiteMaximo -> (reg?.valorProgresoDecimal ?: 0.0) > 0
                isCuant -> (reg?.valorProgreso ?: 0) > 0
                else -> reg?.completado == true
            }
            if (cumplido) completadosMes++
        }

        totalCumplidos += completadosMes
        totalObjetivo += objetivoMes

        if (mes.year == anioActual) {
            anioCumplidos += completadosMes
            anioObjetivo += objetivoMes
            if (primerMesAnio == null) primerMesAnio = mes
            ultimoMesAnio = mes
        }

        mes = mes.plusMonths(1)
    }

    val mesesAbrev = listOf("Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")
    val rangoTexto = when {
        primerMesAnio != null && primerMesAnio != ultimoMesAnio ->
            "${mesesAbrev[primerMesAnio!!.monthValue - 1]}–${mesesAbrev[ultimoMesAnio!!.monthValue - 1]}"
        primerMesAnio != null -> mesesAbrev[primerMesAnio!!.monthValue - 1]
        else -> ""
    }
    val labelObj = if (esLimiteMaximo) "días activos" else "días objetivo"

    return ResumenDias(totalCumplidos, totalObjetivo, labelObj, anioCumplidos, anioObjetivo, anioActual, rangoTexto)
}

// ─── Helpers internos ────────────────────────────────────────────────────────

private class PageState(private val doc: PdfDocument) {
    private var page: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var y = MARGIN + LINE_H

    fun newPage() {
        finalizePage()
        val pi = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, doc.pages.size + 1).create()
        page = doc.startPage(pi)
        canvas = page!!.canvas
        y = MARGIN + LINE_H
    }

    fun finalizePage() {
        page?.let { doc.finishPage(it) }
        page = null; canvas = null
    }

    private fun checkPageBreak(needed: Float = LINE_H * 2) {
        if (y + needed > PAGE_H - MARGIN) newPage()
    }

    fun checkPageBreakPublic(needed: Float = LINE_H * 2) = checkPageBreak(needed)

    fun drawTableRow(col1: String, col2: String, sep: String, col3: String,
                     w1: Float, w2: Float, wSep: Float,
                     paint: Paint, paintBold: Paint) {
        checkPageBreak()
        var x = MARGIN + 6f
        canvas!!.drawText(col1, x, y, paintBold)
        x += w1
        canvas!!.drawText(col2, x, y, paint)
        x += w2
        canvas!!.drawText(sep, x, y, paint)
        x += wSep
        canvas!!.drawText(col3, x, y, paintBold)
        y += LINE_H
    }

    fun nl(lines: Float = 1f) { y += LINE_H * lines }

    fun drawText(text: String, paint: Paint, wrapWidth: Int = 0) {
        checkPageBreak()
        if (wrapWidth > 0) {
            val words = text.split(" ")
            val sb = StringBuilder()
            for (word in words) {
                val test = if (sb.isEmpty()) word else "$sb $word"
                if (paint.measureText(test) > wrapWidth) {
                    canvas!!.drawText(sb.toString(), MARGIN, y, paint)
                    y += LINE_H
                    checkPageBreak()
                    sb.clear()
                    sb.append(word)
                } else sb.append(if (sb.isEmpty()) word else " $word")
            }
            if (sb.isNotEmpty()) { canvas!!.drawText(sb.toString(), MARGIN, y, paint); y += LINE_H }
        } else {
            canvas!!.drawText(text, MARGIN, y, paint)
            y += LINE_H
        }
    }

    fun drawSection(title: String) {
        checkPageBreak(LINE_H * 3)
        canvas!!.drawText(title, MARGIN, y, sectionPaint())
        y += LINE_H
        drawHLine()
        y += LINE_H * 0.5f
    }

    fun drawHLine() {
        checkPageBreak()
        canvas!!.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint())
        y += LINE_H * 0.5f
    }

    fun drawKV(key: String, value: String) {
        checkPageBreak()
        val kp = normalPaint().apply { isFakeBoldText = true }
        val vp = normalPaint()
        canvas!!.drawText("$key:", MARGIN, y, kp)
        canvas!!.drawText(value, MARGIN + 150f, y, vp)
        y += LINE_H
    }

    /**
     * Dibuja un calendario mensual por cada mes desde [fechaInicio] hasta hoy, con cuadrícula
     * y números de día para mayor legibilidad. El objetivo mensual (denominador del pie) se
     * calcula a partir de la versión de definición vigente en cada mes (respeta objetivoPorcentajeDias).
     * Símbolos: v=completado/registrado, o=sin registro, .=antes de inicio o futuro.
     */
    fun drawCalendarioMeses(
        fechaInicio: LocalDate,
        historial: List<HabitoHistorial>,
        pausas: List<HabitoPausa>,
        versiones: List<HabitoVersion>,
        habito: Habito,
        esLimiteMaximo: Boolean,
        isCuant: Boolean
    ) {
        val hoy = LocalDate.now()
        val mesInicio = YearMonth.from(fechaInicio)
        // Solo meses cerrados: el mes en curso todavía no tiene datos completos
        val mesFin = YearMonth.from(hoy).minusMonths(1)
        val histPorFecha = historial
            .groupBy { it.fecha }
            .mapValues { e -> e.value.maxByOrNull { it.id }!! }

        fun esFechaPausada(fecha: LocalDate): Boolean = pausas.any { p ->
            !fecha.isBefore(p.fechaInicio) && (if (p.fechaFin != null) fecha.isBefore(p.fechaFin) else true)
        }
        fun versionPara(fecha: LocalDate): HabitoVersion? =
            versiones.lastOrNull { !it.fechaInicio.isAfter(fecha) } ?: versiones.firstOrNull()

        val colW = (PAGE_W - 2 * MARGIN) / 7f
        val diasSem = listOf("L", "M", "X", "J", "V", "S", "D")

        var mes = mesInicio
        while (!mes.isAfter(mesFin)) {
            checkPageBreak(CAL_ROW_H * 8 + LINE_H * 3)

            // Cabecera del mes
            val mesNom = "${MESES_ES[mes.monthValue - 1]} ${mes.year}"
            val mesHdrPaint = smallPaint().apply { isFakeBoldText = true; color = Color.rgb(50, 80, 180) }
            canvas!!.drawText(mesNom, MARGIN, y, mesHdrPaint)
            y += LINE_H

            // Días reales activos (acotados a inicio/hoy, excluyendo pausas) y objetivo del mes
            val d1Mes = mes.atDay(1)
            val d2Mes = mes.atEndOfMonth()
            val d1 = if (d1Mes.isBefore(fechaInicio)) fechaInicio else d1Mes
            val d2 = if (d2Mes.isAfter(hoy)) hoy else d2Mes
            val diasActivosMes = (0..ChronoUnit.DAYS.between(d1, d2).toInt())
                .count { i -> !esFechaPausada(d1.plusDays(i.toLong())) }
            val vMes = versionPara(d1)
            val objetivoMes: Int? = when {
                esLimiteMaximo -> null
                vMes?.objetivoPorcentajeDias != null ->
                    kotlin.math.ceil(diasActivosMes * vMes.objetivoPorcentajeDias!! / 100.0).toInt()
                isCuant -> vMes?.objetivoValor ?: vMes?.vecesPorDia ?: habito.vecesPorDia
                else -> vMes?.vecesPorDia ?: habito.vecesPorDia
            }

            // Cabecera días de semana
            val semPaint = smallPaint().apply { isFakeBoldText = true; color = Color.rgb(80, 80, 80) }
            diasSem.forEachIndexed { i, d ->
                val xc = MARGIN + i * colW + (colW - semPaint.measureText(d)) / 2f
                canvas!!.drawText(d, xc, y, semPaint)
            }
            y += CAL_ROW_H * 0.6f
            val yGridTop = y

            // Cuadrícula
            val primerDia = mes.atDay(1)
            val diasEnMes = mes.lengthOfMonth()
            var col = primerDia.dayOfWeek.value - 1  // 0=Lunes
            var completadosMes = 0
            var filaActual = 0

            for (diaN in 1..diasEnMes) {
                val fecha = mes.atDay(diaN)
                val esAntes = fecha.isBefore(fechaInicio)
                val esFuturo = fecha.isAfter(hoy)
                val reg = histPorFecha[fecha]

                val simbolo: String
                val colorSim: Int
                when {
                    esAntes || esFuturo -> {
                        simbolo = "·"
                        colorSim = Color.rgb(210, 210, 210)
                    }
                    esLimiteMaximo -> {
                        if ((reg?.valorProgresoDecimal ?: 0.0) > 0) {
                            simbolo = "v"; colorSim = Color.rgb(46, 125, 50); completadosMes++
                        } else {
                            simbolo = "o"; colorSim = Color.rgb(160, 160, 160)
                        }
                    }
                    else -> {
                        val cumplido = if (isCuant) (reg?.valorProgreso ?: 0) > 0 else reg?.completado == true
                        if (cumplido) {
                            simbolo = "v"; colorSim = Color.rgb(46, 125, 50); completadosMes++
                        } else {
                            simbolo = "o"; colorSim = Color.rgb(160, 160, 160)
                        }
                    }
                }

                val cellY = yGridTop + filaActual * CAL_ROW_H
                val cellX = MARGIN + col * colW

                // Número de día (esquina superior-izquierda de la celda, con padding top)
                canvas!!.drawText("$diaN", cellX + 3f, cellY + 11f, calDayPaint())
                // Símbolo de estado (centrado, claramente separado del número)
                val sp = smallPaint().apply { color = colorSim; isFakeBoldText = true }
                val simX = cellX + (colW - sp.measureText(simbolo)) / 2f
                canvas!!.drawText(simbolo, simX, cellY + CAL_ROW_H - 5f, sp)

                col++
                if (col == 7) { col = 0; filaActual++ }
            }
            val filasUsadas = filaActual + (if (col > 0) 1 else 0)

            // Líneas de cuadrícula (verticales y horizontales) para separar celdas con claridad
            val gp = calGridPaint()
            for (c in 0..7) {
                val xLine = MARGIN + c * colW
                canvas!!.drawLine(xLine, yGridTop, xLine, yGridTop + filasUsadas * CAL_ROW_H, gp)
            }
            for (r in 0..filasUsadas) {
                val yLine = yGridTop + r * CAL_ROW_H
                canvas!!.drawLine(MARGIN, yLine, PAGE_W - MARGIN, yLine, gp)
            }

            y = yGridTop + filasUsadas * CAL_ROW_H + LINE_H * 1.5f

            // Pie del mes — objetivo real del mes (versionado), no días totales del mes
            val piePaint = smallPaint().apply { color = Color.rgb(80, 80, 80) }
            val pieTexto = if (objetivoMes != null && objetivoMes > 0) {
                val pct = (completadosMes * 100 / objetivoMes).coerceAtMost(999)
                "  v = $completadosMes de $objetivoMes días objetivo ($pct%)"
            } else {
                "  v = $completadosMes de $diasActivosMes días activos"
            }
            canvas!!.drawText(pieTexto, MARGIN, y, piePaint)
            y += LINE_H * 1.8f

            mes = mes.plusMonths(1)
        }
    }
}
