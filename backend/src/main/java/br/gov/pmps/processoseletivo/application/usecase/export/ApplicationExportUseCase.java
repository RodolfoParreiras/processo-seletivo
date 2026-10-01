package br.gov.pmps.processoseletivo.application.usecase.export;

import br.gov.pmps.processoseletivo.application.service.AuditService;
import br.gov.pmps.processoseletivo.application.usecase.export.ApplicationExportFormat.ExportContext;
import br.gov.pmps.processoseletivo.application.usecase.export.ApplicationExportFormat.ExportRow;
import br.gov.pmps.processoseletivo.application.usecase.export.ApplicationExportFormat.SummaryLine;
import br.gov.pmps.processoseletivo.application.usecase.process.ProcessSupport;
import br.gov.pmps.processoseletivo.domain.model.Adaptation;
import br.gov.pmps.processoseletivo.domain.model.application.ApplicationStatus;
import br.gov.pmps.processoseletivo.domain.model.process.SelectionProcess;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Exportações de inscrições (ESPECIFICACAO §31, §34 e §35). Os dados vêm do snapshot da inscrição.
 * A leitura usa cursor no banco (fetch size) e é escrita diretamente na resposta.
 */
@Service
public class ApplicationExportUseCase {

    public enum Kind {
        /** Planilha para a classificação externa. */
        CLASSIFICATION_SPREADSHEET,
        /** Relatório em PDF com resumo e relação de inscrições. */
        APPLICATIONS_REPORT
    }

    /** Exportação preparada: auditada e pronta para ser escrita no fluxo de resposta. */
    public record PreparedExport(String fileName, String contentType, StreamWriter writer) {
    }

    @FunctionalInterface
    public interface StreamWriter {
        void writeTo(OutputStream output) throws IOException;
    }

    private static final int FETCH_SIZE = 500;
    private static final String ROWS_SQL = """
            select a.application_number, s.full_name, s.cpf, s.birth_date, s.position_name, a.status,
                   s.has_disability, s.adaptations, a.confirmed_at
              from application a
              join application_snapshot s on s.application_id = a.id
             where a.process_id = ? and a.status <> 'RASCUNHO' and (cast(? as varchar) is null or a.status = ?)
             order by s.position_name, a.sequence_number
            """;

    private final ProcessSupport processSupport;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate readOnlyTransaction;
    private final AuditService auditService;
    private final Map<Kind, ApplicationExportFormat> formats;
    private final Clock clock;

    public ApplicationExportUseCase(
            ProcessSupport processSupport,
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate,
            AuditService auditService,
            List<ApplicationExportFormat> availableFormats,
            Clock clock) {
        this.processSupport = processSupport;
        this.jdbcTemplate = jdbcTemplate;
        this.readOnlyTransaction = new TransactionTemplate(transactionTemplate.getTransactionManager());
        this.readOnlyTransaction.setReadOnly(true);
        this.readOnlyTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.auditService = auditService;
        this.formats = availableFormats.stream()
                .collect(Collectors.toUnmodifiableMap(ApplicationExportFormat::kind, format -> format));
        this.clock = clock;
    }

    public PreparedExport prepare(
            Kind kind, UUID processId, ApplicationStatus statusFilter, boolean includeDisabilityData,
            UUID actorId, String ipAddress) {
        SelectionProcess process = processSupport.find(processId);
        String status = statusFilter == null ? null : statusFilter.name();
        List<SummaryLine> summary = summary(processId, status);
        long total = summary.stream().mapToLong(SummaryLine::count).sum();
        Instant now = clock.instant();

        Map<String, Object> details = new HashMap<>();
        details.put("kind", kind.name());
        details.put("statusFilter", status == null ? "TODAS" : status);
        details.put("includesDisabilityData", includeDisabilityData);
        details.put("records", total);
        auditService.record(new AuditService.Entry("EXPORT_GENERATED", AuditService.Outcome.SUCCESS, actorId,
                "SELECTION_PROCESS", processId.toString(), ipAddress, details));

        ApplicationExportFormat format = formats.get(kind);
        ExportContext context = new ExportContext(process.getDisplayNumber(), process.getTitle(), statusFilter,
                includeDisabilityData, now, summary);
        String baseName = (kind == Kind.CLASSIFICATION_SPREADSHEET ? "inscricoes-" : "relatorio-inscricoes-")
                + process.getDisplayNumber().replaceAll("[^0-9A-Za-z-]", "-");
        return new PreparedExport(baseName + "." + format.fileExtension(), format.contentType(),
                output -> format.write(context, consumer -> streamRows(processId, status, consumer), output));
    }

    private List<SummaryLine> summary(UUID processId, String status) {
        return jdbcTemplate.query("""
                select s.position_name, a.status, count(*)
                  from application a
                  join application_snapshot s on s.application_id = a.id
                 where a.process_id = ? and a.status <> 'RASCUNHO' and (cast(? as varchar) is null or a.status = ?)
                 group by s.position_name, a.status
                 order by s.position_name, a.status
                """,
                (row, index) -> new SummaryLine(row.getString(1), ApplicationStatus.valueOf(row.getString(2)), row.getLong(3)),
                processId, status, status);
    }

    private void streamRows(UUID processId, String status, Consumer<ExportRow> consumer) {
        // Cursor no PostgreSQL exige transação; as linhas são lidas em blocos de FETCH_SIZE.
        readOnlyTransaction.executeWithoutResult(transaction -> {
            JdbcTemplate streaming = new JdbcTemplate(jdbcTemplate.getDataSource());
            streaming.setFetchSize(FETCH_SIZE);
            RowCallbackHandler handler = resultSet -> consumer.accept(toRow(resultSet));
            streaming.query(ROWS_SQL, handler, processId, status, status);
        });
    }

    private static ExportRow toRow(ResultSet row) throws SQLException {
        String adaptations = row.getString(8);
        Set<Adaptation> adaptationSet = adaptations == null
                ? EnumSet.noneOf(Adaptation.class)
                : Arrays.stream(adaptations.split(",")).map(Adaptation::valueOf)
                        .collect(Collectors.toCollection(() -> EnumSet.noneOf(Adaptation.class)));
        return new ExportRow(
                row.getString(1),
                row.getString(2),
                row.getString(3),
                row.getDate(4).toLocalDate(),
                row.getString(5),
                ApplicationStatus.valueOf(row.getString(6)),
                row.getBoolean(7),
                adaptationSet,
                row.getTimestamp(9).toInstant());
    }
}
