package br.gov.pmps.processoseletivo.application.file;

import br.gov.pmps.processoseletivo.domain.model.StoredFile;
import br.gov.pmps.processoseletivo.domain.repository.StoredFileRepository;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Valida e armazena arquivos enviados (AI_RULES §19/§20, ESPECIFICACAO §22/§23):
 * extensão, tipo declarado, assinatura do conteúdo e tamanho; nome físico aleatório.
 * Se a transação for desfeita, o conteúdo gravado é removido.
 */
@Service
public class FileUploadService {

    private static final int MAX_ORIGINAL_NAME_LENGTH = 255;

    private final FileStorage fileStorage;
    private final StoredFileRepository storedFileRepository;
    private final Clock clock;

    public FileUploadService(FileStorage fileStorage, StoredFileRepository storedFileRepository, Clock clock) {
        this.fileStorage = fileStorage;
        this.storedFileRepository = storedFileRepository;
        this.clock = clock;
    }

    public StoredFile store(IncomingFile file, Set<AllowedFileType> allowedTypes, long maxBytes, UUID accountId) {
        String allowedDescription = allowedTypes.stream().map(Enum::name).sorted().collect(Collectors.joining(", "));
        AllowedFileType type = typeFromName(file.originalName())
                .filter(allowedTypes::contains)
                .orElseThrow(() -> invalid("Formato não permitido. Envie: " + allowedDescription + "."));
        if (!type.contentType().equalsIgnoreCase(normalizeContentType(file.declaredContentType()))) {
            throw invalid("O tipo do arquivo não corresponde à extensão.");
        }
        if (file.declaredSize() <= 0) {
            throw invalid("Arquivo vazio.");
        }
        if (file.declaredSize() > maxBytes) {
            throw tooLarge(maxBytes);
        }

        FileStorage.StoredContent content;
        try (InputStream input = new BufferedInputStream(file.openStream())) {
            requireSignature(input, type);
            content = fileStorage.store(input, maxBytes);
        } catch (FileStorage.FileTooLargeException exception) {
            throw tooLarge(maxBytes);
        } catch (IOException exception) {
            throw new UncheckedIOException("Falha ao ler arquivo enviado", exception);
        }
        deleteContentOnRollback(content.key());

        return storedFileRepository.save(new StoredFile(
                content.key(), sanitizeName(file.originalName()), type.contentType(),
                content.sizeBytes(), content.sha256(), accountId, clock.instant()));
    }

    /** Remove o registro agora e o conteúdo somente após o commit, para não perder o arquivo se houver rollback. */
    public void delete(StoredFile storedFile) {
        storedFileRepository.delete(storedFile);
        String key = storedFile.getStorageKey();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileStorage.delete(key);
                }
            });
        } else {
            fileStorage.delete(key);
        }
    }

    public InputStream open(StoredFile storedFile) {
        return fileStorage.open(storedFile.getStorageKey());
    }

    private void deleteContentOnRollback(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    fileStorage.delete(key);
                }
            }
        });
    }

    private static void requireSignature(InputStream input, AllowedFileType type) throws IOException {
        byte[] header = new byte[AllowedFileType.MAX_SIGNATURE_LENGTH];
        input.mark(header.length);
        int read = input.readNBytes(header, 0, header.length);
        input.reset();
        if (!type.matchesSignature(header, read)) {
            throw invalid("O conteúdo do arquivo não corresponde ao formato informado.");
        }
    }

    private static Optional<AllowedFileType> typeFromName(String originalName) {
        if (originalName == null) {
            return Optional.empty();
        }
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return Optional.empty();
        }
        return AllowedFileType.fromExtension(originalName.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private static String normalizeContentType(String declared) {
        if (declared == null) {
            return "";
        }
        int parameters = declared.indexOf(';');
        return (parameters >= 0 ? declared.substring(0, parameters) : declared).trim();
    }

    /** O nome original é só informativo; remove caminho e caracteres de controle. */
    static String sanitizeName(String originalName) {
        String name = originalName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").trim();
        if (name.isEmpty()) {
            name = "arquivo";
        }
        return name.length() > MAX_ORIGINAL_NAME_LENGTH ? name.substring(name.length() - MAX_ORIGINAL_NAME_LENGTH) : name;
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, message);
    }

    private static BusinessException tooLarge(long maxBytes) {
        return new BusinessException(HttpStatus.BAD_REQUEST,
                "Arquivo acima do tamanho máximo de " + (maxBytes / (1024 * 1024)) + " MB.");
    }
}
