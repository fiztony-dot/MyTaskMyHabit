import { useNavigate } from 'react-router-dom'
import SidebarItem from './SidebarItem'
import SidebarSeccion from './SidebarSeccion'
import '../../styles/layout.css'

const TIEMPO_ITEMS = [
  { key: 'vencidas', label: 'Vencidas',    icono: 'warning'    },
  { key: 'hoy',      label: 'Hoy',         icono: 'today'      },
  { key: 'semana',   label: 'Esta semana', icono: 'date_range' },
  { key: 'resto',    label: 'Resto',       icono: 'schedule'   },
  { key: 'todas',    label: 'Todas',       icono: 'list'       },
]

export default function Sidebar({
  modo,
  tiempos,
  categorias,
  contadores,
  categoriasLista,
  realtimeConectado,
  mostrarLimpiar,
  onBandeja,
  onCompletadas,
  onToggleTiempo,
  onToggleCategoria,
  onLimpiar,
  onLogout,
}) {
  const navigate = useNavigate()
  const hayBandeja = contadores.bandeja > 0
  const catActivas = categoriasLista.filter((c) => c.activa !== false)

  return (
    <>
      {/* ── Cabecera ── */}
      <div className="sidebar-header">
        <span className="sidebar-logo">MyTaskMyHabit</span>
        <span
          className="sidebar-realtime"
          style={{ background: realtimeConectado ? '#22c55e' : '#6b7280' }}
          title={realtimeConectado ? 'Realtime activo' : 'Sin Realtime'}
        />
        <button
          className="sidebar-logout-btn"
          onClick={onLogout}
          aria-label="Cerrar sesión"
        >
          <span className="material-icons">exit_to_app</span>
        </button>
      </div>

      {/* ── Bandeja de entrada ── */}
      <SidebarSeccion titulo="Bandeja">
        <SidebarItem
          icono="inbox"
          label="Bandeja de entrada"
          badge={contadores.bandeja || 0}
          badgeAlerta={hayBandeja}
          activo={modo === 'bandeja'}
          onClick={onBandeja}
        />
      </SidebarSeccion>

      {/* ── Por tiempo ── */}
      <SidebarSeccion titulo="Por tiempo">
        {TIEMPO_ITEMS.map((item) => (
          <SidebarItem
            key={item.key}
            icono={item.icono}
            label={item.label}
            badge={item.key !== 'todas' ? (contadores[item.key] ?? 0) : undefined}
            activo={modo === 'tiempo' && tiempos.has(item.key)}
            onClick={() => onToggleTiempo(item.key)}
          />
        ))}
      </SidebarSeccion>

      {/* ── Por categoría ── */}
      <SidebarSeccion
        titulo="Categorías"
        colapsable={catActivas.length > 7}
        link="Gestionar"
        onLink={() => navigate('/categorias')}
      >
        {catActivas.length === 0 && (
          <p style={{ padding: '0.25rem 0.875rem', fontSize: '0.8125rem', color: '#9ca3af', margin: 0 }}>
            Sin categorías
          </p>
        )}
        {catActivas.map((cat) => (
          <SidebarItem
            key={cat.id}
            icono={cat.icono || 'label'}
            label={cat.titulo}
            badge={contadores.cat[cat.id] ?? 0}
            activo={categorias.has(cat.id)}
            onClick={() => onToggleCategoria(cat.id)}
          />
        ))}
      </SidebarSeccion>

      {/* ── Completadas ── */}
      <SidebarSeccion titulo="Archivo">
        <SidebarItem
          icono="done_all"
          label="Completadas"
          activo={modo === 'completadas'}
          onClick={onCompletadas}
        />
      </SidebarSeccion>

      {/* ── Limpiar filtros ── */}
      {mostrarLimpiar && (
        <div className="sidebar-limpiar-wrap">
          <button className="sidebar-limpiar" onClick={onLimpiar}>
            <span className="material-icons">filter_alt_off</span>
            Limpiar filtros
          </button>
        </div>
      )}
    </>
  )
}
