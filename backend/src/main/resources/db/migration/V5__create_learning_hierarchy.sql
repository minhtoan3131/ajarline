CREATE TABLE series
(
    content_id        uuid NOT NULL,
    overview_markdown text,

    CONSTRAINT pk_series PRIMARY KEY (content_id),
    CONSTRAINT fk_series_content_id FOREIGN KEY (content_id)
        REFERENCES content_item (id) ON DELETE CASCADE
);

COMMENT ON TABLE series IS
    'Stores Series-only data. The common record is in content_item.';
COMMENT ON COLUMN series.content_id IS
    'Primary key and subtype reference to content_item.';
COMMENT ON COLUMN series.overview_markdown IS
    'Markdown overview. Needed before the Series can be published.';

CREATE TABLE module
(
    content_id        uuid    NOT NULL,
    series_id         uuid    NOT NULL,
    position          integer NOT NULL,
    overview_markdown text,

    CONSTRAINT pk_module PRIMARY KEY (content_id),
    CONSTRAINT fk_module_content_id FOREIGN KEY (content_id)
        REFERENCES content_item (id) ON DELETE CASCADE,
    CONSTRAINT fk_module_series_id FOREIGN KEY (series_id)
        REFERENCES series (content_id) ON DELETE RESTRICT,
    CONSTRAINT ck_module_position CHECK (position >= 1),
    CONSTRAINT uk_module_series_position UNIQUE (series_id, position)
        DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE module IS
    'Stores Module-only data. A Module always belongs to exactly one Series.';
COMMENT ON COLUMN module.content_id IS
    'Primary key and subtype reference to content_item.';
COMMENT ON COLUMN module.series_id IS
    'The Series that owns this Module. Never null, also for a draft.';
COMMENT ON COLUMN module.position IS
    'Position in the Series, starting at 1. The application keeps positions as 1..N.';
COMMENT ON COLUMN module.overview_markdown IS
    'Markdown overview. Needed before the Module can be published.';
COMMENT ON CONSTRAINT uk_module_series_position ON module IS
    'Checked at commit, not per row. This lets the code shift siblings with several statements.';

CREATE TABLE module_article
(
    article_id uuid    NOT NULL,
    module_id  uuid    NOT NULL,
    position   integer NOT NULL,

    CONSTRAINT pk_module_article PRIMARY KEY (article_id),
    CONSTRAINT fk_module_article_article_id FOREIGN KEY (article_id)
        REFERENCES article (content_id) ON DELETE CASCADE,
    CONSTRAINT fk_module_article_module_id FOREIGN KEY (module_id)
        REFERENCES module (content_id) ON DELETE RESTRICT,
    CONSTRAINT ck_module_article_position CHECK (position >= 1),
    CONSTRAINT uk_module_article_module_position UNIQUE (module_id, position)
        DEFERRABLE INITIALLY DEFERRED
);

COMMENT ON TABLE module_article IS
    'Membership of an Article in a Module. An Article with no row here is standalone.';
COMMENT ON COLUMN module_article.article_id IS
    'Primary key. An Article is in at most one Module.';
COMMENT ON COLUMN module_article.module_id IS
    'The Module that contains the Article.';
COMMENT ON COLUMN module_article.position IS
    'Position in the Module, starting at 1. The application keeps positions as 1..N.';
COMMENT ON CONSTRAINT uk_module_article_module_position ON module_article IS
    'Checked at commit, not per row. See uk_module_series_position.';
