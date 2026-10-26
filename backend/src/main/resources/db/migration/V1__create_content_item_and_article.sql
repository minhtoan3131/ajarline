CREATE TABLE content_item
(
    id             uuid                     NOT NULL,
    type           varchar(16)              NOT NULL,
    slug           varchar(180)             NOT NULL,
    title          varchar(300)             NOT NULL,
    summary        text,
    status         varchar(16)              NOT NULL,
    revision       bigint                   NOT NULL,
    published_at   timestamp with time zone,
    unpublished_at timestamp with time zone,
    created_at     timestamp with time zone NOT NULL,
    updated_at     timestamp with time zone NOT NULL,

    CONSTRAINT pk_content_item PRIMARY KEY (id),
    CONSTRAINT uk_content_item_type_slug UNIQUE (type, slug),
    CONSTRAINT ck_content_item_type CHECK ( type IN ('ARTICLE')),
    CONSTRAINT ck_content_item_status CHECK ( status IN ('DRAFT', 'PUBLISHED', 'UNPUBLISHED') ),
    CONSTRAINT ck_content_item_revision CHECK ( revision >= 1 ),
    CONSTRAINT ck_content_item_slug_not_blank CHECK ( btrim(slug) <> '' ),
    CONSTRAINT ck_content_item_slug_lowercase CHECK ( slug = lower(slug) )
);

COMMENT
ON TABLE content_item IS
        'Stores canonical state shared by public result entities.';
COMMENT ON COLUMN content_item.id IS
    'Stable UUID assigned by the application.';
COMMENT
ON COLUMN content_item.type IS
    'Canonical content type. V02 activates ARTICLE only.';
COMMENT
ON COLUMN content_item.slug IS
    'Lowercase route identifier unique within one content type.';
COMMENT
ON COLUMN content_item.status IS
    'Canonical publication state.';
COMMENT
ON COLUMN content_item.revision IS
    'Semantic revision managed by the application, not by ORM auto-versioning.';
COMMENT
ON COLUMN content_item.published_at IS
    'Time of first publication; null before the first publish.';
COMMENT
ON COLUMN content_item.unpublished_at IS
    'Time of the latest unpublish action; null when never unpublished.';
COMMENT
ON COLUMN content_item.created_at IS
    'Time when the canonical content item was created.';
COMMENT
ON COLUMN content_item.updated_at IS
    'Time of the latest effective canonical change.';

CREATE TABLE article
(
    content_id    uuid NOT NULL,
    body_markdown text,
    CONSTRAINT pk_article PRIMARY KEY (content_id),
    CONSTRAINT fk_article_content_id FOREIGN KEY (content_id) REFERENCES content_item (id) ON DELETE CASCADE
);
COMMENT
ON TABLE article IS
    'Stores Article-specific canonical content.';
COMMENT
ON COLUMN article.content_id IS
    'Primary key and subtype reference to content_item.';
COMMENT
ON COLUMN article.body_markdown IS
    'Canonical Markdown body; drafts may leave it null.';
