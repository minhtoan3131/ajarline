import { sameIds, type PublicationStatus } from '../../admin-content/model/article'

/** The source of a News item on the wire. All three keys are always present. */
export interface NewsSourceEditable {
  name: string | null
  url: string | null
  /** ISO-8601 date-time with an offset. */
  publishedAt: string | null
}

/** Full request body of create and update. Update replaces every field. */
export interface NewsEditable {
  title: string
  slug: string
  summary: string | null
  bodyMarkdown: string | null
  source: NewsSourceEditable
  topicIds: string[]
}

export interface NewsAdminDetail extends NewsEditable {
  id: string
  type: 'news'
  status: PublicationStatus
  revision: number
  publishedAt: string | null
  unpublishedAt: string | null
  createdAt: string
  updatedAt: string
}

/** Values the editor lets the Admin change. Optional text is an empty string here. */
export interface NewsFields {
  title: string
  slug: string
  summary: string
  bodyMarkdown: string
  /** Ids of the chosen Topics. Their order carries no meaning. */
  topicIds: string[]
  sourceName: string
  sourceUrl: string
  /** `datetime-local` text: `YYYY-MM-DDTHH:mm` in the browser time zone, or empty. */
  sourcePublishedAt: string
}

/** Fields that are edited as text. The Topic choice has its own control. */
export type NewsTextField = Exclude<keyof NewsFields, 'topicIds'>

/** The fields that make up the source block. */
export type SourceField = 'sourceName' | 'sourceUrl' | 'sourcePublishedAt'

export const EMPTY_NEWS_FIELDS: NewsFields = {
  title: '',
  slug: '',
  summary: '',
  bodyMarkdown: '',
  topicIds: [],
  sourceName: '',
  sourceUrl: '',
  sourcePublishedAt: '',
}

export function fieldsOf(news: NewsAdminDetail): NewsFields {
  return {
    title: news.title,
    slug: news.slug,
    summary: news.summary ?? '',
    bodyMarkdown: news.bodyMarkdown ?? '',
    topicIds: [...news.topicIds],
    sourceName: news.source.name ?? '',
    sourceUrl: news.source.url ?? '',
    sourcePublishedAt: isoToLocalInput(news.source.publishedAt),
  }
}

/**
 * Builds the request body. Empty optional text is sent as null, which is
 * how the server stores it, so saving an unchanged form stays a no-op.
 * No rule of the server is checked here. The server decides.
 */
export function toEditable(fields: NewsFields): NewsEditable {
  return {
    title: fields.title,
    slug: fields.slug,
    summary: emptyToNull(fields.summary),
    bodyMarkdown: emptyToNull(fields.bodyMarkdown),
    source: {
      name: emptyToNull(fields.sourceName),
      url: emptyToNull(fields.sourceUrl),
      publishedAt: localInputToIso(fields.sourcePublishedAt),
    },
    topicIds: [...fields.topicIds],
  }
}

function emptyToNull(text: string): string | null {
  return text === '' ? null : text
}

function pad(value: number): string {
  return String(value).padStart(2, '0')
}

/** Turns an ISO date-time into the text of a `datetime-local` input, in the browser time zone. */
export function isoToLocalInput(iso: string | null): string {
  if (iso === null) {
    return ''
  }
  const date = new Date(iso)
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
    `T${pad(date.getHours())}:${pad(date.getMinutes())}`
  )
}

/**
 * Turns the text of a `datetime-local` input into an ISO date-time with `Z`.
 * The input only gives empty text or a valid date, so the Date below is valid.
 */
export function localInputToIso(text: string): string | null {
  if (text === '') {
    return null
  }
  // The text has no offset, so Date reads it in the browser time zone.
  const date = new Date(text)
  // The input counts whole minutes, so the milliseconds are always .000.
  // Drop them to match the form the server sends.
  return date.toISOString().replace('.000Z', 'Z')
}

export function sameFields(left: NewsFields, right: NewsFields): boolean {
  return (
    left.title === right.title &&
    left.slug === right.slug &&
    left.summary === right.summary &&
    left.bodyMarkdown === right.bodyMarkdown &&
    sameIds(left.topicIds, right.topicIds) &&
    left.sourceName === right.sourceName &&
    left.sourceUrl === right.sourceUrl &&
    left.sourcePublishedAt === right.sourcePublishedAt
  )
}
