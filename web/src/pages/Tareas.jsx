import { useState, useMemo } from 'react'
import { useTareas } from '../hooks/useTareas'
import { useCategorias } from '../hooks/useCategorias'
import { useAuth } from '../hooks/useAuth'
import CabeceraTareas from '../components/tareas/CabeceraTareas'
import BuscadorTareas from '../components/tareas/BuscadorTareas'
import FiltrosCategorias from '../components/tareas/FiltrosCategorias'
import SeccionVencimiento from '../components/tareas/SeccionVencimiento'
import SeccionCategoria from '../components/tareas/SeccionCategoria'
import TareaForm from '../components/tareas/TareaForm'
import Spinner from '../components/Spinner'
import '../styles/tareas.css'

// ── Configuración de secciones de vencimiento ──────────────────────────────
const SECCIONES_VENC = [
  { key: 'clasificar', label: 'Pendientes de clasificar', color: '#ea580c', icono: 'help_outline',    defaultOpen: true  },
  { key: 'vencidas',   label: 'Vencidas',                 color: '#ef4444', icono: 'warning',         defaultOpen: true  },
  { key: 'hoy',        label: 'Hoy',                      color: '#f59e0b', icono: 'today',            defaultOpen: false },
  { key: 'semana',     label: 'Esta semana',              color: '#10b981', icono: 'date_range',       defaultOpen: false },
  { key: 'mes',        label: 'Este mes',                 color: '#3b82f6', icono: 'calendar_month',   defaultOpen: false },
  { key: 'adelante',   label: 'Más adelante',             color: '#6b7280', icono: 'schedule',         defaultOpen: false },
  { key: 'sin-fecha',  label: 'Sin fecha',                color: '#9ca3af', icono: 'event_busy',       defaultOpen: false },
]

// ── Utilidades de fecha ─────────────────────────────────────────────────────
function startOfDay(d) {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate())
}

function tareaDay(tarea) {
  const [y, m, d] = tarea.fecha_limite.substring(0, 10).split('-').map(Number)
  return new Date(y, m - 1, d)
}

function endOfWeek(hoy) {
  const d = new Date(hoy)
  const diasHastaDomingo = d.getDay() === 0 ? 0 : 7 - d.getDay()
  d.setDate(d.getDate() + diasHastaDomingo)
  return d
}

function clasificarTarea(tarea) {
  if (tarea.pendiente_clasificar) return 'clasificar'
  if (!tarea.fecha_limite) return 'sin-fecha'

  const hoy = startOfDay(new Date())
  const dia = tareaDay(tarea)

  if (dia < hoy) return 'vencidas'
  if (dia.getTime() === hoy.getTime()) return 'hoy'

  const finSemana = endOfWeek(hoy)
  if (dia <= finSemana) return 'semana'

  const finMes = startOfDay(new Date(hoy.getFullYear(), hoy.getMonth() + 1, 0))
  if (dia <= finMes) return 'mes'

  return 'adelante'
}

