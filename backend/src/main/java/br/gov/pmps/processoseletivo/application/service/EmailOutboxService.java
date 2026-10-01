package br.gov.pmps.processoseletivo.application.service;

import br.gov.pmps.processoseletivo.domain.model.EmailOutboxMessage;
import br.gov.pmps.processoseletivo.domain.repository.EmailOutboxRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Fila de e-mails no banco (ESPECIFICACAO §28): a mensagem é gravada junto com a operação e enviada depois.
 * Falha de SMTP não desfaz a operação; o erro fica registrado e o envio é tentado novamente.
 */
@Service
public class EmailOutboxService {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxService.class);
    private static final int BATCH_SIZE = 50;

    private final EmailOutboxRepository outboxRepository;
    private final EmailGateway emailGateway;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public EmailOutboxService(
            EmailOutboxRepository outboxRepository,
            EmailGateway emailGateway,
            TransactionTemplate transactionTemplate,
            Clock clock) {
        this.outboxRepository = outboxRepository;
        this.emailGateway = emailGateway;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /** Deve ser chamado dentro da transação da operação de negócio. */
    public void enqueue(String recipient, String subject, String body) {
        outboxRepository.save(new EmailOutboxMessage(recipient, subject, body, clock.instant()));
    }

    /** @return quantidade de mensagens enviadas nesta execução */
    public int dispatchDue() {
        Integer sent = transactionTemplate.execute(status -> {
            Instant now = clock.instant();
            List<EmailOutboxMessage> due = outboxRepository.lockDueMessages(now, BATCH_SIZE);
            int delivered = 0;
            for (EmailOutboxMessage message : due) {
                try {
                    emailGateway.send(message.getRecipient(), message.getSubject(), message.getBody());
                    message.markSent(now);
                    delivered++;
                } catch (RuntimeException exception) {
                    // Sem destinatário no log (AI_RULES §14); o id permite localizar a mensagem.
                    message.markFailed(exception.getClass().getSimpleName(), now);
                    log.warn("Falha ao enviar e-mail da fila [id={}, tentativa={}]: {}",
                            message.getId(), message.getAttempts(), exception.getClass().getSimpleName());
                }
            }
            return delivered;
        });
        return sent == null ? 0 : sent;
    }
}
