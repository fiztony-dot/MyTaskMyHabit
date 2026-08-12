import { useState, useCallback, useMemo } from 'react'

const LS_KEY = 'mtmh_orden'
const PRIORIDAD_ORDEN = { ALTA: 0, MEDIA: 1, BAJA: 2 }

function loadLS() {
  try {
    const raw = localStorage.getItem(LS_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch { return {} }
}

function saveLS(partial) {
  try {
    const current = loadLS()
    localStorage.setItem(LS_KEY, JSON.stringify({ ...current, ...partial }))
  } catch {}
}

function compareFechaLimite(a, b) {
  if (!a.fecha_limite && !b.fecha_limite) return 0
  if (!a.fecha_limite) return 1
  if (!b.fecha_limite) return -1
  return a.fecha_limite.localeCompare(b.fecha_limite)
}

export function useOrden() {
  const [agrupacion,  setAgrupacionState]  = useState(() => loadLS().agrupacion  ?? 'tiempo')
  const [ordenarPor,  setOrdenarPorState]  = useState(() => loadLS().ordenarPor  ?? 'fecha_limite')
  const [orden,       setOrdenState]       = useState(() => loadLS().orden        ?? 'asc')

  const setAgrupacion = useCallback((v) => {
    setAgrupacionState(v); saveLS({ agrupacion: v })
  }, [])

  const setOrdenarPor = useCallback((v) => {
    setOrdenarPorState(v); saveLS({ ordenarPor: v })
  }, [])

  const setOrden = useCallback((v) => {
    setOrdenState(v); saveLS({ orden: v })
  }, [])

  const sortFn = useMemo(() => {
    const dir = orden === 'desc' ? -1 : 1
    return (a, b) => {
      let r = 0
      switch (ordenarPor) {
        case 'fecha_limite':   r = compareFechaLimite(a, b); break
        case 'prioridad':      r = (PRIORIDAD_ORDEN[a.prioridad] ?? 2) - (PRIORIDAD_ORDEN[b.prioridad] ?? 2); break
        case 'fecha_creacion': r = a.fecha_creacion.localeCompare(b.fecha_creacion); break
        case 'titulo':         r = a.titulo.localeCompare(b.titulo, 'es', { sensitivity: 'base' }); break
        default:               r = 0
      }
      return r * dir
    }
  }, [ordenarPor, orden])

  return { agrupacion, ordenarPor, orden, setAgrupacion, setOrdenarPor, setOrden, sortFn }
}
