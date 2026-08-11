import { useNavigate } from 'react-router-dom'

export default function Tareas() {
  const navigate = useNavigate()
  const username = localStorage.getItem('username') || 'usuario'

  function handleLogout() {
    localStorage.removeItem('jwt_token')
    localStorage.removeItem('username')
    navigate('/login', { replace: true })
  }

  return (
    <div style={styles.wrapper}>
      <header style={styles.header}>
        <h1 style={styles.title}>Tareas</h1>
        <button onClick={handleLogout} style={styles.logoutBtn}>
          Cerrar sesión
        </button>
      </header>
      <main style={styles.main}>
        <p style={styles.placeholder}>Módulo Tareas — próximamente</p>
        <p style={styles.subtext}>
          Sesión iniciada como <strong>{username}</strong>
        </p>
      </main>
    </div>
  )
}

const styles = {
  wrapper: {
    maxWidth: '640px',
    margin: '0 auto',
    padding: '1.5rem 1rem',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '2rem',
  },
  title: {
    margin: 0,
    color: '#6366f1',
    fontSize: '1.5rem',
  },
  logoutBtn: {
    padding: '.45rem .9rem',
    background: 'transparent',
    color: '#6b7280',
    border: '1px solid #d1d5db',
    borderRadius: '6px',
    cursor: 'pointer',
    fontSize: '.875rem',
  },
  main: {
    textAlign: 'center',
    marginTop: '5rem',
  },
  placeholder: {
    color: '#6b7280',
    fontSize: '1.1rem',
    margin: '0 0 .5rem',
  },
  subtext: {
    color: '#9ca3af',
    fontSize: '.875rem',
  },
}
