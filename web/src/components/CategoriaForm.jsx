import { useState, useEffect } from 'react'
import '../styles/categorias.css'

const FORM_VACIO = { titulo: '', icono: '', activa: true }

function categoriaToForm(cat) {
  return {
    titulo: cat.titulo || '',
    icono: cat.icono || '',
    activa: cat.activa !== false,
  }
}

export default function CategoriaForm({ categoria, onGuardar, onCerrar }) {
  const esEdicion = Boolean(categoria)
  const [form, setForm] = useState(esEdicion ? categoriaToForm(categoria) : FORM_VACIO)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    setForm(categoria ? categoriaToForm(categoria) : FORM_VACIO)
    setError('')
  }, [categoria])

  function set(field) {
    return (e) => {
      const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value
      setForm((prev) => ({ ...prev, [field]: value }))
    }
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!form.titulo.trim()) {
      setError('El nombre es obligatorio.')
      return
    }
    setGuardando(true)
    setError('')
    try {
      const body = {
        titulo: form.titulo.trim(),
        icono: form.icono.trim() || null,
        ...(esEdicion && { activa: form.activa }),
      }
      await onGuardar(body, categoria?.id)
      onCerrar()
    } catch (err) {
      setError(err.response?.data?.error || 'Error al guardar la categoría.')
    } finally {
      setGuardando(false)
    }
  }

  function handleOverlayClick(e) {
    if (e.target === e.currentTarget) onCerrar()
  }

  return (
    <div className="cf-overlay" onClick={handleOverlayClick}>
      <div className="cf-panel" role="dialog" aria-modal="true">
        <div className="cf-header">
          <h2 className="cf-title">{esEdicion ? 'Editar categoría' : 'Nueva categoría'}</h2>
          <button className="cf-close" onClick={onCerrar} aria-label="Cerrar">✕</button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="cf-field">
            <label className="cf-label" htmlFor="cat-titulo">Nombre *</label>
            <input
              id="cat-titulo"
              className="cf-input"
              type="text"
              value={form.titulo}
              onChange={set('titulo')}
              required
              autoFocus
              placeholder="Nombre de la categoría"
            />
          </div>

          <div className="cf-field">
            <label className="cf-label" htmlFor="cat-icono">Icono</label>
            <input
              id="cat-icono"
              className="cf-input"
              type="text"
              value={form.icono}
              onChange={set('icono')}
              placeholder="p. ej. work, home, list, favorite…"
            />
          </div>

          {esEdicion && (
            <div className="cf-field cf-field-check">
              <label className="cf-label-check">
                <input
                  type="checkbox"
                  checked={form.activa}
                  onChange={set('activa')}
                  className="cf-checkbox"
                />
                Categoría activa
              </label>
              {!form.activa && (
                <p className="cf-hint">
                  Las categorías inactivas no aparecen en el selector al crear tareas.
                </p>
              )}
            </div>
          )}

          {error && <p className="cf-error">{error}</p>}

          <div className="cf-footer">
            <button
              type="button"
              className="cf-btn-secondary"
              onClick={onCerrar}
              disabled={guardando}
            >
              Cancelar
            </button>
            <button type="submit" className="cf-btn-primary" disabled={guardando}>
              {guardando ? 'Guardando…' : esEdicion ? 'Guardar cambios' : 'Crear categoría'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
