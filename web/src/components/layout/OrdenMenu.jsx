import '../../styles/layout.css'

const AGRUPACIONES = [
  { key: 'tiempo',    label: 'Tiempo',    icono: 'schedule'      },
  { key: 'prioridad', label: 'Prioridad', icono: 'priority_high' },
  { key: 'categoria', label: 'Categoría', icono: 'label'         },
]

const ORDENAR_POR = [
  { key: 'fecha_limite',   label: 'Fecha límite' },
  { key: 'prioridad',      label: 'Prioridad'    },
  { key: 'fecha_creacion', label: 'Creación'     },
  { key: 'titulo',         label: 'Título'       },
]

const ORDEN_DIR = [
  { key: 'asc',  label: 'Ascendente',  icono: 'arrow_upward'   },
  { key: 'desc', label: 'Descendente', icono: 'arrow_downward' },
]

export default function OrdenMenu({
  agrupacion, ordenarPor, orden,
  onAgrupacion, onOrdenarPor, onOrden,
  modoFlat,
}) {
  return (
    <div className="orden-menu">
      {!modoFlat && (
        <div className="orden-menu-grupo">
          <p className="orden-menu-label">Agrupar por</p>
          <div className="orden-menu-opciones">
            {AGRUPACIONES.map((op) => (
              <button
                key={op.key}
                className={`orden-menu-opt${agrupacion === op.key ? ' activo' : ''}`}
                onClick={() => onAgrupacion(op.key)}
              >
                <span className="material-icons">{op.icono}</span>
                {op.label}
              </button>
            ))}
          </div>
        </div>
      )}

      <div className="orden-menu-grupo">
        <p className="orden-menu-label">Ordenar por</p>
        <div className="orden-menu-opciones">
          {ORDENAR_POR.map((op) => (
            <button
              key={op.key}
              className={`orden-menu-opt${ordenarPor === op.key ? ' activo' : ''}`}
              onClick={() => onOrdenarPor(op.key)}
            >
              {op.label}
            </button>
          ))}
        </div>
      </div>

      <div className="orden-menu-grupo">
        <p className="orden-menu-label">Orden</p>
        <div className="orden-menu-opciones">
          {ORDEN_DIR.map((op) => (
            <button
              key={op.key}
              className={`orden-menu-opt${orden === op.key ? ' activo' : ''}`}
              onClick={() => onOrden(op.key)}
            >
              <span className="material-icons">{op.icono}</span>
              {op.label}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
