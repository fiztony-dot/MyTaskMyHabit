import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import Spinner from '../components/Spinner'

export default function Login() {
  const { login, isLoading, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  // Si ya hay sesión activa, ir directo a /tareas
  useEffect(() => {
    if (!isLoading && isAuthenticated) {
      navigate('/tareas', { replace: true })
    }
  }, [isLoading, isAuthenticated, navigate])

  // Mostrar spinner mientras se verifica el token al arrancar
  if (isLoading) return <Spinner />
  // Evitar flash del formulario si ya está autenticado
  if (isAuthenticated) return null

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login(username, password)
      // La navegación la gestiona AuthContext.login()
    } catch (err) {
      setError(err.response?.data?.error || 'Error al iniciar sesión')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div style={styles.wrapper}>
      <div style={styles.card}>
        <h1 style={styles.title}>MyTaskMyHabit</h1>
        <form onSubmit={handleSubmit}>
          <div style={styles.field}>
            <label style={styles.label}>Usuario</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              autoComplete="username"
              style={styles.input}
            />
          </div>
          <div style={styles.field}>
            <label style={styles.label}>Contraseña</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
              style={styles.input}
            />
          </div>
          {error && <p style={styles.error}>{error}</p>}
          <button
            type="submit"
            disabled={submitting}
            style={{ ...styles.button, opacity: submitting ? 0.7 : 1 }}
          >
            {submitting ? 'Iniciando sesión...' : 'Iniciar sesión'}
          </button>
        </form>
      </div>
    </div>
  )
}

const styles = {
  wrapper: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    minHeight: '100vh',
    padding: '1rem',
  },
  card: {
    width: '100%',
    maxWidth: '360px',
    background: '#fff',
    borderRadius: '12px',
    padding: '2rem',
    boxShadow: '0 1px 3px rgba(0,0,0,.1), 0 4px 12px rgba(0,0,0,.05)',
  },
  title: {
    textAlign: 'center',
    marginTop: 0,
    marginBottom: '2rem',
    color: '#6366f1',
    fontSize: '1.5rem',
  },
  field: {
    marginBottom: '1rem',
  },
  label: {
    display: 'block',
    marginBottom: '.25rem',
    fontWeight: 600,
    fontSize: '.875rem',
    color: '#374151',
  },
  input: {
    width: '100%',
    padding: '.6rem .8rem',
    border: '1px solid #d1d5db',
    borderRadius: '6px',
    fontSize: '1rem',
    outline: 'none',
  },
  error: {
    color: '#dc2626',
    fontSize: '.875rem',
    textAlign: 'center',
    marginBottom: '1rem',
  },
  button: {
    width: '100%',
    padding: '.75rem',
    background: '#6366f1',
    color: '#fff',
    border: 'none',
    borderRadius: '6px',
    fontSize: '1rem',
    fontWeight: 600,
    cursor: 'pointer',
    marginTop: '.5rem',
  },
}
