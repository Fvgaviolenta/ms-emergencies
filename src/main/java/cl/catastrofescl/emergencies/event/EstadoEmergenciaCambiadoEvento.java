package cl.catastrofescl.emergencies.event;

import cl.catastrofescl.emergencies.entity.EstadoEmergencia;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EstadoEmergenciaCambiadoEvento implements EventoDominio {

    public static final String ROUTING_KEY = "emergency.status.changed";

    private UUID eventoId;
    private OffsetDateTime ocurridoEn;
    private String correlacionId;

    private UUID emergenciaId;
    private EstadoEmergencia estadoAnterior;
    private EstadoEmergencia estadoNuevo;
    private UUID actualizadaPorUsuarioId;

    @Override
    @JsonIgnore
    public String getRoutingKey() {
        return ROUTING_KEY;
    }
}
