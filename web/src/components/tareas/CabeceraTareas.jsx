import { useNavigate } from 'react-router-dom'
import '../../styles/tareas.css'

export default function CabeceraTareas({
  totalPendientes,
  searchOpen,
  filterOpen,
  mostrarCompletadas,
  expandirTodas,
  onSearch,
  onFilter,
  onToggleCompletadas,
  onExpandirTodas,
  onLogout,
}) {
  const navigate = useNavigate()

  return (
    <header className="t-header">
      <h1 className="t-header-title">MIS TAREAS ({totalPendientes})</h1>

      <button
        className={`t-header-btn${searchOpen ? ' activo' : ''}`}
        onClick={onSearch}
        aria-label={searchOpen ? 'Cerrar búsqueda' : 'Buscar'}
      >
        <span className="material-icons">{searchOpen ? 'close' : 'search'}</span>
      </button>

      <button
        className={`t-header-btn${filterOpen ? ' activo' : ''}`}
        onClick={onFilter}
        aria-label={filterOpen ? 'Cerrar filtro' : 'Filtrar por categoría'}
      >
        <span className="material-icons">filter_list</span>
      </button>

      <button
        className={`t-header-btn${mostrarCompletadas ? ' activo' : ''}`}
        onClick={onToggleCompletadas}
        aria-label={mostrarCompletadas ? 'Ocultar completadas' : 'Mostrar completadas'}
      >
        <span className="material-icons">
          {mostrarCompletadas ? 'visibility' : 'visibility_off'}
        </span>
      </button>

      <button
        className="t-header-btn"
        onClick={onExpandirTodas}
        aria-label={expandirTodas?.open === true ? 'Contraer todas las secciones' : 'Expandir todas las secciones'}
      >
        <span className="material-icons">
          {expandirTodas?.open === true ? 'unfold_less' : 'unfold_more'}
        </span>
      </button>

      <button
        className="t-header-btn"
        onClick={() => navigate('/categorias')}
        aria-label="Gestionar categorías"
      >
        <span className="material-icons">category</span>
      </button>

      <button
        className="t-header-btn"
        onClick={onLogout}
        aria-label="Cerrar sesión"
      >
        <span className="material-icons">exit_to_app</span>
      </button>
    </header>
  )
}
