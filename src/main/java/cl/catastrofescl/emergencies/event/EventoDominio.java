package cl.catastrofescl.emergencies.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Contrato comun de todos los eventos de dominio publicados por este microservicio.
 */
public interface EventoDominio {

    UUID getEventoId();

    OffsetDateTime getOcurridoEn();

    String getCorrelacionId();

    String getRoutingKey();
}
