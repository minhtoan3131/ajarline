CREATE TABLE topic
(
    id          uuid                     NOT NULL,
    slug        varchar(160)             NOT NULL,
    name        varchar(200)             NOT NULL,
    description text,
    active      boolean                  NOT NULL,
    created_at  timestamp with time zone NOT NULL,
    updated_at  timestamp with time zone NOT NULL,

    CONSTRAINT pk_topic PRIMARY KEY (id),
    CONSTRAINT uk_topic_slug UNIQUE (slug),
    CONSTRAINT ck_topic_slug_not_blank CHECK ( btrim(slug) <> '' ),
    CONSTRAINT ck_topic_slug_lowercase CHECK ( slug = lower(slug) )
);

COMMENT ON TABLE topic IS
    'Stores the Topic catalog. A Topic labels content but is not a public result entity.';
COMMENT ON COLUMN topic.id IS
    'Stable UUID assigned by the application.';
COMMENT ON COLUMN topic.slug IS
    'Lowercase route identifier. Unique and never changed after creation.';
COMMENT ON COLUMN topic.name IS
    'Display name. Not unique.';
COMMENT ON COLUMN topic.active IS
    'When false, the Topic cannot be assigned to more content. Existing assignments stay valid.';
COMMENT ON COLUMN topic.updated_at IS
    'Time of the latest effective change to name, description or active.';

CREATE TABLE content_topic
(
    content_id uuid NOT NULL,
    topic_id   uuid NOT NULL,

    CONSTRAINT pk_content_topic PRIMARY KEY (content_id, topic_id),
    CONSTRAINT fk_content_topic_content_id FOREIGN KEY (content_id)
        REFERENCES content_item (id) ON DELETE CASCADE,
    CONSTRAINT fk_content_topic_topic_id FOREIGN KEY (topic_id)
        REFERENCES topic (id) ON DELETE RESTRICT
);

CREATE INDEX ix_content_topic_topic_id_content_id
    ON content_topic (topic_id, content_id);

COMMENT ON TABLE content_topic IS
    'Direct assignment of a Topic to one content item. Not inherited by child content.';
COMMENT ON COLUMN content_topic.content_id IS
    'Deleting the content item removes its assignments.';
COMMENT ON COLUMN content_topic.topic_id IS
    'A Topic that still has assignments cannot be deleted.';
