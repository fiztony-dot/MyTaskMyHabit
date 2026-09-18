import client from './client'

/**
 * API de adjuntos de tareas (Fase 1).
 * El backend (Cloudflare Worker) sube a Supabase Storage (bucket privado)
 * y devuelve URLs firmadas de 1h. DTO: { id, tarea_id, nombre_fichero,
 * tipo_mime, tamano_bytes, creado_en, url_firmada }.
 */

export const ADJUNTO_MAX_BYTES = 10 * 1024 * 1024 // 10 MB

// Extensiones aceptadas para el atributo `accept` del input de fichero.
export const ADJUNTO_ACCEPT =
  'image/jpeg,image/png,image/gif,image/webp,application/pdf,' +
  'application/msword,' +
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document,' +
  'application/vnd.ms-excel,' +
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,' +
  'text/plain'

const MIME_PERMITIDOS = new Set([
  'image/jpeg', 'image/png', 'image/gif', 'image/webp',
  'application/pdf', 'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'text/plain',
])

const EXT_PERMITIDAS = new Set(['jpg', 'jpeg', 'png', 'gif', 'webp', 'pdf', 'doc', 'docx', 'xls', 'xlsx', 'txt'])

/** Valida tamaño y tipo en cliente antes de subir. Devuelve mensaje de error o null. */
export function validarFichero(file) {
  if (!file) return 'No se seleccionó ningún fichero.'
  if (file.size > ADJUNTO_MAX_BYTES) return 'El fichero supera el límite de 10MB.'
  const ext = (file.name.split('.').pop() || '').toLowerCase()
  if (!MIME_PERMITIDOS.has(file.type) && !EXT_PERMITIDAS.has(ext)) {
    return 'Tipo de fichero no permitido. Acepta imágenes (jpg, png, gif, webp) y documentos (pdf, doc, docx, xls, xlsx, txt).'
  }
  return null
}

export async function getAdjuntos(tareaId) {
  const { data } = await client.get(`/api/tareas/${tareaId}/adjuntos`)
  return data.data
}

/**
 * Sube un fichero a una tarea.
 * @param {number} tareaId
 * @param {File} file
 * @param {(pct:number)=>void} [onProgress] progreso 0-100
 */
export async function subirAdjunto(tareaId, file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await client.post(`/api/tareas/${tareaId}/adjuntos`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e) => {
      if (onProgress && e.total) onProgress(Math.round((e.loaded * 100) / e.total))
    },
  })
  return data.data
}

export async function eliminarAdjunto(tareaId, adjuntoId) {
  const { data } = await client.delete(`/api/tareas/${tareaId}/adjuntos/${adjuntoId}`)
  return data.data
}
