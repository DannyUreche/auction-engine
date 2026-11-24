import { useEffect, useState } from 'react'
import { api } from './api.js'

// Progressive Web App helpers: service worker, "install app" prompt and browser push notifications.

export function registerServiceWorker() {
  if (!('serviceWorker' in navigator)) return
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {})
  })
}

// The browser fires beforeinstallprompt once per page load; keep it so a button can show it later
let installEvent = null
const installListeners = new Set()
if (typeof window !== 'undefined') {
  window.addEventListener('beforeinstallprompt', (event) => {
    event.preventDefault()
    installEvent = event
    installListeners.forEach((listener) => listener(true))
  })
  window.addEventListener('appinstalled', () => {
    installEvent = null
    installListeners.forEach((listener) => listener(false))
  })
}

// [canInstall, install()] - canInstall is false when already installed or the browser can't install
export function useInstallPrompt() {
  const [canInstall, setCanInstall] = useState(() => installEvent != null)
  useEffect(() => {
    installListeners.add(setCanInstall)
    return () => installListeners.delete(setCanInstall)
  }, [])
  async function install() {
    if (!installEvent) return
    installEvent.prompt()
    await installEvent.userChoice.catch(() => {})
    installEvent = null
    setCanInstall(false)
  }
  return [canInstall, install]
}

export const pushSupported = () => 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window

// The VAPID public key is base64url; the Push API wants raw bytes
function urlBase64ToUint8Array(base64) {
  const padded = (base64 + '='.repeat((4 - (base64.length % 4)) % 4)).replace(/-/g, '+').replace(/_/g, '/')
  return Uint8Array.from(atob(padded), (char) => char.charCodeAt(0))
}

async function currentSubscription() {
  const registration = await navigator.serviceWorker.ready
  return registration.pushManager.getSubscription()
}

export async function isPushEnabled() {
  if (!pushSupported() || Notification.permission !== 'granted') return false
  return (await currentSubscription()) != null
}

export async function enablePush() {
  if (!pushSupported()) throw new Error('Browserul tau nu suporta notificari push.')
  const permission = await Notification.requestPermission()
  if (permission !== 'granted') throw new Error('Ai blocat notificarile pentru acest site (le poti permite din setarile browserului).')
  const { publicKey } = await api('/push/public-key', { auth: false })
  const registration = await navigator.serviceWorker.ready
  const subscription =
    (await registration.pushManager.getSubscription()) ??
    (await registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: urlBase64ToUint8Array(publicKey) }))
  const { endpoint, keys } = subscription.toJSON()
  await api('/push/subscribe', { method: 'POST', body: { endpoint, keys } })
}

export async function disablePush() {
  const subscription = await currentSubscription()
  if (!subscription) return
  await api('/push/unsubscribe', { method: 'POST', body: { endpoint: subscription.endpoint } }).catch(() => {})
  await subscription.unsubscribe()
}
