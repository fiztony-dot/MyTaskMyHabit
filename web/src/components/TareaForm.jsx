import { useState, useEffect } from 'react'
import '../styles/tareas.css'

const FORM_VACIO = {
  titulo: '',
  descripcion: '',
  prioridad: 'MEDIA',
  categoria_id: '',
  fecha_limite: '',
  hora_limite: '',
}

function tareaToForm(tarea) {
  return {
    titulo: tarea.titulo || '',
    descripcion: tarea.descripcion || '',
    prioridad: tarea.prioridad || 'MEDIA',
    categoria_id: tarea.categoria_id != null ? String(tarea.categoria_id) : '',
    fecha_limite: tarea.fecha_limite ? tarea.fecha_limite.substring(0, 10) : '',
    hora_limite: tarea.hora_limite ? tarea.hora_limite.substring(0, 5) : '',
  }
}

function formToBody(form) {
  return {
    titulo: form.titulo.trim(),
    descripcion: form.descripcion.trim() || null,
    prioridad: form.prioridad,
    categoria_id: form.categoria_id ? Number(form.categoria_id) : null,
    fecha_limite: form.fecha_limite || null,
    hora_limite: form.fecha_limite && form.hora_limite ? form.hora_limite : null,
  }
}

export default function TareaForm({ tarea, categorias, onGuardar, onEliminar, onCerrar }) {
  const esEdicion = Boolean(tarea)
  const [form, setForm] = useState(esEdicion ? tareaToForm(tarea) : FORM_VACIO)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  // Si se abre el mismo modal con otra tarea (edición a edición), recargar form
  useEffect(() => {
    setForm(tarea ? tareaToForm(tarea) : FORM_VACIO)
    setError('')
  }, [tarea])

  function set(field) {
    return (e) => setForm((prev) => ({ ...prev, [field]: e.target.value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!form.titulo.trim()) {
      setError('El título es obligatorio.')
      return
    }
    setGuardando(true)
    setError('')
    try {
      await onGuardar(formToBody(form), tarea?.id)
      onCerrar()
    } catch (err) {
      setError(err.response?.data?.error || 'Error al guardar la tarea.')
    } finally {
      setGuardando(false)
    }
  }

  async function handleEliminar() {
    if (!window.confirm(`¿Eliminar "${tarea.titulo}"?`)) return
    setGuardando(true)
    try {
      await onEliminar(tarea.id)
      onCerrar()
    } catch {
      setError('Error al eliminar la tarea.')
      setGuardando(false)
    }
  }

  // Cerrar al pulsar fuera del panel
  function handleOverlayClick(e) {
    if (e.target === e.currentTarget) onCerrar()
  }

  return (
    <div className="m-overlay" onClick={handleOverlayClick}>
      <div className="m-panel" role="dialog" aria-modal="true">
        <div className="m-header">
          <h2 className="m-title">{esEdicion ? 'Editar tarea' : 'Nueva tarea'}</h2>
          <button className="m-close" onClick={onCerrar} aria-label="Cerrar">✕</button>
        </div>

        <form onSubmit={handleSubmit}>
          {/* Título */}
          <div className="m-field">
            <label className="m-label" htmlFor="tf-titulo">Título *</label>
            <input
              id="tf-titulo"
              className="m-input"
              type="text"
              value={form.titulo}
              onChange={set('titulo')}
              required
              autoFocus
              placeholder="¿Qué hay que hacer?"
            />
          </div>

          {/* Descripción */}
          <div className="m-field">
            <label className="m-label" htmlFor="tf-desc">Descripción</label>
            <textarea
              id="tf-desc"
              className="m-textarea"
              value={form.descripcion}
              onChange={set('descripcion')}
              placeholder="Detalles opcionales…"
            />
          </div>

          {/* Prioridad y Categoría */}
          <div className="m-row">
            <div className="m-field">
              <label className="m-label" htmlFor="tf-prio">Prioridad</label>
              <select id="tf-prio" className="m-select" value={form.prioridad} onChange={set('prioridad')}>
                <option value="ALTA">🔴 Alta</option>
                <option value="MEDIA">🟡 Media</option>
                <option value="BAJA">🟢 Baja</option>
              </select>
            </div>
            <div className="m-field">
              <label className="m-label" htmlFor="tf-cat">Categoría</label>
              <select id="tf-cat" className="m-select" value={form.categoria_id} onChange={set('categoria_id')}>
                <option value="">Sin categoría</option>
                {categorias.filter((c) => c.activa !== false).map((c) => (
                  <option key={c.id} value={String(c.id)}>{c.titulo}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Fecha y hora límite */}
          <div className="m-row">
            <div className="m-field">
              <label className="m-label" htmlFor="tf-fecha">Fecha límite</label>
              <input
                id="tf-fecha"
                className="m-input"
                type="date"
                value={form.fecha_limite}
                onChange={set('fecha_limite')}
              />
            </div>
            <div className="m-field">
              <label className="m-label" htmlFor="tf-hora">
                Hora límite{!form.fecha_limite && <span style={{ color: '#d1d5db' }}> (requiere fecha)</span>}
              </label>
              <input
                id="tf-hora"
                className="m-input"
                type="time"
                value={form.hora_limite}
                onChange={set('hora_limite')}
                disabled={!form.fecha_limite}
              />
            </div>
          </div>

          {error && <p style={{ color: '#ef4444', fontSize: '.8125rem', margin: '0 0 .75rem' }}>{error}</p>}

          <div className="m-footer">
            {esEdicion && (
              <button
                type="button"
                className="m-btn-danger"
                onClick={handleEliminar}
                disabled={guardando}
              >
                Eliminar
              </button>
            )}
            <button type="button" className="m-btn-secondary" onClick={onCerrar} disabled={guardando}>
              Cancelar
            </button>
            <button type="submit" className="m-btn-primary" disabled={guardando}>
              {guardando ? 'Guardando…' : esEdicion ? 'Guardar cambios' : 'Crear tarea'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
