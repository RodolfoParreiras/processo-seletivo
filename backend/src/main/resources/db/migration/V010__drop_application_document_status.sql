-- Sem análise de documento a documento (docs/DECISOES.md, fase 6). Remoção autorizada pela Prefeitura em 01/10/2026.
ALTER TABLE application_document DROP CONSTRAINT application_document_status_ck;
ALTER TABLE application_document DROP COLUMN status;
