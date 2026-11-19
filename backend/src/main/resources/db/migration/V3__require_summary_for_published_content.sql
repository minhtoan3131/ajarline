ALTER TABLE content_item
    ADD CONSTRAINT ck_content_item_published_summary
        CHECK (status <> 'PUBLISHED' OR summary IS NOT NULL);

COMMENT ON CONSTRAINT ck_content_item_published_summary ON content_item IS
    'A PUBLISHED item must have a summary. The application checks this first; the database is the last guard.';
