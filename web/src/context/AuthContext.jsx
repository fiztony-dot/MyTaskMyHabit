import { createContext, useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { login as apiLogin, getMe } from '../api/auth'
import { setUnauthorizedHandler } from '../api/client'

const TOKEN_KEY = 'mtmh_token'
const USER_KEY = 'mtmh_user'

export const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const navigate = useNavigate()
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY))
  const [isLoading, setIsLoading] = useState(true)

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    setToken(null)
    setUser(null)
    navigate('/login', { replace: true })
  }, [navigate])

  // Registrar logout en el cliente HTTP para que lo llame en 401 automáticamente
  useEffect(() => {
    setUnauthorizedHandler(logout)
    return () => setUnauthorizedHandler(null)
  }, [logout])

  // Al montar: verificar token existente con GET /auth/me
  useEffect(() => {
    const storedToken = localStorage.getItem(TOKEN_KEY)
    if (!storedToken) {
      setIsLoading(false)
      return
    }

    getMe()
      .then(({ user: serverUser }) => {
        const u = { id: serverUser.id, username: serverUser.username }
        setUser(u)
        setToken(storedToken)
        localStorage.setItem(USER_KEY, JSON.stringify(u))
      })
      .catch((err) => {
        const isAuthError = err.response?.status === 401 || err.response?.status === 403
        if (isAuthError) {
          // Token expirado o inválido — limpiar sesión
          localStorage.removeItem(TOKEN_KEY)
          localStorage.removeItem(USER_KEY)
          setToken(null)
          setUser(null)
        } else {
          // Error de red / servidor dormido — mantener sesión desde caché
          try {
            const cached = localStorage.getItem(USER_KEY)
            if (cached) setUser(JSON.parse(cached))
          } catch {
            // caché corrupta: sesión limitada sin datos de usuario
          }
        }
      })
      .finally(() => setIsLoading(false))
  }, []) // solo al montar

  const login = useCallback(
    async (username, password) => {
      const data = await apiLogin(username, password)
      const newToken = data.token
      const basicUser = { username: data.user.username }

      localStorage.setItem(TOKEN_KEY, newToken)
      localStorage.setItem(USER_KEY, JSON.stringify(basicUser))
      setToken(newToken)
      setUser(basicUser)

      // Enriquecer con id del usuario en segundo plano
      try {
        const { user: serverUser } = await getMe()
        const fullUser = { id: serverUser.id, username: serverUser.username }
        setUser(fullUser)
        localStorage.setItem(USER_KEY, JSON.stringify(fullUser))
      } catch {
        // Si /auth/me falla, continuamos con el usuario básico
      }

      navigate('/tareas', { replace: true })
    },
    [navigate]
  )

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isLoading,
        isAuthenticated: user !== null,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}
