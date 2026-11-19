import type { ComponentProps } from 'react'

import './content-rendering.css'

export type ExternalLinkProps = Omit<ComponentProps<'a'>, 'href' | 'target' | 'rel'> & {
  /** A URL that already passed the URL policy. */
  href: string
}

/**
 * A link that leaves Ajarline. This is the only place that decides how an
 * external link opens: in a new tab, without access back to this page.
 */
export function ExternalLink({ href, children, ...attributes }: ExternalLinkProps) {
  return (
    <a {...attributes} href={href} target="_blank" rel="noopener noreferrer external">
      {children}
      <span className="external-link-mark" aria-hidden="true">
        ↗
      </span>
      {' '}
      <span className="external-link-note">(mở trong tab mới)</span>
    </a>
  )
}
