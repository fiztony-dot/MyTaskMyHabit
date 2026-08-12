import { useEffect, useRef, useState } from 'react'
import { supabase } from '../lib/supabase'

/**
 * Suscribe a cambios en tareas_table via Supabase Realtime.
 * Llama a onInsert/onUpdate/onDelete con el payload correspondiente.
 * Usa useRef para los callbacks y evitar re-suscripciones al re-render.
 */
export function useRealtimeTareas({ onInsert, onUpdate, onDelete }) {
  const [conectado, setConectado] = useState(false)

  // Ref para tener siempre la versión más reciente de los callbacks
  // sin necesitar incluirlos como dependencia del effect
  const cb = useRef({ onInsert, onUpdate, onDelete })
  cb.current = { onInsert, onUpdate, onDelete }

  useEffect(() => {
    if (!supabase) {
      console.warn('[realtime] supabase=null — faltan VITE_SUPABASE_URL / VITE_SUPABASE_ANON_KEY')
      return
    }

    console.log('[realtime] Abriendo canal tareas-realtime...')

    const channel = supabase
      .channel('tareas-realtime')
      .on(
        'postgres_changes',
        { event: '*', schema: 'public', table: 'tareas_table' },
        (payload) => {
          console.log('[realtime] Evento recibido:', payload.eventType, payload)
          const { eventType, new: nueva, old } = payload
          if (eventType === 'INSERT')      cb.current.onInsert?.(nueva)
          else if (eventType === 'UPDATE') cb.current.onUpdate?.(nueva)
          else if (eventType === 'DELETE') cb.current.onDelete?.(old)
        }
      )
      .subscribe((status, err) => {
        console.log('[realtime] Estado canal:', status, err ? `| error: ${err.message}` : '')
        if (err) console.error('[realtime] Error de suscripción:', err)
        setConectado(status === 'SUBSCRIBED')
      })

    return () => {
      console.log('[realtime] Cerrando canal')
      supabase.removeChannel(channel)
      setConectado(false)
    }
  }, []) // Solo al montar/desmontar — los callbacks se leen via ref

  return { conectado }
}
