package cl.catastrofescl.emergencies.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PropiedadesPoligonoEmergenciaResponse(
        UUID id,
        String tipo,
        String severidad,
        String region,
        String estado,
        OffsetDateTime declaradaEn
) {
}
