import '../../styles/tareas.css'

export default function FiltrosCategorias({ categorias, catSelec, onChange }) {
  const activas = categorias.filter((c) => c.activa !== false)

  return (
    <div className="t-chips" role="group" aria-label="Filtrar por categoría">
      <button
        className={`t-chip${catSelec === null ? ' activa' : ''}`}
        onClick={() => onChange(null)}
      >
        Todas
      </button>

      {activas.map((c) => (
        <button
          key={c.id}
          className={`t-chip${catSelec === c.id ? ' activa' : ''}`}
          onClick={() => onChange(c.id)}
        >
          {c.icono && (
            <span className="material-icons">{c.icono}</span>
          )}
          {c.titulo}
        </button>
      ))}
    </div>
  )
}
