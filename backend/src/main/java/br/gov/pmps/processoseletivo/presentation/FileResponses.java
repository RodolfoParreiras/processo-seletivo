package br.gov.pmps.processoseletivo.presentation;

import br.gov.pmps.processoseletivo.application.file.FileDownload;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
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
    public static ResponseEntity<InputStreamResource> attachment(FileDownload download) {
        return ResponseEntity.ok()
                // Documentos pessoais não devem ficar em cache de navegador ou proxy.
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(download.fileName()).build().toString())
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .body(new InputStreamResource(download.content()));
    }
}
