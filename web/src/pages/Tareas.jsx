import { useState, useMemo, useCallback } from 'react'
import { useTareas } from '../hooks/useTareas'
import { useCategorias } from '../hooks/useCategorias'
import { useAuth } from '../hooks/useAuth'
import { useFiltros, clasificarSidebar, sortPrioFecha } from '../hooks/useFiltros'
import AppLayout from '../components/layout/AppLayout'
import Sidebar from '../components/layout/Sidebar'
import PanelCabecera from '../components/layout/PanelCabecera'
import BuscadorTareas from '../components/tareas/BuscadorTareas'
import SeccionVencimiento from '../components/tareas/SeccionVencimiento'
import TareaItem from '../components/tareas/TareaItem'
import TareaForm from '../components/tareas/TareaForm'
import Spinner from '../components/Spinner'
import '../styles/tareas.css'
import '../styles/layout.css'

// ── Secciones del panel (alineadas con los ítems del sidebar) ──────────────
const SECCIONES_PANEL = [
  { key: 'vencidas', label: 'Vencidas',    color: '#ef4444', icono: 'warning',    defaultOpen: true  },
  { key: 'hoy',      label: 'Hoy',         color: '#f59e0b', icono: 'today',      defaultOpen: true  },
  { key: 'semana',   label: 'Esta semana', color: '#10b981', icono: 'date_range', defaultOpen: false },
  { key: 'resto',    label: 'Resto',       color: '#6b7280', icono: 'schedule',   defaultOpen: false },
]

const TIEMPO_LABELS = {
  vencidas: 'Vencidas',
  hoy: 'Hoy',
  semana: 'Esta semana',
  resto: 'Resto',
  todas: 'Todas las tareas',
}

