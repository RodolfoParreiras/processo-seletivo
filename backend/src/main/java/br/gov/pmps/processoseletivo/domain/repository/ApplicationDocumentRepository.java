package br.gov.pmps.processoseletivo.domain.repository;

import br.gov.pmps.processoseletivo.domain.model.application.ApplicationDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, UUID> {

    List<ApplicationDocument> findByApplicationIdOrderByUploadedAtAsc(UUID applicationId);

    long countByApplicationId(UUID applicationId);

    Optional<ApplicationDocument> findByIdAndApplicationId(UUID id, UUID applicationId);
}
