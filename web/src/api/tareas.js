import client from './client'

export async function getTareas() {
  const { data } = await client.get('/api/tareas')
  return data.data
}

export async function crearTarea(body) {
  const { data } = await client.post('/api/tareas', body)
  return data.data
}

export async function editarTarea(id, body) {
  const { data } = await client.put(`/api/tareas/${id}`, body)
  return data.data
}

export async function completarTarea(id, estaCompletada) {
  const { data } = await client.patch(`/api/tareas/${id}/completar`, {
    esta_completada: estaCompletada,
  })
  return data.data
}

export async function eliminarTarea(id) {
  const { data } = await client.delete(`/api/tareas/${id}`)
  return data.data
}
