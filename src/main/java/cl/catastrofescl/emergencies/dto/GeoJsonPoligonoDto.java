package cl.catastrofescl.emergencies.dto;

import java.util.List;

/**
 * Representacion GeoJSON de un poligono.
 * { "type": "Polygon", "coordinates": [ [[lng,lat], [lng,lat], ...] ] }
 */
public record GeoJsonPoligonoDto(
        String type,
        List<List<List<Double>>> coordinates
) {
    public static GeoJsonPoligonoDto de(List<List<Double>> anilloExterior) {
        return new GeoJsonPoligonoDto("Polygon", List.of(anilloExterior));
    }
}
