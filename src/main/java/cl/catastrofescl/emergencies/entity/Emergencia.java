package cl.catastrofescl.emergencies.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad que representa una emergencia declarada en el sistema.
 * Incluye coordenadas del epicentro (POINT) y zona de impacto (POLYGON) en SRID 4326.
 */
@Entity
@Table(name = "emergencias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Emergencia {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoEmergencia tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "severidad", nullable = false, length = 20)
    private SeveridadEmergencia severidad;

    @Column(name = "region", nullable = false, length = 100)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoEmergencia estado;

    @JdbcTypeCode(SqlTypes.GEOGRAPHY)
    @Column(name = "coordenadas_epicentro", columnDefinition = "geography(Point,4326)")
    private Point coordenadasEpicentro;

    @JdbcTypeCode(SqlTypes.GEOGRAPHY)
    @Column(name = "zona_impacto", columnDefinition = "geography(Polygon,4326)")
    private Polygon zonaImpacto;

    @Column(name = "declarada_por_usuario_id", nullable = false)
    private UUID declaradaPorUsuarioId;

    @Column(name = "declarada_en", nullable = false)
    private OffsetDateTime declaradaEn;

    @Column(name = "actualizada_en", nullable = false)
    private OffsetDateTime actualizadaEn;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (estado == null) {
            estado = EstadoEmergencia.ACTIVA;
        }
        OffsetDateTime ahora = OffsetDateTime.now();
        if (declaradaEn == null) {
            declaradaEn = ahora;
        }
        actualizadaEn = ahora;
    }

    @PreUpdate
    public void preUpdate() {
        actualizadaEn = OffsetDateTime.now();
    }
}
