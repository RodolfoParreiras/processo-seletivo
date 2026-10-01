-- Documentos do Processo (docs/DECISOES.md): publicações anexadas pelo administrador com nome livre.
-- Não há exclusão: a retirada da página pública é registrada com justificativa.
CREATE TABLE process_document (
    id                      UUID          NOT NULL,
    process_id              UUID          NOT NULL,
    name                    VARCHAR(200)  NOT NULL,
    file_id                 UUID          NOT NULL,
    published_at            TIMESTAMPTZ   NOT NULL,
    published_by_account_id UUID          NOT NULL,
    withdrawn_at            TIMESTAMPTZ,
    withdrawn_by_account_id UUID,
    withdrawal_reason       VARCHAR(1000),
    CONSTRAINT process_document_pk PRIMARY KEY (id),
    CONSTRAINT process_document_process_fk FOREIGN KEY (process_id) REFERENCES selection_process (id),
    CONSTRAINT process_document_file_fk FOREIGN KEY (file_id) REFERENCES stored_file (id),
    CONSTRAINT process_document_file_uk UNIQUE (file_id),
    CONSTRAINT process_document_published_by_fk FOREIGN KEY (published_by_account_id) REFERENCES user_account (id),
    CONSTRAINT process_document_withdrawn_by_fk FOREIGN KEY (withdrawn_by_account_id) REFERENCES user_account (id),
    CONSTRAINT process_document_withdrawal_ck CHECK (
        (withdrawn_at IS NULL) = (withdrawn_by_account_id IS NULL)
        AND (withdrawn_at IS NULL) = (withdrawal_reason IS NULL))
);

CREATE INDEX process_document_process_ix ON process_document (process_id, published_at);

-- Publicações não são excluídas pela aplicação.
REVOKE DELETE, TRUNCATE ON process_document FROM "${appUser}";
