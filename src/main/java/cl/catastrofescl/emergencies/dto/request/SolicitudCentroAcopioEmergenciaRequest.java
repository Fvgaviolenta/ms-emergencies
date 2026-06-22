package cl.catastrofescl.emergencies.dto.request;

import cl.catastrofescl.emergencies.dto.CoordenadaDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Datos de un centro de acopio que se declara junto a una emergencia.
 */
public record SolicitudCentroAcopioEmergenciaRequest(
        @NotBlank @Size(max = 200) String nombre,
        @NotNull @Valid CoordenadaDto ubicacion,
        @Positive Integer capacidadEstimada
) {
}
