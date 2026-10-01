-- Conta de acesso (autenticação), separada dos dados cadastrais (ESPECIFICACAO §51).
-- Candidato e administrador são contas distintas: o mesmo CPF pode ter uma de cada tipo.
CREATE TABLE user_account (
    id                    UUID         NOT NULL,
    account_type          VARCHAR(20)  NOT NULL,
    cpf                   CHAR(11)     NOT NULL,
    email                 VARCHAR(254) NOT NULL,
    password_hash         VARCHAR(255) NOT NULL,
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,
    password_changed_at   TIMESTAMPTZ  NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,
    version               BIGINT       NOT NULL,
    CONSTRAINT user_account_pk PRIMARY KEY (id),
    CONSTRAINT user_account_type_ck CHECK (account_type IN ('CANDIDATE', 'ADMIN')),
    CONSTRAINT user_account_cpf_ck CHECK (cpf ~ '^[0-9]{11}$'),
    CONSTRAINT user_account_failed_attempts_ck CHECK (failed_login_attempts >= 0),
    CONSTRAINT user_account_type_cpf_uk UNIQUE (account_type, cpf)
);

CREATE UNIQUE INDEX user_account_type_email_uk ON user_account (account_type, lower(email));

CREATE TABLE candidate (
    id              UUID         NOT NULL,
    user_account_id UUID         NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    birth_date      DATE         NOT NULL,
    mother_name     VARCHAR(150) NOT NULL,
    phone           VARCHAR(11)  NOT NULL,
    cep             CHAR(8)      NOT NULL,
    street          VARCHAR(150) NOT NULL,
    address_number  VARCHAR(10)  NOT NULL,
    complement      VARCHAR(60),
    neighborhood    VARCHAR(80)  NOT NULL,
    city            VARCHAR(80)  NOT NULL,
    uf              CHAR(2)      NOT NULL,
    has_disability  BOOLEAN      NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    version         BIGINT       NOT NULL,
    CONSTRAINT candidate_pk PRIMARY KEY (id),
    CONSTRAINT candidate_user_account_uk UNIQUE (user_account_id),
    CONSTRAINT candidate_user_account_fk FOREIGN KEY (user_account_id) REFERENCES user_account (id),
    CONSTRAINT candidate_phone_ck CHECK (phone ~ '^[0-9]{10,11}$'),
    CONSTRAINT candidate_cep_ck CHECK (cep ~ '^[0-9]{8}$'),
    CONSTRAINT candidate_uf_ck CHECK (uf ~ '^[A-Z]{2}$')
);

-- Necessidades de adaptação: dado sensível (ESPECIFICACAO §9), acesso restrito.
CREATE TABLE candidate_adaptation (
    candidate_id UUID        NOT NULL,
    adaptation   VARCHAR(30) NOT NULL,
    CONSTRAINT candidate_adaptation_pk PRIMARY KEY (candidate_id, adaptation),
    CONSTRAINT candidate_adaptation_candidate_fk FOREIGN KEY (candidate_id) REFERENCES candidate (id),
    CONSTRAINT candidate_adaptation_ck CHECK (adaptation IN ('LIBRAS_INTERPRETER', 'READING_ASSISTANCE', 'ENLARGED_TEST', 'NONE'))
);

CREATE TABLE administrator (
    id              UUID         NOT NULL,
    user_account_id UUID         NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    version         BIGINT       NOT NULL,
    CONSTRAINT administrator_pk PRIMARY KEY (id),
    CONSTRAINT administrator_user_account_uk UNIQUE (user_account_id),
    CONSTRAINT administrator_user_account_fk FOREIGN KEY (user_account_id) REFERENCES user_account (id)
);
