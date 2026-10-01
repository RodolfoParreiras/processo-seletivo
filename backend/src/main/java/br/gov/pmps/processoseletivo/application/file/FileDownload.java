package br.gov.pmps.processoseletivo.application.file;

import java.io.InputStream;

/** Arquivo a ser entregue ao usuário. O nome é gerado pelo sistema, nunca o nome enviado no upload. */
public record FileDownload(String fileName, String contentType, long sizeBytes, InputStream content) {
}
