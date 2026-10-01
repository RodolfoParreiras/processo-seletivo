package br.gov.pmps.processoseletivo.domain.model.application;

import br.gov.pmps.processoseletivo.domain.model.process.SelectionProcess;
import br.gov.pmps.processoseletivo.shared.error.DomainRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** Inscrição de um candidato em um cargo de um processo (ESPECIFICACAO §18). */
@Entity
@Table(name = "application")
public class Application {

    @Id
    private UUID id;

    @Column(name = "process_id", nullable = false, updatable = false)
    private UUID processId;

    @Column(name = "position_id", nullable = false, updatable = false)
    private UUID positionId;

    @Column(name = "candidate_id", nullable = false, updatable = false)
    private UUID candidateId;

    @Column(name = "uniqueness_key", nullable = false, updatable = false)
    private String uniquenessKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(name = "sequence_number")
    private Integer sequenceNumber;

    @Column(name = "application_number")
    private String applicationNumber;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    // Justificativa da decisão vigente; o histórico completo está em ApplicationDecision.
    @Column(name = "decision_reason")
    private String decisionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Application() {
    }

    private Application(UUID processId, UUID positionId, UUID candidateId, String uniquenessKey, Instant now) {
        this.id = UUID.randomUUID();
        this.processId = processId;
        this.positionId = positionId;
        this.candidateId = candidateId;
        this.uniquenessKey = uniquenessKey;
        this.status = ApplicationStatus.RASCUNHO;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Inicia o rascunho. A chave de unicidade reflete a regra do processo e é garantida também no banco. */
    public static Application draft(SelectionProcess process, UUID positionId, UUID candidateId, Instant now) {
        if (!process.acceptsApplications(now)) {
            throw new DomainRuleException("O período de inscrições deste processo não está aberto.");
        }
        boolean positionBelongsToProcess = process.getPositions().stream()
                .anyMatch(position -> position.getId().equals(positionId));
        if (!positionBelongsToProcess) {
            throw new DomainRuleException("Cargo não encontrado neste processo.");
        }
        return new Application(process.getId(), positionId, candidateId, uniquenessKey(process, positionId), now);
    }

    /** Uma inscrição por processo, ou uma por cargo quando o processo permite (docs/DECISOES.md). */
    public static String uniquenessKey(SelectionProcess process, UUID positionId) {
        return process.isMultipleApplicationsAllowed()
                ? process.getId() + ":" + positionId
                : process.getId().toString();
    }

    public void confirm(SelectionProcess.ApplicationNumber number, String newVerificationCode, Instant now) {
        requireDraft();
        this.status = ApplicationStatus.RECEBIDA;
        this.sequenceNumber = number.sequence();
        this.applicationNumber = number.formatted();
        this.verificationCode = newVerificationCode;
        this.confirmedAt = now;
        this.updatedAt = now;
    }

    /**
     * Defere ou indefere. Indeferimento e qualquer revisão de decisão anterior (ex.: recurso) exigem
     * justificativa (docs/DECISOES.md).
     *
     * @return situação anterior
     */
    public ApplicationStatus decide(ApplicationStatus target, String reason, Instant now) {
        if (target != ApplicationStatus.DEFERIDA && target != ApplicationStatus.INDEFERIDA) {
            throw new IllegalArgumentException("Decisão inválida: " + target);
        }
        if (status == ApplicationStatus.RASCUNHO) {
            throw new DomainRuleException("Rascunho não confirmado não pode ser deferido ou indeferido.");
        }
        if (status == target) {
            throw new DomainRuleException("A inscrição já está com esta situação.");
        }
        boolean revision = status != ApplicationStatus.RECEBIDA;
        boolean hasReason = reason != null && !reason.isBlank();
        if ((target == ApplicationStatus.INDEFERIDA || revision) && !hasReason) {
            throw new DomainRuleException(revision
                    ? "Informe a justificativa da alteração da decisão."
                    : "Informe a justificativa do indeferimento.");
        }
        ApplicationStatus previous = status;
        status = target;
        decisionReason = hasReason ? reason.trim() : null;
        updatedAt = now;
        return previous;
    }

    /** Após a confirmação a inscrição não muda de cargo nem de documentos (docs/DECISOES.md). */
    public void requireDraft() {
        if (status != ApplicationStatus.RASCUNHO) {
            throw new DomainRuleException("A inscrição já foi confirmada. Não é possível alterar cargo ou documentos.");
        }
    }

    public boolean isDraft() {
        return status == ApplicationStatus.RASCUNHO;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProcessId() {
        return processId;
    }

    public UUID getPositionId() {
        return positionId;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public String getApplicationNumber() {
        return applicationNumber;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
