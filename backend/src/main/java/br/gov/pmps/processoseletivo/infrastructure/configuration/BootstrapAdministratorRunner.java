package br.gov.pmps.processoseletivo.infrastructure.configuration;

import br.gov.pmps.processoseletivo.application.usecase.BootstrapAdministratorUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Provisionamento do primeiro Administrador Geral. Desligado por padrão; ver README. */
@Component
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
public class BootstrapAdministratorRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdministratorRunner.class);

    private final BootstrapAdministratorUseCase bootstrapAdministrator;
    private final AppProperties.BootstrapAdmin properties;

    public BootstrapAdministratorRunner(
            BootstrapAdministratorUseCase bootstrapAdministrator, AppProperties appProperties) {
        this.bootstrapAdministrator = bootstrapAdministrator;
        this.properties = appProperties.bootstrapAdmin();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (isBlank(properties.cpf()) || isBlank(properties.fullName()) || isBlank(properties.email())) {
            throw new IllegalStateException(
                    "BOOTSTRAP_ADMIN_CPF, BOOTSTRAP_ADMIN_NAME e BOOTSTRAP_ADMIN_EMAIL são obrigatórios "
                            + "quando BOOTSTRAP_ADMIN_ENABLED=true");
        }
        boolean created = bootstrapAdministrator.execute(properties.cpf(), properties.fullName(), properties.email());
        if (created) {
            log.info("Administrador Geral criado. Link de definição de senha enviado ao e-mail informado. "
                    + "Desative BOOTSTRAP_ADMIN_ENABLED.");
        } else {
            log.info("Já existe conta administrativa; provisionamento ignorado. Desative BOOTSTRAP_ADMIN_ENABLED.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
