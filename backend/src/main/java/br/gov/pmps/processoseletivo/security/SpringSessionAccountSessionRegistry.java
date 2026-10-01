package br.gov.pmps.processoseletivo.security;

import br.gov.pmps.processoseletivo.application.service.AccountSessionRegistry;
import java.util.UUID;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

@Component
public class SpringSessionAccountSessionRegistry implements AccountSessionRegistry {

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public SpringSessionAccountSessionRegistry(FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    public void terminateAllSessions(UUID accountId) {
        sessionRepository.findByPrincipalName(accountId.toString())
                .keySet()
                .forEach(sessionRepository::deleteById);
    }
}
