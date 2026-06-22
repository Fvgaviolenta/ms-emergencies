package cl.catastrofescl.emergencies.dto.response;

import cl.catastrofescl.emergencies.dto.GeoJsonPoligonoDto;

public record CaracteristicaGeoJsonEmergenciaResponse(
        String type,
        GeoJsonPoligonoDto geometry,
        PropiedadesPoligonoEmergenciaResponse properties
) {
    public static CaracteristicaGeoJsonEmergenciaResponse de(
            GeoJsonPoligonoDto geometria,
            PropiedadesPoligonoEmergenciaResponse propiedades) {
        return new CaracteristicaGeoJsonEmergenciaResponse("Feature", geometria, propiedades);
    }
}
