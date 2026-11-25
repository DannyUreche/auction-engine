import { afterEach, describe, expect, it, vi } from 'vitest'
import { dutchPrice, formatPrice, isEnglish, parseDate, timeAgo } from '../api.js'

// api.js reads <html lang> for English texts; the tests run without a browser, so a tiny stand-in is enough
function setLang(lang) {
  vi.stubGlobal('document', { documentElement: { lang } })
}

afterEach(() => vi.unstubAllGlobals())

describe('parseDate', () => {
  it('reads server dates (UTC without a zone) as UTC', () => {
    expect(parseDate('2026-11-23T22:05:00').toISOString()).toBe('2026-11-23T22:05:00.000Z')
  })
  it('keeps dates that already have a zone', () => {
    expect(parseDate('2026-11-23T22:05:00+02:00').toISOString()).toBe('2026-11-23T20:05:00.000Z')
    expect(parseDate('2026-11-23T22:05:00Z').toISOString()).toBe('2026-11-23T22:05:00.000Z')
  })
  it('returns null for missing values', () => {
    expect(parseDate(null)).toBeNull()
  })
})

describe('formatPrice', () => {
  it('formats lei the Romanian way', () => {
    expect(formatPrice(1234.5).replace(/\s/g, ' ')).toBe('1.234,50 RON')
  })
})

describe('timeAgo', () => {
  const now = Date.parse('2026-11-25T12:00:00Z')
  it('speaks Romanian by default', () => {
    setLang('ro')
    expect(timeAgo('2026-11-25T11:59:30', now)).toBe('acum 30s')
    expect(timeAgo('2026-11-25T11:45:00', now)).toBe('acum 15 min')
    expect(timeAgo('2026-11-25T09:00:00', now)).toBe('acum 3 h')
    expect(timeAgo('2026-11-23T12:00:00', now)).toBe('acum 2 z')
  })
  it('switches to English with the interface', () => {
    setLang('en')
    expect(isEnglish()).toBe(true)
    expect(timeAgo('2026-11-23T12:00:00', now)).toBe('2 d ago')
  })
  it('never goes negative for clocks slightly ahead', () => {
    setLang('ro')
    expect(timeAgo('2026-11-25T12:00:05', now)).toBe('acum 0s')
  })
})

describe('dutchPrice', () => {
  const item = { startTime: '2026-11-25T12:00:00', startPrice: 500, priceDrop: 10, floorPrice: 200 }
  it('drops once per full minute', () => {
    expect(dutchPrice(item, Date.parse('2026-11-25T12:00:59Z'))).toBe(500)
    expect(dutchPrice(item, Date.parse('2026-11-25T12:05:00Z'))).toBe(450)
  })
  it('never goes under the floor or above the start', () => {
    expect(dutchPrice(item, Date.parse('2026-11-26T12:00:00Z'))).toBe(200)
    expect(dutchPrice(item, Date.parse('2026-11-25T11:00:00Z'))).toBe(500)
  })
})
