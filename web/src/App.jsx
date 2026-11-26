import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import Login from './pages/Login'
import Tareas from './pages/Tareas'
import Categorias from './pages/Categorias'
import ForgotPassword from './pages/ForgotPassword'
import ResetPassword from './pages/ResetPassword'
import CambiarPassword from './pages/CambiarPassword'
import ProtectedRoute from './components/ProtectedRoute'

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route
          path="/tareas"
          element={
            <ProtectedRoute>
              <Tareas />
            </ProtectedRoute>
          }
        />
        <Route
          path="/categorias"
          element={
            <ProtectedRoute>
              <Categorias />
            </ProtectedRoute>
          }
        />
        <Route
          path="/cambiar-password"
          element={
            <ProtectedRoute>
              <CambiarPassword />
            </ProtectedRoute>
          }
        />
        <Route path="/" element={<Navigate to="/tareas" replace />} />
        <Route path="*" element={<Navigate to="/tareas" replace />} />
      </Routes>
    </AuthProvider>
  )
}

export default App
