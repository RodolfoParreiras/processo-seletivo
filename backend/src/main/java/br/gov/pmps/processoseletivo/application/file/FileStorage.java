package br.gov.pmps.processoseletivo.application.file;

import java.io.InputStream;

/** Armazenamento privado de conteúdo binário, endereçado por chave aleatória gerada pela implementação. */
public interface FileStorage {

    record StoredContent(String key, long sizeBytes, String sha256) {
    }

    /** @throws FileTooLargeException se o conteúdo ultrapassar {@code maxBytes}; nada fica gravado */
    StoredContent store(InputStream content, long maxBytes);

    InputStream open(String key);

    void delete(String key);

    class FileTooLargeException extends RuntimeException {
        public FileTooLargeException() {
            super("Arquivo acima do tamanho permitido");
        }
    }
}
