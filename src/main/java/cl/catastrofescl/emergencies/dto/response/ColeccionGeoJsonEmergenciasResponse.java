package cl.catastrofescl.emergencies.dto.response;

import java.util.List;

public record ColeccionGeoJsonEmergenciasResponse(
        String type,
        List<CaracteristicaGeoJsonEmergenciaResponse> features
) {
    public static ColeccionGeoJsonEmergenciasResponse de(
            List<CaracteristicaGeoJsonEmergenciaResponse> caracteristicas) {
        return new ColeccionGeoJsonEmergenciasResponse("FeatureCollection", caracteristicas);
    }
}
