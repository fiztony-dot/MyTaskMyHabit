import '../../styles/layout.css'

export default function AppLayout({ sidebarOpen, onCloseSidebar, sidebar, children }) {
  return (
    <div className="app-layout">
      {/* Sidebar */}
      <div className={`app-sidebar${sidebarOpen ? ' sidebar-open' : ''}`}>
        {sidebar}
      </div>

      {/* Overlay móvil */}
      <div
        className={`sidebar-overlay${sidebarOpen ? ' sidebar-open' : ''}`}
        onClick={onCloseSidebar}
        aria-hidden="true"
      />

      {/* Panel de contenido */}
      <div className="app-panel">
        {children}
      </div>
    </div>
  )
}
