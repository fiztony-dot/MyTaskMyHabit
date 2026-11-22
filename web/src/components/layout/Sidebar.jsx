import { useNavigate } from 'react-router-dom'
import SidebarItem from './SidebarItem'
import SidebarSeccion from './SidebarSeccion'
import { getIconColor } from '../../lib/iconColors'
import { usePushNotifications } from '../../hooks/usePushNotifications'
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
  const {
    soportado, permiso, suscrito, cargando: pushCargando,
    suscribir, desuscribir,
    enviarPrueba, mensajePrueba, cargandoPrueba,
  } = usePushNotifications()

  const permisoDenegado = permiso === 'denied'

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
        {soportado && !permisoDenegado && (
          <button
            className="sidebar-logout-btn"
            onClick={suscrito ? desuscribir : suscribir}
            disabled={pushCargando}
            aria-label={suscrito ? 'Desactivar notificaciones push' : 'Activar notificaciones push'}
            title={suscrito ? 'Notificaciones activas — pulsa para desactivar' : 'Activar notificaciones push'}
            style={{ color: suscrito ? '#6366f1' : undefined }}
          >
            <span className="material-icons">
              {suscrito ? 'notifications_active' : 'notifications_none'}
            </span>
          </button>
        )}
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
            iconColor={getIconColor(cat.icono || 'label')}
            onClick={() => onToggleCategoria(cat.id)}
          />
        ))}
        {contadores.sinCat > 0 && (
          <SidebarItem
            icono="inbox"
            label="Sin categoría"
            badge={contadores.sinCat}
            activo={categorias.has(null)}
            iconColor="#9ca3af"
            onClick={() => onToggleCategoria(null)}
          />
        )}
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

      {/* ── Notificaciones push ── */}
      <div style={{ borderTop: '1px solid #e5e7eb', margin: '0.5rem 0 0', padding: '0.75rem 0.875rem 0' }}>
        <p style={{ fontSize: '0.6875rem', fontWeight: 600, textTransform: 'uppercase',
          letterSpacing: '0.05em', color: '#9ca3af', margin: '0 0 0.5rem' }}>
          Notificaciones
        </p>

        {!soportado ? (
          <p style={{ fontSize: '0.8125rem', color: '#9ca3af', margin: 0 }}>
            No soportadas en este navegador.
          </p>
        ) : permisoDenegado ? (
          <p style={{ fontSize: '0.8125rem', color: '#6b7280', margin: 0, lineHeight: 1.4 }}>
            <span className="material-icons" style={{ fontSize: '1rem', verticalAlign: 'middle', marginRight: '0.25rem' }}>lock</span>
            Permiso denegado. Para activarlas, haz clic en el candado (🔒) de la barra de direcciones y permite las notificaciones.
          </p>
        ) : suscrito ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
            <span style={{ fontSize: '0.8125rem', color: '#6366f1', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
              <span className="material-icons" style={{ fontSize: '1rem' }}>notifications_active</span>
              Activas
            </span>
            <button
              onClick={enviarPrueba}
              disabled={cargandoPrueba}
              style={{ background: 'none', border: '1px solid #d1d5db', borderRadius: '6px',
                padding: '0.3rem 0.6rem', fontSize: '0.8125rem', cursor: cargandoPrueba ? 'default' : 'pointer',
                color: cargandoPrueba ? '#9ca3af' : '#374151', textAlign: 'left' }}
            >
              {cargandoPrueba ? 'Enviando…' : 'Enviar notificación de prueba'}
            </button>
            {mensajePrueba && (
              <p style={{ fontSize: '0.75rem', margin: 0,
                color: mensajePrueba.includes('✓') ? '#16a34a' : '#dc2626' }}>
                {mensajePrueba}
              </p>
            )}
          </div>
        ) : (
          <button
            onClick={suscribir}
            disabled={pushCargando}
            style={{ background: 'none', border: '1px solid #d1d5db', borderRadius: '6px',
              padding: '0.3rem 0.6rem', fontSize: '0.8125rem', cursor: pushCargando ? 'default' : 'pointer',
              color: pushCargando ? '#9ca3af' : '#374151' }}
          >
            {pushCargando ? 'Activando…' : 'Activar notificaciones'}
          </button>
        )}
      </div>

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
