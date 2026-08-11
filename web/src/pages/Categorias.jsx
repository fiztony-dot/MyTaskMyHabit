import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCategorias } from '../hooks/useCategorias'
import { useTareas } from '../hooks/useTareas'
import CategoriaItem from '../components/CategoriaItem'
import CategoriaForm from '../components/CategoriaForm'
import Spinner from '../components/Spinner'
import '../styles/categorias.css'

export default function Categorias() {
  const navigate = useNavigate()
  const { categorias, isLoading, error, cargar, crear, editar, eliminar } = useCategorias()
  const { tareas } = useTareas()

  const [formCat, setFormCat] = useState(null) // null=cerrado, false=nueva, objeto=editar

  // Mapa categoriaId → nº de tareas asociadas
  const tareasXCat = tareas.reduce((acc, t) => {
    if (t.categoria_id != null) {
      acc[t.categoria_id] = (acc[t.categoria_id] || 0) + 1
    }
    return acc
  }, {})

  async function handleGuardar(body, id) {
    if (id) {
      await editar(id, body)
    } else {
      await crear(body)
    }
  }

  if (isLoading) return <Spinner />

  return (
    <div className="c-page">
      <header className="c-header">
        <button
          className="c-btn-back"
          onClick={() => navigate('/tareas')}
          aria-label="Volver a Tareas"
        >
          ←
        </button>
        <h1 className="c-header-title">Categorías</h1>
        <button className="c-btn-nueva" onClick={() => setFormCat(false)}>
          + Nueva
        </button>
      </header>

      {error && (
        <div className="c-error">
          <p>{error}</p>
          <button onClick={cargar}>Reintentar</button>
        </div>
      )}

      {!error && categorias.length === 0 && (
        <div className="c-vacio">
          <p className="c-vacio-titulo">No hay categorías</p>
          <p>Pulsa "+ Nueva" para crear la primera.</p>
        </div>
      )}

      <div className="c-list">
        {categorias.map((cat) => (
          <CategoriaItem
            key={cat.id}
            categoria={cat}
            tareasCount={tareasXCat[cat.id] || 0}
            onEdit={setFormCat}
            onDelete={eliminar}
          />
        ))}
      </div>

      {formCat !== null && (
        <CategoriaForm
          categoria={formCat || null}
          onGuardar={handleGuardar}
          onCerrar={() => setFormCat(null)}
        />
      )}
    </div>
  )
}
