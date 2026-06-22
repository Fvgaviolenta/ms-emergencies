package cl.catastrofescl.emergencies.controller;

import cl.catastrofescl.emergencies.dto.request.CambiarEstadoEmergenciaRequest;
import cl.catastrofescl.emergencies.dto.request.DeclararEmergenciaRequest;
import cl.catastrofescl.emergencies.dto.response.ColeccionGeoJsonEmergenciasResponse;
import cl.catastrofescl.emergencies.dto.response.EmergenciaResponse;
import cl.catastrofescl.emergencies.service.ServicioEmergencias;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import java.util.List;
import java.util.UUID;

@Tag(name = "Emergencias", description = "Ciclo de vida de emergencias (declaracion, transicion de estados, consulta)")
@RestController
@RequestMapping("/emergencies")
@RequiredArgsConstructor
public class ControladorEmergencias {

    private final ServicioEmergencias servicioEmergencias;

    @Operation(summary = "Declara una nueva emergencia",
            description = "Requiere permiso EMERGENCIA_DECLARAR. Opcionalmente incluye centros de acopio "
                    + "asociados (`centrosAcopio` o alias JSON `centers`) en el payload; los centros NO se "
                    + "persisten aqui sino en ms-resources via evento emergency.created. Consulte "
                    + "GET /centros?emergenciaId= tras un breve intervalo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Emergencia creada",
                    content = @Content(schema = @Schema(implementation = EmergenciaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Cuerpo invalido (validacion)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Permiso insuficiente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @PreAuthorize("hasAuthority('EMERGENCIA_DECLARAR')")
    public ResponseEntity<EmergenciaResponse> declarar(@Valid @RequestBody DeclararEmergenciaRequest solicitud) {
        EmergenciaResponse creada = servicioEmergencias.declarar(solicitud);
        URI ubicacion = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creada.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @Operation(summary = "Cambia el estado de una emergencia",
            description = "Transiciones validas: ACTIVA -> CONTROLADA -> FINALIZADA. Publica emergency.status.changed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "404", description = "Emergencia no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Transicion invalida",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('EMERGENCIA_GESTIONAR')")
    public EmergenciaResponse cambiarEstado(@PathVariable("id") UUID id,
                                            @Valid @RequestBody CambiarEstadoEmergenciaRequest solicitud) {
        return servicioEmergencias.cambiarEstado(id, solicitud);
    }

    @Operation(summary = "Lista las emergencias en estado ACTIVA",
            description = "Endpoint PUBLICO. Devuelve datos geoespaciales completos en formato GeoJSON.")
    @ApiResponse(responseCode = "200", description = "Listado de emergencias activas")
    @GetMapping("/active")
    public List<EmergenciaResponse> listarActivas() {
        return servicioEmergencias.listarActivas();
    }

    @Operation(summary = "Lista los poligonos GeoJSON de emergencias activas",
            description = "Endpoint PUBLICO. Devuelve un FeatureCollection minimo para pintar zonas de impacto en mapas.")
    @ApiResponse(responseCode = "200", description = "Coleccion GeoJSON de poligonos activos",
            content = @Content(schema = @Schema(implementation = ColeccionGeoJsonEmergenciasResponse.class)))
    @GetMapping("/active/geojson")
    public ColeccionGeoJsonEmergenciasResponse listarGeoJsonActivas() {
        return servicioEmergencias.listarGeoJsonActivas();
    }

    @Operation(summary = "Obtiene el detalle de una emergencia por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Emergencia encontrada"),
            @ApiResponse(responseCode = "404", description = "Emergencia no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EMERGENCIA_GESTIONAR')")
    public EmergenciaResponse obtener(@PathVariable("id") UUID id) {
        return servicioEmergencias.obtener(id);
    }
}
