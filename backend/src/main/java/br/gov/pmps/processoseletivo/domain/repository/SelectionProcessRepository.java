package br.gov.pmps.processoseletivo.domain.repository;

import br.gov.pmps.processoseletivo.domain.model.process.ProcessStatus;
import br.gov.pmps.processoseletivo.domain.model.process.SelectionProcess;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SelectionProcessRepository extends JpaRepository<SelectionProcess, UUID> {

    boolean existsByNumberIgnoreCaseAndYear(String number, int year);

    boolean existsByNumberIgnoreCaseAndYearAndIdNot(String number, int year, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SelectionProcess p where p.id = :id")
    Optional<SelectionProcess> findForUpdate(@Param("id") UUID id);

    Page<SelectionProcess> findByStatusIn(Collection<ProcessStatus> statuses, Pageable pageable);

    /** Processos cuja situação deve mudar pelas datas do período de inscrição. */
    @Query("""
            select p.id from SelectionProcess p
             where (p.status = br.gov.pmps.processoseletivo.domain.model.process.ProcessStatus.PUBLICADO
                    and p.registrationStart <= :now)
                or (p.status = br.gov.pmps.processoseletivo.domain.model.process.ProcessStatus.INSCRICOES_ABERTAS
                    and p.registrationEnd <= :now)
            """)
    List<UUID> findIdsDueForScheduledTransition(@Param("now") Instant now);
}
