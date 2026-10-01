-- Auditoria somente de inserção (ESPECIFICACAO §36 e §78).
-- O usuário da aplicação perde UPDATE/DELETE/TRUNCATE concedidos pelos privilégios padrão do schema.
CREATE TABLE audit_log (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY,
    occurred_at           TIMESTAMPTZ  NOT NULL,
    action                VARCHAR(60)  NOT NULL,
    outcome               VARCHAR(10)  NOT NULL,
    actor_user_account_id UUID,
    target_type           VARCHAR(40),
    target_id             VARCHAR(64),
    ip_address            VARCHAR(45),
    details               JSONB,
    CONSTRAINT audit_log_pk PRIMARY KEY (id),
    CONSTRAINT audit_log_outcome_ck CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT audit_log_actor_fk FOREIGN KEY (actor_user_account_id) REFERENCES user_account (id)
);

CREATE INDEX audit_log_occurred_at_ix ON audit_log (occurred_at);
CREATE INDEX audit_log_actor_ix ON audit_log (actor_user_account_id);
CREATE INDEX audit_log_target_ix ON audit_log (target_type, target_id);

REVOKE UPDATE, DELETE, TRUNCATE ON audit_log FROM "${appUser}";
