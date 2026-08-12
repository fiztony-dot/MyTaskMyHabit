import '../../styles/layout.css'

export default function SidebarItem({ icono, label, badge, badgeAlerta, activo, onClick }) {
  return (
    <button
      className={`sidebar-item${activo ? ' activo' : ''}`}
      onClick={onClick}
      aria-pressed={activo}
    >
      <span className="material-icons">{icono}</span>
      <span className="sidebar-item-label">{label}</span>
      {badge != null && badge > 0 && (
        <span className={`sidebar-item-badge${badgeAlerta ? ' alerta' : ''}`}>
          {badge}
        </span>
      )}
    </button>
  )
}
