package cl.catastrofescl.emergencies.seguridad;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LectorRolesDeclaradosTest {

    @Test
    void desdeClaimsFirebaseLeeListaDeRoles() {
        Set<String> roles = LectorRolesDeclarados.desdeClaimsFirebase(
                Map.of("roles", List.of("ADMINISTRADOR", "REGISTRADO")));
        assertThat(roles).containsExactlyInAnyOrder("ADMINISTRADOR", "REGISTRADO");
    }

    @Test
    void desdeClaimsFirebaseLeeArregloJava() {
        Set<String> roles = LectorRolesDeclarados.desdeClaimsFirebase(
                Map.of("roles", new String[]{"AUTORIDAD"}));
        assertThat(roles).containsExactly("AUTORIDAD");
    }

    @Test
    void desdeClaimsFirebaseLeeClaimLegacyRole() {
        Set<String> roles = LectorRolesDeclarados.desdeClaimsFirebase(Map.of("role", "ADMIN"));
        assertThat(roles).containsExactly("ADMIN");
    }

    @Test
    void desdeListaSeparadaPorComaNormalizaEspacios() {
        Set<String> roles = LectorRolesDeclarados.desdeListaSeparadaPorComa(" AUTORIDAD , ADMINISTRADOR ");
        assertThat(roles).containsExactlyInAnyOrder("AUTORIDAD", "ADMINISTRADOR");
    }
}
