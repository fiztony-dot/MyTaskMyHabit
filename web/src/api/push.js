import client from './client'

function urlBase64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4)
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
  const rawData = atob(base64)
  return Uint8Array.from([...rawData].map((c) => c.charCodeAt(0)))
}

export async function subscribePush() {
  const registration = await navigator.serviceWorker.ready
  const vapidKey = import.meta.env.VITE_VAPID_PUBLIC_KEY
  if (!vapidKey) throw new Error('VITE_VAPID_PUBLIC_KEY no definida')

  const subscription = await registration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: urlBase64ToUint8Array(vapidKey),
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
