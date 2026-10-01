package br.gov.pmps.processoseletivo.application.service;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/**
 * Registra eventos de auditoria (ESPECIFICACAO §36). A tabela é somente de inserção:
 * não há entidade JPA nem métodos de alteração, e o banco nega UPDATE/DELETE ao usuário da aplicação.
 * Os detalhes nunca devem conter senha, token ou dados pessoais completos.
 */
@Service
public class AuditService {

    public enum Outcome {
        SUCCESS,
        FAILURE
    }

    public record Entry(
            String action,
            Outcome outcome,
            UUID actorAccountId,
            String targetType,
            String targetId,
            String ipAddress,
            Map<String, ?> details) {
    }

    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public AuditService(JdbcTemplate jdbcTemplate, JsonMapper jsonMapper, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    public void record(Entry entry) {
        String details = entry.details() == null || entry.details().isEmpty()
                ? null
                : jsonMapper.writeValueAsString(entry.details());
        jdbcTemplate.update("""
                insert into audit_log
                    (occurred_at, action, outcome, actor_user_account_id, target_type, target_id, ip_address, details)
                values (?, ?, ?, ?, ?, ?, ?, cast(? as jsonb))
                """,
                Timestamp.from(clock.instant()),
                entry.action(),
                entry.outcome().name(),
                entry.actorAccountId(),
                entry.targetType(),
                entry.targetId(),
                entry.ipAddress(),
                details);
    }
}
