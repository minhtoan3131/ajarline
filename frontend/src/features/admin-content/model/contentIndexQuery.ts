import { pageFromQuery } from '../../../shared/lib/pageFromQuery'
import type { PublicationStatus } from './article'
import {
  ADMIN_CONTENT_TYPES,
  PUBLICATION_STATUSES,
  type AdminContentIndex,
  type AdminContentType,
} from './contentIndex'

/** Longest search text the server accepts. */
export const MAX_SEARCH_LENGTH = 300

/** What the Admin asks for. An empty list means "no filter on this". */
export interface ContentIndexQuery {
  types: AdminContentType[]
  statuses: PublicationStatus[]
  q: string
  page: number
}

export const NO_FILTER: ContentIndexQuery = { types: [], statuses: [], q: '', page: 0 }

/** Why the Index shows no item. */
export type EmptyKind = 'store-empty' | 'no-match' | 'past-end'

/** Keeps the known values of `wanted`, once each, in the order of `all`. */
function inOrder<T extends string>(all: readonly T[], wanted: readonly string[]): T[] {
  return all.filter((value) => wanted.includes(value))
}

function listOf(value: string | null): string[] {
  return value === null ? [] : value.split(',')
}

/**
 * Reads the query from the page URL. The URL can be old or edited by hand,
 * so an unknown value is dropped and a missing one takes its default.
 */
export function parseContentIndexQuery(params: URLSearchParams): ContentIndexQuery {
  return {
    types: inOrder(ADMIN_CONTENT_TYPES, listOf(params.get('types'))),
    statuses: inOrder(PUBLICATION_STATUSES, listOf(params.get('statuses'))),
    q: (params.get('q') ?? '').trim().slice(0, MAX_SEARCH_LENGTH).trim(),
    page: pageFromQuery(params.get('page')),
  }
}

/**
 * Writes the query as the search part of the page URL, with a leading `?`.
 * A default value is left out, so the default query gives an empty string.
 * The same set of values always gives the same text.
 */
export function contentIndexSearch(query: ContentIndexQuery): string {
  const parts: string[] = []
  const types = inOrder(ADMIN_CONTENT_TYPES, query.types)
  const statuses = inOrder(PUBLICATION_STATUSES, query.statuses)
  if (types.length > 0) {
    parts.push(`types=${types.join(',')}`)
  }
  if (statuses.length > 0) {
    parts.push(`statuses=${statuses.join(',')}`)
  }
  if (query.q !== '') {
    parts.push(`q=${encodeURIComponent(query.q)}`)
  }
  if (query.page > 0) {
    parts.push(`page=${query.page}`)
  }
  return parts.length === 0 ? '' : `?${parts.join('&')}`
}

export function isFiltered(query: ContentIndexQuery): boolean {
  return query.types.length > 0 || query.statuses.length > 0 || query.q !== ''
}

/**
 * Tells why a page has no item, or `null` when it has items.
 * A page after the last one is checked first: it can never be an empty store.
 */
export function describeEmpty(query: ContentIndexQuery, index: AdminContentIndex): EmptyKind | null {
  if (index.items.length > 0) {
    return null
  }
  if (query.page > 0) {
    return 'past-end'
  }
  return isFiltered(query) ? 'no-match' : 'store-empty'
}
