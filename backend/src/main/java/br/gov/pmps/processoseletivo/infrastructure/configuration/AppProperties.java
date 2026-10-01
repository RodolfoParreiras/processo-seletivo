package br.gov.pmps.processoseletivo.infrastructure.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties("app")
public record AppProperties(
        String publicUrl,
        String mailFrom,
        Files files,
        Security security,
        BootstrapAdmin bootstrapAdmin) {

    public record Security(
            Duration candidateSessionIdleTimeout,
            Duration adminSessionIdleTimeout,
            Duration adminSessionAbsoluteTimeout,
            int maxFailedLoginAttempts,
            Duration accountLockDuration,
            Duration passwordResetTokenValidity,
            Duration passwordSetupTokenValidity) {
    }

    public record Files(DataSize noticeMaxSize, DataSize documentMaxSize, int maxDocumentsPerApplication) {
    }

    public record BootstrapAdmin(boolean enabled, String cpf, String fullName, String email) {
    }
}
