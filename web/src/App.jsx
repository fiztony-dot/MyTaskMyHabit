import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import Login from './pages/Login'
import Tareas from './pages/Tareas'
import Categorias from './pages/Categorias'
import ProtectedRoute from './components/ProtectedRoute'

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<Login />} />
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
        <Route path="/" element={<Navigate to="/tareas" replace />} />
        <Route path="*" element={<Navigate to="/tareas" replace />} />
      </Routes>
    </AuthProvider>
  )
}

export default App
