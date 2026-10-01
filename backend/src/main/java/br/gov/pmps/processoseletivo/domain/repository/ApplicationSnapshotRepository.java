package br.gov.pmps.processoseletivo.domain.repository;

import br.gov.pmps.processoseletivo.domain.model.application.ApplicationSnapshot;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationSnapshotRepository extends JpaRepository<ApplicationSnapshot, UUID> {
}
