import { useState } from 'react'
import TareaItem from './TareaItem'
import '../../styles/tareas.css'

export default function SeccionVencimiento({ config, tareas, catData, onEdit, onToggle }) {
  const [open, setOpen] = useState(config.defaultOpen)

  if (!tareas || tareas.length === 0) return null

  return (
    <section className="t-section">
      <button
        className="t-section-header"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
      >
        <span className="t-section-dot" style={{ background: config.color }} />
        <span
          className="material-icons t-section-mat-icon"
          style={{ color: config.color }}
        >
          {config.icono}
        </span>
        <span className="t-section-name" style={{ color: config.color }}>
          {config.label}
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
        />
      ))}
    </section>
  )
}
