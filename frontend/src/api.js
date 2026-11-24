// Comunicarea cu backend-ul Spring. Toate cererile merg la /api (vezi proxy-ul din vite.config.js).

const TOKEN_KEY = 'auction_token'

export const getToken = () => localStorage.getItem(TOKEN_KEY)
export const setToken = (token) => localStorage.setItem(TOKEN_KEY, token)
export const clearToken = () => localStorage.removeItem(TOKEN_KEY)

export async function api(path, { method = 'GET', body, auth = true } = {}) {
  const headers = {}
  // FormData (fisiere) se trimite asa cum e: browserul seteaza singur Content-Type cu "boundary"
  const isFormData = body instanceof FormData
  if (body !== undefined && !isFormData) headers['Content-Type'] = 'application/json'

  const token = getToken()
  if (auth && token) headers.Authorization = `Bearer ${token}`

  const response = await fetch(`/api${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : isFormData ? body : JSON.stringify(body),
  })

  // Unele rute intorc text simplu (ex: inregistrarea), altele JSON
  const text = await response.text()
  let data = text
  try {
    data = text ? JSON.parse(text) : null
  } catch {
    // raspunsul nu e JSON, il pastram ca text
  }

  if (!response.ok) {
    // Token expirat sau invalid: anuntam aplicatia sa deconecteze utilizatorul
    if (response.status === 401 && auth) {
      window.dispatchEvent(new Event('auth:expired'))
    }
    throw new Error(data?.eroare || `A aparut o eroare (${response.status})`)
  }

  return data
}

// ---------- Pozele unui obiect (doar vanzatorul are voie); fiecare intoarce obiectul actualizat ----------

export const MAX_IMAGES = 5

// Adauga o poza la sfarsitul listei
export function uploadItemImage(itemId, file) {
  const form = new FormData()
  form.append('image', file)
  return api(`/items/${itemId}/images`, { method: 'POST', body: form })
}

// imageUrl arata ca /api/images/<nume-fisier>; serverul are nevoie doar de numele fisierului
const imageName = (imageUrl) => imageUrl.split('/').pop()

export const deleteItemImage = (itemId, imageUrl) =>
  api(`/items/${itemId}/images/${imageName(imageUrl)}`, { method: 'DELETE' })

export const setMainItemImage = (itemId, imageUrl) =>
  api(`/items/${itemId}/images/${imageName(imageUrl)}/main`, { method: 'POST' })

// Aceleasi reguli ca pe server, ca utilizatorul sa afle imediat daca fisierul nu e bun
export const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
export const MAX_IMAGE_BYTES = 5 * 1024 * 1024

export function checkImage(file) {
  if (!IMAGE_TYPES.includes(file.type)) return 'Fisierul trebuie sa fie o imagine JPG, PNG, WebP sau GIF!'
  if (file.size > MAX_IMAGE_BYTES) return 'Imaginea poate avea maxim 5 MB!'
  return null
}

export const formatPrice = (value) =>
  new Intl.NumberFormat('ro-RO', { style: 'currency', currency: 'RON' }).format(value)

// Serverul trimite orele in UTC, dar fara fus orar (ex: "2026-11-23T22:05:00").
// Fara "Z" la final, browserul le-ar citi ca ora locala si ar gresi cu cateva ore.
export function parseDate(value) {
  if (!value) return null
  return new Date(/[zZ]|[+-]\d\d:\d\d$/.test(value) ? value : `${value}Z`)
}

export const formatDate = (value) =>
  new Intl.DateTimeFormat('ro-RO', { dateStyle: 'medium', timeStyle: 'short' }).format(parseDate(value))

// "acum 12s", "acum 3 min"...
export function timeAgo(value, now) {
  const seconds = Math.max(0, Math.round((now - parseDate(value).getTime()) / 1000))
  if (seconds < 60) return `acum ${seconds}s`
  if (seconds < 3600) return `acum ${Math.floor(seconds / 60)} min`
  if (seconds < 86400) return `acum ${Math.floor(seconds / 3600)} h`
  return `acum ${Math.floor(seconds / 86400)} z`
}
