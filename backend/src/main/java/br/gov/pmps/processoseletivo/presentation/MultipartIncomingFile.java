package br.gov.pmps.processoseletivo.presentation;

import br.gov.pmps.processoseletivo.application.file.IncomingFile;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

/** Adapta o upload HTTP para a camada de aplicação, sem expor tipos do Spring MVC. */
public record MultipartIncomingFile(MultipartFile file) implements IncomingFile {

    @Override
    public String originalName() {
        return file.getOriginalFilename();
    }

    @Override
    public String declaredContentType() {
        return file.getContentType();
    }

    @Override
    public long declaredSize() {
        return file.getSize();
    }

    @Override
    public InputStream openStream() throws IOException {
        return file.getInputStream();
    }
}
