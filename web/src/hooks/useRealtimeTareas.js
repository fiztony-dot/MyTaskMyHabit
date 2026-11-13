import { useEffect, useRef, useState } from 'react'
import { supabase } from '../lib/supabase'
import { getRealtimeToken } from '../api/realtimeToken'

/**
 * Suscribe a cambios en tareas_table via Supabase Realtime.
 * Obtiene un JWT de corta duración (1h) del Worker en /auth/realtime-token,
 * lo inyecta en el cliente de Supabase y programa la renovación automática
 * 5 minutos antes de que caduque.
 */
export function useRealtimeTareas({ onInsert, onUpdate, onDelete }) {
  const [conectado, setConectado] = useState(false)

  const cb = useRef({ onInsert, onUpdate, onDelete })
  cb.current = { onInsert, onUpdate, onDelete }

  useEffect(() => {
    if (!supabase) {
      console.warn('[realtime] supabase=null — faltan VITE_SUPABASE_URL / VITE_SUPABASE_ANON_KEY')
      return
    }

    let channel = null
    let renewTimer = null
    let active = true

    function scheduleRenew(expiresAt) {
      const now = Math.floor(Date.now() / 1000)
      // Renovar 5 minutos antes de la expiración, mínimo 30 s
      const ms = Math.max((expiresAt - now - 300) * 1000, 30_000)
      renewTimer = setTimeout(async () => {
        if (!active) return
        try {
          const { token, expiresAt: newExp } = await getRealtimeToken()
          supabase.realtime.setAuth(token)
          scheduleRenew(newExp)
        } catch (err) {
          console.error('[realtime] Error renovando token, reintentando en 60 s:', err)
          if (active) renewTimer = setTimeout(() => scheduleRenew(expiresAt), 60_000)
        }
      }, ms)
    }

    getRealtimeToken()
      .then(({ token, expiresAt }) => {
        if (!active) return

        // Autenticar el cliente de Realtime con el token del Worker
        supabase.realtime.setAuth(token)

        channel = supabase
          .channel('tareas-realtime')
          .on(
            'postgres_changes',
            { event: '*', schema: 'public', table: 'tareas_table' },
            (payload) => {
              const { eventType, new: nueva, old } = payload
              if (eventType === 'INSERT')      cb.current.onInsert?.(nueva)
              else if (eventType === 'UPDATE') cb.current.onUpdate?.(nueva)
              else if (eventType === 'DELETE') cb.current.onDelete?.(old)
            }
          )
          .subscribe((status, err) => {
            if (err) console.error('[realtime] Error:', err)
            if (active) setConectado(status === 'SUBSCRIBED')
          })

        scheduleRenew(expiresAt)
      })
      .catch((err) => {
        console.error('[realtime] No se pudo obtener el token de Realtime:', err)
      })

    return () => {
      active = false
      clearTimeout(renewTimer)
      if (channel) supabase.removeChannel(channel)
      setConectado(false)
    }
  }, [])

  return { conectado }
}
