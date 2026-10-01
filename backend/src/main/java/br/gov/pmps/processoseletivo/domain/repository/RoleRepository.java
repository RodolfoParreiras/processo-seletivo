package br.gov.pmps.processoseletivo.domain.repository;

import br.gov.pmps.processoseletivo.domain.model.Role;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<Role, String> {

    List<Role> findAllByOrderByNameAsc();

    @Query("select count(r) > 0 from Role r where lower(r.name) = lower(:name) and r.code <> :ignoredCode")
    boolean existsByNameIgnoringCode(@Param("name") String name, @Param("ignoredCode") String ignoredCode);

    @Query(value = "select user_account_id from user_account_role where role_code = :roleCode", nativeQuery = true)
    List<UUID> findMemberAccountIds(@Param("roleCode") String roleCode);
}
