-- Deferimento/indeferimento (docs/DECISOES.md, fase 6). Não há situação "Em análise".
ALTER TABLE application DROP CONSTRAINT application_status_ck;
ALTER TABLE application
    ADD CONSTRAINT application_status_ck CHECK (status IN ('RASCUNHO', 'RECEBIDA', 'DEFERIDA', 'INDEFERIDA'));

-- Justificativa vigente, exibida ao candidato. O histórico completo fica em application_decision.
ALTER TABLE application ADD COLUMN decision_reason VARCHAR(2000);

-- Histórico de decisões: somente inserção (ESPECIFICACAO §21 e §36, AI_RULES §69).
CREATE TABLE application_decision (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY,
    application_id         UUID          NOT NULL,
    from_status            VARCHAR(20)   NOT NULL,
    to_status              VARCHAR(20)   NOT NULL,
    reason                 VARCHAR(2000),
    decided_by_account_id  UUID          NOT NULL,
    decided_at             TIMESTAMPTZ   NOT NULL,
    CONSTRAINT application_decision_pk PRIMARY KEY (id),
    CONSTRAINT application_decision_application_fk FOREIGN KEY (application_id) REFERENCES application (id),
    CONSTRAINT application_decision_account_fk FOREIGN KEY (decided_by_account_id) REFERENCES user_account (id),
    CONSTRAINT application_decision_to_status_ck CHECK (to_status IN ('DEFERIDA', 'INDEFERIDA')),
    CONSTRAINT application_decision_denial_reason_ck CHECK (to_status <> 'INDEFERIDA' OR reason IS NOT NULL)
);

CREATE INDEX application_decision_application_ix ON application_decision (application_id, decided_at);

REVOKE UPDATE, DELETE, TRUNCATE ON application_decision FROM "${appUser}";

-- Acesso a dados de pessoa com deficiência restrito a quem precisa (ESPECIFICACAO §9 e §66).
INSERT INTO permission (code, description)
VALUES ('DADOS_PCD_VISUALIZAR', 'Visualizar dados de pessoa com deficiência e necessidade de adaptações');

INSERT INTO role_permission (role_code, permission_code) VALUES ('ADMINISTRADOR_GERAL', 'DADOS_PCD_VISUALIZAR');
