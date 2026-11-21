import type { HttpClient } from '../../../shared/api/httpClient'
import type { ConceptAdminItem, ConceptCreatable } from '../model/concept'

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
