# Plan de accion: mover IA a backend (sin API key en cliente)

## Objetivo
Eliminar la API key del APK y enrutar la funcionalidad de IA a traves de un backend propio para evitar filtraciones recurrentes.

## Checklist general
- [ ] Fase 0: preparacion
- [ ] Fase 1: backend proxy de IA
- [ ] Fase 2: integracion Android con feature flag
- [ ] Fase 3: hardening de seguridad y operacion
- [ ] Fase 4: retirada de key en cliente y cierre

## Fase 0 - Preparacion (0.5-1 dia)
- Congelar cambios funcionales en IA durante la migracion.
- Inventariar puntos de uso actuales (`IAProcessor`, widget, flujos de voz).
- Definir contrato unico request/response JSON compatible con el formato actual.
- Entregable: documento corto de contrato y mapa de llamadas.

## Fase 1 - Backend proxy de IA (1-2 dias)
- Crear servicio (Cloud Run o Cloud Functions) con `POST /ai/parse`.
- Mover ahi la llamada a Google IA.
- Guardar la key solo en servidor (Secret Manager).
- Implementar validacion de entrada, timeout y errores controlados.
- Añadir logs estructurados (sin datos sensibles) y `correlationId`.
- Entregable: endpoint funcional con respuestas equivalentes al cliente actual.

## Fase 2 - Integracion Android (1-2 dias)
- En `NetworkClient`/`IAProcessor`, llamar al backend en lugar de Google directo.
- Añadir `feature flag` (`useBackendAi=true`) para activar/desactivar sin tocar prompts.
- Mantener fallback de guardado simple para no romper UX.
- Probar casos: tarea normal, voz, widget, timeout, sin red, error 5xx.
- Entregable: APK funcional usando backend.

## Fase 3 - Hardening (1 dia)
- Añadir control de acceso al backend (token app; ideal Play Integrity/App Check).
- Aplicar rate limit por usuario/dispositivo/IP.
- Configurar cuotas y alertas (errores, latencia, volumen).
- Añadir healthcheck y dashboard basico.
- Entregable: backend protegido y monitorizado.

## Fase 4 - Cierre (0.5 dia)
- Eliminar API key de IA del cliente (manifest/build config/properties de app).
- Rotar y revocar claves antiguas expuestas.
- Limpiar codigo muerto de llamadas directas a proveedor IA.
- Entregable: app sin key sensible embebida.

## Criterios de exito
- Cero errores por key expuesta desde cliente.
- Flujos de voz y creacion IA operativos sin regresiones.
- Si backend falla, app no crashea y usa fallback controlado.
- Logs permiten diagnostico rapido.

## Riesgos y mitigacion
- Riesgo: incompatibilidad de JSON.  
  Mitigacion: contrato fijo + pruebas con casos reales.
- Riesgo: mayor latencia.  
  Mitigacion: timeout, reintento corto y medicion p95.
- Riesgo: impacto en produccion.  
  Mitigacion: feature flag y rollback rapido.

