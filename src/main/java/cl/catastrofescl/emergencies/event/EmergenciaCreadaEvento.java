package cl.catastrofescl.emergencies.event;

import cl.catastrofescl.emergencies.entity.SeveridadEmergencia;
import cl.catastrofescl.emergencies.entity.TipoEmergencia;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Cuerpo del mensaje RabbitMQ ({@code emergency.created}). El routing key va solo en metadatos AMQP;
 * no debe serializarse como {@code routingKey} en JSON (provocaba fallo al deserializar en el consumidor).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmergenciaCreadaEvento implements EventoDominio {

    public static final String ROUTING_KEY = "emergency.created";

    private UUID eventoId;
    private OffsetDateTime ocurridoEn;
    private String correlacionId;
    private String versionEvento;
    private String fuente;

    private UUID emergenciaId;
    private TipoEmergencia tipo;
    private SeveridadEmergencia severidad;
    private String estado;
    private String region;
    private String resumen;
    private UUID declaradaPorUsuarioId;

    /**
     * Centros de acopio vinculados a la emergencia (puede ser vacia).
     */
    private List<CentroAsociadoEnEventoDto> centrosAsociados;

    @Override
    @JsonIgnore
    public String getRoutingKey() {
        return ROUTING_KEY;
    }
}
