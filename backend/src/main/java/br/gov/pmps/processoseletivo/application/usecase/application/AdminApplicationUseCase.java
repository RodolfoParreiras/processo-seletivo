package br.gov.pmps.processoseletivo.application.usecase.application;

import br.gov.pmps.processoseletivo.application.dto.PageResponse;
import br.gov.pmps.processoseletivo.application.dto.application.AdminApplicationDtos.AdminApplicationDetail;
import br.gov.pmps.processoseletivo.application.dto.application.AdminApplicationDtos.CandidateData;
import br.gov.pmps.processoseletivo.application.dto.application.AdminApplicationDtos.DecisionEntry;
import br.gov.pmps.processoseletivo.application.dto.application.AdminApplicationDtos.DocumentEntry;
import br.gov.pmps.processoseletivo.application.file.FileDownload;
import br.gov.pmps.processoseletivo.application.file.FileUploadService;
import br.gov.pmps.processoseletivo.application.usecase.process.ProcessSupport;
import br.gov.pmps.processoseletivo.domain.model.Administrator;
import br.gov.pmps.processoseletivo.domain.model.StoredFile;
import br.gov.pmps.processoseletivo.domain.model.application.Application;
import br.gov.pmps.processoseletivo.domain.model.application.ApplicationDecision;
import br.gov.pmps.processoseletivo.domain.model.application.ApplicationDocument;
import br.gov.pmps.processoseletivo.domain.model.application.ApplicationSnapshot;
import br.gov.pmps.processoseletivo.domain.model.application.ApplicationStatus;
import br.gov.pmps.processoseletivo.domain.model.process.DocumentRequirement;
import br.gov.pmps.processoseletivo.domain.model.process.SelectionProcess;
import br.gov.pmps.processoseletivo.domain.repository.AdminApplicationRow;
import br.gov.pmps.processoseletivo.domain.repository.AdministratorRepository;
import br.gov.pmps.processoseletivo.domain.repository.ApplicationDecisionRepository;
import br.gov.pmps.processoseletivo.domain.repository.ApplicationDocumentRepository;
import br.gov.pmps.processoseletivo.domain.repository.ApplicationRepository;
import br.gov.pmps.processoseletivo.domain.repository.ApplicationSnapshotRepository;
import br.gov.pmps.processoseletivo.domain.repository.StoredFileRepository;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta e decisão sobre inscrições na área administrativa (docs/DECISOES.md, fase 6).
 * Leituras de dados sensíveis e de documentos de candidatos são auditadas (ESPECIFICACAO §36).
 */
