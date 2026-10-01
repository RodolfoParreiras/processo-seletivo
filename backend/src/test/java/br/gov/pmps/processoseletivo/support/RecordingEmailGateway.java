package br.gov.pmps.processoseletivo.support;

import br.gov.pmps.processoseletivo.application.service.EmailGateway;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Guarda os e-mails em memória em vez de enviá-los. */
public class RecordingEmailGateway implements EmailGateway {

    public record Message(String recipient, String subject, String body) {
    }

    private final ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();

    @Override
    public void send(String recipient, String subject, String body) {
        messages.add(new Message(recipient, subject, body));
    }

    /** O envio é assíncrono; aguarda até alguns segundos pela mensagem. */
    public Message awaitMessageTo(String recipient) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            Optional<Message> message = messagesTo(recipient).stream().reduce((first, second) -> second);
            if (message.isPresent()) {
                return message.get();
            }
            sleep();
        }
        throw new AssertionError("Nenhum e-mail recebido");
    }

    public List<Message> messagesTo(String recipient) {
        return messages.stream().filter(message -> message.recipient().equalsIgnoreCase(recipient)).toList();
    }

    public static String extractToken(Message message) {
        String marker = "#token=";
        int start = message.body().indexOf(marker) + marker.length();
        int end = start;
        while (end < message.body().length() && !Character.isWhitespace(message.body().charAt(end))) {
            end++;
        }
        return message.body().substring(start, end);
    }

    private static void sleep() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    @TestConfiguration
    public static class Configuration {
        @Bean
        @Primary
        RecordingEmailGateway recordingEmailGateway() {
            return new RecordingEmailGateway();
        }
    }
}
