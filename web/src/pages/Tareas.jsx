import { useState, useMemo, useCallback } from 'react'
import { useTareas } from '../hooks/useTareas'
import { useCategorias } from '../hooks/useCategorias'
import { useAuth } from '../hooks/useAuth'
import { useFiltros, clasificarSidebar } from '../hooks/useFiltros'
import { useOrden } from '../hooks/useOrden'
import { getIconColor } from '../lib/iconColors'
import AppLayout from '../components/layout/AppLayout'
import Sidebar from '../components/layout/Sidebar'
import PanelCabecera from '../components/layout/PanelCabecera'
import OrdenMenu from '../components/layout/OrdenMenu'
import BuscadorTareas from '../components/tareas/BuscadorTareas'
import SeccionVencimiento from '../components/tareas/SeccionVencimiento'
import TareaItem from '../components/tareas/TareaItem'
import TareaForm from '../components/tareas/TareaForm'
import Spinner from '../components/Spinner'
import '../styles/tareas.css'
import '../styles/layout.css'

// ── Secciones temporales ──────────────────────────────────────────────────────
const SECCIONES_PANEL = [
  { key: 'vencidas', label: 'Vencidas',    color: '#ef4444', icono: 'warning',       defaultOpen: true  },
  { key: 'hoy',      label: 'Hoy',         color: '#f59e0b', icono: 'today',         defaultOpen: true  },
  { key: 'semana',   label: 'Esta semana', color: '#10b981', icono: 'date_range',    defaultOpen: false },
  { key: 'resto',    label: 'Resto',       color: '#6b7280', icono: 'schedule',      defaultOpen: false },
]

// ── Secciones por prioridad ───────────────────────────────────────────────────
const SECCIONES_PRIO = [
  { key: 'ALTA',  label: 'Alta',  color: '#ef4444', icono: 'arrow_upward',   defaultOpen: true  },
  { key: 'MEDIA', label: 'Media', color: '#f59e0b', icono: 'drag_handle',    defaultOpen: true  },
  { key: 'BAJA',  label: 'Baja',  color: '#10b981', icono: 'arrow_downward', defaultOpen: false },
]

const TIEMPO_LABELS = {
  vencidas: 'Vencidas',
  hoy: 'Hoy',
  semana: 'Esta semana',
  resto: 'Resto',
  todas: 'Todas las tareas',
}

