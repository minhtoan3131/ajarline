/** The four language hints that the server accepts. */
export type LanguageHint = 'vi' | 'en' | 'technical' | 'mixed'

export const LANGUAGE_HINT_OPTIONS: readonly { value: LanguageHint; label: string }[] = [
  { value: 'vi', label: 'Tiếng Việt' },
  { value: 'en', label: 'Tiếng Anh' },
  { value: 'technical', label: 'Thuật ngữ kỹ thuật' },
  { value: 'mixed', label: 'Pha trộn' },
]

/** One expression of a Concept as the Admin API returns it. It has no id. */
export interface ConceptExpressionItem {
  expression: string
  languageHint: LanguageHint | null
  autoExpand: boolean
}

/** One Concept as the Admin API returns it, active or not. */
export interface ConceptAdminItem {
  id: string
  key: string
  displayName: string
  description: string | null
  active: boolean
  expressions: ConceptExpressionItem[]
  createdAt: string
  updatedAt: string
}

/** A Concept can have at most this many expressions. */
export const MAX_EXPRESSIONS = 50

/** One expression row of the form. `rowKey` is only for React and is never sent. */
export interface ExpressionRow {
  rowKey: string
  expression: string
  /** Empty text means that no language is chosen. */
  languageHint: LanguageHint | ''
  autoExpand: boolean
}

/** Text and switch values of the Concept form. */
export interface ConceptFields {
  key: string
  displayName: string
  description: string
  active: boolean
  expressions: ExpressionRow[]
}

/** The changes that the form can make to the fields other than the expression rows. */
export type ConceptFieldChanges = Partial<Omit<ConceptFields, 'expressions'>>

/** The changes that the form can make to one expression row. */
export type ExpressionChanges = Partial<Omit<ExpressionRow, 'rowKey'>>

/** A new Concept starts active and has no expression. */
export const EMPTY_CONCEPT_FIELDS: ConceptFields = {
  key: '',
  displayName: '',
  description: '',
  active: true,
  expressions: [],
}

/** Full request body of create. Every field is required; description may be null. */
export interface ConceptCreatable {
  key: string
  displayName: string
  description: string | null
  active: boolean
  expressions: ConceptExpressionItem[]
}

function toExpressionItem(row: ExpressionRow): ConceptExpressionItem {
  return {
    expression: row.expression,
    languageHint: row.languageHint === '' ? null : row.languageHint,
    autoExpand: row.autoExpand,
  }
}

/**
 * Builds the create body. An empty description and an empty language are sent
 * as null. The key and the texts go as typed; the server normalizes them.
 * The row keys stay on the client.
 */
export function toCreatable(fields: ConceptFields): ConceptCreatable {
  return {
    key: fields.key,
    displayName: fields.displayName,
    description: fields.description === '' ? null : fields.description,
    active: fields.active,
    expressions: fields.expressions.map(toExpressionItem),
  }
}

function sameRow(left: ExpressionRow, right: ExpressionRow): boolean {
  return (
    left.expression === right.expression &&
    left.languageHint === right.languageHint &&
    left.autoExpand === right.autoExpand
  )
}

/** Compares values and row order. The row keys do not count (Q4.11). */
export function sameConceptFields(left: ConceptFields, right: ConceptFields): boolean {
  return (
    left.key === right.key &&
    left.displayName === right.displayName &&
    left.description === right.description &&
    left.active === right.active &&
    left.expressions.length === right.expressions.length &&
    left.expressions.every((row, index) => sameRow(row, right.expressions[index]))
  )
}
