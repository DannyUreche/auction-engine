import { describe, expect, it } from 'vitest'
import { urlBase64ToUint8Array } from '../pwa.js'

describe('urlBase64ToUint8Array', () => {
  it('decodes base64url without padding (VAPID public keys)', () => {
    // "hello?" -> base64 "aGVsbG8/" -> base64url "aGVsbG8_"
    expect(Array.from(urlBase64ToUint8Array('aGVsbG8_'))).toEqual([104, 101, 108, 108, 111, 63])
    // "hi" -> "aGk=" -> without padding "aGk"
    expect(Array.from(urlBase64ToUint8Array('aGk'))).toEqual([104, 105])
  })
})
