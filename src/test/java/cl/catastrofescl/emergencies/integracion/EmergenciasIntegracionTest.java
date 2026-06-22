package cl.catastrofescl.emergencies.integracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.web.FilterChainProxy;

import java.util.UUID;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EmergenciasIntegracionTest extends BaseIntegracionTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(wac)
                .addFilter(springSecurityFilterChain, "/*")
                .build();
    }

    private final UUID uidDev = UUID.randomUUID();

    @Test
    void declararEmergenciaDevuelve201YLaEmergenciaEsListadaEnActivas() throws Exception {
        ObjectNode cuerpo = objectMapper.createObjectNode();
        cuerpo.put("tipo", "TERREMOTO");
        cuerpo.put("severidad", "ALTA");
        cuerpo.put("region", "Metropolitana");
        ObjectNode epicentro = cuerpo.putObject("epicentro");
        epicentro.put("longitud", -70.65);
        epicentro.put("latitud", -33.43);

        MvcResult resultado = mvc().perform(post("/emergencies")
                        .header("X-Dev-Firebase-Uid", uidDev.toString())
                        .header("X-Dev-Roles", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.tipo", is("TERREMOTO")))
                .andExpect(jsonPath("$.estado", is("ACTIVA")))
                .andExpect(jsonPath("$.coordenadasEpicentro.type", is("Point")))
                .andReturn();

        String id = objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asText();

        mvc().perform(get("/emergencies/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", notNullValue()));

        // Transicion valida ACTIVA -> CONTROLADA
        ObjectNode cambio = objectMapper.createObjectNode();
        cambio.put("nuevoEstado", "CONTROLADA");

        mvc().perform(patch("/emergencies/" + id + "/status")
                        .header("X-Dev-Firebase-Uid", uidDev.toString())
                        .header("X-Dev-Roles", "AUTORIDAD")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cambio)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("CONTROLADA")));
    }

    @Test
    void transicionInvalidaDevuelveRfc7807() throws Exception {
        // Primero creamos y finalizamos una emergencia
        ObjectNode cuerpo = objectMapper.createObjectNode();
        cuerpo.put("tipo", "INCENDIO");
        cuerpo.put("severidad", "MEDIA");
        cuerpo.put("region", "Biobio");
        ObjectNode epicentro = cuerpo.putObject("epicentro");
        epicentro.put("longitud", -73.1);
        epicentro.put("latitud", -37.0);

        MvcResult creada = mvc().perform(post("/emergencies")
                        .header("X-Dev-Firebase-Uid", uidDev.toString())
                        .header("X-Dev-Roles", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(creada.getResponse().getContentAsString()).get("id").asText();

        // Pasamos directo a FINALIZADA (valido)
        ObjectNode aFinalizada = objectMapper.createObjectNode();
        aFinalizada.put("nuevoEstado", "FINALIZADA");
        mvc().perform(patch("/emergencies/" + id + "/status")
                        .header("X-Dev-Firebase-Uid", uidDev.toString())
                        .header("X-Dev-Roles", "AUTORIDAD")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aFinalizada)))
                .andExpect(status().isOk());

        // Intentamos volver a ACTIVA (invalido)
        ObjectNode aActiva = objectMapper.createObjectNode();
        aActiva.put("nuevoEstado", "ACTIVA");
        mvc().perform(patch("/emergencies/" + id + "/status")
                        .header("X-Dev-Firebase-Uid", uidDev.toString())
                        .header("X-Dev-Roles", "AUTORIDAD")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aActiva)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("EMERGENCY_INVALID_TRANSITION")))
                .andExpect(jsonPath("$.estadoActual", is("FINALIZADA")))
                .andExpect(jsonPath("$.estadoSolicitado", is("ACTIVA")));
    }

    @Test
    void listadoPublicoDeActivasNoRequiereAutenticacion() throws Exception {
        mvc().perform(get("/emergencies/active"))
                .andExpect(status().isOk());
    }

    @Test
    void listadoGeoJsonDeActivasDevuelveFeatureCollectionPublico() throws Exception {
        ObjectNode cuerpo = objectMapper.createObjectNode();
        cuerpo.put("tipo", "INUNDACION");
        cuerpo.put("severidad", "ALTA");
        cuerpo.put("region", "Maule");
        ObjectNode epicentro = cuerpo.putObject("epicentro");
        epicentro.put("longitud", -71.66);
        epicentro.put("latitud", -35.42);
        ArrayNode zonaImpacto = cuerpo.putArray("zonaImpacto");
        agregarCoordenada(zonaImpacto, -71.70, -35.45);
        agregarCoordenada(zonaImpacto, -71.60, -35.45);
        agregarCoordenada(zonaImpacto, -71.60, -35.35);
        agregarCoordenada(zonaImpacto, -71.70, -35.45);

        mvc().perform(post("/emergencies")
                        .header("X-Dev-Firebase-Uid", uidDev.toString())
                        .header("X-Dev-Roles", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated());

        mvc().perform(get("/emergencies/active/geojson"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("FeatureCollection")))
                .andExpect(jsonPath("$.features[0].type", is("Feature")))
                .andExpect(jsonPath("$.features[0].geometry.type", is("Polygon")))
                .andExpect(jsonPath("$.features[0].properties.id", notNullValue()))
                .andExpect(jsonPath("$.features[0].properties.estado", is("ACTIVA")));
    }

    @Test
    void listadoPublicoDeAnunciosNoRequiereAutenticacion() throws Exception {
        mvc().perform(get("/announcements"))
                .andExpect(status().isOk());
    }

    @Test
    void endpointProtegidoSinAuthDevuelve401() throws Exception {
        ObjectNode cuerpo = objectMapper.createObjectNode();
        cuerpo.put("tipo", "TSUNAMI");
        cuerpo.put("severidad", "ALTA");
        cuerpo.put("region", "Valparaiso");
        ObjectNode epicentro = cuerpo.putObject("epicentro");
        epicentro.put("longitud", -71.6);
        epicentro.put("latitud", -33.0);

        mvc().perform(post("/emergencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rolSinPermisoDevuelve403() throws Exception {
        ObjectNode cuerpo = objectMapper.createObjectNode();
        cuerpo.put("tipo", "INUNDACION");
        cuerpo.put("severidad", "BAJA");
        cuerpo.put("region", "OHiggins");
        ObjectNode epicentro = cuerpo.putObject("epicentro");
        epicentro.put("longitud", -71.0);
        epicentro.put("latitud", -34.0);

        mvc().perform(post("/emergencies")
                        .header("X-Dev-Firebase-Uid", uidDev.toString())
                        .header("X-Dev-Roles", "PARTICULAR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    void publicarAnuncioEnEmergenciaFinalizadaDevuelve409() throws Exception {
        // Crear emergencia
        ObjectNode cuerpo = objectMapper.createObjectNode();
        cuerpo.put("tipo", "ALUVION");
        cuerpo.put("severidad", "MEDIA");
        cuerpo.put("region", "Atacama");
        ObjectNode epicentro = cuerpo.putObject("epicentro");
        epicentro.put("longitud", -70.3);
        epicentro.put("latitud", -27.3);

        MvcResult res = mvc().perform(post("/emergencies")
                        .header("X-Dev-Firebase-Uid", uidDev.toString())
                        .header("X-Dev-Roles", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andReturn();
        String emergenciaId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asText();

        // Finalizarla
        ObjectNode fin = objectMapper.createObjectNode();
        fin.put("nuevoEstado", "FINALIZADA");
        mvc().perform(patch("/emergencies/" + emergenciaId + "/status")
                        .header("X-Dev-Firebase-Uid", uidDev.toString())
                        .header("X-Dev-Roles", "AUTORIDAD")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fin)))
                .andExpect(status().isOk());

        // Intentar publicar anuncio
        ObjectNode anuncio = objectMapper.createObjectNode();
        anuncio.put("emergenciaId", emergenciaId);
        anuncio.put("titulo", "Prueba");
        anuncio.put("contenido", "Cuerpo");
        anuncio.put("severidad", "URGENTE");
        anuncio.put("alcance", "REGIONAL");
        anuncio.put("region", "Atacama");

        mvc().perform(post("/announcements")
                        .header("X-Dev-Firebase-Uid", uidDev.toString())
                        .header("X-Dev-Roles", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(anuncio)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode", is("EMERGENCY_NOT_ACTIVE")));
    }

    private void agregarCoordenada(ArrayNode arreglo, double longitud, double latitud) {
        ObjectNode coordenada = arreglo.addObject();
        coordenada.put("longitud", longitud);
        coordenada.put("latitud", latitud);
    }

    @Test
    void declararConCentrosPublicaEventoYConsumidorRegistraProcesamiento() throws Exception {
        ObjectNode cuerpo = objectMapper.createObjectNode();
        cuerpo.put("tipo", "TERREMOTO");
        cuerpo.put("severidad", "ALTA");
        cuerpo.put("region", "Metropolitana");
        ObjectNode epicentro = cuerpo.putObject("epicentro");
        epicentro.put("longitud", -70.65);
        epicentro.put("latitud", -33.43);
        var centros = cuerpo.putArray("centers");
        ObjectNode c1 = centros.addObject();
        c1.put("nombre", "Acopio Estadio");
        ObjectNode u1 = c1.putObject("ubicacion");
        u1.put("longitud", -70.64);
        u1.put("latitud", -33.44);
        c1.put("capacidadEstimada", 900);

        MvcResult creada = mvc().perform(post("/emergencies")
                        .header("X-Dev-Firebase-Uid", uidDev.toString())
                        .header("X-Dev-Roles", "ADMINISTRADOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        String id = objectMapper.readTree(creada.getResponse().getContentAsString()).get("id").asText();

        await().atMost(5, SECONDS).pollInterval(200, MILLISECONDS).until(() -> {
            try {
                MvcResult detalle = mvc().perform(get("/emergencies/" + id)
                                .header("X-Dev-Firebase-Uid", uidDev.toString())
                                .header("X-Dev-Roles", "AUTORIDAD"))
                        .andExpect(status().isOk())
                        .andReturn();
                var nodo = objectMapper.readTree(detalle.getResponse().getContentAsString());
                return nodo.hasNonNull("procesamientoColaEmergenciaCreadaEn");
            } catch (Exception ex) {
                return false;
            }
        });

        mvc().perform(get("/announcements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].emergenciaId", is(id)))
                .andExpect(jsonPath("$.content[0].titulo", org.hamcrest.Matchers.containsString("Emergencia activa")));
    }
}
