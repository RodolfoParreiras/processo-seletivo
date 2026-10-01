package br.gov.pmps.processoseletivo.application.service;

import br.gov.pmps.processoseletivo.domain.model.Candidate;
import br.gov.pmps.processoseletivo.domain.model.UserAccount;
import br.gov.pmps.processoseletivo.domain.repository.AdministratorRepository;
import br.gov.pmps.processoseletivo.domain.repository.CandidateRepository;
import br.gov.pmps.processoseletivo.domain.rule.PasswordPolicy;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Aplica a política de senha usando o nome e a data de nascimento do titular da conta. */
@Service
public class AccountPasswordPolicy {

    private final CandidateRepository candidateRepository;
    private final AdministratorRepository administratorRepository;

    public AccountPasswordPolicy(
            CandidateRepository candidateRepository, AdministratorRepository administratorRepository) {
        this.candidateRepository = candidateRepository;
        this.administratorRepository = administratorRepository;
    }

    public void validate(UserAccount account, String newPassword) {
        List<String> violations = switch (account.getAccountType()) {
            case CANDIDATE -> {
                Candidate candidate = candidateRepository.findByUserAccountId(account.getId()).orElseThrow();
                yield PasswordPolicy.violations(newPassword, candidate.getFullName(), candidate.getBirthDate());
            }
            case ADMIN -> PasswordPolicy.violations(
                    newPassword,
                    administratorRepository.findByUserAccountId(account.getId()).orElseThrow().getFullName(),
                    null);
        };
        if (!violations.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "A senha não atende à política de segurança.", violations);
        }
    }
}
