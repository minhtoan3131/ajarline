import { vi } from 'vitest'

import type { ConceptAdminItem } from '../model/concept'

export const CONCEPTS_PATH = '/api/admin/v1/concepts'
export const CONCEPT_ID = '3b8f6a2e-4c15-4d79-9e0a-6f1d2c8b7a45'
export const OTHER_CONCEPT_ID = '9d4e1c73-2a68-4b5f-8c30-1e7a9f5b2d86'

export function conceptPath(conceptId: string): string {
  return `${CONCEPTS_PATH}/${conceptId}`
}

/**
 * An active Concept with three expressions, in the order the server returns
 * them: by normalized text, then by original text.
 */
export function activeConcept(changes: Partial<ConceptAdminItem> = {}): ConceptAdminItem {
  return {
    id: CONCEPT_ID,
    key: 'thread',
    displayName: 'Thread',
    description: 'Luồng thực thi trong chương trình.',
    active: true,
    expressions: [
      { expression: 'multithreading', languageHint: 'en', autoExpand: true },
      { expression: 'thread', languageHint: 'technical', autoExpand: false },
      { expression: 'đa luồng', languageHint: 'vi', autoExpand: true },
    ],
    createdAt: '2026-11-21T02:00:00Z',
    updatedAt: '2026-11-21T02:00:00Z',
    ...changes,
  }
}

/** A Concept that is turned off. It has no description and no expression. */
export function inactiveConcept(changes: Partial<ConceptAdminItem> = {}): ConceptAdminItem {
  return activeConcept({
    id: OTHER_CONCEPT_ID,
    key: 'spring-boot',
    displayName: 'Spring Boot',
    description: null,
    active: false,
    expressions: [],
    ...changes,
  })
}

/** Body of the list endpoint. */
export function conceptList(...concepts: ConceptAdminItem[]) {
  return { concepts }
}

/**
 * Holds back the requests with this method until the returned function is
 * called. Install the stub first.
 */
export function holdRequestsTo(method: string): () => void {
  const respond = globalThis.fetch
  let release: () => void = () => undefined
  const gate = new Promise<void>((resolve) => {
    release = resolve
  })
  vi.stubGlobal('fetch', async (input: RequestInfo | URL, init?: RequestInit) => {
    if ((init?.method ?? 'GET') === method) {
      await gate
    }
    return respond(input, init)
  })
  return release
}
