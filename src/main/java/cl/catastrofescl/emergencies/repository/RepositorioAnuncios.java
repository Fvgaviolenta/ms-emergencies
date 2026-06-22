package cl.catastrofescl.emergencies.repository;

import cl.catastrofescl.emergencies.entity.Anuncio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface RepositorioAnuncios extends JpaRepository<Anuncio, UUID> {

    /**
     * Lista todos los anuncios vigentes en un momento dado.
     * Vigente = vigenteDesde <= ahora AND (vigenteHasta IS NULL OR vigenteHasta > ahora).
     */
    @Query("""
            SELECT a FROM Anuncio a
            WHERE a.vigenteDesde <= :ahora
              AND (a.vigenteHasta IS NULL OR a.vigenteHasta > :ahora)
            ORDER BY a.severidad DESC, a.vigenteDesde DESC
            """)
    Page<Anuncio> listarVigentes(@Param("ahora") OffsetDateTime ahora, Pageable pageable);
}
