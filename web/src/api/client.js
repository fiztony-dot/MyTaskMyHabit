import axios from 'axios'

const BASE_URL = import.meta.env.VITE_BACKEND_URL || 'https://mytaskmyhabit-worker.fiztony.workers.dev'

// AuthContext registra aquí su función logout para que el interceptor la llame en 401
let onUnauthorized = null
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

const client = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('mtmh_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error) => {
    // error.response es undefined en errores de red/timeout — no interpretar como 401
    const isLoginEndpoint = error.config?.url?.includes('/auth/login')
    if (error.response?.status === 401 && !isLoginEndpoint) {
      if (onUnauthorized) {
        onUnauthorized()
      } else {
        // Fallback si el contexto aún no está montado
        localStorage.removeItem('mtmh_token')
        localStorage.removeItem('mtmh_user')
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default client
