import { useState, useEffect, useCallback } from 'react'
import * as api from '../api/tareas'
import { useRealtimeTareas } from './useRealtimeTareas'

// Merge incoming data preserving object identity for unchanged items.
// Returns the same array reference if nothing changed (prevents re-renders).
function mergeTareas(prev, next) {
  const prevMap = new Map(prev.map(t => [t.id, t]))

  let changed = prev.length !== next.length
  const result = next.map(t => {
    const existing = prevMap.get(t.id)
    if (!existing) { changed = true; return t }
    // updated_at is the canonical change signal from Supabase/Postgres
    const same = existing.updated_at != null && existing.updated_at === t.updated_at
    if (same) return existing  // same reference → no re-render for this item
    changed = true
    return { ...t, adjuntos_count: existing.adjuntos_count }
  })

  return changed ? result : prev  // unchanged array → same reference
}

// Skip silent fetch if the user is actively typing or a modal dialog is open.
function usuarioInteractuando() {
  const el = document.activeElement
  if (el && ['INPUT', 'TEXTAREA', 'SELECT'].includes(el.tagName)) return true
  if (document.querySelector('[role="dialog"]')) return true
  return false
}

export function useTareas() {
  const [tareas, setTareas] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)

  // Full load — shows spinner; used only on mount and manual refresh
  const cargar = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await api.getTareas()
      setTareas(data)
    } catch {
      setError('Error al cargar las tareas. Comprueba tu conexión.')
    } finally {
      setIsLoading(false)
    }
  }, [])

  // Silent background refresh — no spinner, no empty list, deduplicates unchanged items
  const refetchSilencioso = useCallback(() => {
    if (usuarioInteractuando()) return
    api.getTareas()
      .then(data => setTareas(prev => mergeTareas(prev, data)))
      .catch(() => {})  // fail silently — polling will retry next cycle
  }, [])

  useEffect(() => {
    cargar()
  }, [cargar])

  // Supabase Realtime — apply individual events without re-fetching
  const { conectado: realtimeConectado } = useRealtimeTareas({
    onInsert: useCallback((nueva) => {
      setTareas((prev) =>
        prev.some((t) => t.id === nueva.id) ? prev : [...prev, nueva]
      )
    }, []),
    onUpdate: useCallback((actualizada) => {
      setTareas((prev) => prev.map((t) =>
        t.id === actualizada.id ? { ...actualizada, adjuntos_count: t.adjuntos_count } : t
      ))
    }, []),
    onDelete: useCallback((eliminada) => {
      setTareas((prev) => prev.filter((t) => t.id !== eliminada.id))
    }, []),
  })

  // Polling fallback when Realtime is not connected.
  // Pauses when the tab is hidden, resumes (with an immediate fetch) on show.
  useEffect(() => {
    if (realtimeConectado) return

    let intervalId = null

    function iniciarIntervalo() {
      if (!document.hidden && intervalId === null) {
        intervalId = setInterval(refetchSilencioso, 30_000)
      }
    }

    function handleVisibility() {
      if (document.hidden) {
        clearInterval(intervalId)
        intervalId = null
      } else {
        // Tab became visible — fetch now, then start interval
        refetchSilencioso()
        iniciarIntervalo()
      }
    }

    iniciarIntervalo()
    document.addEventListener('visibilitychange', handleVisibility)

    return () => {
      clearInterval(intervalId)
      document.removeEventListener('visibilitychange', handleVisibility)
    }
  }, [realtimeConectado, refetchSilencioso])

  // ── Mutations ────────────────────────────────────────────────────────────────

  const toggleCompletada = useCallback(async (tarea) => {
    const nueva = !tarea.esta_completada
    setTareas((prev) =>
      prev.map((t) => (t.id === tarea.id ? { ...t, esta_completada: nueva } : t))
    )
    try {
      const updated = await api.completarTarea(tarea.id, nueva)
      setTareas((prev) => prev.map((t) =>
        t.id === updated.id ? { ...updated, adjuntos_count: t.adjuntos_count } : t
      ))
    } catch {
      setTareas((prev) => prev.map((t) => (t.id === tarea.id ? tarea : t)))
    }
  }, [])

  const crear = useCallback(async (body) => {
    const nueva = await api.crearTarea(body)
    setTareas((prev) => prev.some((t) => t.id === nueva.id) ? prev : [...prev, nueva])
    return nueva
  }, [])

  const editar = useCallback(async (id, body) => {
    const updated = await api.editarTarea(id, body)
    setTareas((prev) => prev.map((t) =>
      t.id === id ? { ...updated, adjuntos_count: t.adjuntos_count } : t
    ))
    return updated
  }, [])

  const eliminar = useCallback(async (id) => {
    await api.eliminarTarea(id)
    setTareas((prev) => prev.filter((t) => t.id !== id))
  }, [])

  return {
    tareas, isLoading, error, cargar,
    toggleCompletada, crear, editar, eliminar,
    realtimeConectado,
  }
}
