package br.gov.pmps.processoseletivo.domain.repository;

import br.gov.pmps.processoseletivo.domain.model.PasswordResetToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /** Bloqueia o token para que duas requisições simultâneas não o utilizem duas vezes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetToken t where t.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findForUse(@Param("tokenHash") String tokenHash);

    boolean existsByUserAccountIdAndCreatedAtAfter(UUID userAccountId, Instant createdAfter);

    @Modifying
    @Query("update PasswordResetToken t set t.usedAt = :now where t.userAccountId = :accountId and t.usedAt is null")
    int invalidateAll(@Param("accountId") UUID accountId, @Param("now") Instant now);
}
