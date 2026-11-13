import client from './client'

export async function getRealtimeToken() {
  const { data } = await client.get('/auth/realtime-token')
  return data
}
