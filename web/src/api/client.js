import axios from 'axios'

const BASE_URL = import.meta.env.VITE_BACKEND_URL || 'https://api.myafiappdomain.com'

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
    // /auth/login y /auth/realtime-token no deben disparar logout en 401:
    // el primero porque el 401 es "credenciales incorrectas", no sesión caducada;
    // el segundo porque Realtime es secundario y su fallo no debe expulsar al usuario.
    const isAuthEndpoint =
      error.config?.url?.includes('/auth/login') ||
      error.config?.url?.includes('/auth/realtime-token')
    if (error.response?.status === 401 && !isAuthEndpoint) {
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
