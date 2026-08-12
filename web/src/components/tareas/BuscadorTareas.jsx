import { useEffect, useRef } from 'react'
import '../../styles/tareas.css'

export default function BuscadorTareas({ valor, onChange }) {
  const ref = useRef(null)

  useEffect(() => {
    ref.current?.focus()
  }, [])

  function handleClear() {
    onChange('')
    ref.current?.focus()
  }

  return (
    <div className="t-search-bar">
      <div className="t-search-wrap">
        <input
          ref={ref}
          className="t-search-input"
          type="text"
          value={valor}
          onChange={(e) => onChange(e.target.value)}
          placeholder="Buscar por título…"
          aria-label="Buscar tareas"
        />
        {valor && (
          <button
            className="t-search-clear"
            onClick={handleClear}
            aria-label="Limpiar búsqueda"
            tabIndex={-1}
          >
            <span className="material-icons">close</span>
          </button>
        )}
      </div>
    </div>
  )
}
