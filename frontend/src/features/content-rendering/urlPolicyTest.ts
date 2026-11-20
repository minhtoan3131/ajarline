import { describe, expect, it } from 'vitest'

import { isExternalHref, safeImageSrc, safeLinkHref } from './urlPolicy'

describe('urlPolicy', () => {
  it.each([
    'https://example.com/a',
    'http://example.com',
    'mailto:a@example.com',
    '/articles/bai-khac',
    '#phan-1',
    'bai-khac',
    '../tai-lieu',
  ])('safeLinkHref_ShouldKeepUrl_WhenUrlIsAllowed [%s]', (url) => {
    // When / Then
    expect(safeLinkHref(url)).toBe(url)
  })

  it.each([
    'javascript:alert(1)',
    'JaVaScRiPt:alert(1)',
    ' javascript:alert(1)',
    'java\tscript:alert(1)',
    'data:text/html,<b>x</b>',
    'vbscript:msgbox(1)',
    'file:///etc/passwd',
    '//evil.example',
    '/\\evil.example',
    '',
  ])('safeLinkHref_ShouldRejectUrl_WhenUrlIsUnsafe [%s]', (url) => {
    // When / Then
    expect(safeLinkHref(url)).toBeNull()
  })

  // Keep this list equal to the "valid" list in SourceUrlPolicyTest.java. The renderer must keep
  // every link that the server accepts.
  it.each([
    'http://example.com',
    'https://example.com',
    'HTTPS://EXAMPLE.COM/Path',
    'https://example.com:8080/a/b?x=1&y=2#top',
    'https://user:secret@example.com/x',
    'https://[::1]/status',
    'https://example.com/a|b{c}',
    'https://example.com/bai-viet/tiếng-việt',
  ])('safeLinkHref_ShouldKeepUrl_WhenServerAcceptsSourceUrl [%s]', (url) => {
    expect(safeLinkHref(url)).toBe(url)
  })

  it.each(['https://cdn.example.com/a.png', '/images/a.png'])(
    'safeImageSrc_ShouldKeepUrl_WhenSourceIsApproved [%s]',
    (url) => {
      // When / Then
      expect(safeImageSrc(url)).toBe(url)
    },
  )

  it.each([
    'http://example.com/a.png',
    'data:image/png;base64,AAAA',
    'javascript:alert(1)',
    'images/a.png',
    '//cdn.example.com/a.png',
  ])('safeImageSrc_ShouldRejectUrl_WhenSourceIsUnsafe [%s]', (url) => {
    // When / Then
    expect(safeImageSrc(url)).toBeNull()
  })

  it('isExternalHref_ShouldOnlyMatchAbsoluteHttpUrls_WhenHrefIsChecked', () => {
    // When / Then
    expect(isExternalHref('https://example.com')).toBe(true)
    expect(isExternalHref('/articles/a')).toBe(false)
    expect(isExternalHref('mailto:a@example.com')).toBe(false)
  })
})
