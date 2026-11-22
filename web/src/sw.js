/**
 * Service Worker personalizado — MyTaskMyHabit
 *
 * Estrategia injectManifest: Vite-plugin-pwa inyecta self.__WB_MANIFEST aquí
 * y construye el SW como un módulo separado.
 *
 * Este fichero NO se importa directamente desde la app — es el punto de
 * entrada del SW. La versión final compilada aparece en dist/sw.js.
 */

import { precacheAndRoute, cleanupOutdatedCaches } from 'workbox-precaching'
import { registerRoute } from 'workbox-routing'
import { NetworkFirst } from 'workbox-strategies'

// ── Precache ──────────────────────────────────────────────────────────────────
// self.__WB_MANIFEST es reemplazado por vite-plugin-pwa con la lista real.
precacheAndRoute(self.__WB_MANIFEST)
cleanupOutdatedCaches()

// ── Runtime caching: API del Worker (NetworkFirst, 24h) ───────────────────────
registerRoute(
  ({ url }) => url.origin === 'https://api.myafiappdomain.com',
  new NetworkFirst({
    cacheName: 'api-cache',
    networkTimeoutSeconds: 10,
    plugins: [
      {
        cacheWillUpdate: async ({ response }) =>
          response && response.status === 200 ? response : null,
      },
    ],
  })
)

// ── Web Push ──────────────────────────────────────────────────────────────────

self.addEventListener('push', (event) => {
  let data = {}
  try { data = event.data?.json() ?? {} } catch { /* payload no JSON — ignorar */ }

  const title   = data.title  ?? 'MyTaskMyHabit'
  const options = {
    body:    data.body   ?? '',
    icon:    data.icon   ?? '/icons/icon-192.png',
    badge:   '/icons/icon-192.png',
    tag:     data.tag    ?? 'mtmh-default',
    renotify: Boolean(data.renotify),
    data:    { url: data.url ?? '/' },
  }

  event.waitUntil(self.registration.showNotification(title, options))
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const url = event.notification.data?.url ?? '/'

  event.waitUntil(
    clients
      .matchAll({ type: 'window', includeUncontrolled: true })
      .then((clientList) => {
        // Si ya hay una ventana abierta de la app, fórzala al frente y navega
        for (const client of clientList) {
          if ('focus' in client) {
            client.focus()
            if ('navigate' in client) client.navigate(url)
            return
          }
        }
        // Sin ventana abierta: abrir una nueva
        return clients.openWindow(url)
      })
  )
})
