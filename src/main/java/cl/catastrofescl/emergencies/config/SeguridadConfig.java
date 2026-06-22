package cl.catastrofescl.emergencies.config;

import cl.catastrofescl.emergencies.seguridad.FiltroAutenticacionDev;
import cl.catastrofescl.emergencies.seguridad.FiltroAutenticacionFirebase;
import cl.catastrofescl.emergencies.seguridad.ProveedorPermisos;
import com.google.firebase.auth.FirebaseAuth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Slf4j
@Configuration
@EnableMethodSecurity
public class SeguridadConfig {

    @Value("${catastrofescl.auth.dev-mode:false}")
    private boolean devMode;

    @Value("${catastrofescl.firebase.enabled:false}")
    private boolean firebaseEnabled;

    @Value("${catastrofescl.cors.allowed-origins:http://localhost:3000}")
    private List<String> origenesPermitidos;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ProveedorPermisos proveedorPermisos,
                                                   ObjectProvider<FirebaseAuth> firebaseAuthProvider)
            throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publicos
                        .requestMatchers(HttpMethod.GET, "/emergencies/active").permitAll()
                        .requestMatchers(HttpMethod.GET, "/emergencies/active/geojson").permitAll()
                        .requestMatchers(HttpMethod.GET, "/announcements").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Todo lo demas requiere autenticacion
                        .anyRequest().authenticated()
                );

        if (firebaseEnabled) {
            FirebaseAuth firebaseAuth = firebaseAuthProvider.getIfAvailable();
            if (firebaseAuth == null) {
                throw new IllegalStateException(
                        "catastrofescl.firebase.enabled=true pero no se pudo inicializar FirebaseAuth");
            }
            log.info("Seguridad configurada con FiltroAutenticacionFirebase (perfil prod)");
            http.addFilterBefore(new FiltroAutenticacionFirebase(firebaseAuth, proveedorPermisos),
                    UsernamePasswordAuthenticationFilter.class);
        } else if (devMode) {
            log.warn("Seguridad configurada en MODO DEV (filtro por headers X-Dev-*). NO usar en produccion.");
            http.addFilterBefore(new FiltroAutenticacionDev(proveedorPermisos),
                    UsernamePasswordAuthenticationFilter.class);
        } else {
            log.warn("Seguridad sin filtro de autenticacion configurado. Todos los endpoints protegidos responderan 401.");
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origenesPermitidos);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setExposedHeaders(List.of("Location"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", cors);
        return fuente;
    }
}
