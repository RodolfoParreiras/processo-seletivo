package br.gov.pmps.processoseletivo.infrastructure.email;

import br.gov.pmps.processoseletivo.application.service.AccountSecurityNotice;
import br.gov.pmps.processoseletivo.application.service.EmailGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AccountSecurityNoticeEmailListener {

    private static final Logger log = LoggerFactory.getLogger(AccountSecurityNoticeEmailListener.class);

    private static final String FOOTER = """

            Se você não reconhece esta alteração, redefina sua senha imediatamente pela opção \
            "Esqueci minha senha" e comunique a Prefeitura Municipal de Paraíba do Sul.
            """;

    private final EmailGateway emailGateway;

    public AccountSecurityNoticeEmailListener(EmailGateway emailGateway) {
        this.emailGateway = emailGateway;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotice(AccountSecurityNotice notice) {
        try {
            switch (notice.type()) {
                case EMAIL_CHANGED -> emailGateway.send(notice.email(),
                        "E-mail da conta alterado — Processos Seletivos",
                        "O e-mail da sua conta no Sistema de Processos Seletivos foi alterado. "
                                + "Este endereço não receberá mais comunicações do sistema.\n" + FOOTER);
                case PASSWORD_CHANGED -> emailGateway.send(notice.email(),
                        "Senha alterada — Processos Seletivos",
                        "A senha da sua conta no Sistema de Processos Seletivos foi alterada.\n" + FOOTER);
            }
        } catch (RuntimeException exception) {
            log.warn("Falha ao enviar aviso de segurança [type={}]: {}",
                    notice.type(), exception.getClass().getSimpleName());
        }
    }
}
