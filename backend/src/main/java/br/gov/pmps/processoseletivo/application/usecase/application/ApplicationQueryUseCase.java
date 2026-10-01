package br.gov.pmps.processoseletivo.application.usecase.application;

import br.gov.pmps.processoseletivo.application.dto.application.ApplicationDtos.ApplicationDetail;
import br.gov.pmps.processoseletivo.application.dto.application.ApplicationDtos.ApplicationSummary;
import br.gov.pmps.processoseletivo.application.dto.application.ApplicationDtos.DocumentItem;
import br.gov.pmps.processoseletivo.application.dto.application.ApplicationDtos.ReceiptVerification;
import br.gov.pmps.processoseletivo.application.dto.application.ApplicationDtos.RequirementItem;
import br.gov.pmps.processoseletivo.application.file.FileUploadService;
import br.gov.pmps.processoseletivo.application.file.FileDownload;
import br.gov.pmps.processoseletivo.application.usecase.process.ProcessSupport;
import br.gov.pmps.processoseletivo.domain.model.Adaptation;
import br.gov.pmps.processoseletivo.domain.model.StoredFile;
import br.gov.pmps.processoseletivo.domain.model.application.Application;
import br.gov.pmps.processoseletivo.domain.model.application.ApplicationDocument;
import br.gov.pmps.processoseletivo.domain.model.application.ApplicationSnapshot;
import br.gov.pmps.processoseletivo.domain.model.process.ProcessPosition;
import br.gov.pmps.processoseletivo.domain.model.process.SelectionProcess;
import br.gov.pmps.processoseletivo.domain.repository.ApplicationDocumentRepository;
import br.gov.pmps.processoseletivo.domain.repository.ApplicationRepository;
import br.gov.pmps.processoseletivo.domain.repository.ApplicationSnapshotRepository;
import br.gov.pmps.processoseletivo.domain.repository.StoredFileRepository;
import br.gov.pmps.processoseletivo.domain.rule.Cpf;
import br.gov.pmps.processoseletivo.infrastructure.configuration.AppProperties;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** "Minhas Candidaturas", arquivos do próprio candidato, comprovante e verificação pública. */
@Service
@Transactional(readOnly = true)
public class ApplicationQueryUseCase {

    static final String DECLARATION = "Declaro, sob as penas da lei, que as informações prestadas nesta inscrição "
            + "são verdadeiras e que conheço e aceito as normas estabelecidas no edital do processo seletivo.";

    private static final Map<Adaptation, String> ADAPTATION_LABELS = Map.of(
            Adaptation.LIBRAS_INTERPRETER, "Intérprete de Libras",
            Adaptation.READING_ASSISTANCE, "Auxílio Ledor",
            Adaptation.ENLARGED_TEST, "Prova Ampliada",
            Adaptation.NONE, "Nenhuma");

    private final ApplicationRepository applicationRepository;
    private final ApplicationDocumentRepository documentRepository;
    private final ApplicationSnapshotRepository snapshotRepository;
    private final StoredFileRepository storedFileRepository;
    private final FileUploadService fileUploadService;
    private final ReceiptRenderer receiptRenderer;
    private final ApplicationSupport support;
    private final ProcessSupport processSupport;
    private final AppProperties appProperties;
    private final Clock clock;

