import client from './client'

export async function login(username, password) {
  const { data } = await client.post('/auth/login', { username, password })
  // Server response: { token, user: { username } }
  return data
}

export async function getMe() {
  const { data } = await client.get('/auth/me')
  // Server response: { user: { id, username, nombreVisible } }
  return data
}
