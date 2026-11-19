import { articlePath } from '../../article-reading/articleRoutes'
import { modulePath, seriesPath } from '../../learning-navigation/learningRoutes'
import type { TopicContentItem } from './publicTopic'

export type TopicContentGroupKey = 'series' | 'in-series' | 'standalone' | 'news'

export interface TopicContentGroup {
  key: TopicContentGroupKey
  label: string
  items: TopicContentItem[]
}

const GROUP_ORDER: TopicContentGroupKey[] = ['series', 'in-series', 'standalone', 'news']

const GROUP_LABELS: Record<TopicContentGroupKey, string> = {
  series: 'Series',
  'in-series': 'Trong Series',
  standalone: 'Bài viết độc lập',
  news: 'Tin công nghệ',
}

function groupKeyOf(item: TopicContentItem): TopicContentGroupKey {
  switch (item.type) {
    case 'series':
      return 'series'
    case 'module':
      return 'in-series'
    case 'article':
      return item.context.module === null ? 'standalone' : 'in-series'
    case 'news':
      return 'news'
  }
}

/**
 * Splits one page into the Topic Home groups. Groups keep a fixed order and
 * items keep the server order inside a group. Empty groups are left out.
 */
export function groupTopicContent(items: TopicContentItem[]): TopicContentGroup[] {
  return GROUP_ORDER.map((key) => ({
    key,
    label: GROUP_LABELS[key],
    items: items.filter((item) => groupKeyOf(item) === key),
  })).filter((group) => group.items.length > 0)
}

/** Reader route of an item, or null for a content type that has no Reader page yet. */
export function readerPathOf(item: TopicContentItem): string | null {
  switch (item.type) {
    case 'series':
      return seriesPath(item.slug)
    case 'module':
      return modulePath(item.slug)
    case 'article':
      return articlePath(item.slug)
    case 'news':
      return null
  }
}

/** "Series › Module" of the public parents, or null when the item has none. */
export function contextTrailOf(item: TopicContentItem): string | null {
  const titles = [item.context.series?.title, item.context.module?.title].filter(
    (title): title is string => title !== undefined,
  )
  return titles.length === 0 ? null : titles.join(' › ')
}
