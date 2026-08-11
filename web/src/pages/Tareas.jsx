import { useState } from 'react'
import { useAuth } from '../hooks/useAuth'
import { useTareas } from '../hooks/useTareas'
import { useCategorias } from '../hooks/useCategorias'
import TareaItem from '../components/TareaItem'
import TareaForm from '../components/TareaForm'
import Spinner from '../components/Spinner'
import '../styles/tareas.css'

const PRIORIDADES = ['ALTA', 'MEDIA', 'BAJA']
const PRIORIDAD_LABEL = { ALTA: 'Alta', MEDIA: 'Media', BAJA: 'Baja' }
const DOT_CLASS = { ALTA: 't-dot-alta', MEDIA: 't-dot-media', BAJA: 't-dot-baja' }

// ── Agrupa las tareas pendientes por prioridad ──
function agruparPorPrioridad(tareas) {
  return PRIORIDADES.reduce((acc, p) => {
    const grupo = tareas.filter((t) => t.prioridad === p && !t.esta_completada)
    if (grupo.length) acc[p] = grupo
    return acc
  }, {})
}

export default function Tareas() {
  const { user, logout } = useAuth()
  const { tareas, isLoading, error, cargar, toggleCompletada, crear, editar, eliminar } = useTareas()
  const { categorias, catMap } = useCategorias()

  const [soloP, setSoloP] = useState(false)         // filtro Todas / Pendientes
  const [formTarea, setFormTarea] = useState(null)   // null=cerrado, false=nueva, tarea=editar
  const [mostrarCompletadas, setMostrarCompletadas] = useState(false)

  // Separar pendientes y completadas
  const pendientes = tareas.filter((t) => !t.esta_completada)
  const completadas = tareas.filter((t) => t.esta_completada)

  // Grupos por prioridad (solo sobre pendientes)
  const grupos = agruparPorPrioridad(pendientes)
  const hayPendientes = pendientes.length > 0
  const hayCompletadas = completadas.length > 0 && !soloP

  async function handleGuardar(body, id) {
    if (id) {
      await editar(id, body)
    } else {
      await crear(body)
    }
  }

  if (isLoading) return <Spinner />

  return (
    <div className="t-page">
      {/* ── Header ── */}
      <header className="t-header">
        <h1 className="t-header-title">Tareas</h1>
        <button className="t-btn-nueva" onClick={() => setFormTarea(false)}>
          + Nueva
        </button>
        <button className="t-btn-logout" onClick={logout} title={`Sesión: ${user?.username}`}>
          Salir
        </button>
      </header>

      {/* ── Filtro ── */}
      <div className="t-filtro">
        <button
          className={`t-filtro-btn${!soloP ? ' activo' : ''}`}
          onClick={() => setSoloP(false)}
        >
          Todas ({tareas.length})
        </button>
        <button
          className={`t-filtro-btn${soloP ? ' activo' : ''}`}
          onClick={() => setSoloP(true)}
        >
          Pendientes ({pendientes.length})
        </button>
      </div>

      {/* ── Error ── */}
      {error && (
        <div className="t-error">
          <p>{error}</p>
          <button onClick={cargar}>Reintentar</button>
        </div>
      )}

      {/* ── Lista vacía ── */}
      {!error && !hayPendientes && !hayCompletadas && (
        <div className="t-vacio">
          <p className="t-vacio-titulo">No hay tareas {soloP ? 'pendientes' : ''}</p>
          <p>Pulsa "+ Nueva" para crear la primera.</p>
        </div>
      )}

      {/* ── Grupos por prioridad (pendientes) ── */}
      {PRIORIDADES.map((p) => {
        const grupo = grupos[p]
        if (!grupo) return null
        return (
          <section key={p} className="t-section">
            <div className="t-section-header">
              <span className={`t-dot ${DOT_CLASS[p]}`} />
              {PRIORIDAD_LABEL[p]} · {grupo.length}
            </div>
            {grupo.map((t) => (
              <TareaItem
                key={t.id}
                tarea={t}
                catMap={catMap}
                onToggle={toggleCompletada}
                onEdit={setFormTarea}
                onDelete={eliminar}
              />
            ))}
          </section>
        )
      })}

      {/* ── Sección Completadas ── */}
      {hayCompletadas && (
        <section className="t-section">
          <button
            className="t-completadas-toggle"
            onClick={() => setMostrarCompletadas((v) => !v)}
          >
            <span className="t-dot t-dot-done" />
            Completadas ({completadas.length}) {mostrarCompletadas ? '▲' : '▼'}
          </button>
          {mostrarCompletadas &&
            completadas.map((t) => (
              <TareaItem
                key={t.id}
                tarea={t}
                catMap={catMap}
                onToggle={toggleCompletada}
                onEdit={setFormTarea}
                onDelete={eliminar}
              />
            ))}
        </section>
      )}

      {/* ── Modal Crear / Editar ── */}
      {formTarea !== null && (
        <TareaForm
          tarea={formTarea || null}
          categorias={categorias}
          onGuardar={handleGuardar}
          onEliminar={eliminar}
          onCerrar={() => setFormTarea(null)}
        />
      )}
    </div>
  )
}
