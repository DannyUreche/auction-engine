import { describe, expect, it } from 'vitest'
import { translate } from '../i18n.jsx'

describe('translate', () => {
  it('returns English when the dictionary has the text', () => {
    expect(translate('en', 'Contul meu')).toBe('My account')
  })
  it('keeps Romanian for Romanian and for unknown text', () => {
    expect(translate('ro', 'Contul meu')).toBe('Contul meu')
    expect(translate('en', 'Text fara traducere')).toBe('Text fara traducere')
  })
  it('fills in placeholders', () => {
    expect(translate('en', '{n} licitatii active chiar acum', { n: 7 })).toBe('7 live auctions right now')
    expect(translate('ro', '{shown} din {total}', { shown: 12, total: 96 })).toBe('12 din 96')
  })
})
