package cl.catastrofescl.emergencies.service;

import cl.catastrofescl.emergencies.dto.GeoJsonPoligonoDto;
import cl.catastrofescl.emergencies.dto.GeoJsonPuntoDto;
import cl.catastrofescl.emergencies.dto.response.EmergenciaResponse;
import cl.catastrofescl.emergencies.entity.Emergencia;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class MapeadorEmergencias {

    private final GeometriaMapper geometriaMapper;

    public EmergenciaResponse aResponse(Emergencia emergencia,
                                        OffsetDateTime procesamientoColaEmergenciaCreadaEn) {
        return new EmergenciaResponse(
                emergencia.getId(),
                emergencia.getTipo(),
                emergencia.getSeveridad(),
                emergencia.getRegion(),
                emergencia.getEstado(),
                geometriaMapper.aPuntoGeoJson(emergencia.getCoordenadasEpicentro()),
                geometriaMapper.aPoligonoGeoJson(emergencia.getZonaImpacto()),
                emergencia.getDeclaradaPorUsuarioId(),
                emergencia.getDeclaradaEn(),
                emergencia.getActualizadaEn(),
                procesamientoColaEmergenciaCreadaEn
        );
    }
}
