package cl.catastrofescl.emergencies.service;

import cl.catastrofescl.emergencies.dto.CoordenadaDto;
import cl.catastrofescl.emergencies.dto.GeoJsonPuntoDto;
import cl.catastrofescl.emergencies.exception.GeometriaInvalidaException;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeometriaMapperTest {

    private final GeometriaMapper mapper = new GeometriaMapper();

    @Test
    void puntoDesdeCoordenada() {
        Point p = mapper.aPunto(new CoordenadaDto(-70.65, -33.43));
        assertThat(p).isNotNull();
        assertThat(p.getX()).isEqualTo(-70.65);
        assertThat(p.getY()).isEqualTo(-33.43);
        assertThat(p.getSRID()).isEqualTo(GeometriaMapper.SRID_WGS84);
    }

    @Test
    void puntoNuloSiCoordenadaNula() {
        assertThat(mapper.aPunto(null)).isNull();
    }

    @Test
    void poligonoCerradoValidoSeConstruye() {
        Polygon polygon = mapper.aPoligono(List.of(
                new CoordenadaDto(-70.7, -33.5),
                new CoordenadaDto(-70.6, -33.5),
                new CoordenadaDto(-70.6, -33.4),
                new CoordenadaDto(-70.7, -33.5)
        ));
        assertThat(polygon).isNotNull();
        assertThat(polygon.getExteriorRing().getNumPoints()).isEqualTo(4);
    }

    @Test
    void poligonoAbiertoLanzaExcepcion() {
        assertThatThrownBy(() -> mapper.aPoligono(List.of(
                new CoordenadaDto(-70.7, -33.5),
                new CoordenadaDto(-70.6, -33.5),
                new CoordenadaDto(-70.6, -33.4),
                new CoordenadaDto(-70.5, -33.3)
        ))).isInstanceOf(GeometriaInvalidaException.class)
                .hasMessageContaining("anillo");
    }

    @Test
    void menosDeCuatroCoordenadasLanzaExcepcion() {
        assertThatThrownBy(() -> mapper.aPoligono(List.of(
                new CoordenadaDto(-70.7, -33.5),
                new CoordenadaDto(-70.6, -33.4),
                new CoordenadaDto(-70.7, -33.5)
        ))).isInstanceOf(GeometriaInvalidaException.class)
                .hasMessageContaining("al menos 4");
    }

    @Test
    void puntoAGeoJson() {
        Point p = mapper.aPunto(new CoordenadaDto(-70.65, -33.43));
        GeoJsonPuntoDto dto = mapper.aPuntoGeoJson(p);
        assertThat(dto.type()).isEqualTo("Point");
        assertThat(dto.coordinates()).containsExactly(-70.65, -33.43);
    }
}
