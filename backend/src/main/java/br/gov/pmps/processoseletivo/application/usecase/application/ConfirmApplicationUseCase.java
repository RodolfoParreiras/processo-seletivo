package br.gov.pmps.processoseletivo.application.usecase.application;

import br.gov.pmps.processoseletivo.application.service.EmailOutboxService;
import br.gov.pmps.processoseletivo.application.usecase.process.ProcessSupport;
import br.gov.pmps.processoseletivo.domain.model.Candidate;
import br.gov.pmps.processoseletivo.domain.model.UserAccount;
import br.gov.pmps.processoseletivo.domain.model.application.Application;
import br.gov.pmps.processoseletivo.domain.model.application.ApplicationDocument;
import br.gov.pmps.processoseletivo.domain.model.application.ApplicationSnapshot;
import br.gov.pmps.processoseletivo.domain.model.process.DocumentRequirement;
import br.gov.pmps.processoseletivo.domain.model.process.ProcessPosition;
import br.gov.pmps.processoseletivo.domain.model.process.SelectionProcess;
import br.gov.pmps.processoseletivo.domain.repository.ApplicationDocumentRepository;
import br.gov.pmps.processoseletivo.domain.repository.ApplicationSnapshotRepository;
import br.gov.pmps.processoseletivo.domain.repository.UserAccountRepository;
import br.gov.pmps.processoseletivo.shared.OfficialTime;
import br.gov.pmps.processoseletivo.shared.error.BusinessException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirma a inscrição numa única transação: número, snapshot dos dados e e-mail na fila
 * (ESPECIFICACAO §18 a §20 e §28, AI_RULES §58).
 */
@Service
public class ConfirmApplicationUseCase {

    // Sem caracteres ambíguos (0/O, 1/I) para facilitar a digitação na verificação pública.
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 12;

    private final ApplicationDocumentRepository documentRepository;
    private final ApplicationSnapshotRepository snapshotRepository;
    private final UserAccountRepository accountRepository;
    private final EmailOutboxService emailOutbox;
    private final ApplicationSupport support;
    private final ProcessSupport processSupport;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Clock clock;

    public ConfirmApplicationUseCase(
            ApplicationDocumentRepository documentRepository,
            ApplicationSnapshotRepository snapshotRepository,
            UserAccountRepository accountRepository,
            EmailOutboxService emailOutbox,
            ApplicationSupport support,
            ProcessSupport processSupport,
            Clock clock) {
        this.documentRepository = documentRepository;
        this.snapshotRepository = snapshotRepository;
        this.accountRepository = accountRepository;
        this.emailOutbox = emailOutbox;
        this.support = support;
        this.processSupport = processSupport;
        this.clock = clock;
    }

    @Transactional
    public String confirm(UUID accountId, UUID applicationId, String ipAddress) {
        Instant now = clock.instant();
        Application application = support.ownApplicationForUpdate(accountId, applicationId);
        application.requireDraft();
        // Bloqueia o processo: confirmações simultâneas recebem números sequenciais distintos.
        SelectionProcess process = processSupport.findForUpdate(application.getProcessId());
        requireMandatoryDocuments(process, applicationId);

        SelectionProcess.ApplicationNumber number = process.nextApplicationNumber(now);
        application.confirm(number, newVerificationCode(), now);

        UserAccount account = accountRepository.findById(accountId).orElseThrow();
        Candidate candidate = support.candidateOf(accountId);
        String positionName = positionName(process, application.getPositionId());
        snapshotRepository.save(new ApplicationSnapshot(application.getId(), account, candidate, positionName, now));

        emailOutbox.enqueue(account.getEmail(), "Inscrição recebida — Processo Seletivo " + process.getDisplayNumber(),
                confirmationBody(process, application, positionName));
        support.audit("APPLICATION_CONFIRMED", application, accountId, ipAddress,
                Map.of("applicationNumber", number.formatted()));
        return number.formatted();
    }

    private void requireMandatoryDocuments(SelectionProcess process, UUID applicationId) {
        Set<UUID> sentRequirements = documentRepository.findByApplicationIdOrderByUploadedAtAsc(applicationId).stream()
                .map(ApplicationDocument::getRequirementId)
                .collect(Collectors.toSet());
        List<String> missing = process.getDocumentRequirements().stream()
                .filter(DocumentRequirement::isMandatory)
                .filter(requirement -> !sentRequirements.contains(requirement.getId()))
                .map(DocumentRequirement::getName)
                .toList();
        if (!missing.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Envie todos os documentos obrigatórios antes de confirmar a inscrição.", missing);
        }
    }

    private static String positionName(SelectionProcess process, UUID positionId) {
        return process.getPositions().stream()
                .filter(position -> position.getId().equals(positionId))
                .map(ProcessPosition::getName)
                .findFirst()
                .orElseThrow();
    }

    private String newVerificationCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            if (i > 0 && i % 4 == 0) {
                code.append('-');
            }
            code.append(CODE_ALPHABET[secureRandom.nextInt(CODE_ALPHABET.length)]);
        }
        return code.toString();
    }

    private static String confirmationBody(SelectionProcess process, Application application, String positionName) {
        return """
                Sua inscrição foi recebida.

                Processo Seletivo: %s — %s
                Cargo: %s
                Número da inscrição: %s
                Data/hora: %s (horário de Brasília)
                Código de autenticidade do comprovante: %s

                O comprovante está disponível na Área do Candidato.
                Prefeitura Municipal de Paraíba do Sul
                """.formatted(
                process.getDisplayNumber(), process.getTitle(), positionName, application.getApplicationNumber(),
                OfficialTime.format(application.getConfirmedAt()), application.getVerificationCode());
    }
}
