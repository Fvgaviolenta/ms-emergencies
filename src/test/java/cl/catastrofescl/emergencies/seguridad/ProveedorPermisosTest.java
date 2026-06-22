package cl.catastrofescl.emergencies.seguridad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProveedorPermisosTest {

    private ProveedorPermisos proveedor;

    @BeforeEach
    void inicializar() throws IOException {
        proveedor = new ProveedorPermisos(new DefaultResourceLoader());
    }

    @Test
    void administradorTieneTodosLosPermisosDelModulo() {
        Set<String> permisos = proveedor.permisosPara(List.of(ProveedorPermisos.ROL_ADMINISTRADOR));
        assertThat(permisos).contains(
                ProveedorPermisos.EMERGENCIA_DECLARAR,
                ProveedorPermisos.EMERGENCIA_GESTIONAR,
                ProveedorPermisos.ANUNCIO_PUBLICAR
        );
    }

    @Test
    void aliasAdminDeFirebaseOtorgaPermisosDeAdministrador() {
        Set<String> permisos = proveedor.permisosPara(List.of("ADMIN"));
        assertThat(permisos).contains(
                ProveedorPermisos.EMERGENCIA_DECLARAR,
                ProveedorPermisos.ANUNCIO_PUBLICAR
        );
    }

    @Test
    void autoridadTieneLosMismosPermisosCriticosQueAdmin() {
        Set<String> permisos = proveedor.permisosPara(List.of(ProveedorPermisos.ROL_AUTORIDAD));
        assertThat(permisos).contains(
                ProveedorPermisos.EMERGENCIA_DECLARAR,
                ProveedorPermisos.EMERGENCIA_GESTIONAR,
                ProveedorPermisos.ANUNCIO_PUBLICAR
        );
    }

    @Test
    void operadorNoDeclaraEmergencias() {
        Set<String> permisos = proveedor.permisosPara(List.of(ProveedorPermisos.ROL_OPERADOR));
        assertThat(permisos).isEmpty();
    }

    @Test
    void particularNoGestionaAnuncios() {
        Set<String> permisos = proveedor.permisosPara(List.of(ProveedorPermisos.ROL_PARTICULAR));
        assertThat(permisos).isEmpty();
    }

    @Test
    void combinacionDeRolesSumaPermisos() {
        Set<String> permisos = proveedor.permisosPara(
                List.of(ProveedorPermisos.ROL_PARTICULAR, ProveedorPermisos.ROL_AUTORIDAD));
        assertThat(permisos).contains(ProveedorPermisos.ANUNCIO_PUBLICAR);
    }

    @Test
    void combinacionAliasEInterno() {
        Set<String> permisos = proveedor.permisosPara(List.of("PARTICULAR", "AUTHORITY"));
        assertThat(permisos).contains(ProveedorPermisos.EMERGENCIA_DECLARAR);
    }

    @Test
    void rolesNulosDevuelveSetVacio() {
        assertThat(proveedor.permisosPara(null)).isEmpty();
    }

    @Test
    void rolDesconocidoSeIgnora() {
        Set<String> permisos = proveedor.permisosPara(List.of("ROL_INEXISTENTE"));
        assertThat(permisos).isEmpty();
    }

    @Test
    void matrizYamlSobrescribibleEnPruebasAisladas() {
        ProveedorPermisos custom = new ProveedorPermisos(Map.of(
                ProveedorPermisos.ROL_VOLUNTARIO,
                Set.of(ProveedorPermisos.EMERGENCIA_GESTIONAR)
        ));
        assertThat(custom.permisosPara(List.of(ProveedorPermisos.ROL_VOLUNTARIO)))
                .containsExactly(ProveedorPermisos.EMERGENCIA_GESTIONAR);
    }
}
