package cl.catastrofescl.emergencies.dto;

import java.util.List;

/**
 * Representacion GeoJSON de un punto.
 * { "type": "Point", "coordinates": [lng, lat] }
 */
public record GeoJsonPuntoDto(
        String type,
        List<Double> coordinates
) {
    public static GeoJsonPuntoDto de(double longitud, double latitud) {
        return new GeoJsonPuntoDto("Point", List.of(longitud, latitud));
    }
}
