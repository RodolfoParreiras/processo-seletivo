package br.gov.pmps.processoseletivo.application.usecase.admin;

import br.gov.pmps.processoseletivo.application.dto.PageResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta da auditoria (ESPECIFICACAO §78): somente leitura. Filtros entram sempre como parâmetros
 * da consulta, nunca concatenados ao SQL (AI_RULES §10).
 */
@Service
public class AuditQueryUseCase {

    public static final int MAX_PAGE_SIZE = 100;

    public record Filter(String action, String outcome, UUID actorAccountId, String targetId, Instant from, Instant to) {
    }

    /** {@code actorName}: nome do administrador; para candidatos, apenas o tipo de conta (minimização). */
    public record AuditEntry(
            long id,
            Instant occurredAt,
            String action,
            String outcome,
            UUID actorAccountId,
            String actorType,
            String actorName,
            String targetType,
            String targetId,
            String ipAddress,
            String details) {
    }

    private final JdbcTemplate jdbcTemplate;

    public AuditQueryUseCase(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEntry> search(Filter filter, int page, int size) {
        int pageSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        int pageNumber = Math.max(page, 0);
        StringBuilder where = new StringBuilder(" where 1 = 1");
        List<Object> parameters = new ArrayList<>();
        if (filter.action() != null && !filter.action().isBlank()) {
            where.append(" and l.action = ?");
            parameters.add(filter.action().trim());
        }
        if (filter.outcome() != null && !filter.outcome().isBlank()) {
            where.append(" and l.outcome = ?");
            parameters.add(filter.outcome().trim());
        }
        if (filter.actorAccountId() != null) {
            where.append(" and l.actor_user_account_id = ?");
            parameters.add(filter.actorAccountId());
        }
        if (filter.targetId() != null && !filter.targetId().isBlank()) {
            where.append(" and l.target_id = ?");
            parameters.add(filter.targetId().trim());
        }
        if (filter.from() != null) {
            where.append(" and l.occurred_at >= ?");
            parameters.add(Timestamp.from(filter.from()));
        }
        if (filter.to() != null) {
            where.append(" and l.occurred_at < ?");
            parameters.add(Timestamp.from(filter.to()));
        }

        Long total = jdbcTemplate.queryForObject(
                "select count(*) from audit_log l" + where, Long.class, parameters.toArray());
        List<Object> pageParameters = new ArrayList<>(parameters);
        pageParameters.add(pageSize);
        pageParameters.add((long) pageNumber * pageSize);
        List<AuditEntry> entries = jdbcTemplate.query("""
                select l.id, l.occurred_at, l.action, l.outcome, l.actor_user_account_id, a.account_type,
                       adm.full_name, l.target_type, l.target_id, l.ip_address, l.details::text
                  from audit_log l
                  left join user_account a on a.id = l.actor_user_account_id
                  left join administrator adm on adm.user_account_id = l.actor_user_account_id
                """ + where + " order by l.occurred_at desc, l.id desc limit ? offset ?",
                (row, index) -> new AuditEntry(
                        row.getLong(1),
                        row.getTimestamp(2).toInstant(),
                        row.getString(3),
                        row.getString(4),
                        row.getObject(5, UUID.class),
                        row.getString(6),
                        row.getString(7),
                        row.getString(8),
                        row.getString(9),
                        row.getString(10),
                        row.getString(11)),
                pageParameters.toArray());
        long totalElements = total == null ? 0 : total;
        return new PageResponse<>(entries, pageNumber, pageSize, totalElements,
                (int) Math.ceil((double) totalElements / pageSize));
    }
}
