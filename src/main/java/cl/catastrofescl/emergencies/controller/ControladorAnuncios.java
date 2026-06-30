package cl.catastrofescl.emergencies.controller;

import cl.catastrofescl.emergencies.dto.request.ActualizarAnuncioRequest;
import cl.catastrofescl.emergencies.dto.request.PublicarAnuncioRequest;
import cl.catastrofescl.emergencies.dto.response.AnuncioResponse;
import cl.catastrofescl.emergencies.service.ServicioAnuncios;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Anuncios", description = "Publicacion y consulta de anuncios criticos asociados a emergencias")
@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class ControladorAnuncios {

        private final ServicioAnuncios servicioAnuncios;

        @Operation(summary = "Publica un anuncio oficial asociado a una emergencia", description = "Requiere permiso ANUNCIO_PUBLICAR. Publica el evento announcement.published.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Anuncio publicado"),
                        @ApiResponse(responseCode = "404", description = "Emergencia referenciada no existe", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "409", description = "Emergencia en estado FINALIZADA", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        @PostMapping
        @PreAuthorize("hasAuthority('ANUNCIO_PUBLICAR')")
        public ResponseEntity<AnuncioResponse> publicar(@Valid @RequestBody PublicarAnuncioRequest solicitud) {
                AnuncioResponse creado = servicioAnuncios.publicar(solicitud);
                URI ubicacion = ServletUriComponentsBuilder
                                .fromCurrentRequest()
                                .path("/{id}")
                                .buildAndExpand(creado.id())
                                .toUri();
                return ResponseEntity.created(ubicacion).body(creado);
        }

        @Operation(summary = "Lista los anuncios vigentes", description = "Endpoint PUBLICO. Paginado (page, size). El orden viene fijado por el servidor "
                        + "(severidad descendente, luego vigenteDesde). Los parametros `sort` del Pageable "
                        + "no se utilizan.")
        @GetMapping
        public Page<AnuncioResponse> listarVigentes(
                        @PageableDefault(size = 20) Pageable pageable) {
                return servicioAnuncios.listarVigentes(pageable);
        }

        @Operation(summary = "Actualiza un anuncio vigente", description = "Requiere permiso ANUNCIO_PUBLICAR.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Anuncio actualizado"),
                        @ApiResponse(responseCode = "404", description = "Anuncio no encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        // Publicar anuncio
        @PatchMapping("/{id}")
        @PreAuthorize("hasAuthority('ANUNCIO_PUBLICAR')")
        public AnuncioResponse actualizar(
                        @PathVariable UUID id,
                        @Valid @RequestBody ActualizarAnuncioRequest solicitud) {
                return servicioAnuncios.actualizar(id, solicitud);
        }
}
