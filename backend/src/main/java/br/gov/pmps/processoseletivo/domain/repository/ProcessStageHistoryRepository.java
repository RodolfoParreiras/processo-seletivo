package br.gov.pmps.processoseletivo.domain.repository;

import br.gov.pmps.processoseletivo.domain.model.process.ProcessStageHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessStageHistoryRepository extends JpaRepository<ProcessStageHistory, Long> {

    List<ProcessStageHistory> findByProcessIdOrderByChangedAtAscIdAsc(UUID processId);
}
