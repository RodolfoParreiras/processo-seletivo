package br.gov.pmps.processoseletivo.domain.repository;

import br.gov.pmps.processoseletivo.domain.model.process.ProcessDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessDocumentRepository extends JpaRepository<ProcessDocument, UUID> {

    List<ProcessDocument> findByProcessIdOrderByPublishedAtDesc(UUID processId);

    List<ProcessDocument> findByProcessIdAndWithdrawnAtIsNullOrderByPublishedAtDesc(UUID processId);

    Optional<ProcessDocument> findByIdAndProcessId(UUID id, UUID processId);
}
