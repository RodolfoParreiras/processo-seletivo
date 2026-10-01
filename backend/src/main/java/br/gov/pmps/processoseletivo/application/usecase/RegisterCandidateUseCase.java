package br.gov.pmps.processoseletivo.application.usecase;

import br.gov.pmps.processoseletivo.application.dto.RegisterCandidateRequest;
import br.gov.pmps.processoseletivo.application.service.AuditService;
import br.gov.pmps.processoseletivo.domain.model.AccountType;
import br.gov.pmps.processoseletivo.domain.model.Adaptation;
import br.gov.pmps.processoseletivo.domain.model.Address;
import br.gov.pmps.processoseletivo.domain.model.Candidate;
import br.gov.pmps.processoseletivo.domain.model.UserAccount;
import br.gov.pmps.processoseletivo.domain.repository.CandidateRepository;
import br.gov.pmps.processoseletivo.domain.repository.UserAccountRepository;
import br.gov.pmps.processoseletivo.domain.rule.Cpf;
import br.gov.pmps.processoseletivo.domain.rule.PasswordPolicy;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterCandidateUseCase {

    // Mesma mensagem para CPF ou e-mail já cadastrados, sem indicar qual dos dois.
    static final String REGISTRATION_REJECTED = "Não foi possível concluir o cadastro com os dados informados. "
            + "Se você já possui conta, utilize a recuperação de senha.";

    private final UserAccountRepository accountRepository;
    private final CandidateRepository candidateRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final Clock clock;

    public RegisterCandidateUseCase(
            UserAccountRepository accountRepository,
            CandidateRepository candidateRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.candidateRepository = candidateRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public void execute(RegisterCandidateRequest request, String ipAddress) {
        Cpf cpf = Cpf.of(request.cpf());
        String email = UserAccount.normalizeEmail(request.email());

        List<String> violations = PasswordPolicy.violations(request.password(), request.fullName(), request.birthDate());
        if (!violations.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "A senha não atende à política de segurança.", violations);
        }

        if (accountRepository.existsByAccountTypeAndCpf(AccountType.CANDIDATE, cpf.digits())
                || accountRepository.existsByAccountTypeAndEmail(AccountType.CANDIDATE, email)) {
            throw new BusinessException(HttpStatus.CONFLICT, REGISTRATION_REJECTED);
        }

        Instant now = clock.instant();
        UserAccount account = new UserAccount(
                AccountType.CANDIDATE, cpf, email, passwordEncoder.encode(request.password()), now);
        Candidate candidate = newCandidate(request, account, now);

        try {
            accountRepository.saveAndFlush(account);
            candidateRepository.saveAndFlush(candidate);
        } catch (DataIntegrityViolationException exception) {
            // Cadastro simultâneo com o mesmo CPF/e-mail: a constraint do banco é a garantia final.
            throw new BusinessException(HttpStatus.CONFLICT, REGISTRATION_REJECTED);
        }

        auditService.record(new AuditService.Entry(
                "CANDIDATE_REGISTERED", AuditService.Outcome.SUCCESS, account.getId(),
                "CANDIDATE", candidate.getId().toString(), ipAddress, Map.of()));
    }

    private static Candidate newCandidate(RegisterCandidateRequest request, UserAccount account, Instant now) {
        Set<Adaptation> adaptations = request.adaptations() == null ? Set.of() : request.adaptations();
        Address address = new Address(
                request.cep(),
                request.street().trim(),
                request.addressNumber().trim(),
                request.complement() == null || request.complement().isBlank() ? null : request.complement().trim(),
                request.neighborhood().trim(),
                request.city().trim(),
                request.uf());
        try {
            return new Candidate(
                    account.getId(),
                    new Candidate.PersonalData(
                            request.fullName(), request.birthDate(), request.motherName(), request.phone()),
                    address,
                    request.hasDisability(),
                    adaptations,
                    now);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }
}
