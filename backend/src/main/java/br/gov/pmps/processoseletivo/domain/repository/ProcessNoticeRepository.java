package br.gov.pmps.processoseletivo.domain.repository;

import br.gov.pmps.processoseletivo.domain.model.process.ProcessNotice;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessNoticeRepository extends JpaRepository<ProcessNotice, UUID> {

    /** Versão em rascunho ou vigente (no máximo uma por processo, garantido por índice único). */
    Optional<ProcessNotice> findByProcessIdAndStatusIn(UUID processId, Collection<ProcessNotice.Status> statuses);

    List<ProcessNotice> findByProcessIdOrderByNoticeVersionDesc(UUID processId);

    List<ProcessNotice> findByProcessIdAndStatusInOrderByNoticeVersionDesc(
            UUID processId, Collection<ProcessNotice.Status> statuses);

    Optional<ProcessNotice> findByProcessIdAndNoticeVersion(UUID processId, int noticeVersion);

    @Query("select coalesce(max(n.noticeVersion), 0) from ProcessNotice n where n.processId = :processId")
    int findLatestVersion(@Param("processId") UUID processId);
}
