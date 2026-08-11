import { useState, useEffect } from 'react'
import TareaItem from './TareaItem'
import '../../styles/tareas.css'

export default function SeccionCategoria({ categoria, tareas, catData, onEdit, onToggle, expandirCtrl, fadingIds }) {
  const [open, setOpen] = useState(false)

  useEffect(() => {
    if (expandirCtrl) setOpen(expandirCtrl.open)
  }, [expandirCtrl])

  if (!tareas || tareas.length === 0) return null

  const nombre = categoria?.titulo ?? 'Sin categoría'
  const icono = categoria?.icono ?? 'label_off'
  const color = categoria ? '#6366f1' : '#9ca3af'

  return (
    <section className="t-section">
      <button
        className="t-section-header"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
      >
        <span
          className="material-icons t-section-mat-icon"
          style={{ color }}
        >
          {icono}
        </span>
        <span className="t-section-name" style={{ color }}>
          {nombre.toUpperCase()}
        </span>
        <span className="t-section-count">{tareas.length}</span>
        <span className="material-icons t-section-chevron">
          {open ? 'expand_less' : 'expand_more'}
        </span>
      </button>

      {open && tareas.map((t) => (
        <TareaItem
          key={t.id}
          tarea={t}
          catData={catData}
          onEdit={onEdit}
          onToggle={onToggle}
          fadingOut={fadingIds?.has(t.id) ?? false}
        />
      ))}
    </section>
  )
}
