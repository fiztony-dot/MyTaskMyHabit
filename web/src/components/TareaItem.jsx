import '../styles/tareas.css'

const PRIORIDAD_CLASS = { ALTA: 'alta', MEDIA: 'media', BAJA: 'baja' }

function formatFecha(str) {
  if (!str) return null
  // Fecha puede venir como "YYYY-MM-DD" o "YYYY-MM-DDT..." — tomamos los primeros 10 chars
  const [year, month, day] = str.substring(0, 10).split('-')
  return `${day}/${month}/${year}`
}

function isVencida(str) {
  if (!str) return false
  const fecha = new Date(str.substring(0, 10) + 'T12:00:00')
  return fecha < new Date()
}

export default function TareaItem({ tarea, catMap, onToggle, onEdit, onDelete }) {
  const priClass = PRIORIDAD_CLASS[tarea.prioridad] || 'media'
  const catNombre = tarea.categoria_id ? catMap[tarea.categoria_id] : null
  const fechaStr = formatFecha(tarea.fecha_limite)
  const vencida = isVencida(tarea.fecha_limite) && !tarea.esta_completada

  function handleDelete(e) {
    e.stopPropagation()
    if (window.confirm(`¿Eliminar "${tarea.titulo}"?`)) {
      onDelete(tarea.id)
    }
  }

  function handleEdit(e) {
    e.stopPropagation()
    onEdit(tarea)
  }

  function handleToggle(e) {
    e.stopPropagation()
    onToggle(tarea)
  }

  return (
    <div
      className={`t-item ${priClass} ${tarea.esta_completada ? 'completada' : ''}`}
      onClick={() => onEdit(tarea)}
    >
      <input
        type="checkbox"
        className="t-check"
        checked={tarea.esta_completada}
        onChange={handleToggle}
        onClick={(e) => e.stopPropagation()}
        aria-label={`Marcar "${tarea.titulo}" como ${tarea.esta_completada ? 'pendiente' : 'completada'}`}
      />

      <div className="t-body">
        <p className={`t-titulo${tarea.esta_completada ? ' completada' : ''}`}>
          {tarea.titulo}
        </p>
        <div className="t-meta">
          {catNombre && <span className="t-cat">{catNombre}</span>}
          {fechaStr && (
            <span className={`t-fecha${vencida ? ' vencida' : ''}`}>
              {vencida ? '⚠ ' : '📅 '}{fechaStr}
            </span>
          )}
          {tarea.pendiente_clasificar && (
            <span className="t-badge-clasificar">Sin clasificar</span>
          )}
        </div>
      </div>

      <div className="t-actions">
        <button
          className="t-btn-icon"
          onClick={handleEdit}
          title="Editar"
          aria-label="Editar tarea"
        >
          ✏
        </button>
        <button
          className="t-btn-icon del"
          onClick={handleDelete}
          title="Eliminar"
          aria-label="Eliminar tarea"
        >
          ✕
        </button>
      </div>
    </div>
  )
}
