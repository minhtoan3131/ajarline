import { Link } from 'react-router'

import { articlePath } from '../../article-reading/articleRoutes'
import { MarkdownRenderer } from '../../content-rendering/MarkdownRenderer'
import { TopicChips } from '../../topic-browsing/components/TopicChips'
import { seriesPath } from '../learningRoutes'
import type { PublicModule } from '../model/publicModule'

/**
 * Module page: overview and the public Articles in order. The Series link
 * shows only when the Series is public. A hidden Series leaves no trace.
 */
export function ModuleView({ module }: { module: PublicModule }) {
  return (
    <section className="reader-browse" aria-labelledby="module-title">
      <title>{`${module.title} · Ajarline`}</title>
      <header className="reader-browse-header">
        {module.series !== null && (
          <p className="reader-browse-back">
            Thuộc Series <Link to={seriesPath(module.series.slug)}>{module.series.title}</Link>
          </p>
        )}
        <h1 id="module-title">{module.title}</h1>
        <p className="reader-browse-intro">{module.summary}</p>
        <TopicChips topics={module.topics} label="Chủ đề của Module" />
      </header>
      <div className="reader-article-body reader-learning-overview">
        <MarkdownRenderer markdown={module.overviewMarkdown} linkBehavior="navigate" />
      </div>
      <section className="reader-content-group" aria-labelledby="module-articles-title">
        <h2 id="module-articles-title">Các bài viết</h2>
        <ol className="reader-content-list">
          {module.articles.map((article) => (
            <li key={article.id} className="reader-content-entry">
              <p className="reader-content-context">Bài {article.position}</p>
              <h3>
                <Link to={articlePath(article.slug)}>{article.title}</Link>
              </h3>
              <p className="reader-content-summary">{article.summary}</p>
            </li>
          ))}
        </ol>
      </section>
    </section>
  )
}
