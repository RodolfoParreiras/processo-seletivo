package br.gov.pmps.processoseletivo.application.file;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * Formatos aceitos (ESPECIFICACAO §22). A extensão e o tipo declarado não bastam:
 * o conteúdo precisa começar com a assinatura (magic bytes) do formato.
 */
public enum AllowedFileType {
    PDF("application/pdf", Set.of("pdf"), new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D}),
    JPEG("image/jpeg", Set.of("jpg", "jpeg"), new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG("image/png", Set.of("png"), new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

    public static final int MAX_SIGNATURE_LENGTH = 8;

    private final String contentType;
    private final Set<String> extensions;
    private final byte[] signature;

    AllowedFileType(String contentType, Set<String> extensions, byte[] signature) {
        this.contentType = contentType;
        this.extensions = extensions;
        this.signature = signature;
    }

    public static Optional<AllowedFileType> fromExtension(String extension) {
        return Arrays.stream(values()).filter(type -> type.extensions.contains(extension)).findFirst();
    }

    public boolean matchesSignature(byte[] header, int length) {
        if (length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (header[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    public String contentType() {
        return contentType;
    }
}
