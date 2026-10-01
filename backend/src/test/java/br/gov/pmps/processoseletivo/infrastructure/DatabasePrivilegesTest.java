package br.gov.pmps.processoseletivo.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gov.pmps.processoseletivo.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Garante que a aplicação não usa usuário administrador do banco (ESPECIFICACAO §49). */
class DatabasePrivilegesTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void applicationConnectsWithRestrictedUser() {
        String currentUser = jdbcTemplate.queryForObject("SELECT current_user", String.class);
        Boolean superuser = jdbcTemplate.queryForObject(
                "SELECT rolsuper FROM pg_roles WHERE rolname = current_user", Boolean.class);

        assertThat(currentUser).isEqualTo(APP_USER);
        assertThat(superuser).isFalse();
    }

    @Test
    void applicationUserCannotChangeSchemaStructure() {
        assertThatThrownBy(() -> jdbcTemplate.execute("CREATE TABLE app.not_allowed (id integer)"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.execute("CREATE TABLE public.not_allowed (id integer)"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void migrationsAreOwnedBySchemaOwner() {
        String historyOwner = jdbcTemplate.queryForObject(
                "SELECT tableowner FROM pg_tables WHERE schemaname = 'app' AND tablename = 'flyway_schema_history'",
                String.class);

        assertThat(historyOwner).isEqualTo(OWNER_USER);
    }
}
