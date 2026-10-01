package br.gov.pmps.processoseletivo.presentation;

import br.gov.pmps.processoseletivo.application.usecase.process.ProcessQueryUseCase;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public final class FileResponses {

    private FileResponses() {
    }

    /**
     * Sempre como anexo e com CSP restritiva: um arquivo enviado por usuário nunca é interpretado
     * pelo navegador como página da aplicação.
     */
    public static ResponseEntity<InputStreamResource> attachment(ProcessQueryUseCase.FileDownload download) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(download.fileName()).build().toString())
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .body(new InputStreamResource(download.content()));
    }
}
