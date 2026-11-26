import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { resetPassword } from '../api/auth'

export default function ResetPassword() {
  const [token,       setToken]       = useState('')
  const [nueva,       setNueva]       = useState('')
  const [confirmar,   setConfirmar]   = useState('')
  const [error,       setError]       = useState('')
  const [ok,          setOk]          = useState(false)
  const [submitting,  setSubmitting]  = useState(false)

  // Leer el token del fragmento de URL (#token=…)
  useEffect(() => {
    const hash = window.location.hash
    const match = hash.match(/[#&]token=([^&]+)/)
    if (match) setToken(decodeURIComponent(match[1]))
  }, [])

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')

    if (nueva.length < 10) {
      setError('La contraseña debe tener al menos 10 caracteres')
      return
    }
    if (nueva !== confirmar) {
      setError('Las contraseñas no coinciden')
      return
    }
    if (!token) {
      setError('Enlace no válido o caducado')
      return
    }

    setSubmitting(true)
    try {
      await resetPassword(token, nueva)
      setOk(true)
    } catch (err) {
      setError(err.response?.data?.error || 'Enlace no válido o caducado')
    } finally {
      setSubmitting(false)
    }
  }

  if (!token && !ok) {
    return (
      <div style={s.wrapper}>
        <div style={s.card}>
          <h1 style={s.title}>MyTaskMyHabit</h1>
          <p style={s.errMsg}>Enlace no válido o caducado.</p>
          <p style={s.back}><Link to="/login" style={s.link}>← Iniciar sesión</Link></p>
        </div>
      </div>
    )
  }

  return (
    <div style={s.wrapper}>
      <div style={s.card}>
        <h1 style={s.title}>MyTaskMyHabit</h1>
        <h2 style={s.subtitle}>Nueva contraseña</h2>

        {ok ? (
          <>
            <p style={s.okMsg}>Contraseña cambiada correctamente.</p>
            <p style={s.back}><Link to="/login" style={s.link}>Iniciar sesión</Link></p>
          </>
        ) : (
          <form onSubmit={handleSubmit}>
            <div style={s.field}>
              <label style={s.label}>Nueva contraseña (mín. 10 caracteres)</label>
              <input
                type="password"
                value={nueva}
                onChange={(e) => setNueva(e.target.value)}
                required
                autoComplete="new-password"
                style={s.input}
              />
            </div>
            <div style={s.field}>
              <label style={s.label}>Confirmar contraseña</label>
              <input
                type="password"
                value={confirmar}
                onChange={(e) => setConfirmar(e.target.value)}
                required
                autoComplete="new-password"
                style={s.input}
              />
            </div>
            {error && <p style={s.errMsg}>{error}</p>}
            <button
              type="submit"
              disabled={submitting}
              style={{ ...s.button, opacity: submitting ? 0.7 : 1 }}
            >
              {submitting ? 'Guardando...' : 'Cambiar contraseña'}
            </button>
          </form>
        )}
      </div>
    </div>
  )
}

const s = {
  wrapper:  { display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', padding: '1rem' },
  card:     { width: '100%', maxWidth: '360px', background: '#fff', borderRadius: '12px', padding: '2rem', boxShadow: '0 1px 3px rgba(0,0,0,.1), 0 4px 12px rgba(0,0,0,.05)' },
  title:    { textAlign: 'center', marginTop: 0, marginBottom: '.5rem', color: '#6366f1', fontSize: '1.5rem' },
  subtitle: { textAlign: 'center', marginTop: 0, marginBottom: '1.5rem', fontSize: '1rem', fontWeight: 600, color: '#374151' },
  field:    { marginBottom: '1rem' },
  label:    { display: 'block', marginBottom: '.25rem', fontWeight: 600, fontSize: '.875rem', color: '#374151' },
  input:    { width: '100%', padding: '.6rem .8rem', border: '1px solid #d1d5db', borderRadius: '6px', fontSize: '1rem', outline: 'none', boxSizing: 'border-box' },
  button:   { width: '100%', padding: '.75rem', background: '#6366f1', color: '#fff', border: 'none', borderRadius: '6px', fontSize: '1rem', fontWeight: 600, cursor: 'pointer', marginTop: '.5rem' },
  okMsg:    { color: '#16a34a', textAlign: 'center', lineHeight: 1.5 },
  errMsg:   { color: '#dc2626', fontSize: '.875rem', textAlign: 'center', marginBottom: '1rem' },
  back:     { textAlign: 'center', marginTop: '1.25rem', marginBottom: 0, fontSize: '.875rem' },
  link:     { color: '#6366f1', textDecoration: 'none' },
}
