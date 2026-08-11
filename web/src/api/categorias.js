import client from './client'

export async function getCategorias() {
  const { data } = await client.get('/api/categorias')
  return data.data
}
