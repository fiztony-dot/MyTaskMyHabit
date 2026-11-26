import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { changePassword } from '../api/auth'

export default function CambiarPassword() {
  const { logout, updateToken } = useAuth()
  const navigate = useNavigate()

  const [actual,      setActual]      = useState('')
  const [nueva,       setNueva]       = useState('')
  const [confirmar,   setConfirmar]   = useState('')
  const [error,       setError]       = useState('')
  const [ok,          setOk]          = useState(false)
  const [submitting,  setSubmitting]  = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')

    if (nueva.length < 10) {
      setError('La contraseña nueva debe tener al menos 10 caracteres')
      return
    }
    if (nueva !== confirmar) {
      setError('Las contraseñas nuevas no coinciden')
      return
    }
    if (nueva === actual) {
      setError('La contraseña nueva debe ser diferente a la actual')
      return
    }

    setSubmitting(true)
    try {
      const res = await changePassword(actual, nueva)
      // El Worker devuelve un nuevo token; actualizarlo para evitar logout inmediato
      if (res?.token && typeof updateToken === 'function') {
        updateToken(res.token)
      }
      setOk(true)
    } catch (err) {
      setError(err.response?.data?.error || 'Error al cambiar la contraseña')
    } finally {
      setSubmitting(false)
    }
  }

  if (ok) {
    return (
      <div style={s.wrapper}>
        <div style={s.card}>
          <h1 style={s.title}>Contraseña cambiada</h1>
          <p style={s.okMsg}>Tu contraseña se ha actualizado correctamente.</p>
          <button onClick={() => navigate(-1)} style={s.button}>Volver</button>
        </div>
      </div>
    )
  }

  return (
    <div style={s.wrapper}>
      <div style={s.card}>
        <h1 style={s.title}>Cambiar contraseña</h1>
        <form onSubmit={handleSubmit}>
          <div style={s.field}>
            <label style={s.label}>Contraseña actual</label>
            <input
              type="password"
              value={actual}
              onChange={(e) => setActual(e.target.value)}
              required
              autoComplete="current-password"
              style={s.input}
            />
          </div>
          <div style={s.field}>
            <label style={s.label}>Contraseña nueva (mín. 10 caracteres)</label>
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
            <label style={s.label}>Confirmar contraseña nueva</label>
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
          <div style={s.actions}>
            <button
              type="button"
              onClick={() => navigate(-1)}
              style={s.cancelButton}
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={submitting}
              style={{ ...s.button, opacity: submitting ? 0.7 : 1 }}
            >
              {submitting ? 'Guardando...' : 'Cambiar contraseña'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

const s = {
  wrapper:      { display: 'flex', justifyContent: 'center', alignItems: 'flex-start', minHeight: '100vh', padding: '2rem 1rem' },
  card:         { width: '100%', maxWidth: '400px', background: '#fff', borderRadius: '12px', padding: '2rem', boxShadow: '0 1px 3px rgba(0,0,0,.1), 0 4px 12px rgba(0,0,0,.05)' },
  title:        { marginTop: 0, marginBottom: '1.5rem', color: '#374151', fontSize: '1.25rem', fontWeight: 700 },
  field:        { marginBottom: '1rem' },
  label:        { display: 'block', marginBottom: '.25rem', fontWeight: 600, fontSize: '.875rem', color: '#374151' },
  input:        { width: '100%', padding: '.6rem .8rem', border: '1px solid #d1d5db', borderRadius: '6px', fontSize: '1rem', outline: 'none', boxSizing: 'border-box' },
  errMsg:       { color: '#dc2626', fontSize: '.875rem', marginBottom: '1rem' },
  okMsg:        { color: '#16a34a', marginBottom: '1.5rem', lineHeight: 1.5 },
  actions:      { display: 'flex', gap: '.75rem', marginTop: '1.25rem' },
  button:       { flex: 1, padding: '.75rem', background: '#6366f1', color: '#fff', border: 'none', borderRadius: '6px', fontSize: '1rem', fontWeight: 600, cursor: 'pointer' },
  cancelButton: { flex: 1, padding: '.75rem', background: '#f3f4f6', color: '#374151', border: 'none', borderRadius: '6px', fontSize: '1rem', fontWeight: 600, cursor: 'pointer' },
}
