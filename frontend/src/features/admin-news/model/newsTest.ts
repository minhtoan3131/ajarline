import { describe, expect, it } from 'vitest'

import { draftNews } from '../fixture/newsAdminFixture'
import { EMPTY_NEWS_FIELDS, fieldsOf, isoToLocalInput, localInputToIso, sameFields, toEditable } from './news'

describe('news model', () => {
  it('toEditable_ShouldEqualSavedValues_WhenFormIsUnchanged', () => {
    const news = draftNews({ topicIds: ['topic-a'] })

    expect(toEditable(fieldsOf(news))).toEqual({
      title: news.title,
      slug: news.slug,
      summary: news.summary,
      bodyMarkdown: news.bodyMarkdown,
      source: news.source,
      topicIds: news.topicIds,
    })
  })

  it('toEditable_ShouldSendNull_WhenOptionalTextIsEmpty', () => {
    expect(toEditable(EMPTY_NEWS_FIELDS)).toEqual({
      title: '',
      slug: '',
      summary: null,
      bodyMarkdown: null,
      source: { name: null, url: null, publishedAt: null },
      topicIds: [],
    })
  })

  it('toEditable_ShouldSendUrlAsTyped_WhenUrlIsNotValid', () => {
    const fields = { ...EMPTY_NEWS_FIELDS, sourceUrl: 'abc' }

    expect(toEditable(fields).source.url).toBe('abc')
  })

  it('fieldsOf_ShouldShowEmptyText_WhenSourceIsNotFilled', () => {
    const news = draftNews({ source: { name: null, url: null, publishedAt: null } })

    expect(fieldsOf(news)).toMatchObject({ sourceName: '', sourceUrl: '', sourcePublishedAt: '' })
  })

  it('isoToLocalInput_ShouldReturnEmpty_WhenValueIsNull', () => {
    expect(isoToLocalInput(null)).toBe('')
  })

  it('localInputToIso_ShouldReturnNull_WhenTextIsEmpty', () => {
    expect(localInputToIso('')).toBeNull()
  })

  it('localInputToIso_ShouldReadTextAsBrowserTime_WhenTextHasNoOffset', () => {
    const expected = new Date(2026, 10, 18, 9, 30).toISOString().replace('.000Z', 'Z')

    expect(localInputToIso('2026-11-18T09:30')).toBe(expected)
  })

  it.each(['2026-11-18T02:00:00Z', '2026-11-18T09:30:00+07:00'])(
    'localInputToIso_ShouldKeepTheInstant_WhenIsoGoesThroughTheInput [%s]',
    (iso) => {
      const roundTrip = localInputToIso(isoToLocalInput(iso))

      expect(new Date(roundTrip as string).getTime()).toBe(new Date(iso).getTime())
    },
  )

  it('sameFields_ShouldIgnoreTopicOrder_WhenTopicsAreTheSame', () => {
    const left = { ...EMPTY_NEWS_FIELDS, topicIds: ['a', 'b'] }
    const right = { ...EMPTY_NEWS_FIELDS, topicIds: ['b', 'a'] }

    expect(sameFields(left, right)).toBe(true)
  })

  it('sameFields_ShouldDetectChange_WhenSourceUrlChanges', () => {
    const changed = { ...EMPTY_NEWS_FIELDS, sourceUrl: 'https://example.com' }

    expect(sameFields(EMPTY_NEWS_FIELDS, changed)).toBe(false)
  })
})
