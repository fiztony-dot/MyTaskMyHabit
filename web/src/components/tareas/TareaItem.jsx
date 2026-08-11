import '../../styles/tareas.css'

const PRIO_CLASS = { ALTA: 'alta', MEDIA: 'media', BAJA: 'baja' }

function formatFechaHora(fechaStr, horaStr) {
  if (!fechaStr) return null
  const [y, m, d] = fechaStr.substring(0, 10).split('-')
  const fecha = `${d}/${m}/${y}`
  const hora = horaStr ? horaStr.substring(0, 5) : null
  return hora ? `${fecha} ${hora}` : fecha
}

function isVencida(fechaStr) {
  if (!fechaStr) return false
  const [y, m, d] = fechaStr.substring(0, 10).split('-').map(Number)
  const dia = new Date(y, m - 1, d)
  const hoy = new Date()
  hoy.setHours(0, 0, 0, 0)
  return dia < hoy
}

export default function TareaItem({ tarea, catData, onEdit, onToggle, fadingOut }) {
  const cat = catData?.[tarea.categoria_id]
  const fechaHora = formatFechaHora(tarea.fecha_limite, tarea.hora_limite)
  const vencida = isVencida(tarea.fecha_limite) && !tarea.esta_completada
  const priClass = PRIO_CLASS[tarea.prioridad] || 'baja'

  function handleToggle(e) {
    e.stopPropagation()
    onToggle(tarea)
  }

  return (
    <div
      className={`ti-item ${priClass}${tarea.esta_completada ? ' completada' : ''}${fadingOut ? ' ti-fading-out' : ''}`}
      onClick={() => onEdit(tarea)}
    >
      <input
        type="checkbox"
        className="ti-check"
        checked={tarea.esta_completada}
        onChange={handleToggle}
        onClick={(e) => e.stopPropagation()}
        aria-label={`${tarea.esta_completada ? 'Desmarcar' : 'Completar'} "${tarea.titulo}"`}
      />

      <div className="ti-body">
        <p className={`ti-titulo${tarea.esta_completada ? ' completada' : ''}`}>
          {tarea.titulo}
        </p>
        <div className="ti-meta">
          {fechaHora && (
            <span className={`ti-fecha${vencida ? ' vencida' : ''}`}>
              <span className="material-icons">schedule</span>
              {fechaHora}
            </span>
          )}
          {tarea.pendiente_clasificar && (
            <span className="ti-badge-sin-cat">Sin clasificar</span>
          )}
        </div>
      </div>

      <div className="ti-right">
        {cat?.icono && (
          <span className="material-icons ti-cat-icon" title={cat.titulo}>
            {cat.icono}
          </span>
        )}
        {tarea.repeticion && (
          <span className="material-icons ti-rep-icon" title="Tarea repetitiva">
            repeat
          </span>
        )}
      </div>
    </div>
  )
}
