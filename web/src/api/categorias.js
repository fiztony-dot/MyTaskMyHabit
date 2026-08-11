import client from './client'

export async function getCategorias() {
  const { data } = await client.get('/api/categorias')
  return data.data
}

export async function crearCategoria(body) {
  const { data } = await client.post('/api/categorias', body)
  return data.data
}

export async function editarCategoria(id, body) {
  const { data } = await client.put(`/api/categorias/${id}`, body)
  return data.data
}

export async function eliminarCategoria(id) {
  await client.delete(`/api/categorias/${id}`)
}
