import { Navigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import Spinner from './Spinner'

function ProtectedRoute({ children }) {
  const { isLoading, isAuthenticated } = useAuth()
  if (isLoading) return <Spinner />
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return children
}

export default ProtectedRoute
