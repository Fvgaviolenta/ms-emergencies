package cl.catastrofescl.emergencies.dto.request;

import cl.catastrofescl.emergencies.entity.EstadoEmergencia;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoEmergenciaRequest(
        @NotNull EstadoEmergencia nuevoEstado
) {
}
