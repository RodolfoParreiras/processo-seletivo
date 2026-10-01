package br.gov.pmps.processoseletivo.domain.repository;

import br.gov.pmps.processoseletivo.domain.model.application.ApplicationDecision;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationDecisionRepository extends JpaRepository<ApplicationDecision, Long> {

    List<ApplicationDecision> findByApplicationIdOrderByDecidedAtAscIdAsc(UUID applicationId);
}
