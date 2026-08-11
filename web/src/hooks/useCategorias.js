import { useState, useEffect } from 'react'
import { getCategorias } from '../api/categorias'

export function useCategorias() {
  const [categorias, setCategorias] = useState([])
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    getCategorias()
      .then((data) => setCategorias(data.filter((c) => c.activa !== false)))
      .catch(() => {})
      .finally(() => setIsLoading(false))
  }, [])

  // Mapa id → titulo para resolución rápida en TareaItem
  const catMap = Object.fromEntries(categorias.map((c) => [c.id, c.titulo]))

  return { categorias, catMap, isLoading }
}
