import { describe, expect, it } from 'vitest'

import { articleContextOf } from './articleLearningContext'

const SERIES = { slug: 'java-concurrency', title: 'Java Concurrency' }
const MODULE = { slug: 'thread-co-ban', title: 'Thread cơ bản', position: 3 }

describe('articleContextOf', () => {
  const seriesCases: [string, typeof SERIES | null][] = [
    ['no Series', null],
    ['a Series', SERIES],
  ]

  it.each(seriesCases)('articleContextOf_ShouldReturnNull_WhenModuleIsNull (%s)', (_label, series) => {
    expect(articleContextOf({ series, module: null, previousArticle: null })).toBeNull()
  })

  it('articleContextOf_ShouldLinkSeriesAndKeepModulePosition_WhenModuleAndSeriesArePublic', () => {
    expect(articleContextOf({ series: SERIES, module: MODULE, previousArticle: null })).toEqual({
      module: { title: 'Thread cơ bản', path: '/modules/thread-co-ban', position: 3 },
      series: { title: 'Java Concurrency', path: '/series/java-concurrency' },
      ownRank: 1,
    })
  })

  it('articleContextOf_ShouldDropSeriesAndModulePosition_WhenSeriesIsHidden', () => {
    expect(articleContextOf({ series: null, module: MODULE, previousArticle: null })).toEqual({
      module: { title: 'Thread cơ bản', path: '/modules/thread-co-ban', position: null },
      series: null,
      ownRank: 1,
    })
  })

  const rankCases: [{ position: number } | null, number][] = [
    [null, 1],
    [{ position: 1 }, 2],
    [{ position: 7 }, 8],
  ]

  it.each(rankCases)('articleContextOf_ShouldRankAfterPreviousArticle_WhenPreviousIs (%j)', (previousArticle, ownRank) => {
    expect(articleContextOf({ series: SERIES, module: MODULE, previousArticle })?.ownRank).toBe(ownRank)
  })

  it('articleContextOf_ShouldBuildPathsFromSlugs_WhenSlugNeedsEncoding', () => {
    const context = articleContextOf({
      series: { slug: 'a b', title: 'S' },
      module: { slug: 'c d', title: 'M', position: 1 },
      previousArticle: null,
    })

    expect(context?.series?.path).toBe('/series/a%20b')
    expect(context?.module.path).toBe('/modules/c%20d')
  })
})
