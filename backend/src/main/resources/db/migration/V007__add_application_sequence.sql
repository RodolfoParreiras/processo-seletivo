-- Último sequencial de inscrição usado no processo; incrementado com a linha do processo bloqueada.
ALTER TABLE selection_process ADD COLUMN last_application_sequence INT NOT NULL DEFAULT 0;
