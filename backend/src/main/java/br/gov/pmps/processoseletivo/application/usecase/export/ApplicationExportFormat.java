package br.gov.pmps.processoseletivo.application.usecase.export;

import br.gov.pmps.processoseletivo.domain.model.Adaptation;
import br.gov.pmps.processoseletivo.domain.model.application.ApplicationStatus;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Formato de exportação das inscrições (Strategy, ESPECIFICACAO §57.2): Excel para a classificação
 * externa e PDF para relatório. As linhas chegam em fluxo, sem lista completa em memória (§61).
 */
public interface ApplicationExportFormat {

    record ExportRow(
            String applicationNumber,
            String fullName,
            String cpf,
            LocalDate birthDate,
            String positionName,
            ApplicationStatus status,
            boolean hasDisability,
            Set<Adaptation> adaptations,
            Instant confirmedAt) {
    }

    record SummaryLine(String positionName, ApplicationStatus status, long count) {
    }

    record ExportContext(
            String processNumber,
            String processTitle,
            ApplicationStatus statusFilter,
            boolean includeDisabilityData,
            Instant generatedAt,
            List<SummaryLine> summary) {
    }

    /** Percorre as linhas em ordem, entregando uma de cada vez. */
    @FunctionalInterface
    interface RowSource {
        void forEach(Consumer<ExportRow> consumer);
    }

    ApplicationExportUseCase.Kind kind();

    String contentType();

    String fileExtension();

    void write(ExportContext context, RowSource rows, OutputStream output) throws IOException;
}
