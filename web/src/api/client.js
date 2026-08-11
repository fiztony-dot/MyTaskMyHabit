import axios from 'axios'

const BASE_URL = 'https://mytaskmyhabit-api.onrender.com'

const client = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error) => {
    // Solo redirigir al login cuando falla auth en rutas protegidas,
    // no cuando el propio endpoint de login devuelve 401 (credenciales incorrectas)
    const isLoginEndpoint = error.config?.url?.includes('/auth/login')
    if (error.response?.status === 401 && !isLoginEndpoint) {
      localStorage.removeItem('jwt_token')
      localStorage.removeItem('username')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default client
