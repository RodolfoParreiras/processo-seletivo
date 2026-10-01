package br.gov.pmps.processoseletivo.infrastructure.email;

import br.gov.pmps.processoseletivo.application.service.EmailGateway;
import br.gov.pmps.processoseletivo.infrastructure.configuration.AppProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailGateway implements EmailGateway {

    private final JavaMailSender mailSender;
    private final String sender;

    public SmtpEmailGateway(JavaMailSender mailSender, AppProperties appProperties) {
        this.mailSender = mailSender;
        this.sender = appProperties.mailFrom();
    }

    @Override
    public void send(String recipient, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
