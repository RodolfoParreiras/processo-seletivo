-- Metadados de arquivos armazenados fora do banco (ESPECIFICACAO §23/§24).
-- storage_key é aleatório; o nome original é apenas informativo e nunca define o caminho físico.
CREATE TABLE stored_file (
    id                    UUID         NOT NULL,
    storage_key           VARCHAR(64)  NOT NULL,
    original_name         VARCHAR(255) NOT NULL,
    content_type          VARCHAR(100) NOT NULL,
    size_bytes            BIGINT       NOT NULL,
    sha256                CHAR(64)     NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL,
    created_by_account_id UUID,
    CONSTRAINT stored_file_pk PRIMARY KEY (id),
    CONSTRAINT stored_file_storage_key_uk UNIQUE (storage_key),
    CONSTRAINT stored_file_size_ck CHECK (size_bytes > 0),
    CONSTRAINT stored_file_created_by_fk FOREIGN KEY (created_by_account_id) REFERENCES user_account (id)
);

CREATE TABLE selection_process (
    id                            UUID         NOT NULL,
    process_number                VARCHAR(20)  NOT NULL,
    process_year                  INT          NOT NULL,
    title                         VARCHAR(200) NOT NULL,
    department                    VARCHAR(150) NOT NULL,
    status                        VARCHAR(30)  NOT NULL,
    status_before_suspension      VARCHAR(30),
    registration_start            TIMESTAMPTZ  NOT NULL,
    registration_end              TIMESTAMPTZ  NOT NULL,
    multiple_applications_allowed BOOLEAN      NOT NULL,
    title_evaluation_enabled      BOOLEAN      NOT NULL,
    published_at                  TIMESTAMPTZ,
    created_at                    TIMESTAMPTZ  NOT NULL,
    updated_at                    TIMESTAMPTZ  NOT NULL,
    version                       BIGINT       NOT NULL,
    CONSTRAINT selection_process_pk PRIMARY KEY (id),
    CONSTRAINT selection_process_number_year_uk UNIQUE (process_number, process_year),
    CONSTRAINT selection_process_status_ck CHECK (status IN (
        'RASCUNHO', 'PUBLICADO', 'INSCRICOES_ABERTAS', 'INSCRICOES_ENCERRADAS',
        'RESULTADO_PRELIMINAR', 'RESULTADO_DEFINITIVO', 'ARQUIVADO', 'SUSPENSO', 'CANCELADO')),
    CONSTRAINT selection_process_suspension_ck CHECK (
        (status = 'SUSPENSO') = (status_before_suspension IS NOT NULL)),
    CONSTRAINT selection_process_period_ck CHECK (registration_end > registration_start),
    CONSTRAINT selection_process_year_ck CHECK (process_year BETWEEN 2000 AND 2100)
);

-- Usado pela transição automática de estados e pela listagem pública.
CREATE INDEX selection_process_status_ix ON selection_process (status);

CREATE TABLE process_position (
    id         UUID         NOT NULL,
    process_id UUID         NOT NULL,
    name       VARCHAR(150) NOT NULL,
    vacancies  INT          NOT NULL,
    CONSTRAINT process_position_pk PRIMARY KEY (id),
    CONSTRAINT process_position_process_fk FOREIGN KEY (process_id) REFERENCES selection_process (id),
    CONSTRAINT process_position_vacancies_ck CHECK (vacancies > 0)
);

CREATE UNIQUE INDEX process_position_name_uk ON process_position (process_id, lower(name));

CREATE TABLE document_requirement (
    id          UUID         NOT NULL,
    process_id  UUID         NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    mandatory   BOOLEAN      NOT NULL,
    is_title    BOOLEAN      NOT NULL,
    CONSTRAINT document_requirement_pk PRIMARY KEY (id),
    CONSTRAINT document_requirement_process_fk FOREIGN KEY (process_id) REFERENCES selection_process (id)
);

CREATE UNIQUE INDEX document_requirement_name_uk ON document_requirement (process_id, lower(name));

-- Versões do edital (ESPECIFICACAO §15): versões publicadas nunca são substituídas sem histórico.
CREATE TABLE process_notice (
    id                      UUID         NOT NULL,
    process_id              UUID         NOT NULL,
    notice_version          INT          NOT NULL,
    file_id                 UUID         NOT NULL,
    status                  VARCHAR(20)  NOT NULL,
    change_reason           VARCHAR(1000),
    published_at            TIMESTAMPTZ,
    published_by_account_id UUID,
    created_at              TIMESTAMPTZ  NOT NULL,
    created_by_account_id   UUID         NOT NULL,
    CONSTRAINT process_notice_pk PRIMARY KEY (id),
    CONSTRAINT process_notice_version_uk UNIQUE (process_id, notice_version),
    CONSTRAINT process_notice_process_fk FOREIGN KEY (process_id) REFERENCES selection_process (id),
    CONSTRAINT process_notice_file_fk FOREIGN KEY (file_id) REFERENCES stored_file (id),
    CONSTRAINT process_notice_published_by_fk FOREIGN KEY (published_by_account_id) REFERENCES user_account (id),
    CONSTRAINT process_notice_created_by_fk FOREIGN KEY (created_by_account_id) REFERENCES user_account (id),
    CONSTRAINT process_notice_status_ck CHECK (status IN ('DRAFT', 'CURRENT', 'SUPERSEDED'))
);

-- Uma única versão vigente por processo.
CREATE UNIQUE INDEX process_notice_current_uk ON process_notice (process_id) WHERE status IN ('DRAFT', 'CURRENT');

-- Histórico de mudanças de estado; changed_by nulo indica transição automática pelas datas.
CREATE TABLE process_status_history (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY,
    process_id            UUID          NOT NULL,
    from_status           VARCHAR(30),
    to_status             VARCHAR(30)   NOT NULL,
    reason                VARCHAR(1000),
    changed_by_account_id UUID,
    changed_at            TIMESTAMPTZ   NOT NULL,
    CONSTRAINT process_status_history_pk PRIMARY KEY (id),
    CONSTRAINT process_status_history_process_fk FOREIGN KEY (process_id) REFERENCES selection_process (id),
    CONSTRAINT process_status_history_account_fk FOREIGN KEY (changed_by_account_id) REFERENCES user_account (id)
);

CREATE INDEX process_status_history_process_ix ON process_status_history (process_id, changed_at);

REVOKE UPDATE, DELETE, TRUNCATE ON process_status_history FROM "${appUser}";
