package cl.catastrofescl.emergencies.dto.request;

import cl.catastrofescl.emergencies.dto.CoordenadaDto;
import cl.catastrofescl.emergencies.entity.SeveridadEmergencia;
import cl.catastrofescl.emergencies.entity.TipoEmergencia;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Solicitud para declarar una nueva emergencia.
 * La zona de impacto es opcional; si se entrega, debe ser un anillo cerrado
 * (el primer y ultimo punto deben coincidir) con al menos 4 coordenadas.
 * <p>Los centros de acopio asociados son opcionales; en JSON el campo puede
 * llamarse {@code centers} o {@code centrosAcopio}.</p>
 */
public record DeclararEmergenciaRequest(
        @NotNull TipoEmergencia tipo,
        @NotNull SeveridadEmergencia severidad,
        @NotBlank @Size(max = 100) String region,

        @Valid CoordenadaDto epicentro,

        @Valid @Size(min = 4, message = "La zona de impacto debe tener al menos 4 coordenadas y formar un anillo cerrado")
        List<CoordenadaDto> zonaImpacto,

        @Valid @Size(max = 50, message = "Se permiten como maximo 50 centros de acopio por emergencia")
        @com.fasterxml.jackson.annotation.JsonAlias("centers")
        List<SolicitudCentroAcopioEmergenciaRequest> centrosAcopio
) {
    public DeclararEmergenciaRequest {
        if (centrosAcopio == null) {
            centrosAcopio = List.of();
        }
    }
}
