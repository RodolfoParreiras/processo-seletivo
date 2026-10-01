package br.gov.pmps.processoseletivo.domain.repository;

import br.gov.pmps.processoseletivo.domain.model.Administrator;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministratorRepository extends JpaRepository<Administrator, UUID> {

    Optional<Administrator> findByUserAccountId(UUID userAccountId);

    List<Administrator> findByUserAccountIdIn(Collection<UUID> userAccountIds);
}