// ── Componente principal ────────────────────────────────────────────────────
export default function Tareas() {
  const { logout } = useAuth()
  const {
    tareas, isLoading, error, cargar,
    toggleCompletada, crear, editar, eliminar,
    realtimeConectado,
  } = useTareas()
  const { categorias, catData } = useCategorias()

  const filtros = useFiltros(tareas, isLoading)
  const {
    modo, tiempos, categorias: catFiltros, contadores,
    displayMode, tareasFiltradas: tareasBase, mostrarLimpiar,
    irABandeja, irACompletadas, toggleTiempo, toggleCategoria, limpiarFiltros,
  } = filtros

  // ── UI state ───────────────────────────────────────────────────────────────
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [searchOpen, setSearchOpen]   = useState(false)
  const [textoBusqueda, setTextoBusqueda] = useState('')
  const [formTarea, setFormTarea]     = useState(null)
  const [expandirCtrl, setExpandirCtrl] = useState(null)
  const [fadingIds, setFadingIds]     = useState(new Set())

  // ── Tareas: reintegrar fading + aplicar búsqueda ─────────────────────────
  const tareasFiltradas = useMemo(() => {
    // Reintegrar items en fading que ya no están en tareasBase
    let base = tareasBase
    if (fadingIds.size > 0) {
      const ids = new Set(tareasBase.map((t) => t.id))
      const extra = tareas.filter((t) => fadingIds.has(t.id) && !ids.has(t.id))
      base = [...tareasBase, ...extra]
    }
    if (!textoBusqueda) return base
    const txt = textoBusqueda.toLowerCase()
    return base.filter((t) => t.titulo.toLowerCase().includes(txt))
  }, [tareasBase, fadingIds, tareas, textoBusqueda])

  // ── Agrupar por sección temporal (4 buckets del sidebar) ─────────────────
  const gruposPanel = useMemo(() => {
    if (!displayMode.startsWith('sections')) return null
    const g = { vencidas: [], hoy: [], semana: [], resto: [] }
    tareasFiltradas.forEach((t) => {
      g[clasificarSidebar(t)].push(t)
    })
    Object.values(g).forEach((arr) => arr.sort(sortPrioFecha))
    return g
  }, [tareasFiltradas, displayMode])

  const seccionesAMostrar = useMemo(() => {
    if (displayMode === 'sections-all') return SECCIONES_PANEL
    if (displayMode === 'sections' || displayMode === 'sections-cat') {
      return SECCIONES_PANEL.filter((s) => tiempos.has(s.key))
    }
    return []
  }, [displayMode, tiempos])

  // ── Título dinámico del panel ─────────────────────────────────────────────
  const tituloPanelTexto = useMemo(() => {
    if (modo === 'bandeja') return 'Bandeja de entrada'
    if (modo === 'completadas') return 'Completadas'
    const partes = []
    if (!tiempos.has('todas') && tiempos.size > 0) {
      tiempos.forEach((k) => { if (TIEMPO_LABELS[k]) partes.push(TIEMPO_LABELS[k]) })
    }
    catFiltros.forEach((id) => {
      const cat = catData?.[id]
      if (cat) partes.push(cat.titulo)
    })
    return partes.length > 0 ? partes.join(' · ') : 'Todas las tareas'
  }, [modo, tiempos, catFiltros, catData])

  // ── Handlers ──────────────────────────────────────────────────────────────
  const handleToggle = useCallback(
    (tarea) => {
      const completando = !tarea.esta_completada
      if (completando && modo !== 'completadas') {
        setFadingIds((prev) => { const s = new Set(prev); s.add(tarea.id); return s })
        toggleCompletada(tarea)
        setTimeout(() => {
          setFadingIds((prev) => { const s = new Set(prev); s.delete(tarea.id); return s })
        }, 400)
      } else {
        toggleCompletada(tarea)
      }
    },
    [modo, toggleCompletada]
  )

  function handleExpandirTodas() {
    setExpandirCtrl((prev) => ({ open: prev?.open !== true, seq: (prev?.seq ?? 0) + 1 }))
  }

  async function handleGuardar(body, id) {
    if (id) await editar(id, body)
    else await crear(body)
  }

  function toggleSearch() {
    if (searchOpen) setTextoBusqueda('')
    setSearchOpen((v) => !v)
  }

  // Cierra el sidebar móvil tras seleccionar un filtro
  function filtroYCierra(fn) {
    return (...args) => { fn(...args); setSidebarOpen(false) }
  }

  if (isLoading) return <Spinner />

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <AppLayout
      sidebarOpen={sidebarOpen}
      onCloseSidebar={() => setSidebarOpen(false)}
      sidebar={
        <Sidebar
          modo={modo}
          tiempos={tiempos}
          categorias={catFiltros}
          contadores={contadores}
          categoriasLista={categorias}
          realtimeConectado={realtimeConectado}
          mostrarLimpiar={mostrarLimpiar}
          onBandeja={filtroYCierra(irABandeja)}
          onCompletadas={filtroYCierra(irACompletadas)}
          onToggleTiempo={filtroYCierra(toggleTiempo)}
          onToggleCategoria={filtroYCierra(toggleCategoria)}
          onLimpiar={filtroYCierra(limpiarFiltros)}
          onLogout={logout}
        />
      }
    >
      {/* ── Panel sticky (cabecera + buscador) ── */}
      <div className="panel-sticky">
        <PanelCabecera
          titulo={tituloPanelTexto}
          count={tareasFiltradas.length}
          searchOpen={searchOpen}
          expandirCtrl={expandirCtrl}
          onMenuMobile={() => setSidebarOpen(true)}
          onSearch={toggleSearch}
          onExpandirTodas={handleExpandirTodas}
          onNuevaTarea={() => setFormTarea(false)}
        />
        {searchOpen && (
          <BuscadorTareas valor={textoBusqueda} onChange={setTextoBusqueda} />
        )}
      </div>

      {/* ── Cuerpo del panel ── */}
      <div className="panel-body">
        {error && (
          <div className="t-error">
            <p>{error}</p>
            <button onClick={cargar}>Reintentar</button>
          </div>
        )}

        {!error && tareasFiltradas.length === 0 && (
          <div className="t-vacio">
            <span className="material-icons">
              {textoBusqueda ? 'search_off' : 'check_circle_outline'}
            </span>
            <p className="t-vacio-titulo">
              {textoBusqueda ? `Sin resultados para "${textoBusqueda}"` : 'Sin tareas'}
            </p>
            {textoBusqueda && <p className="t-vacio-sub">Prueba con otras palabras</p>}
          </div>
        )}

        {/* Lista plana (bandeja, completadas, categoría sin tiempo) */}
        {(displayMode === 'flat' || displayMode === 'flat-desc') && tareasFiltradas.length > 0 && (
          <div className="t-lista">
            <div className="t-section">
              {tareasFiltradas.map((t) => (
                <TareaItem
                  key={t.id}
                  tarea={t}
                  catData={catData}
                  onEdit={setFormTarea}
                  onToggle={handleToggle}
                  fadingOut={fadingIds.has(t.id)}
                />
              ))}
            </div>
          </div>
        )}

        {/* Lista por secciones temporales */}
        {gruposPanel && seccionesAMostrar.length > 0 && (
          <div className="t-lista">
            {seccionesAMostrar.map((s) => (
              <SeccionVencimiento
                key={s.key}
                config={s}
                tareas={gruposPanel[s.key]}
                catData={catData}
                onEdit={setFormTarea}
                onToggle={handleToggle}
                expandirCtrl={expandirCtrl}
                fadingIds={fadingIds}
              />
            ))}
          </div>
        )}
      </div>

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
    </AppLayout>
  )
}
