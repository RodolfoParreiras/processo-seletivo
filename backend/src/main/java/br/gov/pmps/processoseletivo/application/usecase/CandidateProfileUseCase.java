package br.gov.pmps.processoseletivo.application.usecase;

import br.gov.pmps.processoseletivo.application.dto.CandidateProfileRequest;
import br.gov.pmps.processoseletivo.application.dto.CandidateProfileResponse;
import br.gov.pmps.processoseletivo.application.service.AuditService;
import br.gov.pmps.processoseletivo.domain.model.Adaptation;
import br.gov.pmps.processoseletivo.domain.model.Address;
import br.gov.pmps.processoseletivo.domain.model.Candidate;
import br.gov.pmps.processoseletivo.domain.model.UserAccount;
import br.gov.pmps.processoseletivo.domain.repository.CandidateRepository;
import br.gov.pmps.processoseletivo.domain.repository.UserAccountRepository;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Meus Dados" (ESPECIFICACAO §25). O candidato é sempre identificado pela sessão,
 * nunca por identificador enviado na requisição (AI_RULES §8 e §11).
 */
@Service
public class CandidateProfileUseCase {

    private final UserAccountRepository accountRepository;
    private final CandidateRepository candidateRepository;
    private final AuditService auditService;
    private final Clock clock;

    public CandidateProfileUseCase(
            UserAccountRepository accountRepository,
            CandidateRepository candidateRepository,
            AuditService auditService,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.candidateRepository = candidateRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CandidateProfileResponse get(UUID accountId) {
        UserAccount account = accountRepository.findById(accountId).orElseThrow();
        return toResponse(account, candidateRepository.findByUserAccountId(accountId).orElseThrow());
    }

    /** @return dados atualizados */
    @Transactional
    public CandidateProfileResponse update(UUID accountId, CandidateProfileRequest request, String ipAddress) {
        UserAccount account = accountRepository.findById(accountId).orElseThrow();
        Candidate candidate = candidateRepository.findByUserAccountId(accountId).orElseThrow();
        Set<Adaptation> adaptations = request.adaptations() == null ? Set.of() : request.adaptations();

        Set<String> changedFields;
        try {
            changedFields = candidate.update(
                    new Candidate.PersonalData(
                            request.fullName(), request.birthDate(), request.motherName(), request.phone()),
                    Address.normalized(request.cep(), request.street(), request.addressNumber(),
                            request.complement(), request.neighborhood(), request.city(), request.uf()),
                    request.hasDisability(),
                    adaptations,
                    clock.instant());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }

        if (!changedFields.isEmpty()) {
            // Apenas os nomes dos campos: os valores são dados pessoais (AI_RULES §14).
            auditService.record(new AuditService.Entry(
                    "CANDIDATE_DATA_UPDATED", AuditService.Outcome.SUCCESS, accountId, "CANDIDATE",
                    candidate.getId().toString(), ipAddress, Map.of("fields", changedFields)));
        }
        return toResponse(account, candidate);
    }

    private static CandidateProfileResponse toResponse(UserAccount account, Candidate candidate) {
        Address address = candidate.getAddress();
        return new CandidateProfileResponse(
                account.getCpf(),
                candidate.getFullName(),
                candidate.getBirthDate(),
                candidate.getMotherName(),
                account.getEmail(),
                candidate.getPhone(),
                address.cep(),
                address.street(),
                address.number(),
                address.complement(),
                address.neighborhood(),
                address.city(),
                address.uf(),
                candidate.hasDisability(),
                candidate.getAdaptations());
    }
}