    public ApplicationQueryUseCase(
            ApplicationRepository applicationRepository,
            ApplicationDocumentRepository documentRepository,
            ApplicationSnapshotRepository snapshotRepository,
            StoredFileRepository storedFileRepository,
            FileUploadService fileUploadService,
            ReceiptRenderer receiptRenderer,
            ApplicationSupport support,
            ProcessSupport processSupport,
            AppProperties appProperties,
            Clock clock) {
        this.applicationRepository = applicationRepository;
        this.documentRepository = documentRepository;
        this.snapshotRepository = snapshotRepository;
        this.storedFileRepository = storedFileRepository;
        this.fileUploadService = fileUploadService;
        this.receiptRenderer = receiptRenderer;
        this.support = support;
        this.processSupport = processSupport;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    public List<ApplicationSummary> list(UUID accountId) {
        return applicationRepository.findRowsByCandidateId(support.candidateOf(accountId).getId()).stream()
                .map(row -> new ApplicationSummary(
                        row.id(), row.processId(), row.processNumber() + "/" + row.processYear(), row.processTitle(),
                        row.positionName(), row.status(), row.applicationNumber(), row.createdAt(), row.confirmedAt()))
                .toList();
    }

    public ApplicationDetail get(UUID accountId, UUID applicationId) {
        Application application = support.ownApplication(accountId, applicationId);
        SelectionProcess process = processSupport.find(application.getProcessId());
        List<ApplicationDocument> documents = documentRepository.findByApplicationIdOrderByUploadedAtAsc(applicationId);
        Map<UUID, StoredFile> files = storedFileRepository
                .findAllById(documents.stream().map(ApplicationDocument::getFileId).toList()).stream()
                .collect(Collectors.toMap(StoredFile::getId, Function.identity()));

        List<RequirementItem> requirements = process.getDocumentRequirements().stream()
                .map(requirement -> new RequirementItem(
                        requirement.getId(), requirement.getName(), requirement.getDescription(),
                        requirement.isMandatory(), requirement.isTitle(),
                        documents.stream()
                                .filter(document -> document.getRequirementId().equals(requirement.getId()))
                                .map(document -> toItem(document, files.get(document.getFileId())))
                                .toList()))
                .toList();

        return new ApplicationDetail(
                application.getId(), process.getId(), process.getDisplayNumber(), process.getTitle(),
                positionName(process, application.getPositionId()), application.getStatus(),
                application.getApplicationNumber(), application.getVerificationCode(), application.getCreatedAt(),
                application.getConfirmedAt(), process.getRegistrationEnd(), process.acceptsApplications(clock.instant()),
                appProperties.files().maxDocumentsPerApplication(), requirements);
    }

    public FileDownload openDocument(UUID accountId, UUID applicationId, UUID documentId) {
        support.ownApplication(accountId, applicationId);
        ApplicationDocument document = documentRepository.findByIdAndApplicationId(documentId, applicationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Documento não encontrado."));
        StoredFile file = storedFileRepository.findById(document.getFileId()).orElseThrow();
        String extension = file.getContentType().equals("application/pdf") ? "pdf"
                : file.getContentType().equals("image/png") ? "png" : "jpg";
        return new FileDownload("documento." + extension, file.getContentType(), file.getSizeBytes(),
                fileUploadService.open(file));
    }

    /** Comprovante gerado a partir do snapshot: reflete os dados do momento da inscrição (§27). */
    public FileDownload receipt(UUID accountId, UUID applicationId) {
        Application application = support.ownApplication(accountId, applicationId);
        if (application.isDraft()) {
            throw new BusinessException(HttpStatus.CONFLICT, "O comprovante fica disponível após a confirmação.");
        }
        SelectionProcess process = processSupport.find(application.getProcessId());
        ApplicationSnapshot snapshot = snapshotRepository.findById(applicationId).orElseThrow();
        byte[] pdf = receiptRenderer.render(new ReceiptRenderer.ReceiptData(
                process.getDisplayNumber(), process.getTitle(), process.getDepartment(),
                application.getApplicationNumber(), snapshot.getPositionName(), application.getConfirmedAt(),
                snapshot.getFullName(), Cpf.mask(snapshot.getCpf()), snapshot.hasDisability(),
                snapshot.getAdaptations().stream().map(ADAPTATION_LABELS::get).sorted().toList(),
                DECLARATION, application.getVerificationCode(), verificationUrl()));
        String fileName = "comprovante-" + application.getApplicationNumber().replaceAll("[^0-9A-Za-z-]", "-") + ".pdf";
        return new FileDownload(fileName, "application/pdf", pdf.length, new ByteArrayInputStream(pdf));
    }

    public ReceiptVerification verify(String code) {
        Optional<Application> application = normalizeCode(code).flatMap(applicationRepository::findByVerificationCode);
        if (application.isEmpty() || application.get().isDraft()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Comprovante não encontrado. Confira o código informado.");
        }
        Application found = application.get();
        SelectionProcess process = processSupport.find(found.getProcessId());
        return new ReceiptVerification(found.getApplicationNumber(), process.getDisplayNumber(), process.getTitle(),
                positionName(process, found.getPositionId()), found.getConfirmedAt());
    }

    private String verificationUrl() {
        return appProperties.publicUrl().replaceAll("/+$", "") + "/verificar-comprovante";
    }

    private static Optional<String> normalizeCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String compact = code.replaceAll("[^0-9A-Za-z]", "").toUpperCase(Locale.ROOT);
        if (compact.length() != 12) {
            return Optional.empty();
        }
        return Optional.of(compact.substring(0, 4) + "-" + compact.substring(4, 8) + "-" + compact.substring(8));
    }

    private static String positionName(SelectionProcess process, UUID positionId) {
        return process.getPositions().stream()
                .filter(position -> position.getId().equals(positionId))
                .map(ProcessPosition::getName)
                .findFirst()
                .orElseThrow();
    }

    private static DocumentItem toItem(ApplicationDocument document, StoredFile file) {
        return new DocumentItem(document.getId(), file.getOriginalName(), file.getSizeBytes(),
                document.getUploadedAt(), document.getStatus());
    }
}
