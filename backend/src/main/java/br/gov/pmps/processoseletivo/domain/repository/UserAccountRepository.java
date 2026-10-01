package br.gov.pmps.processoseletivo.domain.repository;

import br.gov.pmps.processoseletivo.domain.model.AccountType;
import br.gov.pmps.processoseletivo.domain.model.UserAccount;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    boolean existsByAccountTypeAndCpf(AccountType accountType, String cpf);

    @Query("select count(a) > 0 from UserAccount a where a.accountType = :type and lower(a.email) = lower(:email)")
    boolean existsByAccountTypeAndEmail(@Param("type") AccountType accountType, @Param("email") String email);

    /** Bloqueia a linha durante o login para que tentativas simultâneas não percam a contagem de falhas. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from UserAccount a where a.accountType = :type and a.cpf = :cpf")
    Optional<UserAccount> findForLogin(@Param("type") AccountType accountType, @Param("cpf") String cpf);

    @Query("select a from UserAccount a where a.accountType = :type and lower(a.email) = lower(:email) and a.active = true")
    Optional<UserAccount> findActiveByEmail(@Param("type") AccountType accountType, @Param("email") String email);

    boolean existsByAccountType(AccountType accountType);

    @Query(value = """
            select distinct rp.permission_code
              from user_account_role ur
              join role_permission rp on rp.role_code = ur.role_code
             where ur.user_account_id = :accountId
            """, nativeQuery = true)
    List<String> findPermissionCodes(@Param("accountId") UUID accountId);

    @Query(value = "select role_code from user_account_role where user_account_id = :accountId", nativeQuery = true)
    List<String> findRoleCodes(@Param("accountId") UUID accountId);

    @Modifying
    @Query(value = "delete from user_account_role where user_account_id = :accountId", nativeQuery = true)
    void removeRoles(@Param("accountId") UUID accountId);

    /** Administradores ativos com o perfil informado, exceto a conta indicada. */
    @Query(value = """
            select count(*) from user_account a
              join user_account_role ur on ur.user_account_id = a.id
             where ur.role_code = :roleCode and a.active and a.id <> :excludedAccountId
            """, nativeQuery = true)
    long countActiveWithRoleExcluding(
            @Param("roleCode") String roleCode, @Param("excludedAccountId") UUID excludedAccountId);

    List<UserAccount> findByAccountTypeOrderByCreatedAtAsc(AccountType accountType);

    @Modifying
    @Query(value = "insert into user_account_role (user_account_id, role_code) values (:accountId, :roleCode)",
            nativeQuery = true)
    void assignRole(@Param("accountId") UUID accountId, @Param("roleCode") String roleCode);
}
