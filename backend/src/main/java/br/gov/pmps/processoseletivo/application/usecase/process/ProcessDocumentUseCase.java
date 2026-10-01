package br.gov.pmps.processoseletivo.application.usecase.process;

import br.gov.pmps.processoseletivo.application.file.AllowedFileType;
import br.gov.pmps.processoseletivo.application.file.FileDownload;
import br.gov.pmps.processoseletivo.application.file.FileUploadService;
import br.gov.pmps.processoseletivo.application.file.IncomingFile;
import br.gov.pmps.processoseletivo.domain.model.StoredFile;
import br.gov.pmps.processoseletivo.domain.model.process.ProcessDocument;
import br.gov.pmps.processoseletivo.domain.model.process.SelectionProcess;
import br.gov.pmps.processoseletivo.domain.repository.ProcessDocumentRepository;
import br.gov.pmps.processoseletivo.domain.repository.StoredFileRepository;
import br.gov.pmps.processoseletivo.infrastructure.configuration.AppProperties;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Documentos do Processo: publicações em PDF com nome definido pelo administrador (docs/DECISOES.md). */
@Service
public class ProcessDocumentUseCase {

    public static final int MAX_NAME_LENGTH = 200;

    public record DocumentView(
            UUID id, String name, Instant publishedAt, long sizeBytes, Instant withdrawnAt, String withdrawalReason) {
    }

    private final ProcessDocumentRepository documentRepository;
    private final StoredFileRepository storedFileRepository;
    private final FileUploadService fileUploadService;
    private final ProcessSupport support;
    private final long maxBytes;
    private final Clock clock;

    public ProcessDocumentUseCase(
            ProcessDocumentRepository documentRepository,
            StoredFileRepository storedFileRepository,
            FileUploadService fileUploadService,
            ProcessSupport support,
            AppProperties appProperties,
            Clock clock) {
        this.documentRepository = documentRepository;
        this.storedFileRepository = storedFileRepository;
        this.fileUploadService = fileUploadService;
        this.support = support;
        this.maxBytes = appProperties.files().noticeMaxSize().toBytes();
        this.clock = clock;
    }

    @Transactional
    public UUID publish(UUID processId, String name, IncomingFile file, UUID actorId, String ipAddress) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Informe o nome do documento.");
        }
        if (name.trim().length() > MAX_NAME_LENGTH) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "O nome do documento deve ter no máximo " + MAX_NAME_LENGTH + " caracteres.");
        }
        SelectionProcess process = support.find(processId);
        StoredFile storedFile = fileUploadService.store(file, EnumSet.of(AllowedFileType.PDF), maxBytes, actorId);
        ProcessDocument document = documentRepository.save(
                new ProcessDocument(processId, name, storedFile.getId(), actorId, clock.instant()));
        support.audit("PROCESS_DOCUMENT_PUBLISHED", process, actorId, ipAddress,
                Map.of("documentId", document.getId().toString(), "name", document.getName()));
        return document.getId();
    }

    @Transactional
    public void withdraw(UUID processId, UUID documentId, String reason, UUID actorId, String ipAddress) {
        SelectionProcess process = support.find(processId);
        ProcessDocument document = documentRepository.findByIdAndProcessId(documentId, processId)
                .orElseThrow(ProcessDocumentUseCase::notFound);
        document.withdraw(reason, actorId, clock.instant());
        support.audit("PROCESS_DOCUMENT_WITHDRAWN", process, actorId, ipAddress,
                Map.of("documentId", documentId.toString(), "reason", reason.trim()));
    }

    /** Visão pública: somente documentos não retirados de processos publicados. */
    @Transactional(readOnly = true)
    public List<DocumentView> listPublic(UUID processId) {
        requirePubliclyVisible(processId);
        return toViews(documentRepository.findByProcessIdAndWithdrawnAtIsNullOrderByPublishedAtDesc(processId));
    }

    @Transactional(readOnly = true)
    public List<DocumentView> listAdmin(UUID processId) {
        support.find(processId);
        return toViews(documentRepository.findByProcessIdOrderByPublishedAtDesc(processId));
    }

    @Transactional(readOnly = true)
    public FileDownload openPublic(UUID processId, UUID documentId) {
        requirePubliclyVisible(processId);
        ProcessDocument document = documentRepository.findByIdAndProcessId(documentId, processId)
                .filter(found -> !found.isWithdrawn())
                .orElseThrow(ProcessDocumentUseCase::notFound);
        return open(document);
    }

    @Transactional(readOnly = true)
    public FileDownload openAdmin(UUID processId, UUID documentId) {
        support.find(processId);
        return open(documentRepository.findByIdAndProcessId(documentId, processId)
                .orElseThrow(ProcessDocumentUseCase::notFound));
    }

    private void requirePubliclyVisible(UUID processId) {
        if (!support.find(processId).getStatus().isPubliclyVisible()) {
            throw ProcessSupport.notFound();
        }
    }

    private FileDownload open(ProcessDocument document) {
        StoredFile file = storedFileRepository.findById(document.getFileId()).orElseThrow();
        // Nome do arquivo derivado do nome informado pelo administrador, restrito a caracteres seguros.
        String fileName = document.getName().replaceAll("[^0-9A-Za-z-]+", "-").replaceAll("(^-|-$)", "") + ".pdf";
        return new FileDownload(fileName.equals(".pdf") ? "documento.pdf" : fileName,
                file.getContentType(), file.getSizeBytes(), fileUploadService.open(file));
    }

    private List<DocumentView> toViews(List<ProcessDocument> documents) {
        Map<UUID, Long> sizes = storedFileRepository.findAllById(documents.stream().map(ProcessDocument::getFileId).toList())
                .stream().collect(Collectors.toMap(StoredFile::getId, StoredFile::getSizeBytes));
        return documents.stream()
                .map(document -> new DocumentView(document.getId(), document.getName(), document.getPublishedAt(),
                        sizes.getOrDefault(document.getFileId(), 0L), document.getWithdrawnAt(),
                        document.getWithdrawalReason()))
                .toList();
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "Documento não encontrado.");
    }
}
