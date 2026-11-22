import client from './client'

function urlBase64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4)
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
  const rawData = atob(base64)
  return Uint8Array.from([...rawData].map((c) => c.charCodeAt(0)))
}

export async function subscribePush() {
  const registration = await navigator.serviceWorker.ready

  // Obtener la clave pública VAPID del Worker (no depende de vars de entorno de Pages)
  const { data: keyRes } = await client.get('/api/push/public-key')
  const publicKey = keyRes?.data?.publicKey
  if (!publicKey) throw new Error('Notificaciones no configuradas en el servidor')

  const subscription = await registration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: urlBase64ToUint8Array(publicKey),
  })

  const { endpoint, keys } = subscription.toJSON()
  await client.post('/api/push/subscribe', {
    endpoint,
    p256dh: keys.p256dh,
    auth:   keys.auth,
  })
  return subscription
}

export async function unsubscribePush(subscription) {
  await client.delete('/api/push/subscribe', {
    data: { endpoint: subscription.endpoint },
  })
  await subscription.unsubscribe()
}

// Llamada silenciosa al hacer logout — no bloquea ni lanza si falla.
export async function desuscribirAlLogout() {
  if (!('serviceWorker' in navigator) || !('PushManager' in window)) return
  try {
    const reg = await navigator.serviceWorker.ready
    const sub = await reg.pushManager.getSubscription()
    if (!sub) return
    await unsubscribePush(sub)
  } catch { /* fail silently */ }
}

export async function enviarPrueba() {
  const { data } = await client.post('/api/push/test')
  return data?.data ?? { intentadas: 0, enviadas: 0 }
}
