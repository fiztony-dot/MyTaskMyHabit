import { useEffect, useRef } from 'react'
import '../../styles/tareas.css'

export default function BuscadorTareas({ valor, onChange }) {
  const ref = useRef(null)

  useEffect(() => {
    ref.current?.focus()
  }, [])

  return (
    <div className="t-search-bar">
      <input
        ref={ref}
        className="t-search-input"
        type="text"
        value={valor}
        onChange={(e) => onChange(e.target.value)}
        placeholder="Buscar por título…"
        aria-label="Buscar tareas"
      />
    </div>
  )
}
