import { useState, useEffect, useCallback } from 'react'
import { subscribePush, unsubscribePush } from '../api/push'

function isPushSupported() {
  return (
    'serviceWorker' in navigator &&
    'PushManager'   in window &&
    'Notification'  in window
  )
}

export function usePushNotifications() {
  const [permiso,       setPermiso]       = useState(Notification.permission ?? 'default')
  const [suscrito,      setSuscrito]      = useState(false)
  const [cargando,      setCargando]      = useState(false)
  const [soportado,     setSoportado]     = useState(false)
  const [subscription,  setSubscription]  = useState(null)

  // Detectar soporte y suscripción actual
  useEffect(() => {
    if (!isPushSupported()) return
    setSoportado(true)
    navigator.serviceWorker.ready.then((reg) =>
      reg.pushManager.getSubscription()
    ).then((sub) => {
      setSuscrito(Boolean(sub))
      setSubscription(sub)
    }).catch(() => {})
  }, [])

  const suscribir = useCallback(async () => {
    if (!soportado || cargando) return
    setCargando(true)
    try {
      // Pedir permiso si no está concedido
      if (Notification.permission !== 'granted') {
        const result = await Notification.requestPermission()
        setPermiso(result)
        if (result !== 'granted') return
      }
      const sub = await subscribePush()
      setSuscrito(true)
      setSubscription(sub)
    } catch (err) {
      console.error('[push] suscribir:', err)
    } finally {
      setCargando(false)
    }
  }, [soportado, cargando])

  const desuscribir = useCallback(async () => {
    if (!subscription || cargando) return
    setCargando(true)
    try {
      await unsubscribePush(subscription)
      setSuscrito(false)
      setSubscription(null)
    } catch (err) {
      console.error('[push] desuscribir:', err)
    } finally {
      setCargando(false)
    }
  }, [subscription, cargando])

  return { soportado, permiso, suscrito, cargando, suscribir, desuscribir }
}
