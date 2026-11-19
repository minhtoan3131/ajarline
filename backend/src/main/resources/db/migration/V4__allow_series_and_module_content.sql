ALTER TABLE content_item
    DROP CONSTRAINT ck_content_item_type;

ALTER TABLE content_item
    ADD CONSTRAINT ck_content_item_type CHECK (type IN ('ARTICLE', 'SERIES', 'MODULE'));

COMMENT ON COLUMN content_item.type IS
    'Content type. ARTICLE, SERIES and MODULE are active. NEWS comes later.';
