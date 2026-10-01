package br.gov.pmps.processoseletivo.domain.model.process;

import br.gov.pmps.processoseletivo.shared.error.DomainRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Processo seletivo e suas regras de ciclo de vida (ESPECIFICACAO §13 a §17).
 * Toda alteração passa por este agregado, que é a fonte de verdade das transições permitidas.
 */
@Entity
@Table(name = "selection_process")
public class SelectionProcess {

    public record Details(
            String number,
            int year,
            String title,
            String department,
            Instant registrationStart,
            Instant registrationEnd,
            boolean multipleApplicationsAllowed,
            boolean titleEvaluationEnabled) {
    }

    @Id
    private UUID id;

    @Column(name = "process_number", nullable = false)
    private String number;

    @Column(name = "process_year", nullable = false)
    private int year;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_before_suspension")
    private ProcessStatus statusBeforeSuspension;

    @Column(name = "registration_start", nullable = false)
    private Instant registrationStart;

    @Column(name = "registration_end", nullable = false)
    private Instant registrationEnd;

    @Column(name = "multiple_applications_allowed", nullable = false)
    private boolean multipleApplicationsAllowed;

    @Column(name = "title_evaluation_enabled", nullable = false)
    private boolean titleEvaluationEnabled;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "process", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("name")
    private List<ProcessPosition> positions = new ArrayList<>();

