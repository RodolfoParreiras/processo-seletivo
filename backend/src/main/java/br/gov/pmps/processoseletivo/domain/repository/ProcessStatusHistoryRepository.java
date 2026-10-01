package br.gov.pmps.processoseletivo.domain.repository;

import br.gov.pmps.processoseletivo.domain.model.process.ProcessStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessStatusHistoryRepository extends JpaRepository<ProcessStatusHistory, Long> {

    List<ProcessStatusHistory> findByProcessIdOrderByChangedAtAscIdAsc(UUID processId);
}
