CREATE TABLE retrieval_concept
(
    id           uuid                     NOT NULL,
    key          varchar(160)             NOT NULL,
    display_name varchar(200)             NOT NULL,
    description  text,
    active       boolean                  NOT NULL,
    created_at   timestamp with time zone NOT NULL,
    updated_at   timestamp with time zone NOT NULL,

    CONSTRAINT pk_retrieval_concept PRIMARY KEY (id),
    CONSTRAINT uk_retrieval_concept_key UNIQUE (key),
    CONSTRAINT ck_retrieval_concept_key_not_blank CHECK ( btrim(key) <> '' ),
    CONSTRAINT ck_retrieval_concept_key_lowercase CHECK ( key = lower(key) ),
    CONSTRAINT ck_retrieval_concept_display_name_not_blank CHECK ( btrim(display_name) <> '' )
);

COMMENT ON TABLE retrieval_concept IS
    'Stores the retrieval vocabulary. A Concept groups expressions that mean the same idea. It is not a Topic and has no public page.';
COMMENT ON COLUMN retrieval_concept.id IS
    'Stable UUID assigned by the application.';
COMMENT ON COLUMN retrieval_concept.key IS
    'Lowercase stable identifier. Unique and never changed after creation.';
COMMENT ON COLUMN retrieval_concept.display_name IS
    'Display name. Not unique.';
COMMENT ON COLUMN retrieval_concept.active IS
    'When false, the Concept stays stored but search must not use it to expand a query.';
COMMENT ON COLUMN retrieval_concept.updated_at IS
    'Time of the latest effective change to name, description, active or the expression set.';

CREATE TABLE retrieval_concept_expression
(
    id                    uuid                     NOT NULL,
    concept_id            uuid                     NOT NULL,
    expression            varchar(300)             NOT NULL,
    normalized_expression text                     NOT NULL,
    language_hint         varchar(16),
    auto_expand           boolean                  NOT NULL,
    created_at            timestamp with time zone NOT NULL,
    updated_at            timestamp with time zone NOT NULL,

    CONSTRAINT pk_retrieval_concept_expression PRIMARY KEY (id),
    CONSTRAINT fk_retrieval_concept_expression_concept_id FOREIGN KEY (concept_id)
        REFERENCES retrieval_concept (id) ON DELETE CASCADE,
    CONSTRAINT uk_retrieval_concept_expression_normalized UNIQUE (concept_id, normalized_expression),
    CONSTRAINT ck_retrieval_concept_expression_language_hint
        CHECK ( language_hint IN ('vi', 'en', 'technical', 'mixed') ),
    CONSTRAINT ck_retrieval_concept_expression_expression_not_blank CHECK ( btrim(expression) <> '' ),
    CONSTRAINT ck_retrieval_concept_expression_normalized_not_blank CHECK ( btrim(normalized_expression) <> '' )
);

COMMENT ON TABLE retrieval_concept_expression IS
    'One way to say a Concept. Deleting the Concept deletes its expressions.';
COMMENT ON COLUMN retrieval_concept_expression.expression IS
    'Text as the Admin typed it, without leading or trailing spaces.';
COMMENT ON COLUMN retrieval_concept_expression.normalized_expression IS
    'Text used to compare and match. Computed by the application. The same value may appear in different Concepts.';
COMMENT ON COLUMN retrieval_concept_expression.language_hint IS
    'Optional language hint: vi, en, technical or mixed. Null means not given.';
COMMENT ON COLUMN retrieval_concept_expression.auto_expand IS
    'When true, search may expand a query with this expression if it points to one active Concept.';

CREATE TABLE retrieval_vocabulary_state
(
    id         smallint                 NOT NULL,
    version    bigint                   NOT NULL,
    updated_at timestamp with time zone NOT NULL,

    CONSTRAINT pk_retrieval_vocabulary_state PRIMARY KEY (id),
    CONSTRAINT ck_retrieval_vocabulary_state_singleton CHECK ( id = 1 ),
    CONSTRAINT ck_retrieval_vocabulary_state_version_positive CHECK ( version >= 1 )
);

COMMENT ON TABLE retrieval_vocabulary_state IS
    'Holds the one vocabulary version row. The version grows by one for each real change to a Concept or its expressions.';
COMMENT ON COLUMN retrieval_vocabulary_state.id IS
    'Always 1. The check constraint allows no other row.';

INSERT INTO retrieval_vocabulary_state (id, version, updated_at)
VALUES (1, 1, now());
