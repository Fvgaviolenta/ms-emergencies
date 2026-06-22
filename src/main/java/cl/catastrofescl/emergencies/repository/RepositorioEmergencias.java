package cl.catastrofescl.emergencies.repository;

import cl.catastrofescl.emergencies.entity.Emergencia;
import cl.catastrofescl.emergencies.entity.EstadoEmergencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface RepositorioEmergencias extends JpaRepository<Emergencia, UUID> {

    List<Emergencia> findByEstadoOrderByDeclaradaEnDesc(EstadoEmergencia estado);

    @Query(value = """
            SELECT
                id AS id,
                tipo AS tipo,
                severidad AS severidad,
                region AS region,
                estado AS estado,
                declarada_en AS "declaradaEn",
                ST_AsGeoJSON(zona_impacto::geometry) AS "zonaImpactoGeoJson"
            FROM emergencias
            WHERE estado = 'ACTIVA'
              AND zona_impacto IS NOT NULL
            ORDER BY declarada_en DESC
            """, nativeQuery = true)
    List<ProyeccionPoligonoEmergenciaActiva> listarPoligonosActivosGeoJson();
}
