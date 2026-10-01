package br.gov.pmps.processoseletivo.shared;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Horário oficial do município para datas exibidas ao usuário. O armazenamento continua em UTC. */
public final class OfficialTime {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZONE);

    private OfficialTime() {
    }

    public static String format(Instant instant) {
        return DATE_TIME.format(instant);
    }
}
