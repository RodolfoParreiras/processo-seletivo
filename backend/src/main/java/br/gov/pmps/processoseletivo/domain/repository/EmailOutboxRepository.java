package br.gov.pmps.processoseletivo.domain.repository;

import br.gov.pmps.processoseletivo.domain.model.EmailOutboxMessage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailOutboxRepository extends JpaRepository<EmailOutboxMessage, UUID> {

    /** SKIP LOCKED: instâncias diferentes não enviam a mesma mensagem ao mesmo tempo. */
    @Query(value = """
            select * from email_outbox
             where status = 'PENDING' and next_attempt_at <= :now
             order by next_attempt_at
             limit :batchSize
             for update skip locked
            """, nativeQuery = true)
    List<EmailOutboxMessage> lockDueMessages(@Param("now") Instant now, @Param("batchSize") int batchSize);
}
