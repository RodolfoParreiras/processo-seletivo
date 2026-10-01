package br.gov.pmps.processoseletivo.infrastructure.email;

import br.gov.pmps.processoseletivo.application.service.EmailGateway;
import br.gov.pmps.processoseletivo.application.service.PasswordLinkRequested;
import br.gov.pmps.processoseletivo.domain.model.PasswordResetToken;
import br.gov.pmps.processoseletivo.infrastructure.configuration.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envia o link somente após o commit e fora da thread da requisição: falha de SMTP não desfaz a operação
 * nem altera a resposta, que precisa ser igual para e-mails cadastrados ou não.
 */
@Component
public class PasswordLinkEmailListener {

    private static final Logger log = LoggerFactory.getLogger(PasswordLinkEmailListener.class);

    private final EmailGateway emailGateway;
    private final String publicUrl;

    public PasswordLinkEmailListener(EmailGateway emailGateway, AppProperties appProperties) {
        this.emailGateway = emailGateway;
        this.publicUrl = appProperties.publicUrl().replaceAll("/+$", "");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordLinkRequested(PasswordLinkRequested event) {
        // O token vai no fragmento (#): navegadores não o enviam ao servidor, então não aparece em logs de acesso.
        String link = publicUrl + "/redefinir-senha#token=" + event.rawToken();
        try {
            if (event.purpose() == PasswordResetToken.Purpose.PASSWORD_SETUP) {
                emailGateway.send(event.email(), "Defina sua senha de acesso — Processos Seletivos", setupBody(link));
            } else {
                emailGateway.send(event.email(), "Redefinição de senha — Processos Seletivos", resetBody(link));
            }
        } catch (RuntimeException exception) {
            // Sem destinatário nem token no log (AI_RULES §14).
            log.warn("Falha ao enviar e-mail de senha [purpose={}]: {}", event.purpose(), exception.getClass().getSimpleName());
        }
    }

    private static String resetBody(String link) {
        return """
                Recebemos uma solicitação para redefinir a senha da sua conta no Sistema de Processos Seletivos \
                da Prefeitura Municipal de Paraíba do Sul.

                Para criar uma nova senha, acesse o link abaixo em até 30 minutos:
                %s

                Se você não fez essa solicitação, ignore este e-mail. Sua senha atual continua válida.
                """.formatted(link);
    }

    private static String setupBody(String link) {
        return """
                Uma conta administrativa foi criada para você no Sistema de Processos Seletivos \
                da Prefeitura Municipal de Paraíba do Sul.

                Para definir sua senha, acesse o link abaixo em até 24 horas:
                %s

                Se você não esperava este e-mail, comunique a equipe responsável pelo sistema.
                """.formatted(link);
    }
}
