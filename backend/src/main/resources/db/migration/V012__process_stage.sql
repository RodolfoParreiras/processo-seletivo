-- Etapa de divulgação definida manualmente pelo administrador (docs/DECISOES.md), separada da situação
-- automática das inscrições. Substitui as situações internas de resultado preliminar/definitivo.
ALTER TABLE selection_process ADD COLUMN stage VARCHAR(30);

-- Processos que estavam em situação de resultado passam a "inscrições encerradas" com a etapa equivalente.
UPDATE selection_process SET stage = 'RESULTADO_PRELIMINAR', status = 'INSCRICOES_ENCERRADAS'
 WHERE status = 'RESULTADO_PRELIMINAR';
UPDATE selection_process SET stage = 'RESULTADO_FINAL', status = 'INSCRICOES_ENCERRADAS'
 WHERE status = 'RESULTADO_DEFINITIVO';
UPDATE selection_process SET status_before_suspension = 'INSCRICOES_ENCERRADAS'
 WHERE status_before_suspension IN ('RESULTADO_PRELIMINAR', 'RESULTADO_DEFINITIVO');
UPDATE selection_process SET stage = 'EDITAL_DISPONIVEL' WHERE status <> 'RASCUNHO' AND stage IS NULL;

ALTER TABLE selection_process DROP CONSTRAINT selection_process_status_ck;
ALTER TABLE selection_process ADD CONSTRAINT selection_process_status_ck CHECK (status IN (
    'RASCUNHO', 'PUBLICADO', 'INSCRICOES_ABERTAS', 'INSCRICOES_ENCERRADAS', 'ARQUIVADO', 'SUSPENSO', 'CANCELADO'));
ALTER TABLE selection_process ADD CONSTRAINT selection_process_stage_ck CHECK (
    stage IN ('EDITAL_DISPONIVEL', 'GABARITO_DISPONIVEL', 'RESULTADO_PRELIMINAR', 'RESULTADO_FINAL'));
-- Rascunho não tem etapa; processo publicado sempre tem.
ALTER TABLE selection_process ADD CONSTRAINT selection_process_stage_required_ck CHECK (
    (status = 'RASCUNHO') = (stage IS NULL));

-- Histórico de etapas: somente inserção.
CREATE TABLE process_stage_history (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY,
    process_id            UUID         NOT NULL,
    from_stage            VARCHAR(30),
    to_stage              VARCHAR(30)  NOT NULL,
    changed_by_account_id UUID         NOT NULL,
    changed_at            TIMESTAMPTZ  NOT NULL,
    CONSTRAINT process_stage_history_pk PRIMARY KEY (id),
    CONSTRAINT process_stage_history_process_fk FOREIGN KEY (process_id) REFERENCES selection_process (id),
    CONSTRAINT process_stage_history_account_fk FOREIGN KEY (changed_by_account_id) REFERENCES user_account (id)
);

CREATE INDEX process_stage_history_process_ix ON process_stage_history (process_id, changed_at);

REVOKE UPDATE, DELETE, TRUNCATE ON process_stage_history FROM "${appUser}";
