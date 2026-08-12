import { useState, useEffect, useMemo, useCallback } from 'react'

const PRIORIDAD_ORDEN = { ALTA: 0, MEDIA: 1, BAJA: 2 }

function startOfDay(d) {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate())
}

export function clasificarSidebar(tarea) {
  if (!tarea.fecha_limite) return 'resto'
  const [y, m, d] = tarea.fecha_limite.substring(0, 10).split('-').map(Number)
  const dia = new Date(y, m - 1, d)
  const hoy = startOfDay(new Date())
  if (dia < hoy) return 'vencidas'
  if (dia.getTime() === hoy.getTime()) return 'hoy'
  const finSemana = new Date(hoy)
  finSemana.setDate(finSemana.getDate() + (finSemana.getDay() === 0 ? 0 : 7 - finSemana.getDay()))
  if (dia <= finSemana) return 'semana'
  return 'resto'
}

export function sortPrioFecha(a, b) {
  const po = PRIORIDAD_ORDEN[a.prioridad] - PRIORIDAD_ORDEN[b.prioridad]
  if (po !== 0) return po
  return new Date(a.fecha_creacion) - new Date(b.fecha_creacion)
}

export function useFiltros(tareas, isLoading) {
  const [inicializado, setInicializado] = useState(false)
  const [modo, setModo] = useState('tiempo')      // 'bandeja' | 'tiempo' | 'completadas'
  const [tiempos, setTiempos] = useState(new Set(['hoy']))
  const [categorias, setCategorias] = useState(new Set())

  // Contadores siempre sobre no-completadas
  const contadores = useMemo(() => {
    const c = { bandeja: 0, vencidas: 0, hoy: 0, semana: 0, resto: 0, cat: {} }
    tareas.forEach((t) => {
      if (t.esta_completada) return
      if (t.pendiente_clasificar) { c.bandeja++; return }
      const bucket = clasificarSidebar(t)
      c[bucket] = (c[bucket] ?? 0) + 1
      if (t.categoria_id != null) {
        c.cat[t.categoria_id] = (c.cat[t.categoria_id] ?? 0) + 1
      }
    })
    return c
  }, [tareas])

  // Posicionamiento automático al primer cargado
  useEffect(() => {
    if (isLoading || inicializado) return
    setInicializado(true)
    if (tareas.some((t) => t.pendiente_clasificar && !t.esta_completada)) {
      setModo('bandeja')
    }
  }, [isLoading, inicializado, tareas])

  // ── Acciones ────────────────────────────────────────────────────────────────

  const irABandeja = useCallback(() => {
    setModo('bandeja')
    setCategorias(new Set())
  }, [])

  const irACompletadas = useCallback(() => {
    setModo('completadas')
    setCategorias(new Set())
  }, [])

  const toggleTiempo = useCallback((key) => {
    setModo('tiempo')
    if (key === 'todas') {
      setTiempos(new Set(['todas']))
      return
    }
    setTiempos((prev) => {
      const next = new Set(prev)
      next.delete('todas')
      if (next.has(key)) {
        next.delete(key)
        if (next.size === 0) next.add('todas') // nunca dejar vacío
      } else {
        next.add(key)
      }
      return next
    })
  }, [])

  const toggleCategoria = useCallback(
    (id) => {
      if (modo !== 'tiempo') {
        setModo('tiempo')
        setTiempos(new Set(['todas']))
        setCategorias(new Set([id]))
        return
      }
      setCategorias((prev) => {
        const next = new Set(prev)
        if (next.has(id)) next.delete(id)
        else next.add(id)
        return next
      })
    },
    [modo]
  )

  const limpiarFiltros = useCallback(() => {
    const hayBandeja = tareas.some((t) => t.pendiente_clasificar && !t.esta_completada)
    if (hayBandeja) {
      setModo('bandeja')
    } else {
      setModo('tiempo')
      setTiempos(new Set(['hoy']))
    }
    setCategorias(new Set())
  }, [tareas])

  // ── Display mode ─────────────────────────────────────────────────────────────
  // 'flat'         → bandeja o cat-sin-tiempo: lista plana
  // 'flat-desc'    → completadas: lista plana desc fecha_creacion
  // 'sections-all' → todas las 4 secciones temporales
  // 'sections'     → solo las secciones de tiempos seleccionados
  // 'sections-cat' → secciones temporales filtradas por categoría

  const displayMode = useMemo(() => {
    if (modo === 'bandeja') return 'flat'
    if (modo === 'completadas') return 'flat-desc'
    const hasTodas = tiempos.has('todas') || tiempos.size === 0
    const hasCat = categorias.size > 0
    if (hasTodas) return hasCat ? 'flat' : 'sections-all'
    return hasCat ? 'sections-cat' : 'sections'
  }, [modo, tiempos, categorias])

  // ── Tareas filtradas (sin búsqueda, sin fading — se aplican en Tareas.jsx) ──

  const tareasFiltradas = useMemo(() => {
    if (modo === 'bandeja') {
      return tareas.filter((t) => t.pendiente_clasificar && !t.esta_completada).sort(sortPrioFecha)
    }
    if (modo === 'completadas') {
      return tareas
        .filter((t) => t.esta_completada)
        .sort((a, b) => new Date(b.fecha_creacion) - new Date(a.fecha_creacion))
    }
    // modo === 'tiempo'
    const hasTodas = tiempos.has('todas') || tiempos.size === 0
    let base = tareas.filter((t) => !t.esta_completada && !t.pendiente_clasificar)
    if (!hasTodas) {
      base = base.filter((t) => tiempos.has(clasificarSidebar(t)))
    }
    if (categorias.size > 0) {
      base = base.filter((t) => categorias.has(t.categoria_id))
    }
    return base
  }, [tareas, modo, tiempos, categorias])

  // ── ¿Mostrar botón limpiar? ───────────────────────────────────────────────
  const esEstadoInicial = useMemo(() => {
    const hayBandeja = contadores.bandeja > 0
    if (hayBandeja) return modo === 'bandeja' && categorias.size === 0
    return modo === 'tiempo' && tiempos.has('hoy') && tiempos.size === 1 && categorias.size === 0
  }, [modo, tiempos, categorias, contadores.bandeja])

  return {
    modo,
    tiempos,
    categorias,
    contadores,
    displayMode,
    tareasFiltradas,
    mostrarLimpiar: !esEstadoInicial,
    irABandeja,
    irACompletadas,
    toggleTiempo,
    toggleCategoria,
    limpiarFiltros,
  }
}
