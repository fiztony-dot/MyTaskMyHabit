import client from './client'

export async function login(username, password) {
  const { data } = await client.post('/auth/login', { username, password })
  return data
}

export async function getMe() {
  const { data } = await client.get('/auth/me')
  return data
}

export async function changePassword(actual, nueva) {
  const { data } = await client.post('/auth/change-password', { actual, nueva })
  return data
}

export async function forgotPassword(usuario) {
  const { data } = await client.post('/auth/forgot-password', { usuario })
  return data
}

export async function resetPassword(token, nueva) {
  const { data } = await client.post('/auth/reset-password', { token, nueva })
  return data
}
