package br.gov.pmps.processoseletivo.domain.repository;

import br.gov.pmps.processoseletivo.domain.model.application.Application;
import br.gov.pmps.processoseletivo.domain.model.application.ApplicationStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    /** Consulta sempre restrita ao candidato: possuir o id não basta para acessar (AI_RULES §8). */
    Optional<Application> findByIdAndCandidateId(UUID id, UUID candidateId);

    /** Bloqueia a inscrição durante alterações de documentos, para respeitar o limite de arquivos. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Application a where a.id = :id and a.candidateId = :candidateId")
    Optional<Application> findForUpdate(@Param("id") UUID id, @Param("candidateId") UUID candidateId);

    boolean existsByCandidateIdAndUniquenessKey(UUID candidateId, String uniquenessKey);

    Optional<Application> findByVerificationCode(String verificationCode);

    /** Lista do candidato com processo e cargo em uma única consulta (sem N+1). */
    @Query("""
            select new br.gov.pmps.processoseletivo.domain.repository.CandidateApplicationRow(
                       a.id, a.processId, p.number, p.year, p.title, pos.name, a.status, a.applicationNumber,
                       a.createdAt, a.confirmedAt)
              from Application a, SelectionProcess p, ProcessPosition pos
             where p.id = a.processId and pos.id = a.positionId and a.candidateId = :candidateId
             order by a.createdAt desc
            """)
    List<CandidateApplicationRow> findRowsByCandidateId(@Param("candidateId") UUID candidateId);

    /** Inscrições confirmadas de um processo, para a área administrativa. Rascunhos nunca aparecem. */
    @Query(value = """
            select new br.gov.pmps.processoseletivo.domain.repository.AdminApplicationRow(
                       a.id, a.applicationNumber, s.fullName, s.positionName, a.status, a.confirmedAt)
              from Application a, ApplicationSnapshot s
             where s.applicationId = a.id and a.processId = :processId
               and a.status <> br.gov.pmps.processoseletivo.domain.model.application.ApplicationStatus.RASCUNHO
               and (:status is null or a.status = :status)
            """,
            countQuery = """
            select count(a) from Application a
             where a.processId = :processId
               and a.status <> br.gov.pmps.processoseletivo.domain.model.application.ApplicationStatus.RASCUNHO
               and (:status is null or a.status = :status)
            """)
    Page<AdminApplicationRow> findAdminRows(
            @Param("processId") UUID processId,
            @Param("status") ApplicationStatus status,
            Pageable pageable);

    /**
     * Rascunhos de processos que não aceitam mais inscrições. Processos suspensos mantêm os rascunhos,
     * pois podem ser retomados.
     */
    @Query("""
            select a.id from Application a, SelectionProcess p
             where p.id = a.processId
               and a.status = br.gov.pmps.processoseletivo.domain.model.application.ApplicationStatus.RASCUNHO
               and (p.status in (
                        br.gov.pmps.processoseletivo.domain.model.process.ProcessStatus.INSCRICOES_ENCERRADAS,
                        br.gov.pmps.processoseletivo.domain.model.process.ProcessStatus.ARQUIVADO,
                        br.gov.pmps.processoseletivo.domain.model.process.ProcessStatus.CANCELADO)
                    or (p.status = br.gov.pmps.processoseletivo.domain.model.process.ProcessStatus.INSCRICOES_ABERTAS
                        and p.registrationEnd <= :now))
            """)
    List<UUID> findExpiredDraftIds(@Param("now") Instant now);
}
