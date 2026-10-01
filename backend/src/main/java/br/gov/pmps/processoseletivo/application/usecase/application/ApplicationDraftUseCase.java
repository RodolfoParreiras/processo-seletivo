package br.gov.pmps.processoseletivo.application.usecase.application;

import br.gov.pmps.processoseletivo.application.file.AllowedFileType;
import br.gov.pmps.processoseletivo.application.file.FileUploadService;
import br.gov.pmps.processoseletivo.application.file.IncomingFile;
import br.gov.pmps.processoseletivo.application.usecase.process.ProcessSupport;
import br.gov.pmps.processoseletivo.domain.model.Candidate;
import br.gov.pmps.processoseletivo.domain.model.StoredFile;
import br.gov.pmps.processoseletivo.domain.model.application.Application;
import br.gov.pmps.processoseletivo.domain.model.application.ApplicationDocument;
import br.gov.pmps.processoseletivo.domain.model.process.SelectionProcess;
import br.gov.pmps.processoseletivo.domain.repository.ApplicationDocumentRepository;
import br.gov.pmps.processoseletivo.domain.repository.ApplicationRepository;
import br.gov.pmps.processoseletivo.domain.repository.StoredFileRepository;
import br.gov.pmps.processoseletivo.infrastructure.configuration.AppProperties;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import br.gov.pmps.processoseletivo.shared.error.DomainRuleException;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rascunho da inscrição: escolha do cargo e envio dos documentos antes da confirmação
 * (docs/DECISOES.md: inscrição em rascunho; documentos travados após a confirmação).
 */
@Service
public class ApplicationDraftUseCase {

    private static final Set<AllowedFileType> DOCUMENT_TYPES =
            EnumSet.of(AllowedFileType.PDF, AllowedFileType.JPEG, AllowedFileType.PNG);

    private final ApplicationRepository applicationRepository;
    private final ApplicationDocumentRepository documentRepository;
    private final StoredFileRepository storedFileRepository;
    private final FileUploadService fileUploadService;
    private final ApplicationSupport support;
    private final ProcessSupport processSupport;
    private final AppProperties.Files fileLimits;
    private final Clock clock;

    public ApplicationDraftUseCase(
            ApplicationRepository applicationRepository,
            ApplicationDocumentRepository documentRepository,
            StoredFileRepository storedFileRepository,
            FileUploadService fileUploadService,
            ApplicationSupport support,
            ProcessSupport processSupport,
            AppProperties appProperties,
            Clock clock) {
        this.applicationRepository = applicationRepository;
        this.documentRepository = documentRepository;
        this.storedFileRepository = storedFileRepository;
        this.fileUploadService = fileUploadService;
        this.support = support;
        this.processSupport = processSupport;
        this.fileLimits = appProperties.files();
        this.clock = clock;
    }

    @Transactional
    public UUID start(UUID accountId, UUID processId, UUID positionId, String ipAddress) {
        Candidate candidate = support.candidateOf(accountId);
        SelectionProcess process = processSupport.find(processId);
        if (!process.getStatus().isPubliclyVisible()) {
            throw ProcessSupport.notFound();
        }
        String uniquenessKey = Application.uniquenessKey(process, positionId);
        if (applicationRepository.existsByCandidateIdAndUniquenessKey(candidate.getId(), uniquenessKey)) {
            throw new BusinessException(HttpStatus.CONFLICT, process.isMultipleApplicationsAllowed()
                    ? "Você já possui inscrição (ou rascunho) para este cargo neste processo."
                    : "Você já possui inscrição (ou rascunho) neste processo. Este edital permite uma inscrição por candidato.");
        }
        // Pedidos simultâneos que passem pela verificação acima são barrados pela unique constraint do banco.
        Application application = applicationRepository.saveAndFlush(
                Application.draft(process, positionId, candidate.getId(), clock.instant()));
        support.audit("APPLICATION_DRAFT_STARTED", application, accountId, ipAddress, Map.of());
        return application.getId();
    }

    @Transactional
    public void discard(UUID accountId, UUID applicationId, String ipAddress) {
        Application application = support.ownApplicationForUpdate(accountId, applicationId);
        application.requireDraft();
        deleteDraft(application);
        support.audit("APPLICATION_DRAFT_DISCARDED", application, accountId, ipAddress, Map.of());
    }

    @Transactional
    public UUID uploadDocument(
            UUID accountId, UUID applicationId, UUID requirementId, IncomingFile file, String ipAddress) {
        Instant now = clock.instant();
        Application application = support.ownApplicationForUpdate(accountId, applicationId);
        application.requireDraft();
        SelectionProcess process = processSupport.find(application.getProcessId());
        if (!process.acceptsApplications(now)) {
            throw new DomainRuleException("O período de inscrições deste processo não está aberto.");
        }
        boolean requirementBelongsToProcess = process.getDocumentRequirements().stream()
                .anyMatch(requirement -> requirement.getId().equals(requirementId));
        if (!requirementBelongsToProcess) {
            throw new DomainRuleException("Documento não exigido neste processo.");
        }
        if (documentRepository.countByApplicationId(applicationId) >= fileLimits.maxDocumentsPerApplication()) {
            throw new DomainRuleException(
                    "Limite de " + fileLimits.maxDocumentsPerApplication() + " arquivos por inscrição atingido.");
        }

        StoredFile storedFile = fileUploadService.store(
                file, DOCUMENT_TYPES, fileLimits.documentMaxSize().toBytes(), accountId);
        ApplicationDocument document = documentRepository.save(
                new ApplicationDocument(applicationId, requirementId, storedFile.getId(), now));
        support.audit("APPLICATION_DOCUMENT_UPLOADED", application, accountId, ipAddress,
                Map.of("documentId", document.getId().toString(), "requirementId", requirementId.toString()));
        return document.getId();
    }

    @Transactional
    public void removeDocument(UUID accountId, UUID applicationId, UUID documentId, String ipAddress) {
        Application application = support.ownApplicationForUpdate(accountId, applicationId);
        application.requireDraft();
        ApplicationDocument document = documentRepository.findByIdAndApplicationId(documentId, applicationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Documento não encontrado."));
        removeDocument(document);
        support.audit("APPLICATION_DOCUMENT_REMOVED", application, accountId, ipAddress,
                Map.of("documentId", documentId.toString()));
    }

    /** Remove o rascunho com seus arquivos. Usado pelo candidato e pelo descarte automático. */
    public void deleteDraft(Application application) {
        documentRepository.findByApplicationIdOrderByUploadedAtAsc(application.getId()).forEach(this::removeDocument);
        applicationRepository.delete(application);
    }

    private void removeDocument(ApplicationDocument document) {
        documentRepository.delete(document);
        documentRepository.flush();
        storedFileRepository.findById(document.getFileId()).ifPresent(fileUploadService::delete);
    }
}
