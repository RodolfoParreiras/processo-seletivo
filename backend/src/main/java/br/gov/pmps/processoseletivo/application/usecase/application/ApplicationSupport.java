package br.gov.pmps.processoseletivo.application.usecase.application;

import br.gov.pmps.processoseletivo.application.service.AuditService;
import br.gov.pmps.processoseletivo.domain.model.Candidate;
import br.gov.pmps.processoseletivo.domain.model.application.Application;
import br.gov.pmps.processoseletivo.domain.repository.ApplicationRepository;
import br.gov.pmps.processoseletivo.domain.repository.CandidateRepository;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Localiza recursos sempre a partir da conta da sessão: a inscrição de outro candidato
 * responde como inexistente (AI_RULES §8, ESPECIFICACAO §39).
 */
@Component
public class ApplicationSupport {

    static final String TARGET_TYPE = "APPLICATION";

    private final CandidateRepository candidateRepository;
    private final ApplicationRepository applicationRepository;
    private final AuditService auditService;

    public ApplicationSupport(
            CandidateRepository candidateRepository,
            ApplicationRepository applicationRepository,
            AuditService auditService) {
        this.candidateRepository = candidateRepository;
        this.applicationRepository = applicationRepository;
        this.auditService = auditService;
    }

    public Candidate candidateOf(UUID accountId) {
        return candidateRepository.findByUserAccountId(accountId)
                .orElseThrow(() -> new IllegalStateException("Conta de candidato sem cadastro"));
    }

    public Application ownApplication(UUID accountId, UUID applicationId) {
        return applicationRepository.findByIdAndCandidateId(applicationId, candidateOf(accountId).getId())
                .orElseThrow(ApplicationSupport::notFound);
    }

    public Application ownApplicationForUpdate(UUID accountId, UUID applicationId) {
        return applicationRepository.findForUpdate(applicationId, candidateOf(accountId).getId())
                .orElseThrow(ApplicationSupport::notFound);
    }

    public void audit(String action, Application application, UUID actorId, String ipAddress, Map<String, ?> details) {
        auditService.record(new AuditService.Entry(
                action, AuditService.Outcome.SUCCESS, actorId, TARGET_TYPE,
                application.getId().toString(), ipAddress, details));
    }

    public static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "Inscrição não encontrada.");
    }
}
