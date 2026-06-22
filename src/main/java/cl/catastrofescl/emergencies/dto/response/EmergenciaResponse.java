package cl.catastrofescl.emergencies.dto.response;

import cl.catastrofescl.emergencies.dto.GeoJsonPoligonoDto;
import cl.catastrofescl.emergencies.dto.GeoJsonPuntoDto;
import cl.catastrofescl.emergencies.entity.EstadoEmergencia;
import cl.catastrofescl.emergencies.entity.SeveridadEmergencia;
import cl.catastrofescl.emergencies.entity.TipoEmergencia;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EmergenciaResponse(
        UUID id,
        TipoEmergencia tipo,
        SeveridadEmergencia severidad,
        String region,
        EstadoEmergencia estado,
        GeoJsonPuntoDto coordenadasEpicentro,
        GeoJsonPoligonoDto zonaImpacto,
        UUID declaradaPorUsuarioId,
        OffsetDateTime declaradaEn,
        OffsetDateTime actualizadaEn,
        OffsetDateTime procesamientoColaEmergenciaCreadaEn
) {
}
