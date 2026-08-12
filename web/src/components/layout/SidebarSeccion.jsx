import { useState } from 'react'
import '../../styles/layout.css'

export default function SidebarSeccion({ titulo, link, onLink, colapsable = false, children }) {
  const [open, setOpen] = useState(true)

  return (
    <div className="sidebar-section">
      <div className="sidebar-section-title">
        <span>{titulo}</span>
        <div className="sidebar-section-actions">
          {link && (
            <button className="sidebar-section-link" onClick={onLink}>
              {link}
            </button>
          )}
          {colapsable && (
            <button
              className="sidebar-section-toggle"
              onClick={() => setOpen((v) => !v)}
              aria-label={open ? 'Colapsar' : 'Expandir'}
            >
              <span className="material-icons">
                {open ? 'expand_less' : 'expand_more'}
              </span>
            </button>
          )}
        </div>
      </div>
      {open && children}
    </div>
  )
}
