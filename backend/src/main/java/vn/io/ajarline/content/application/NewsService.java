package vn.io.ajarline.content.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.ajarline.content.contract.ContentRecordInput;
import vn.io.ajarline.content.contract.ContentRecordReader;
import vn.io.ajarline.content.contract.ContentRecordView;
import vn.io.ajarline.content.contract.ContentRecordWriter;
import vn.io.ajarline.content.contract.ContentType;
import vn.io.ajarline.content.contract.EditableRecord;
import vn.io.ajarline.content.contract.NewsContentInput;
import vn.io.ajarline.content.contract.NewsCreator;
import vn.io.ajarline.content.contract.NewsEditor;
import vn.io.ajarline.content.contract.NewsReader;
import vn.io.ajarline.content.contract.NewsSource;
import vn.io.ajarline.content.contract.NewsView;
import vn.io.ajarline.content.contract.PublicNewsView;
import vn.io.ajarline.content.contract.PublicationStatus;
import vn.io.ajarline.content.contract.TopicAssignmentPort;
import vn.io.ajarline.content.infrastructure.persistence.ContentItemJpaRepository;
import vn.io.ajarline.content.infrastructure.persistence.NewsEntity;
import vn.io.ajarline.content.infrastructure.persistence.NewsJpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Creates, reads and edits News.
 *
 * <p>The common record is written through the content port. The body and the source are written
 * here. Both are written in one transaction. It also reads a published News item for the public.</p>
 */
@Service
public class NewsService implements NewsCreator, NewsReader, NewsEditor {
    private final ContentRecordReader recordReader;
    private final ContentRecordWriter recordWriter;
    private final NewsPolicy policy;
    private final NewsJpaRepository newsRepository;
    private final ContentItemJpaRepository contentRepository;
    private final TopicAssignmentPort topicAssignment;

    public NewsService(ContentRecordReader recordReader,
                       ContentRecordWriter recordWriter,
                       NewsPolicy policy,
                       NewsJpaRepository newsRepository,
                       ContentItemJpaRepository contentRepository,
                       TopicAssignmentPort topicAssignment) {
        this.recordReader = recordReader;
        this.recordWriter = recordWriter;
        this.policy = policy;
        this.newsRepository = newsRepository;
        this.contentRepository = contentRepository;
        this.topicAssignment = topicAssignment;
    }

    @Override
    @Transactional
    public NewsView create(NewsContentInput input) {
        ContentRecordInput record = recordWriter.normalize(input.record());
        NewsSource source = policy.normalizeSource(input.source());
        // The common row must exist first, because the news row points at it.
        ContentRecordView created = recordWriter.create(ContentType.NEWS, record);
        NewsEntity news = new NewsEntity(
                created.id(), input.bodyMarkdown(), source.name(), source.url(), source.publishedAt());

        newsRepository.save(news);
        return toView(created, news);
    }

    @Override
    @Transactional(readOnly = true)
    public NewsView get(UUID id) {
        ContentRecordView record = recordReader.get(id, ContentType.NEWS);
        return toView(record, requireNews(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PublicNewsView> findPublishedBySlug(String slug) {
        if (slug == null) {
            return Optional.empty();
        }
        // One query finds the type, the slug and the status together. So a draft, an
        // unpublished item and a slug of another type are all just "not found".
        return contentRepository.findByTypeAndSlugAndStatus(
                        ContentType.NEWS, slug, PublicationStatus.PUBLISHED)
                .map(item -> {
                    NewsEntity news = requireNews(item.getId());
                    return new PublicNewsView(
                            item.getId(),
                            item.getSlug(),
                            item.getTitle(),
                            item.getSummary(),
                            news.getBodyMarkdown(),
                            new NewsSource(
                                    news.getSourceName(),
                                    news.getSourceUrl(),
                                    news.getSourcePublishedAt()),
                            topicAssignment.topicLabelsOf(item.getId()),
                            item.getPublishedAt(),
                            item.getUpdatedAt());
                });
    }

    @Override
    @Transactional
    public NewsView replace(UUID id, long expectedRevision, NewsContentInput input) {
        ContentRecordInput record = recordWriter.normalize(input.record());
        NewsSource source = policy.normalizeSource(input.source());
        // Lock the row before any other read. Then no stale copy of it is in the
        // persistence context when the revision is compared.
        EditableRecord editable = recordWriter.lockForEdit(
                id, ContentType.NEWS, expectedRevision, record);
        NewsEntity news = requireNews(id);

        // A public item is read by everyone as soon as it is saved, so it must stay complete.
        if (editable.current().status() == PublicationStatus.PUBLISHED) {
            policy.requirePublishable(
                    record.summary(), input.bodyMarkdown(), source.name(), source.url());
        }

        boolean hasOwnPartChanged = !news.hasSameBody(input.bodyMarkdown())
                || !news.hasSameSource(source.name(), source.url(), source.publishedAt());
        ContentRecordView saved = recordWriter.replace(editable, record, hasOwnPartChanged);
        if (hasOwnPartChanged) {
            news.replaceBody(input.bodyMarkdown());
            news.replaceSource(source.name(), source.url(), source.publishedAt());
        }
        return toView(saved, news);
    }

    private NewsEntity requireNews(UUID id) {
        return newsRepository.findById(id).orElseThrow(
                () -> new IllegalStateException("News subtype row is missing for " + id));
    }

    private NewsView toView(ContentRecordView record, NewsEntity news) {
        return new NewsView(
                record,
                news.getBodyMarkdown(),
                new NewsSource(news.getSourceName(), news.getSourceUrl(), news.getSourcePublishedAt()),
                recordReader.topicIdsOf(record.id()));
    }
}
