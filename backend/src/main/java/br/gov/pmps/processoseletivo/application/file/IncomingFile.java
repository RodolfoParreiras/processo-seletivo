package br.gov.pmps.processoseletivo.application.file;

import java.io.IOException;
import java.io.InputStream;

/** Arquivo recebido do usuário. Todos os campos são não confiáveis (AI_RULES §19). */
public interface IncomingFile {

    String originalName();

    String declaredContentType();

    long declaredSize();

    InputStream openStream() throws IOException;
}
