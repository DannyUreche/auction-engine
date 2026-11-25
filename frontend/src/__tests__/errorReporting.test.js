import { beforeEach, describe, expect, it, vi } from 'vitest'

// errorReporting keeps a per-page memory of sent errors, so each test loads a fresh copy of the module
async function load() {
  vi.resetModules()
  return import('../errorReporting.js')
}

beforeEach(() => {
  vi.unstubAllGlobals()
  vi.stubGlobal('localStorage', { getItem: () => null })
  vi.stubGlobal('window', { location: { pathname: '/items/5', search: '?x=1' } })
  vi.stubGlobal('fetch', vi.fn(() => Promise.resolve({ ok: true })))
})

describe('reportError', () => {
  it('sends an error once, with the page address', async () => {
    const { reportError } = await load()
    reportError(new Error('Ceva a crapat'))
    reportError(new Error('Ceva a crapat'))
    expect(fetch).toHaveBeenCalledTimes(1)
    const [url, options] = fetch.mock.calls[0]
    expect(url).toBe('/api/client-errors')
    expect(JSON.parse(options.body)).toMatchObject({ message: 'Ceva a crapat', url: '/items/5?x=1' })
  })

  it('ignores browser noise and extensions', async () => {
    const { reportError } = await load()
    reportError(new Error('ResizeObserver loop completed with undelivered notifications.'))
    reportError({ message: 'x', stack: 'at chrome-extension://abc/content.js:1' })
    reportError('Script error.')
    expect(fetch).not.toHaveBeenCalled()
  })

  it('stops after 10 different errors per page', async () => {
    const { reportError } = await load()
    for (let i = 0; i < 15; i++) reportError(new Error(`Eroare ${i}`))
    expect(fetch).toHaveBeenCalledTimes(10)
  })
})
