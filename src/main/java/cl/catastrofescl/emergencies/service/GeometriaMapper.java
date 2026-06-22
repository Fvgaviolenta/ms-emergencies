package cl.catastrofescl.emergencies.service;

import cl.catastrofescl.emergencies.dto.CoordenadaDto;
import cl.catastrofescl.emergencies.dto.GeoJsonPoligonoDto;
import cl.catastrofescl.emergencies.dto.GeoJsonPuntoDto;
import cl.catastrofescl.emergencies.exception.GeometriaInvalidaException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Convierte entre DTOs GeoJSON / coordenadas simples y tipos JTS (Point, Polygon).
 * SRID fijo 4326 (WGS84) para todas las geometrias.
 */
@Component
public class GeometriaMapper {

    public static final int SRID_WGS84 = 4326;

    private final GeometryFactory geometryFactory =
            new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    public Point aPunto(CoordenadaDto coordenada) {
        if (coordenada == null) {
            return null;
        }
        Point punto = geometryFactory.createPoint(
                new Coordinate(coordenada.longitud(), coordenada.latitud()));
        punto.setSRID(SRID_WGS84);
        return punto;
    }

    public Polygon aPoligono(List<CoordenadaDto> coordenadas) {
        if (coordenadas == null || coordenadas.isEmpty()) {
            return null;
        }
        if (coordenadas.size() < 4) {
            throw new GeometriaInvalidaException(
                    "Un poligono requiere al menos 4 coordenadas (anillo cerrado)");
        }

        List<Coordinate> puntos = new ArrayList<>(coordenadas.size());
        for (CoordenadaDto c : coordenadas) {
            puntos.add(new Coordinate(c.longitud(), c.latitud()));
        }

        Coordinate primera = puntos.get(0);
        Coordinate ultima = puntos.get(puntos.size() - 1);
        if (!primera.equals2D(ultima)) {
            throw new GeometriaInvalidaException(
                    "El anillo del poligono debe cerrarse: primer y ultimo punto deben coincidir");
        }

        LinearRing anillo = geometryFactory.createLinearRing(puntos.toArray(new Coordinate[0]));
        Polygon poligono = geometryFactory.createPolygon(anillo);
        poligono.setSRID(SRID_WGS84);
        return poligono;
    }

    public GeoJsonPuntoDto aPuntoGeoJson(Point punto) {
        if (punto == null) {
            return null;
        }
        return GeoJsonPuntoDto.de(punto.getX(), punto.getY());
    }

    public GeoJsonPoligonoDto aPoligonoGeoJson(Polygon poligono) {
        if (poligono == null) {
            return null;
        }
        Coordinate[] coords = poligono.getExteriorRing().getCoordinates();
        List<List<Double>> anillo = new ArrayList<>(coords.length);
        for (Coordinate c : coords) {
            anillo.add(Collections.unmodifiableList(List.of(c.x, c.y)));
        }
        return GeoJsonPoligonoDto.de(anillo);
    }
}
