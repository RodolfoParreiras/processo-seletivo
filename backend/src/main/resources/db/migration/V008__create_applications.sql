-- Inscrições (ESPECIFICACAO §18 a §21).
CREATE TABLE application (
    id                 UUID         NOT NULL,
    process_id         UUID         NOT NULL,
    position_id        UUID         NOT NULL,
    candidate_id       UUID         NOT NULL,
    -- Chave da regra de inscrição única: id do processo (uma por processo) ou processo:cargo (uma por cargo).
    uniqueness_key     VARCHAR(80)  NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    sequence_number    INT,
    application_number VARCHAR(40),
    verification_code  VARCHAR(20),
    confirmed_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    version            BIGINT       NOT NULL,
    CONSTRAINT application_pk PRIMARY KEY (id),
    CONSTRAINT application_process_fk FOREIGN KEY (process_id) REFERENCES selection_process (id),
    CONSTRAINT application_position_fk FOREIGN KEY (position_id) REFERENCES process_position (id),
    CONSTRAINT application_candidate_fk FOREIGN KEY (candidate_id) REFERENCES candidate (id),
    CONSTRAINT application_status_ck CHECK (status IN ('RASCUNHO', 'RECEBIDA', 'EM_ANALISE', 'DEFERIDA', 'INDEFERIDA')),
    CONSTRAINT application_confirmed_ck CHECK (
        (status = 'RASCUNHO') = (application_number IS NULL)
        AND (application_number IS NULL) = (confirmed_at IS NULL)
        AND (application_number IS NULL) = (verification_code IS NULL)
        AND (application_number IS NULL) = (sequence_number IS NULL)),
    -- Garantia final da inscrição única, inclusive com requisições simultâneas (ESPECIFICACAO §19).
    CONSTRAINT application_candidate_uniqueness_uk UNIQUE (candidate_id, uniqueness_key),
    CONSTRAINT application_number_uk UNIQUE (application_number),
    CONSTRAINT application_sequence_uk UNIQUE (process_id, sequence_number),
    CONSTRAINT application_verification_code_uk UNIQUE (verification_code)
);

CREATE INDEX application_candidate_ix ON application (candidate_id);
CREATE INDEX application_process_status_ix ON application (process_id, status);

-- Dados do candidato no momento da confirmação (ESPECIFICACAO §8). Nunca alterados depois.
CREATE TABLE application_snapshot (
    application_id  UUID         NOT NULL,
    cpf             CHAR(11)     NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    birth_date      DATE         NOT NULL,
    mother_name     VARCHAR(150) NOT NULL,
    email           VARCHAR(254) NOT NULL,
    phone           VARCHAR(11)  NOT NULL,
    cep             CHAR(8)      NOT NULL,
    street          VARCHAR(150) NOT NULL,
    address_number  VARCHAR(10)  NOT NULL,
    complement      VARCHAR(60),
    neighborhood    VARCHAR(80)  NOT NULL,
    city            VARCHAR(80)  NOT NULL,
    uf              CHAR(2)      NOT NULL,
    has_disability  BOOLEAN      NOT NULL,
    adaptations     VARCHAR(200),
    position_name   VARCHAR(150) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT application_snapshot_pk PRIMARY KEY (application_id),
    CONSTRAINT application_snapshot_application_fk FOREIGN KEY (application_id) REFERENCES application (id)
);

REVOKE UPDATE, DELETE, TRUNCATE ON application_snapshot FROM "${appUser}";

CREATE TABLE application_document (
    id             UUID        NOT NULL,
    application_id UUID        NOT NULL,
    requirement_id UUID        NOT NULL,
    file_id        UUID        NOT NULL,
    status         VARCHAR(20) NOT NULL,
    uploaded_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT application_document_pk PRIMARY KEY (id),
    CONSTRAINT application_document_application_fk FOREIGN KEY (application_id) REFERENCES application (id),
    CONSTRAINT application_document_requirement_fk FOREIGN KEY (requirement_id) REFERENCES document_requirement (id),
    CONSTRAINT application_document_file_fk FOREIGN KEY (file_id) REFERENCES stored_file (id),
    CONSTRAINT application_document_file_uk UNIQUE (file_id),
    CONSTRAINT application_document_status_ck CHECK (status IN ('PENDENTE', 'APROVADO', 'REJEITADO'))
);

CREATE INDEX application_document_application_ix ON application_document (application_id);

-- Fila de e-mails: a falha de envio não desfaz a operação e fica registrada para nova tentativa (§28).
CREATE TABLE email_outbox (
    id              UUID         NOT NULL,
    recipient       VARCHAR(254) NOT NULL,
    subject         VARCHAR(200) NOT NULL,
    body            TEXT         NOT NULL,
    status          VARCHAR(10)  NOT NULL,
    attempts        INT          NOT NULL DEFAULT 0,
    last_error      VARCHAR(200),
    next_attempt_at TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    sent_at         TIMESTAMPTZ,
    CONSTRAINT email_outbox_pk PRIMARY KEY (id),
    CONSTRAINT email_outbox_status_ck CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX email_outbox_pending_ix ON email_outbox (next_attempt_at) WHERE status = 'PENDING';