// ── Componente principal ────────────────────────────────────────────────────
export default function Tareas() {
  const { logout } = useAuth()
  const { tareas, isLoading, error, cargar, toggleCompletada, crear, editar, eliminar } = useTareas()
  const { categorias, catData } = useCategorias()

  const [vista, setVista] = useState('vencimiento')
  const [searchOpen, setSearchOpen] = useState(false)
  const [filterOpen, setFilterOpen] = useState(false)
  const [textoBusqueda, setTextoBusqueda] = useState('')
  const [catSelec, setCatSelec] = useState(null)
  const [mostrarCompletadas, setMostrarCompletadas] = useState(false)
  const [formTarea, setFormTarea] = useState(null) // null=cerrado, false=nueva, obj=editar

  // Filtrado: búsqueda + categoría + completadas
  const tareasFiltradas = useMemo(() => {
    return tareas.filter((t) => {
      if (!mostrarCompletadas && t.esta_completada) return false
      if (catSelec !== null && t.categoria_id !== catSelec) return false
      if (textoBusqueda) {
        const texto = textoBusqueda.toLowerCase()
        if (!t.titulo.toLowerCase().includes(texto)) return false
      }
      return true
    })
  }, [tareas, mostrarCompletadas, catSelec, textoBusqueda])

  const totalPendientes = useMemo(
    () => tareas.filter((t) => !t.esta_completada).length,
    [tareas]
  )

  // Agrupación por vencimiento
  const gruposVenc = useMemo(() => {
    const grupos = Object.fromEntries(SECCIONES_VENC.map((s) => [s.key, []]))
    tareasFiltradas.forEach((t) => {
      const key = clasificarTarea(t)
      if (grupos[key]) grupos[key].push(t)
    })
    return grupos
  }, [tareasFiltradas])

  // Agrupación por categoría — Map preserva orden de primera aparición
  const gruposCat = useMemo(() => {
    const map = new Map()
    const sinCat = []
    tareasFiltradas.forEach((t) => {
      if (t.categoria_id == null) {
        sinCat.push(t)
      } else {
        if (!map.has(t.categoria_id)) map.set(t.categoria_id, [])
        map.get(t.categoria_id).push(t)
      }
    })
    // Ordenar por nombre de categoría
    const sorted = new Map(
      [...map.entries()].sort((a, b) => {
        const na = catData?.[a[0]]?.titulo ?? ''
        const nb = catData?.[b[0]]?.titulo ?? ''
        return na.localeCompare(nb, 'es')
      })
    )
    return { map: sorted, sinCat }
  }, [tareasFiltradas, catData])

  async function handleGuardar(body, id) {
    if (id) await editar(id, body)
    else await crear(body)
  }

  function toggleSearch() {
    if (searchOpen) setTextoBusqueda('')
    setSearchOpen((v) => !v)
  }

  function toggleFilter() {
    if (filterOpen) setCatSelec(null)
    setFilterOpen((v) => !v)
  }

  if (isLoading) return <Spinner />

  return (
    <div className="t-page">
      {/* ── Cabecera fija ── */}
      <CabeceraTareas
        totalPendientes={totalPendientes}
        searchOpen={searchOpen}
        filterOpen={filterOpen}
        mostrarCompletadas={mostrarCompletadas}
        onSearch={toggleSearch}
        onFilter={toggleFilter}
        onToggleCompletadas={() => setMostrarCompletadas((v) => !v)}
        onLogout={logout}
      />

      {/* ── Buscador ── */}
      {searchOpen && (
        <BuscadorTareas valor={textoBusqueda} onChange={setTextoBusqueda} />
      )}

      {/* ── Chips de filtro por categoría ── */}
      {filterOpen && (
        <FiltrosCategorias
          categorias={categorias}
          catSelec={catSelec}
          onChange={setCatSelec}
        />
      )}

      {/* ── Tabs ── */}
      <div className="t-tabs" role="tablist">
        <button
          role="tab"
          className={`t-tab${vista === 'vencimiento' ? ' activo' : ''}`}
          aria-selected={vista === 'vencimiento'}
          onClick={() => setVista('vencimiento')}
        >
          Por vencimiento
        </button>
        <button
          role="tab"
          className={`t-tab${vista === 'categorias' ? ' activo' : ''}`}
          aria-selected={vista === 'categorias'}
          onClick={() => setVista('categorias')}
        >
          Por categoría
        </button>
      </div>

      {/* ── Error ── */}
      {error && (
        <div className="t-error">
          <p>{error}</p>
          <button onClick={cargar}>Reintentar</button>
        </div>
      )}

      {/* ── Estado vacío ── */}
      {!error && tareasFiltradas.length === 0 && (
        <div className="t-vacio">
          <span className="material-icons">check_circle_outline</span>
          <p className="t-vacio-titulo">
            {textoBusqueda
              ? 'Sin resultados para la búsqueda'
              : catSelec !== null
              ? 'No hay tareas en esta categoría'
              : 'No hay tareas pendientes'}
          </p>
        </div>
      )}

      {/* ── Vista: Por vencimiento ── */}
      {vista === 'vencimiento' && (
        <div className="t-lista" role="tabpanel">
          {SECCIONES_VENC.map((s) => (
            <SeccionVencimiento
              key={s.key}
              config={s}
              tareas={gruposVenc[s.key]}
              catData={catData}
              onEdit={setFormTarea}
              onToggle={toggleCompletada}
            />
          ))}
        </div>
      )}

      {/* ── Vista: Por categoría ── */}
      {vista === 'categorias' && (
        <div className="t-lista" role="tabpanel">
          {Array.from(gruposCat.map.entries()).map(([catId, catTareas]) => (
            <SeccionCategoria
              key={catId}
              categoria={catData?.[catId]}
              tareas={catTareas}
              catData={catData}
              onEdit={setFormTarea}
              onToggle={toggleCompletada}
            />
          ))}
          {gruposCat.sinCat.length > 0 && (
            <SeccionCategoria
              key="sin-categoria"
              categoria={null}
              tareas={gruposCat.sinCat}
              catData={catData}
              onEdit={setFormTarea}
              onToggle={toggleCompletada}
            />
          )}
        </div>
      )}

      {/* ── FAB nueva tarea ── */}
      <button
        className="t-fab"
        onClick={() => setFormTarea(false)}
        aria-label="Nueva tarea"
      >
        <span className="material-icons">add</span>
      </button>

      {/* ── Modal crear/editar ── */}
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
