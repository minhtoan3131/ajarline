import { describe, expect, it } from 'vitest'

import { contentItem } from '../fixture/contentIndexFixture'
import type { AdminContentIndex } from './contentIndex'
import {
  contentIndexSearch,
  describeEmpty,
  NO_FILTER,
  parseContentIndexQuery,
  type ContentIndexQuery,
} from './contentIndexQuery'

function parse(search: string): ContentIndexQuery {
  return parseContentIndexQuery(new URLSearchParams(search))
}

function indexOf(count: number): AdminContentIndex {
  return { page: 0, size: 20, hasMore: false, items: Array.from({ length: count }, () => contentItem()) }
}

describe('parseContentIndexQuery', () => {
  it('parse_ShouldGiveDefaults_WhenUrlHasNoQuery', () => {
    expect(parse('')).toEqual(NO_FILTER)
  })

  it('parse_ShouldKeepFixedOrder_WhenValuesComeInAnotherOrder', () => {
    const query = parse('types=news,article&statuses=UNPUBLISHED,DRAFT')

    expect(query.types).toEqual(['article', 'news'])
    expect(query.statuses).toEqual(['DRAFT', 'UNPUBLISHED'])
  })

  it('parse_ShouldDropUnknownAndRepeatedValues_WhenUrlWasEdited', () => {
    const query = parse('types=video,news,news&statuses=draft,ARCHIVED')

    expect(query.types).toEqual(['news'])
    expect(query.statuses).toEqual([])
  })

  it('parse_ShouldOpenFirstPage_WhenPageIsNotAValidNumber', () => {
    expect(parse('page=abc').page).toBe(0)
    expect(parse('page=-1').page).toBe(0)
    expect(parse('page=1.5').page).toBe(0)
    expect(parse('page=3').page).toBe(3)
  })

  it('parse_ShouldTrimSearchText_WhenItHasSpaces', () => {
    expect(parse('q=%20spring%20').q).toBe('spring')
    expect(parse('q=%20%20').q).toBe('')
  })

  it('parse_ShouldCutSearchText_WhenItIsLongerThanServerLimit', () => {
    expect(parse(`q=${'a'.repeat(301)}`).q).toHaveLength(300)
  })
})

describe('contentIndexSearch', () => {
  it('search_ShouldBeEmpty_WhenNothingIsSet', () => {
    expect(contentIndexSearch(NO_FILTER)).toBe('')
  })

  it('search_ShouldLeaveOutDefaults_WhenOnlyOneFilterIsSet', () => {
    expect(contentIndexSearch({ ...NO_FILTER, types: ['news'] })).toBe('?types=news')
  })

  it('search_ShouldWriteEveryPart_WhenEveryFilterIsSet', () => {
    const query: ContentIndexQuery = { types: ['article', 'news'], statuses: ['DRAFT'], q: 'spring boot', page: 2 }

    expect(contentIndexSearch(query)).toBe('?types=article,news&statuses=DRAFT&q=spring%20boot&page=2')
  })

  it('search_ShouldGiveSameText_WhenSameSetHasAnotherOrder', () => {
    const first = contentIndexSearch({ ...NO_FILTER, types: ['news', 'article'] })
    const second = contentIndexSearch({ ...NO_FILTER, types: ['article', 'news'] })

    expect(first).toBe(second)
  })

  it('search_ShouldGiveBackSameQuery_WhenTextIsParsedAgain', () => {
    const query: ContentIndexQuery = { types: ['series'], statuses: ['PUBLISHED'], q: 'a&b=c%', page: 4 }

    expect(parse(contentIndexSearch(query))).toEqual(query)
  })
})

describe('describeEmpty', () => {
  it('describeEmpty_ShouldBeNull_WhenPageHasItems', () => {
    expect(describeEmpty(NO_FILTER, indexOf(1))).toBeNull()
  })

  it('describeEmpty_ShouldSayStoreIsEmpty_WhenNothingIsFilteredOnFirstPage', () => {
    expect(describeEmpty(NO_FILTER, indexOf(0))).toBe('store-empty')
  })

  it('describeEmpty_ShouldSayNoMatch_WhenAFilterIsSetOnFirstPage', () => {
    expect(describeEmpty({ ...NO_FILTER, q: 'zzz' }, indexOf(0))).toBe('no-match')
    expect(describeEmpty({ ...NO_FILTER, statuses: ['DRAFT'] }, indexOf(0))).toBe('no-match')
  })

  it('describeEmpty_ShouldSayPastEnd_WhenPageIsAfterTheLastOne', () => {
    expect(describeEmpty({ ...NO_FILTER, page: 3 }, indexOf(0))).toBe('past-end')
    expect(describeEmpty({ ...NO_FILTER, types: ['news'], page: 2 }, indexOf(0))).toBe('past-end')
  })
})
