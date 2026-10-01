package br.gov.pmps.processoseletivo;

import br.gov.pmps.processoseletivo.support.RecordingEmailGateway;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Base para testes de integração com PostgreSQL real.
 * Usa o mesmo script de criação de usuários da infraestrutura, para que os testes
 * exercitem a separação de privilégios entre dono do schema (Flyway) e aplicação.
 */
@SpringBootTest
@Import(RecordingEmailGateway.Configuration.class)
public abstract class PostgresIntegrationTest {

    protected static final String OWNER_USER = "ps_owner_test";
    protected static final String APP_USER = "ps_app_test";

    /** Chave fictícia de teste para cifrar segredos de MFA (32 bytes em Base64). */
    public static final String MFA_TEST_KEY = "dGVzdC1rZXktMzItYnl0ZXMtcGFyYS1tZmEtYWVzISE=";

    // Credenciais fictícias, válidas apenas dentro do container efêmero de teste.
    private static final String OWNER_PASSWORD = "owner-test-password";
    private static final String APP_PASSWORD = "app-test-password";

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("processo_seletivo")
            .withEnv("DB_OWNER_USER", OWNER_USER)
            .withEnv("DB_OWNER_PASSWORD", OWNER_PASSWORD)
            .withEnv("DB_APP_USER", APP_USER)
            .withEnv("DB_APP_PASSWORD", APP_PASSWORD)
            .withCopyFileToContainer(
                    MountableFile.forHostPath("../infra/postgres/01-create-roles.sh"),
                    "/docker-entrypoint-initdb.d/01-create-roles.sh");

    private static final Path STORAGE_DIRECTORY = createStorageDirectory();

    static {
        // Container compartilhado por todas as classes de teste da JVM; o Testcontainers o encerra ao final.
        POSTGRES.start();
    }

    private static Path createStorageDirectory() {
        try {
            return Files.createTempDirectory("processo-seletivo-storage-");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", POSTGRES::getJdbcUrl);
        registry.add("DB_OWNER_USER", () -> OWNER_USER);
        registry.add("DB_OWNER_PASSWORD", () -> OWNER_PASSWORD);
        registry.add("DB_APP_USER", () -> APP_USER);
        registry.add("DB_APP_PASSWORD", () -> APP_PASSWORD);
        // O envio real é substituído por RecordingEmailGateway.
        registry.add("MAIL_HOST", () -> "localhost");
        registry.add("MAIL_FROM", () -> "nao-responda@example.test");
        registry.add("APP_PUBLIC_URL", () -> "https://processos.example.test");
        registry.add("STORAGE_PATH", () -> STORAGE_DIRECTORY.toString());
        // Transições automáticas são testadas chamando o caso de uso diretamente.
        registry.add("SCHEDULING_ENABLED", () -> "false");
        registry.add("MFA_ENCRYPTION_KEY", () -> MFA_TEST_KEY);
    }
}
