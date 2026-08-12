import { useState, useEffect, useCallback } from 'react'
import * as api from '../api/tareas'
import { useRealtimeTareas } from './useRealtimeTareas'

export function useTareas() {
  const [tareas, setTareas] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)

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

  useEffect(() => {
    cargar()
  }, [cargar])

  // Supabase Realtime — actualiza el estado local al recibir eventos de BD
  const { conectado: realtimeConectado } = useRealtimeTareas({
    onInsert: useCallback((nueva) => {
      setTareas((prev) =>
        prev.some((t) => t.id === nueva.id) ? prev : [...prev, nueva]
      )
    }, []),
    onUpdate: useCallback((actualizada) => {
      setTareas((prev) => prev.map((t) => (t.id === actualizada.id ? actualizada : t)))
    }, []),
    onDelete: useCallback((eliminada) => {
      setTareas((prev) => prev.filter((t) => t.id !== eliminada.id))
    }, []),
  })

  // Actualización optimista: flip inmediato, revert si falla
  const toggleCompletada = useCallback(async (tarea) => {
    const nueva = !tarea.esta_completada
    setTareas((prev) =>
      prev.map((t) => (t.id === tarea.id ? { ...t, esta_completada: nueva } : t))
    )
    try {
      const updated = await api.completarTarea(tarea.id, nueva)
      setTareas((prev) => prev.map((t) => (t.id === updated.id ? updated : t)))
    } catch {
      // Revertir si la petición falla
      setTareas((prev) => prev.map((t) => (t.id === tarea.id ? tarea : t)))
    }
  }, [])

  const crear = useCallback(async (body) => {
    const nueva = await api.crearTarea(body)
    setTareas((prev) => [...prev, nueva])
    return nueva
  }, [])

  const editar = useCallback(async (id, body) => {
    const updated = await api.editarTarea(id, body)
    setTareas((prev) => prev.map((t) => (t.id === id ? updated : t)))
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
