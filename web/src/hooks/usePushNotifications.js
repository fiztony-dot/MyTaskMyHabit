import { useState, useEffect, useCallback } from 'react'
import { subscribePush, unsubscribePush, enviarPrueba as apiEnviarPrueba } from '../api/push'

function isPushSupported() {
  return (
    'serviceWorker' in navigator &&
    'PushManager'   in window &&
    'Notification'  in window
  )
}

export function usePushNotifications() {
  const [permiso,        setPermiso]        = useState(() => {
    try { return Notification.permission } catch { return 'default' }
  })
  const [suscrito,       setSuscrito]       = useState(false)
  const [cargando,       setCargando]       = useState(false)
  const [soportado,      setSoportado]      = useState(false)
  const [subscription,   setSubscription]   = useState(null)
  const [mensajePrueba,  setMensajePrueba]  = useState(null)
  const [cargandoPrueba, setCargandoPrueba] = useState(false)

  // Detectar soporte y suscripción actual
  useEffect(() => {
    if (!isPushSupported()) return
    setSoportado(true)
    navigator.serviceWorker.ready
      .then((reg) => reg.pushManager.getSubscription())
      .then((sub) => {
        setSuscrito(Boolean(sub))
        setSubscription(sub)
      })
      .catch(() => {})
  }, [])

  const suscribir = useCallback(async () => {
    if (!soportado || cargando) return
    setCargando(true)
    try {
      if (Notification.permission !== 'granted') {
        const result = await Notification.requestPermission()
        setPermiso(result)
        if (result !== 'granted') return
      }
      const sub = await subscribePush()
      setSuscrito(true)
      setSubscription(sub)
      setMensajePrueba(null)
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
      setMensajePrueba(null)
    } catch (err) {
      console.error('[push] desuscribir:', err)
    } finally {
      setCargando(false)
    }
  }, [subscription, cargando])

  const enviarPrueba = useCallback(async () => {
    if (!suscrito || cargandoPrueba) return
    setCargandoPrueba(true)
    setMensajePrueba(null)
    try {
      const result = await apiEnviarPrueba()
      if (result.intentadas === 0) {
        setMensajePrueba('Sin suscripciones activas')
      } else if (result.enviadas === 0) {
        setMensajePrueba('Error al enviar')
      } else {
        setMensajePrueba('Enviada ✓')
      }
    } catch {
      setMensajePrueba('Error al enviar')
    } finally {
      setCargandoPrueba(false)
    }
  }, [suscrito, cargandoPrueba])

  return {
    soportado, permiso, suscrito, cargando,
    suscribir, desuscribir,
    enviarPrueba, mensajePrueba, cargandoPrueba,
  }
}
