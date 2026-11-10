/**
 * URL policy for links and images in rendered Markdown.
 *
 * The policy is an allowlist. A URL that does not pass is dropped, so the
 * browser can never follow or load it.
 */

const LINK_SCHEMES: ReadonlySet<string> = new Set(['http', 'https', 'mailto'])
// NOTE: the content rules ask for "approved HTTPS image hosts", but no host
// list is decided yet. Until then every https: host is accepted.
const IMAGE_SCHEMES: ReadonlySet<string> = new Set(['https'])
const SCHEME = /^([a-z][a-z0-9+.-]*):/i
// Browsers drop tabs and line breaks inside URLs, so "java\tscript:" can still
// run as "javascript:". A backslash can turn "/\host" into another host.
// Rejecting these characters is simpler and safer than normalizing them.
const UNSAFE_CHARACTERS = /[\u0000- \u007f\\]/

/** Returns the link target when it is allowed, otherwise null. */
export function safeLinkHref(url: string): string | null {
  const value = url.trim()
  if (!isWellFormed(value)) {
    return null
  }
  const scheme = schemeOf(value)
  if (scheme === null) {
    // Relative link: a route, an anchor or a file of this site.
    return value
  }
  return LINK_SCHEMES.has(scheme) ? value : null
}

/** Returns the image source when it is allowed, otherwise null. */
export function safeImageSrc(url: string): string | null {
  const value = url.trim()
  if (!isWellFormed(value)) {
    return null
  }
  const scheme = schemeOf(value)
  if (scheme === null) {
    // Only static assets served from the site root are allowed.
    return value.startsWith('/') ? value : null
  }
  return IMAGE_SCHEMES.has(scheme) ? value : null
}

/** True when the link leaves the site (absolute http or https URL). */
export function isExternalHref(href: string): boolean {
  const scheme = schemeOf(href)
  return scheme === 'http' || scheme === 'https'
}

/**
 * URL hook for react-markdown. It runs on every URL attribute of the tree.
 * Returning null removes the attribute.
 */
export function transformContentUrl(url: string, key: string): string | null {
  return key === 'src' ? safeImageSrc(url) : safeLinkHref(url)
}

function isWellFormed(value: string): boolean {
  // "//host" is an absolute URL without a scheme. It is not an internal route.
  return value !== '' && !UNSAFE_CHARACTERS.test(value) && !value.startsWith('//')
}

function schemeOf(value: string): string | null {
  const match = SCHEME.exec(value)
  return match === null ? null : match[1].toLowerCase()
}
