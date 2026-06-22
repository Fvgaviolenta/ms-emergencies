package cl.catastrofescl.emergencies.seguridad;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MapeadorRolesFirebaseTest {

    @Test
    void adminAliasPasaAAdministrador() {
        assertThat(MapeadorRolesFirebase.normalizarUnRol("ADMIN"))
                .isEqualTo(ProveedorPermisos.ROL_ADMINISTRADOR);
    }

    @Test
    void authorityAliasPasaAAutoridad() {
        assertThat(MapeadorRolesFirebase.normalizar(List.of("AUTHORITY")))
                .containsExactly(ProveedorPermisos.ROL_AUTORIDAD);
    }

    @Test
    void citizenAliasPasaAParticular() {
        assertThat(MapeadorRolesFirebase.normalizarUnRol("CITIZEN"))
                .isEqualTo(ProveedorPermisos.ROL_PARTICULAR);
    }

    @Test
    void rolInternoSeMantiene() {
        assertThat(MapeadorRolesFirebase.normalizar(List.of("VOLUNTARIO")))
                .containsExactly(ProveedorPermisos.ROL_VOLUNTARIO);
    }
}
