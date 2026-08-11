import { Routes, Route, Navigate } from 'react-router-dom'
import Login from './pages/Login'
import Tareas from './pages/Tareas'
import ProtectedRoute from './components/ProtectedRoute'

function App() {
  return (
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
      <Route path="/" element={<Navigate to="/tareas" replace />} />
      <Route path="*" element={<Navigate to="/tareas" replace />} />
    </Routes>
  )
}

export default App