    @OneToMany(mappedBy = "process", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("name")
    private List<DocumentRequirement> documentRequirements = new ArrayList<>();

    protected SelectionProcess() {
    }

    public SelectionProcess(Details details, Instant now) {
        this.id = UUID.randomUUID();
        this.status = ProcessStatus.RASCUNHO;
        this.createdAt = now;
        applyDetails(details, now);
    }

    // ---- Edição (somente rascunho) ----

    public void updateDetails(Details details, Instant now) {
        requireDraft();
        applyDetails(details, now);
    }

    public ProcessPosition addPosition(String name, int vacancies, Instant now) {
        requireDraft();
        requireUniquePositionName(name, null);
        ProcessPosition position = new ProcessPosition(this, name, vacancies);
        positions.add(position);
        updatedAt = now;
        return position;
    }

    public void updatePosition(UUID positionId, String name, int vacancies, Instant now) {
        requireDraft();
        requireUniquePositionName(name, positionId);
        findPosition(positionId).change(name, vacancies);
        updatedAt = now;
    }

    public void removePosition(UUID positionId, Instant now) {
        requireDraft();
        positions.remove(findPosition(positionId));
        updatedAt = now;
    }

    public DocumentRequirement addDocumentRequirement(DocumentRequirement.Definition definition, Instant now) {
        requireDraft();
        requireUniqueRequirementName(definition.name(), null);
        DocumentRequirement requirement = new DocumentRequirement(this, definition);
        documentRequirements.add(requirement);
        updatedAt = now;
        return requirement;
    }

    public void updateDocumentRequirement(UUID requirementId, DocumentRequirement.Definition definition, Instant now) {
        requireDraft();
        requireUniqueRequirementName(definition.name(), requirementId);
        findRequirement(requirementId).change(definition);
        updatedAt = now;
    }

    public void removeDocumentRequirement(UUID requirementId, Instant now) {
        requireDraft();
        documentRequirements.remove(findRequirement(requirementId));
        updatedAt = now;
    }

    // ---- Ciclo de vida ----

    /** @param hasNoticeFile se há edital (PDF) anexado; publicar sem edital não é permitido */
    public StatusChange publish(boolean hasNoticeFile, Instant now) {
        requireDraft();
        if (positions.isEmpty()) {
            throw new DomainRuleException("Cadastre ao menos um cargo antes de publicar.");
        }
        if (!hasNoticeFile) {
            throw new DomainRuleException("Anexe o edital antes de publicar.");
        }
        if (!registrationEnd.isAfter(now)) {
            throw new DomainRuleException("O fim do período de inscrição já passou. Ajuste as datas antes de publicar.");
        }
        publishedAt = now;
        return changeStatus(ProcessStatus.PUBLICADO, now);
    }

    /**
     * Abertura e encerramento automáticos pelas datas do período (docs/DECISOES.md).
     * Processos suspensos não avançam até serem retomados.
     */
    public Optional<StatusChange> advanceBySchedule(Instant now) {
        if (status == ProcessStatus.PUBLICADO && !registrationStart.isAfter(now)) {
            return Optional.of(changeStatus(ProcessStatus.INSCRICOES_ABERTAS, now));
        }
        if (status == ProcessStatus.INSCRICOES_ABERTAS && !registrationEnd.isAfter(now)) {
            return Optional.of(changeStatus(ProcessStatus.INSCRICOES_ENCERRADAS, now));
        }
        return Optional.empty();
    }

    /** Somente prorrogação do fim, antes do encerramento (docs/DECISOES.md). */
    public void extendRegistration(Instant newEnd, Instant now) {
        if (status != ProcessStatus.PUBLICADO && status != ProcessStatus.INSCRICOES_ABERTAS) {
            throw new DomainRuleException("A prorrogação só é permitida antes do encerramento das inscrições.");
        }
        if (!newEnd.isAfter(registrationEnd)) {
            throw new DomainRuleException("A nova data final deve ser posterior à data final atual.");
        }
        if (!newEnd.isAfter(now)) {
            throw new DomainRuleException("A nova data final deve estar no futuro.");
        }
        registrationEnd = newEnd;
        updatedAt = now;
    }

    public StatusChange suspend(Instant now) {
        if (!status.isSuspendable()) {
            throw new DomainRuleException("O processo não pode ser suspenso na situação atual.");
        }
        statusBeforeSuspension = status;
        return changeStatus(ProcessStatus.SUSPENSO, now);
    }

    public StatusChange resume(Instant now) {
        if (status != ProcessStatus.SUSPENSO) {
            throw new DomainRuleException("Somente processos suspensos podem ser retomados.");
        }
        ProcessStatus previous = statusBeforeSuspension;
        statusBeforeSuspension = null;
        return changeStatus(previous, now);
    }

    public StatusChange cancel(Instant now) {
        if (status.isTerminal()) {
            throw new DomainRuleException("O processo já está finalizado.");
        }
        statusBeforeSuspension = null;
        return changeStatus(ProcessStatus.CANCELADO, now);
    }

    public StatusChange archive(Instant now) {
        if (status != ProcessStatus.RESULTADO_DEFINITIVO) {
            throw new DomainRuleException("Somente processos com resultado definitivo podem ser arquivados.");
        }
        return changeStatus(ProcessStatus.ARQUIVADO, now);
    }

    /** Retificação do edital: permitida enquanto o processo estiver publicado e não finalizado. */
    public void requireNoticeRectifiable() {
        if (status == ProcessStatus.RASCUNHO || status.isTerminal()) {
            throw new DomainRuleException("A retificação do edital só é permitida em processos publicados e não finalizados.");
        }
    }

    public boolean isDraft() {
        return status == ProcessStatus.RASCUNHO;
    }

    /** Inscrições exigem a situação e a data, sem depender do horário em que a transição automática roda. */
    public boolean acceptsApplications(Instant now) {
        return status == ProcessStatus.INSCRICOES_ABERTAS
                && !registrationStart.isAfter(now)
                && registrationEnd.isAfter(now);
    }

    private StatusChange changeStatus(ProcessStatus target, Instant now) {
        StatusChange change = new StatusChange(status, target);
        status = target;
        updatedAt = now;
        return change;
    }

    private void applyDetails(Details details, Instant now) {
        if (!details.registrationEnd().isAfter(details.registrationStart())) {
            throw new DomainRuleException("O fim do período de inscrição deve ser posterior ao início.");
        }
        this.number = details.number().trim();
        this.year = details.year();
        this.title = details.title().trim();
        this.department = details.department().trim();
        this.registrationStart = details.registrationStart();
        this.registrationEnd = details.registrationEnd();
        this.multipleApplicationsAllowed = details.multipleApplicationsAllowed();
        this.titleEvaluationEnabled = details.titleEvaluationEnabled();
        this.updatedAt = now;
    }

    private void requireDraft() {
        if (!isDraft()) {
            throw new DomainRuleException(
                    "Somente processos em rascunho podem ser editados. Após a publicação, use a retificação do edital.");
        }
    }

    private ProcessPosition findPosition(UUID positionId) {
        return positions.stream()
                .filter(position -> position.getId().equals(positionId))
                .findFirst()
                .orElseThrow(() -> new DomainRuleException("Cargo não encontrado neste processo."));
    }

    private DocumentRequirement findRequirement(UUID requirementId) {
        return documentRequirements.stream()
                .filter(requirement -> requirement.getId().equals(requirementId))
                .findFirst()
                .orElseThrow(() -> new DomainRuleException("Documento não encontrado neste processo."));
    }

    private void requireUniquePositionName(String name, UUID ignoredId) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        boolean duplicated = positions.stream()
                .anyMatch(position -> !position.getId().equals(ignoredId)
                        && position.getName().toLowerCase(Locale.ROOT).equals(normalized));
        if (duplicated) {
            throw new DomainRuleException("Já existe um cargo com este nome no processo.");
        }
    }

    private void requireUniqueRequirementName(String name, UUID ignoredId) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        boolean duplicated = documentRequirements.stream()
                .anyMatch(requirement -> !requirement.getId().equals(ignoredId)
                        && requirement.getName().toLowerCase(Locale.ROOT).equals(normalized));
        if (duplicated) {
            throw new DomainRuleException("Já existe um documento com este nome no processo.");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public int getYear() {
        return year;
    }

    public String getTitle() {
        return title;
    }

    public String getDepartment() {
        return department;
    }

    public ProcessStatus getStatus() {
        return status;
    }

    public Instant getRegistrationStart() {
        return registrationStart;
    }

    public Instant getRegistrationEnd() {
        return registrationEnd;
    }

    public boolean isMultipleApplicationsAllowed() {
        return multipleApplicationsAllowed;
    }

    public boolean isTitleEvaluationEnabled() {
        return titleEvaluationEnabled;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public List<ProcessPosition> getPositions() {
        return Collections.unmodifiableList(positions);
    }

    public List<DocumentRequirement> getDocumentRequirements() {
        return Collections.unmodifiableList(documentRequirements);
    }
}
