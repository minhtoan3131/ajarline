ALTER TABLE content_item
    DROP CONSTRAINT ck_content_item_type;

ALTER TABLE content_item
    ADD CONSTRAINT ck_content_item_type CHECK (type IN ('ARTICLE', 'SERIES', 'MODULE', 'NEWS'));

COMMENT ON COLUMN content_item.type IS
    'Content type. ARTICLE, SERIES, MODULE and NEWS are active.';

CREATE TABLE news
(
    content_id          uuid NOT NULL,
    body_markdown       text,
    source_name         varchar(300),
    source_url          varchar(2048),
    source_published_at timestamp with time zone,

    CONSTRAINT pk_news PRIMARY KEY (content_id),
    CONSTRAINT fk_news_content_id FOREIGN KEY (content_id)
        REFERENCES content_item (id) ON DELETE CASCADE,
    CONSTRAINT ck_news_source_url_scheme
        CHECK (source_url IS NULL OR source_url ~* '^https?://')
);

COMMENT ON TABLE news IS
    'Stores News-only data. The common record is in content_item.';
COMMENT ON COLUMN news.content_id IS
    'Primary key and subtype reference to content_item.';
COMMENT ON COLUMN news.body_markdown IS
    'Markdown body written by Ajarline; a draft may leave it null.';
COMMENT ON COLUMN news.source_name IS
    'Name of the outside source. Needed before the News can be published.';
COMMENT ON COLUMN news.source_url IS
    'Absolute http or https URL of the outside source. Needed before the News can be published.';
COMMENT ON COLUMN news.source_published_at IS
    'Time when the source published the story. Optional, and never replaced by the Ajarline publish time.';
COMMENT ON CONSTRAINT ck_news_source_url_scheme ON news IS
    'Last guard: only http and https. The application checks the full URL shape first.';