// ── Componente principal ──────────────────────────────────────────────────────
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

  const { agrupacion, ordenarPor, orden, setAgrupacion, setOrdenarPor, setOrden, sortFn } = useOrden()

  // ── UI state ──────────────────────────────────────────────────────────────
  const [sidebarOpen,    setSidebarOpen]    = useState(false)
  const [searchOpen,     setSearchOpen]     = useState(false)
  const [textoBusqueda,  setTextoBusqueda]  = useState('')
  const [formTarea,      setFormTarea]      = useState(null)
  const [expandirCtrl,   setExpandirCtrl]   = useState(null)
  const [fadingIds,      setFadingIds]      = useState(new Set())
  const [ordenMenuOpen,  setOrdenMenuOpen]  = useState(false)

  // ── Modo de agrupación efectivo ──────────────────────────────────────────
  // En bandeja/completadas la agrupación no aplica (siempre lista plana).
  // En modo tiempo, la agrupación 'tiempo' puede convertir un 'flat' en 'sections-all'.
  const effectiveDisplayMode = useMemo(() => {
    if (modo === 'bandeja' || modo === 'completadas') return displayMode
    if (agrupacion === 'prioridad') return 'sections-prio'
    if (agrupacion === 'categoria') return 'sections-cat-group'
    // agrupacion === 'tiempo': si el filtro de sidebar daría 'flat' (todas+cat), mostramos secciones igualmente
    return displayMode === 'flat' ? 'sections-all' : displayMode
  }, [modo, displayMode, agrupacion])

  // ── Tareas: reintegrar fading + búsqueda + ordenación ───────────────────
  const tareasFiltradas = useMemo(() => {
    let base = tareasBase
    if (fadingIds.size > 0) {
      const ids = new Set(tareasBase.map((t) => t.id))
      const extra = tareas.filter((t) => fadingIds.has(t.id) && !ids.has(t.id))
      base = [...tareasBase, ...extra]
    }
    if (textoBusqueda) {
      const txt = textoBusqueda.toLowerCase()
      base = base.filter((t) => t.titulo.toLowerCase().includes(txt))
    }
    return [...base].sort(sortFn)
  }, [tareasBase, fadingIds, tareas, textoBusqueda, sortFn])

  // ── Grupos del panel según effectiveDisplayMode ──────────────────────────
  const gruposPanel = useMemo(() => {
    if (effectiveDisplayMode === 'sections-prio') {
      const g = { ALTA: [], MEDIA: [], BAJA: [] }
      tareasFiltradas.forEach((t) => { if (g[t.prioridad]) g[t.prioridad].push(t) })
      return g
    }
    if (effectiveDisplayMode === 'sections-cat-group') {
      const g = {}
      tareasFiltradas.forEach((t) => {
        const k = t.categoria_id != null ? String(t.categoria_id) : '__sin_cat'
        if (!g[k]) g[k] = []
        g[k].push(t)
      })
      return g
    }
    if (effectiveDisplayMode.startsWith('sections')) {
      const g = { vencidas: [], hoy: [], semana: [], resto: [] }
      tareasFiltradas.forEach((t) => { g[clasificarSidebar(t)].push(t) })
      return g
    }
    return null
  }, [tareasFiltradas, effectiveDisplayMode])

  // ── Secciones temporales a mostrar ──────────────────────────────────────
  const seccionesAMostrar = useMemo(() => {
    if (effectiveDisplayMode === 'sections-all') return SECCIONES_PANEL
    if (effectiveDisplayMode === 'sections' || effectiveDisplayMode === 'sections-cat') {
      return SECCIONES_PANEL.filter((s) => tiempos.has(s.key))
    }
    return []
  }, [effectiveDisplayMode, tiempos])

  // ── Secciones por categoría (dinámicas) ─────────────────────────────────
  const seccionesCatGroup = useMemo(() => {
    if (effectiveDisplayMode !== 'sections-cat-group' || !gruposPanel) return []
    const result = []
    categorias.filter((c) => c.activa !== false).forEach((cat) => {
      const k = String(cat.id)
      if (gruposPanel[k]?.length > 0) {
        result.push({ key: k, label: cat.titulo, color: getIconColor(cat.icono || 'label'), icono: cat.icono || 'label', defaultOpen: true })
      }
    })
    if (gruposPanel['__sin_cat']?.length > 0) {
      result.push({ key: '__sin_cat', label: 'Sin categoría', color: '#9ca3af', icono: 'label_off', defaultOpen: true })
    }
    return result
  }, [effectiveDisplayMode, gruposPanel, categorias])

  // ── Título dinámico del panel ────────────────────────────────────────────
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

  // ── Handlers ─────────────────────────────────────────────────────────────
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

  function filtroYCierra(fn) {
    return (...args) => { fn(...args); setSidebarOpen(false) }
  }

  if (isLoading) return <Spinner />

  const modoFlat = modo === 'bandeja' || modo === 'completadas'

  // ── Render ────────────────────────────────────────────────────────────────
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
      {/* ── Panel sticky (cabecera + menú orden + buscador) ── */}
      <div className="panel-sticky">
        <div className="panel-header-wrap">
          <PanelCabecera
            titulo={tituloPanelTexto}
            count={tareasFiltradas.length}
            searchOpen={searchOpen}
            sortActive={ordenMenuOpen}
            expandirCtrl={expandirCtrl}
            onMenuMobile={() => setSidebarOpen(true)}
            onSearch={toggleSearch}
            onSort={() => setOrdenMenuOpen((v) => !v)}
            onExpandirTodas={handleExpandirTodas}
            onNuevaTarea={() => setFormTarea(false)}
          />
          {ordenMenuOpen && (
            <>
              <div className="orden-backdrop" onClick={() => setOrdenMenuOpen(false)} />
              <OrdenMenu
                agrupacion={agrupacion}
                ordenarPor={ordenarPor}
                orden={orden}
                onAgrupacion={setAgrupacion}
                onOrdenarPor={setOrdenarPor}
                onOrden={setOrden}
                modoFlat={modoFlat}
              />
            </>
          )}
        </div>
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

        {/* Lista plana: bandeja, completadas, cat+todas (cuando agrupacion≠tiempo) */}
        {(effectiveDisplayMode === 'flat' || effectiveDisplayMode === 'flat-desc') && tareasFiltradas.length > 0 && (
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

        {/* Secciones temporales */}
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

        {/* Secciones por prioridad */}
        {gruposPanel && effectiveDisplayMode === 'sections-prio' && (
          <div className="t-lista">
            {SECCIONES_PRIO.map((s) => (
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

        {/* Secciones por categoría */}
        {gruposPanel && effectiveDisplayMode === 'sections-cat-group' && (
          <div className="t-lista">
            {seccionesCatGroup.map((s) => (
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
