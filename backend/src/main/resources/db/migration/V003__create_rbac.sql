-- RBAC granular (ESPECIFICACAO §6). Permissões são fixas no código; papéis agrupam permissões.
CREATE TABLE permission (
    code        VARCHAR(60)  NOT NULL,
    description VARCHAR(200) NOT NULL,
    CONSTRAINT permission_pk PRIMARY KEY (code)
);

CREATE TABLE role (
    code VARCHAR(60)  NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT role_pk PRIMARY KEY (code)
);

CREATE TABLE role_permission (
    role_code       VARCHAR(60) NOT NULL,
    permission_code VARCHAR(60) NOT NULL,
    CONSTRAINT role_permission_pk PRIMARY KEY (role_code, permission_code),
    CONSTRAINT role_permission_role_fk FOREIGN KEY (role_code) REFERENCES role (code),
    CONSTRAINT role_permission_permission_fk FOREIGN KEY (permission_code) REFERENCES permission (code)
);

CREATE TABLE user_account_role (
    user_account_id UUID        NOT NULL,
    role_code       VARCHAR(60) NOT NULL,
    CONSTRAINT user_account_role_pk PRIMARY KEY (user_account_id, role_code),
    CONSTRAINT user_account_role_account_fk FOREIGN KEY (user_account_id) REFERENCES user_account (id),
    CONSTRAINT user_account_role_role_fk FOREIGN KEY (role_code) REFERENCES role (code)
);

INSERT INTO permission (code, description) VALUES
    ('PROCESSO_VISUALIZAR',   'Visualizar processos seletivos'),
    ('PROCESSO_CRIAR',        'Criar processos seletivos'),
    ('PROCESSO_EDITAR',       'Editar processos seletivos'),
    ('PROCESSO_PUBLICAR',     'Publicar processos seletivos'),
    ('PROCESSO_ENCERRAR',     'Encerrar processos seletivos'),
    ('INSCRICAO_VISUALIZAR',  'Visualizar inscrições'),
    ('INSCRICAO_EDITAR',      'Editar inscrições'),
    ('INSCRICAO_DEFERIR',     'Deferir inscrições'),
    ('INSCRICAO_INDEFERIR',   'Indeferir inscrições'),
    ('DOCUMENTO_VISUALIZAR',  'Visualizar documentos'),
    ('DOCUMENTO_ANALISAR',    'Analisar documentos'),
    ('DOCUMENTO_APROVAR',     'Aprovar documentos'),
    ('DOCUMENTO_REJEITAR',    'Rejeitar documentos'),
    ('PONTUACAO_VISUALIZAR',  'Visualizar pontuações'),
    ('PONTUACAO_EDITAR',      'Editar pontuações'),
    ('EXPORTACAO_GERAR',      'Gerar exportações'),
    ('RESULTADO_PUBLICAR',    'Publicar resultados'),
    ('RESULTADO_EDITAR',      'Editar resultados'),
    ('RELATORIO_GERAR',       'Gerar relatórios'),
    ('USUARIO_GERENCIAR',     'Gerenciar usuários administrativos'),
    ('PERMISSAO_GERENCIAR',   'Gerenciar perfis e permissões'),
    ('AUDITORIA_VISUALIZAR',  'Visualizar auditoria');

INSERT INTO role (code, name) VALUES ('ADMINISTRADOR_GERAL', 'Administrador Geral');

INSERT INTO role_permission (role_code, permission_code)
SELECT 'ADMINISTRADOR_GERAL', code FROM permission;
