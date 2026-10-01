-- Somente o hash SHA-256 do token é armazenado; o valor original existe apenas no e-mail enviado.
CREATE TABLE password_reset_token (
    id              UUID        NOT NULL,
    user_account_id UUID        NOT NULL,
    token_hash      CHAR(64)    NOT NULL,
    purpose         VARCHAR(20) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    used_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT password_reset_token_pk PRIMARY KEY (id),
    CONSTRAINT password_reset_token_hash_uk UNIQUE (token_hash),
    CONSTRAINT password_reset_token_account_fk FOREIGN KEY (user_account_id) REFERENCES user_account (id),
    CONSTRAINT password_reset_token_purpose_ck CHECK (purpose IN ('PASSWORD_RESET', 'PASSWORD_SETUP'))
);

CREATE INDEX password_reset_token_account_ix ON password_reset_token (user_account_id);
