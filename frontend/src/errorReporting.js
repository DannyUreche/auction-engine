import { getToken } from './api.js'

// Sends JavaScript errors to the server's error monitor (admin page, "Erori" tab). At most 10 different errors
// per page load; noise from browser extensions and harmless browser warnings is skipped.
const sent = new Set()
const IGNORED = [/ResizeObserver loop/i, /chrome-extension:|moz-extension:/i, /^Script error\.?$/i]

export function reportError(error, extra = '') {
  const message = String(error?.message || error || 'Eroare necunoscuta').slice(0, 500)
  const stack = `${error?.stack || ''}${extra ? `\n${extra}` : ''}`.slice(0, 6000)
  if (IGNORED.some((pattern) => pattern.test(message) || pattern.test(stack)) || sent.has(message) || sent.size >= 10) return
  sent.add(message)
  const headers = { 'Content-Type': 'application/json' }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  fetch('/api/client-errors', {
    method: 'POST',
    headers,
    body: JSON.stringify({ message, stack, url: window.location.pathname + window.location.search }),
    keepalive: true,
  }).catch(() => {})
}

export function installErrorReporting() {
  window.addEventListener('error', (event) => reportError(event.error || event.message))
  window.addEventListener('unhandledrejection', (event) => reportError(event.reason))
}
