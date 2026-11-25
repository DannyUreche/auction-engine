import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api, checkImage, deleteItemImage, MAX_IMAGE_BYTES, setMainItemImage } from '../api.js'

// A fake fetch answering with the given status and body
function respond(status, body) {
  return vi.fn(() => Promise.resolve({ ok: status < 400, status, text: () => Promise.resolve(body) }))
}

let stored
beforeEach(() => {
  vi.unstubAllGlobals()
  stored = { auction_token: 'jwt-123' }
  vi.stubGlobal('localStorage', { getItem: (key) => stored[key] ?? null, setItem: (k, v) => (stored[k] = v), removeItem: (k) => delete stored[k] })
  vi.stubGlobal('window', { dispatchEvent: vi.fn() })
})

describe('api', () => {
  it('sends JSON with the token and parses the answer', async () => {
    vi.stubGlobal('fetch', respond(200, '{"id": 7}'))
    await expect(api('/items', { method: 'POST', body: { name: 'Ceas' } })).resolves.toEqual({ id: 7 })
    const [url, options] = fetch.mock.calls[0]
    expect(url).toBe('/api/items')
    expect(options.headers).toEqual({ 'Content-Type': 'application/json', Authorization: 'Bearer jwt-123' })
    expect(options.body).toBe('{"name":"Ceas"}')
  })

  it('leaves the token out for public calls and accepts empty or text answers', async () => {
    vi.stubGlobal('fetch', respond(204, ''))
    await expect(api('/stats', { auth: false })).resolves.toBeNull()
    expect(fetch.mock.calls[0][1].headers).toEqual({})
    vi.stubGlobal('fetch', respond(200, 'Cont creat'))
    await expect(api('/auth/register', { method: 'POST', body: {}, auth: false })).resolves.toBe('Cont creat')
  })

  it('turns {"eroare"} answers into errors', async () => {
    vi.stubGlobal('fetch', respond(400, '{"eroare": "Oferta prea mica!"}'))
    await expect(api('/bids', { method: 'POST', body: {} })).rejects.toThrow('Oferta prea mica!')
    vi.stubGlobal('fetch', respond(502, '<html>Bad gateway</html>'))
    await expect(api('/items')).rejects.toThrow('A aparut o eroare (502)')
  })

  it('announces an expired session on 401', async () => {
    vi.stubGlobal('Event', class { constructor(type) { this.type = type } })
    vi.stubGlobal('fetch', respond(401, '{"eroare": "Token expirat"}'))
    await expect(api('/users/me')).rejects.toThrow('Token expirat')
    expect(window.dispatchEvent).toHaveBeenCalledWith(expect.objectContaining({ type: 'auth:expired' }))
  })

  it('sends only the file name for photo actions', async () => {
    vi.stubGlobal('fetch', respond(200, '{}'))
    await deleteItemImage(5, '/api/images/abc.jpg')
    await setMainItemImage(5, '/api/images/abc.jpg')
    expect(fetch.mock.calls.map(([url, options]) => `${options.method} ${url}`)).toEqual([
      'DELETE /api/items/5/images/abc.jpg',
      'POST /api/items/5/images/abc.jpg/main',
    ])
  })
})

describe('checkImage', () => {
  it('accepts photos up to 5 MB and explains what is wrong otherwise', () => {
    expect(checkImage({ type: 'image/jpeg', size: 1000 })).toBeNull()
    expect(checkImage({ type: 'application/pdf', size: 1000 })).toMatch(/JPG, PNG, WebP sau GIF/)
    expect(checkImage({ type: 'image/png', size: MAX_IMAGE_BYTES + 1 })).toMatch(/5 MB/)
  })
})
