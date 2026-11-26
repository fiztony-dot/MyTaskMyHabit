import { useState } from 'react'
import { Link } from 'react-router-dom'
import { forgotPassword } from '../api/auth'

export default function ForgotPassword() {
  const [usuario, setUsuario]   = useState('')
  const [enviado, setEnviado]   = useState(false)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setSubmitting(true)
    try {
      await forgotPassword(usuario.trim())
    } catch {
      // silencioso: la respuesta siempre es idéntica
    } finally {
      setEnviado(true)
      setSubmitting(false)
    }
  }

  return (
    <div style={s.wrapper}>
      <div style={s.card}>
        <h1 style={s.title}>MyTaskMyHabit</h1>
        <h2 style={s.subtitle}>Recuperar contraseña</h2>

        {enviado ? (
          <p style={s.ok}>
            Si el usuario existe, recibirás un correo con instrucciones.
          </p>
        ) : (
          <form onSubmit={handleSubmit}>
            <div style={s.field}>
              <label style={s.label}>Usuario</label>
              <input
                type="text"
                value={usuario}
                onChange={(e) => setUsuario(e.target.value)}
                required
                autoComplete="username"
                style={s.input}
              />
            </div>
            <button
              type="submit"
              disabled={submitting}
              style={{ ...s.button, opacity: submitting ? 0.7 : 1 }}
            >
              {submitting ? 'Enviando...' : 'Enviar instrucciones'}
            </button>
          </form>
        )}

        <p style={s.back}>
          <Link to="/login" style={s.link}>← Volver al inicio de sesión</Link>
        </p>
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
  ok:       { color: '#16a34a', textAlign: 'center', lineHeight: 1.5 },
  back:     { textAlign: 'center', marginTop: '1.25rem', marginBottom: 0, fontSize: '.875rem' },
  link:     { color: '#6366f1', textDecoration: 'none' },
}
