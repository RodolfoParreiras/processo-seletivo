-- MFA (TOTP) obrigatório para contas administrativas (ESPECIFICACAO §76).
-- O segredo é cifrado na aplicação (AES-GCM) com chave fora do banco e do código (§47).
ALTER TABLE user_account ADD COLUMN mfa_secret_encrypted VARCHAR(200);
ALTER TABLE user_account ADD COLUMN mfa_enabled_at TIMESTAMPTZ;
-- Último passo de tempo aceito: impede reutilizar o mesmo código.
ALTER TABLE user_account ADD COLUMN mfa_last_used_step BIGINT;
ALTER TABLE user_account ADD COLUMN last_login_at TIMESTAMPTZ;
ALTER TABLE user_account ADD CONSTRAINT user_account_mfa_ck CHECK (
    (mfa_secret_encrypted IS NULL) = (mfa_enabled_at IS NULL));

-- Perfis do sistema não podem ser alterados pela interface.
ALTER TABLE role ADD COLUMN system_role BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE role SET system_role = TRUE WHERE code = 'ADMINISTRADOR_GERAL';
CREATE UNIQUE INDEX role_name_uk ON role (lower(name));

-- Consulta da auditoria filtrada por ação e período.
CREATE INDEX audit_log_action_ix ON audit_log (action, occurred_at);
