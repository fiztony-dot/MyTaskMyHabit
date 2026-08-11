import { useState, useEffect, useCallback } from 'react'
import * as api from '../api/categorias'

export function useCategorias() {
  const [categorias, setCategorias] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)

  const cargar = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await api.getCategorias()
      setCategorias(data)
    } catch {
      setError('Error al cargar las categorías.')
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    cargar()
  }, [cargar])

  // Mapa id → titulo (para compatibilidad)
  const catMap = Object.fromEntries(categorias.map((c) => [c.id, c.titulo]))
  // Mapa id → objeto categoría completo (titulo + icono + activa)
  const catData = Object.fromEntries(categorias.map((c) => [c.id, c]))

  const crear = useCallback(async (body) => {
    const nueva = await api.crearCategoria(body)
    setCategorias((prev) => [...prev, nueva])
    return nueva
  }, [])

  const editar = useCallback(async (id, body) => {
    const updated = await api.editarCategoria(id, body)
    setCategorias((prev) => prev.map((c) => (c.id === id ? updated : c)))
    return updated
  }, [])

  const eliminar = useCallback(async (id) => {
    await api.eliminarCategoria(id)
    setCategorias((prev) => prev.filter((c) => c.id !== id))
  }, [])

  return { categorias, catMap, catData, isLoading, error, cargar, crear, editar, eliminar }
}
