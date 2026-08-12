import { getIconColor } from '../lib/iconColors'
import '../styles/categorias.css'

export default function CategoriaItem({ categoria, tareasCount, onEdit, onDelete }) {
  function handleDelete(e) {
    e.stopPropagation()
    if (
      window.confirm(
        `¿Eliminar la categoría "${categoria.titulo}"?\n\nLas tareas asociadas NO se borrarán, pero quedarán sin categoría asignada.`
      )
    ) {
      onDelete(categoria.id)
    }
  }

  function handleEdit(e) {
    e.stopPropagation()
    onEdit(categoria)
  }

  const inactiva = categoria.activa === false
  const iconColor = getIconColor(categoria.icono)

  return (
    <div
      className={`c-item${inactiva ? ' inactiva' : ''}`}
      onClick={() => onEdit(categoria)}
    >
      <span
        className="material-icons c-item-icon-mat"
        style={{ color: iconColor }}
      >
        {categoria.icono || 'label_off'}
      </span>

      <div className="c-item-body">
        <span className="c-item-name">{categoria.titulo}</span>
        <div className="c-item-meta">
          {tareasCount > 0 && (
            <span className="c-item-count">
              {tareasCount} {tareasCount === 1 ? 'tarea' : 'tareas'}
            </span>
          )}
          {inactiva && <span className="c-item-badge-inactiva">Inactiva</span>}
        </div>
      </div>

      <div className="c-item-actions">
        <button
          className="c-btn-icon"
          onClick={handleEdit}
          title="Editar"
          aria-label={`Editar ${categoria.titulo}`}
        >
          ✏
        </button>
        <button
          className="c-btn-icon del"
          onClick={handleDelete}
          title="Eliminar"
          aria-label={`Eliminar ${categoria.titulo}`}
        >
          ✕
        </button>
      </div>
    </div>
  )
}
