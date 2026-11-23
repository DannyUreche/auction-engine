// Comunicarea cu backend-ul Spring. Toate cererile merg la /api (vezi proxy-ul din vite.config.js).

const TOKEN_KEY = 'auction_token'

export const getToken = () => localStorage.getItem(TOKEN_KEY)
export const setToken = (token) => localStorage.setItem(TOKEN_KEY, token)
export const clearToken = () => localStorage.removeItem(TOKEN_KEY)

export async function api(path, { method = 'GET', body, auth = true } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  const token = getToken()
  if (auth && token) headers.Authorization = `Bearer ${token}`

  const response = await fetch(`/api${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
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

export const formatPrice = (value) =>
  new Intl.NumberFormat('ro-RO', { style: 'currency', currency: 'RON' }).format(value)

export const formatDate = (value) =>
  new Intl.DateTimeFormat('ro-RO', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
