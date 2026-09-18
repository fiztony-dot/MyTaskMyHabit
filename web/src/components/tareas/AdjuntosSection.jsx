import {
  useState, useEffect, useRef, useCallback, forwardRef, useImperativeHandle,
} from 'react'
import {
  getAdjuntos, subirAdjunto, eliminarAdjunto, validarFichero, ADJUNTO_ACCEPT,
} from '../../api/adjuntos'

// Icono Material según el tipo MIME (para documentos; las imágenes muestran thumbnail).
function iconoPorTipo(mime) {
  if (mime === 'application/pdf') return 'picture_as_pdf'
  if (mime === 'application/msword' ||
      mime === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document') return 'description'
  if (mime === 'application/vnd.ms-excel' ||
      mime === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet') return 'table_chart'
  if (mime === 'text/plain') return 'article'
  return 'insert_drive_file'
}

function esImagen(mime) {
  return typeof mime === 'string' && mime.startsWith('image/')
}

function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

/**
 * Sección de adjuntos del formulario de tarea.
 *
 * - Modo EDICIÓN (tareaId presente): sube/elimina contra la API inmediatamente.
 * - Modo CREACIÓN (tareaId == null): acumula ficheros en memoria (pendientes)
 *   y los expone al padre vía ref.subirPendientes(nuevoId), que el padre invoca
 *   tras crear la tarea. También expone ref.tienePendientes().
 *
 * Notifica el nº de adjuntos al padre vía onCountChange(nuevoTotal).
 */
const AdjuntosSection = forwardRef(function AdjuntosSection({ tareaId, onCountChange }, ref) {
  const esCreacion = tareaId == null

  // Modo edición: adjuntos ya subidos (DTOs del servidor).
  const [adjuntos, setAdjuntos] = useState([])
  // Modo creación: ficheros pendientes { file, previewUrl }.
  const [pendientes, setPendientes] = useState([])

  const [cargando, setCargando] = useState(!esCreacion)
  const [subiendo, setSubiendo] = useState(false)
  const [progreso, setProgreso] = useState(0)
  const [error, setError] = useState('')
  const [borrandoId, setBorrandoId] = useState(null)
  const inputRef = useRef(null)

  const notificar = useCallback((n) => {
    if (onCountChange) onCountChange(n)
  }, [onCountChange])

  // Carga inicial (solo edición)
  useEffect(() => {
    if (esCreacion) { setCargando(false); return }
    let vivo = true
    setCargando(true)
    getAdjuntos(tareaId)
      .then((lista) => { if (vivo) { setAdjuntos(lista); notificar(lista.length) } })
      .catch(() => { if (vivo) setError('No se pudieron cargar los adjuntos.') })
      .finally(() => { if (vivo) setCargando(false) })
    return () => { vivo = false }
  }, [tareaId, esCreacion, notificar])

  // Limpieza de object URLs de las previews pendientes al desmontar
  useEffect(() => () => {
    pendientes.forEach((p) => URL.revokeObjectURL(p.previewUrl))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Expone acciones al padre para el flujo de creación
  useImperativeHandle(ref, () => ({
    tienePendientes: () => pendientes.length > 0,
    // Sube todos los pendientes a la tarea recién creada. Lanza si alguno falla.
    subirPendientes: async (nuevoTareaId) => {
      for (const p of pendientes) {
        await subirAdjunto(nuevoTareaId, p.file)
      }
    },
  }), [pendientes])

  async function handleSeleccion(e) {
    const file = e.target.files?.[0]
    e.target.value = '' // permite re-seleccionar el mismo fichero
    if (!file) return

    const errValidacion = validarFichero(file)
    if (errValidacion) { setError(errValidacion); return }
    setError('')

    if (esCreacion) {
      // Acumular en memoria; la subida ocurre al crear la tarea.
      const previewUrl = URL.createObjectURL(file)
      setPendientes((prev) => {
        const lista = [...prev, { file, previewUrl }]
        notificar(lista.length)
        return lista
      })
      return
    }

    // Modo edición: subir ya.
    setSubiendo(true)
    setProgreso(0)
    try {
      const nuevo = await subirAdjunto(tareaId, file, setProgreso)
      setAdjuntos((prev) => {
        const lista = [...prev, nuevo]
        notificar(lista.length)
        return lista
      })
    } catch (err) {
      setError(err.response?.data?.error || 'Error al subir el fichero.')
    } finally {
      setSubiendo(false)
      setProgreso(0)
    }
  }

  async function handleEliminar(adj) {
    if (!window.confirm(`¿Eliminar "${adj.nombre_fichero}"?`)) return
    setBorrandoId(adj.id)
    setError('')
    try {
      await eliminarAdjunto(tareaId, adj.id)
      setAdjuntos((prev) => {
        const lista = prev.filter((a) => a.id !== adj.id)
        notificar(lista.length)
        return lista
      })
    } catch (err) {
      setError(err.response?.data?.error || 'Error al eliminar el adjunto.')
    } finally {
      setBorrandoId(null)
    }
  }

  function handleQuitarPendiente(idx) {
    setPendientes((prev) => {
      const p = prev[idx]
      if (p) URL.revokeObjectURL(p.previewUrl)
      const lista = prev.filter((_, i) => i !== idx)
      notificar(lista.length)
      return lista
    })
  }

  const listaVacia = esCreacion ? pendientes.length === 0 : adjuntos.length === 0

  return (
    <div className="m-field m-adjuntos">
      <label className="m-label">Adjuntos</label>

      {cargando ? (
        <p className="m-adj-empty">Cargando adjuntos…</p>
      ) : listaVacia ? (
        <p className="m-adj-empty">
          {esCreacion ? 'Añade ficheros; se subirán al crear la tarea.' : 'Sin adjuntos todavía.'}
        </p>
      ) : esCreacion ? (
        <ul className="m-adj-list">
          {pendientes.map((p, idx) => (
            <li key={idx} className="m-adj-item">
              {esImagen(p.file.type) ? (
                <span className="m-adj-thumb-link">
                  <img src={p.previewUrl} alt={p.file.name} className="m-adj-thumb" />
                </span>
              ) : (
                <span className="m-adj-icon-link">
                  <span className="material-icons m-adj-icon">{iconoPorTipo(p.file.type)}</span>
                </span>
              )}
              <div className="m-adj-info">
                <span className="m-adj-nombre" title={p.file.name}>{p.file.name}</span>
                <span className="m-adj-size">{formatBytes(p.file.size)} · pendiente</span>
              </div>
              <button
                type="button"
                className="m-adj-del"
                onClick={() => handleQuitarPendiente(idx)}
                aria-label={`Quitar ${p.file.name}`}
                title="Quitar"
              >
                <span className="material-icons">delete</span>
              </button>
            </li>
          ))}
        </ul>
      ) : (
        <ul className="m-adj-list">
          {adjuntos.map((adj) => (
            <li key={adj.id} className="m-adj-item">
              {esImagen(adj.tipo_mime) ? (
                <a href={adj.url_firmada} target="_blank" rel="noreferrer" className="m-adj-thumb-link">
                  <img src={adj.url_firmada} alt={adj.nombre_fichero} className="m-adj-thumb" />
                </a>
              ) : (
                <a href={adj.url_firmada} target="_blank" rel="noreferrer" className="m-adj-icon-link">
                  <span className="material-icons m-adj-icon">{iconoPorTipo(adj.tipo_mime)}</span>
                </a>
              )}
              <div className="m-adj-info">
                <a href={adj.url_firmada} target="_blank" rel="noreferrer" className="m-adj-nombre" title={adj.nombre_fichero}>
                  {adj.nombre_fichero}
                </a>
                <span className="m-adj-size">{formatBytes(adj.tamano_bytes)}</span>
              </div>
              <button
                type="button"
                className="m-adj-del"
                onClick={() => handleEliminar(adj)}
                disabled={borrandoId === adj.id}
                aria-label={`Eliminar ${adj.nombre_fichero}`}
                title="Eliminar adjunto"
              >
                <span className="material-icons">{borrandoId === adj.id ? 'hourglass_empty' : 'delete'}</span>
              </button>
            </li>
          ))}
        </ul>
      )}

      {subiendo && (
        <div className="m-adj-progress">
          <div className="m-adj-progress-bar" style={{ width: `${progreso}%` }} />
          <span className="m-adj-progress-txt">Subiendo… {progreso}%</span>
        </div>
      )}

      {error && <p className="m-adj-error">{error}</p>}

      <input
        ref={inputRef}
        type="file"
        accept={ADJUNTO_ACCEPT}
        style={{ display: 'none' }}
        onChange={handleSeleccion}
      />
      <button
        type="button"
        className="m-adj-add"
        onClick={() => inputRef.current?.click()}
        disabled={subiendo}
      >
        <span className="material-icons">attach_file</span>
        {subiendo ? 'Subiendo…' : 'Añadir adjunto'}
      </button>
    </div>
  )
})

export default AdjuntosSection
