import { describe, expect, it } from 'vitest'

import { codeLanguageLabel, normalizeCodeLanguage } from './codeLanguage'

describe('codeLanguage', () => {
  it.each([
    ['java', 'java'],
    ['JAVA', 'java'],
    ['js', 'javascript'],
    ['ts', 'typescript'],
    ['sh', 'bash'],
    ['shell', 'bash'],
    ['yml', 'yaml'],
    ['md', 'markdown'],
    ['htm', 'html'],
    ['txt', 'text'],
    ['plaintext', 'text'],
  ])('normalizeCodeLanguage_ShouldReturnAllowlistedName_WhenLanguageIsKnown [%s]', (declared, expected) => {
    // When / Then
    expect(normalizeCodeLanguage(declared)).toBe(expected)
  })

  it.each([null, '', 'cobol', 'c++', 'javascript;alert(1)'])(
    'normalizeCodeLanguage_ShouldFallBackToText_WhenLanguageIsMissingOrUnknown [%s]',
    (declared) => {
      // When / Then
      expect(normalizeCodeLanguage(declared)).toBe('text')
    },
  )

  it('codeLanguageLabel_ShouldReturnNull_WhenLanguageIsPlainText', () => {
    // When / Then
    expect(codeLanguageLabel('text')).toBeNull()
    expect(codeLanguageLabel('sql')).toBe('SQL')
  })
})
