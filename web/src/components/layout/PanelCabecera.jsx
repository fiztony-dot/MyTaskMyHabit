import '../../styles/layout.css'

export default function PanelCabecera({
  titulo,
  count,
  searchOpen,
  expandirCtrl,
  onMenuMobile,
  onSearch,
  onExpandirTodas,
  onNuevaTarea,
}) {
  return (
    <div className="panel-header">
      {/* Hamburguesa (solo móvil) */}
      <button
        className="panel-header-hamburger"
        onClick={onMenuMobile}
        aria-label="Abrir menú"
      >
        <span className="material-icons">menu</span>
      </button>

      {/* Título dinámico */}
      <div className="panel-header-title-wrap">
        <h1 className="panel-header-title">{titulo}</h1>
        {count != null && (
          <span className="panel-header-count">{count} tarea{count !== 1 ? 's' : ''}</span>
        )}
      </div>

      {/* Búsqueda */}
      <button
        className={`panel-header-btn${searchOpen ? ' activo' : ''}`}
        onClick={onSearch}
        aria-label={searchOpen ? 'Cerrar búsqueda' : 'Buscar'}
      >
        <span className="material-icons">{searchOpen ? 'close' : 'search'}</span>
      </button>

      {/* Expandir/colapsar secciones */}
      <button
        className="panel-header-btn"
        onClick={onExpandirTodas}
        aria-label={expandirCtrl?.open ? 'Colapsar todas' : 'Expandir todas'}
      >
        <span className="material-icons">
          {expandirCtrl?.open ? 'unfold_less' : 'unfold_more'}
        </span>
      </button>

      {/* Nueva tarea */}
      <button
        className="panel-header-btn"
        onClick={onNuevaTarea}
        aria-label="Nueva tarea"
      >
        <span className="material-icons">add</span>
      </button>
    </div>
  )
}
