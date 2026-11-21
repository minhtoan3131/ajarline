import type { HttpClient } from '../../../shared/api/httpClient'
import type { ConceptAdminItem, ConceptCreatable, ConceptEditable } from '../model/concept'

const CONCEPTS_PATH = '/api/admin/v1/concepts'

interface ConceptAdminList {
  concepts: ConceptAdminItem[]
}

/** Returns every Concept, active or not, in the order the server chose. */
export async function listConcepts(client: HttpClient, signal?: AbortSignal): Promise<ConceptAdminItem[]> {
  const response = await client.request<ConceptAdminList>(CONCEPTS_PATH, { signal })
  return response.body.concepts
}

/**
 * Creates a Concept with its expressions. The server answers 201 with the stored Concept.
 *
 * @throws ApiError 409 CONCEPT_KEY_CONFLICT when the key is taken,
 *         422 VALIDATION_FAILED, 422 DUPLICATE_CONCEPT_EXPRESSION
 */
export async function createConcept(client: HttpClient, creatable: ConceptCreatable): Promise<ConceptAdminItem> {
  const response = await client.request<ConceptAdminItem>(CONCEPTS_PATH, { method: 'POST', body: creatable })
  return response.body
}

function conceptPath(conceptId: string): string {
  return `${CONCEPTS_PATH}/${encodeURIComponent(conceptId)}`
}

/**
 * Replaces the editable values and the whole expression set of a Concept.
 * The last save wins; Concepts have no ETag.
 *
 * @throws ApiError 404 CONCEPT_NOT_FOUND, 422 VALIDATION_FAILED, 422 DUPLICATE_CONCEPT_EXPRESSION
 */
export async function updateConcept(
  client: HttpClient,
  conceptId: string,
  editable: ConceptEditable,
): Promise<ConceptAdminItem> {
  const response = await client.request<ConceptAdminItem>(conceptPath(conceptId), { method: 'PUT', body: editable })
  return response.body
}

/**
 * Deletes a Concept and its expressions. The server answers 204 without a body.
 *
 * @throws ApiError 404 CONCEPT_NOT_FOUND
 */
export async function deleteConcept(client: HttpClient, conceptId: string): Promise<void> {
  await client.request(conceptPath(conceptId), { method: 'DELETE' })
}
