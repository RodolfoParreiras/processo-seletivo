package br.gov.pmps.processoseletivo.application.usecase.export;

import br.gov.pmps.processoseletivo.domain.model.Adaptation;
import br.gov.pmps.processoseletivo.domain.model.application.ApplicationStatus;
import java.util.Set;
import java.util.stream.Collectors;

/** Rótulos em português usados nos arquivos exportados. */
public final class ExportLabels {

    private ExportLabels() {
    }

    public static String status(ApplicationStatus status) {
        return switch (status) {
            case RASCUNHO -> "Rascunho";
            case RECEBIDA -> "Recebida";
            case DEFERIDA -> "Deferida";
            case INDEFERIDA -> "Indeferida";
        };
    }

    public static String adaptations(Set<Adaptation> adaptations) {
        return adaptations.stream()
                .map(adaptation -> switch (adaptation) {
                    case LIBRAS_INTERPRETER -> "Intérprete de Libras";
                    case READING_ASSISTANCE -> "Auxílio Ledor";
                    case ENLARGED_TEST -> "Prova Ampliada";
                    case NONE -> "Nenhuma";
                })
                .sorted()
                .collect(Collectors.joining(", "));
    }
}