@Service
public class AdminApplicationUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final ApplicationRepository applicationRepository;
    private final ApplicationSnapshotRepository snapshotRepository;
    private final ApplicationDocumentRepository documentRepository;
    private final ApplicationDecisionRepository decisionRepository;
    private final AdministratorRepository administratorRepository;
    private final StoredFileRepository storedFileRepository;
    private final FileUploadService fileUploadService;
    private final ApplicationSupport support;
    private final ProcessSupport processSupport;
    private final Clock clock;

    public AdminApplicationUseCase(
            ApplicationRepository applicationRepository,
            ApplicationSnapshotRepository snapshotRepository,
            ApplicationDocumentRepository documentRepository,
            ApplicationDecisionRepository decisionRepository,
            AdministratorRepository administratorRepository,
            StoredFileRepository storedFileRepository,
            FileUploadService fileUploadService,
            ApplicationSupport support,
            ProcessSupport processSupport,
            Clock clock) {
        this.applicationRepository = applicationRepository;
        this.snapshotRepository = snapshotRepository;
        this.documentRepository = documentRepository;
        this.decisionRepository = decisionRepository;
        this.administratorRepository = administratorRepository;
        this.storedFileRepository = storedFileRepository;
        this.fileUploadService = fileUploadService;
        this.support = support;
        this.processSupport = processSupport;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminApplicationRow> list(UUID processId, ApplicationStatus status, int page, int size) {
        processSupport.find(processId);
        if (status == ApplicationStatus.RASCUNHO) {
            return new PageResponse<>(List.of(), page, size, 0, 0);
        }
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE),
                Sort.by(Sort.Order.asc("applicationNumber")));
        return PageResponse.from(applicationRepository.findAdminRows(processId, status, pageable), Function.identity());
    }

    @Transactional
    public AdminApplicationDetail get(UUID applicationId, boolean includeDisabilityData, UUID actorId, String ip) {
        Application application = confirmedApplication(applicationId);
        SelectionProcess process = processSupport.find(application.getProcessId());
        ApplicationSnapshot snapshot = snapshotRepository.findById(applicationId).orElseThrow();

        if (includeDisabilityData && snapshot.hasDisability()) {
            support.audit("APPLICATION_SENSITIVE_DATA_VIEWED", application, actorId, ip, Map.of("data", "PCD"));
        }
        CandidateData candidate = new CandidateData(
                snapshot.getFullName(), snapshot.getCpf(), snapshot.getBirthDate(), snapshot.getMotherName(),
                snapshot.getEmail(), snapshot.getPhone(), snapshot.getAddress().city(), snapshot.getAddress().uf(),
                includeDisabilityData ? snapshot.hasDisability() : null,
                includeDisabilityData ? snapshot.getAdaptations() : null);

        return new AdminApplicationDetail(
                application.getId(), process.getId(), process.getDisplayNumber(), process.getTitle(),
                snapshot.getPositionName(), application.getStatus(), application.getApplicationNumber(),
                application.getConfirmedAt(), application.getDecisionReason(), process.allowsApplicationDecisions(),
                candidate, documents(process, applicationId), decisions(applicationId));
    }

    @Transactional
    public FileDownload openDocument(UUID applicationId, UUID documentId, UUID actorId, String ipAddress) {
        Application application = confirmedApplication(applicationId);
        ApplicationDocument document = documentRepository.findByIdAndApplicationId(documentId, applicationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Documento não encontrado."));
        StoredFile file = storedFileRepository.findById(document.getFileId()).orElseThrow();
        support.audit("APPLICATION_DOCUMENT_ACCESSED", application, actorId, ipAddress,
                Map.of("documentId", documentId.toString()));
        String extension = switch (file.getContentType()) {
            case "application/pdf" -> "pdf";
            case "image/png" -> "png";
            default -> "jpg";
        };
        String fileName = "inscricao-" + application.getApplicationNumber().replaceAll("[^0-9A-Za-z-]", "-")
                + "-documento." + extension;
        return new FileDownload(fileName, file.getContentType(), file.getSizeBytes(), fileUploadService.open(file));
    }

    @Transactional
    public void decide(UUID applicationId, ApplicationStatus target, String reason, UUID actorId, String ipAddress) {
        Instant now = clock.instant();
        Application application = confirmedApplication(applicationId);
        // Bloqueia o processo: a situação do processo não muda durante a decisão.
        SelectionProcess process = processSupport.findForUpdate(application.getProcessId());
        process.requireApplicationDecisionsAllowed();

        ApplicationStatus previous = application.decide(target, reason, now);
        decisionRepository.save(new ApplicationDecision(applicationId, previous, target, reason, actorId, now));
        support.audit(target == ApplicationStatus.DEFERIDA ? "APPLICATION_DEFERRED" : "APPLICATION_DENIED",
                application, actorId, ipAddress, Map.of("from", previous.name(), "to", target.name()));
    }

    private Application confirmedApplication(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .filter(application -> !application.isDraft())
                .orElseThrow(ApplicationSupport::notFound);
    }

    private List<DocumentEntry> documents(SelectionProcess process, UUID applicationId) {
        List<ApplicationDocument> documents = documentRepository.findByApplicationIdOrderByUploadedAtAsc(applicationId);
        Map<UUID, StoredFile> files = storedFileRepository
                .findAllById(documents.stream().map(ApplicationDocument::getFileId).toList()).stream()
                .collect(Collectors.toMap(StoredFile::getId, Function.identity()));
        Map<UUID, DocumentRequirement> requirements = process.getDocumentRequirements().stream()
                .collect(Collectors.toMap(DocumentRequirement::getId, Function.identity()));
        return documents.stream()
                .map(document -> {
                    DocumentRequirement requirement = requirements.get(document.getRequirementId());
                    StoredFile file = files.get(document.getFileId());
                    return new DocumentEntry(document.getId(), requirement.getName(), requirement.isTitle(),
                            file.getOriginalName(), file.getSizeBytes(), document.getUploadedAt());
                })
                .toList();
    }

    private List<DecisionEntry> decisions(UUID applicationId) {
        List<ApplicationDecision> decisions = decisionRepository.findByApplicationIdOrderByDecidedAtAscIdAsc(applicationId);
        Set<UUID> accountIds = decisions.stream()
                .map(ApplicationDecision::getDecidedByAccountId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> names = administratorRepository.findByUserAccountIdIn(accountIds).stream()
                .collect(Collectors.toMap(Administrator::getUserAccountId, Administrator::getFullName));
        return decisions.stream()
                .map(decision -> new DecisionEntry(decision.getFromStatus(), decision.getToStatus(), decision.getReason(),
                        names.get(decision.getDecidedByAccountId()), decision.getDecidedAt()))
                .toList();
    }
}
