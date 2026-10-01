package br.gov.pmps.processoseletivo.application.service;

/** Envio de e-mail em texto simples. A implementação fica na infraestrutura (SMTP). */
public interface EmailGateway {

    void send(String recipient, String subject, String body);
}
