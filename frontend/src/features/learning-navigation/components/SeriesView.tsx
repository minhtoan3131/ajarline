import { Link } from 'react-router'

import { articlePath } from '../../article-reading/articleRoutes'
import { MarkdownRenderer } from '../../content-rendering/MarkdownRenderer'
import { TopicChips } from '../../topic-browsing/components/TopicChips'
import { modulePath, seriesIndexPath } from '../learningRoutes'
import type { PublicSeries, SeriesModuleItem } from '../model/publicSeries'

/** Series page: orientation, overview, then the public Modules with their Articles in order. */
export function SeriesView({ series }: { series: PublicSeries }) {
  return (
    <section className="reader-browse" aria-labelledby="series-title">
      <title>{`${series.title} · Ajarline`}</title>
      <header className="reader-browse-header">
        <p className="reader-browse-back">
          <Link to={seriesIndexPath()}>Tất cả Series</Link>
        </p>
        <h1 id="series-title">{series.title}</h1>
        <p className="reader-browse-intro">{series.summary}</p>
        <TopicChips topics={series.topics} label="Chủ đề của Series" />
      </header>
      <div className="reader-article-body reader-learning-overview">
        <MarkdownRenderer markdown={series.overviewMarkdown} linkBehavior="navigate" />
      </div>
      <section className="reader-content-group" aria-labelledby="series-modules-title">
        <h2 id="series-modules-title">Các Module</h2>
        <ol className="reader-content-list">
          {series.modules.map((item) => (
            <SeriesModuleEntry key={item.id} module={item} />
          ))}
        </ol>
      </section>
    </section>
  )
}

function SeriesModuleEntry({ module }: { module: SeriesModuleItem }) {
  return (
    <li className="reader-content-entry">
      <p className="reader-content-context">Module {module.position}</p>
      <h3>
        <Link to={modulePath(module.slug)}>{module.title}</Link>
      </h3>
      <p className="reader-content-summary">{module.summary}</p>
      <ol className="reader-learning-articles" aria-label={`Các bài viết trong ${module.title}`}>
        {module.articles.map((article) => (
          <li key={article.id}>
            <Link to={articlePath(article.slug)}>{article.title}</Link>
          </li>
        ))}
      </ol>
    </li>
  )
}
