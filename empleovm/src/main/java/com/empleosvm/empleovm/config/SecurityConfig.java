package com.empleosvm.empleovm.config;

import com.empleosvm.empleovm.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
            UserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Value("${cors.allowed-origins}")
    private String allowedOriginsRaw;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // ─────────────────────────────────────────────────────────────
                // Configuración base
                // ─────────────────────────────────────────────────────────────
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                // ─────────────────────────────────────────────────────────────
                // Permisos
                // ─────────────────────────────────────────────────────────────
                .authorizeHttpRequests(auth -> auth

                        // =====================================================
                        // FRONTEND / ARCHIVOS ESTÁTICOS
                        // =====================================================

                        // Página principal
                        .requestMatchers("/", "/index.html").permitAll()

                        // HTML / CSS / JS
                        .requestMatchers(
                                "/*.html",
                                "/*.css",
                                "/*.js",
                                "/js/**",
                                "/css/**",
                                "/assets/**",
                                "/images/**",
                                "/img/**",
                                "/favicon.ico")
                        .permitAll()

                        // Uploads
                        .requestMatchers(
                                "/uploads/fotos/**",
                                "/uploads/fotoPerfil/**")
                        .permitAll()

                        // =====================================================
                        // AUTH
                        // =====================================================

                        .requestMatchers(HttpMethod.POST,
                                "/api/usuarios/login")
                        .permitAll()

                        .requestMatchers(HttpMethod.POST,
                                "/api/usuarios")
                        .permitAll()

                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/refresh")
                        .permitAll()

                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/logout")
                        .permitAll()

                        // =====================================================
                        // RUTAS PÚBLICAS
                        // =====================================================

                        .requestMatchers(HttpMethod.GET,
                                "/api/usuarios/generar-hash/**")
                        .permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/api/empleos")
                        .permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/api/empleos/{id}")
                        .permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/api/empleos/buscar")
                        .permitAll()

                        // =====================================================
                        // EMPRESA
                        // =====================================================

                        .requestMatchers(HttpMethod.POST,
                                "/api/empleos/**")
                        .hasAnyAuthority("ROLE_EMPRESA", "ROLE_ADMIN")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/empleos/**")
                        .hasAnyAuthority("ROLE_EMPRESA", "ROLE_ADMIN")

                        .requestMatchers(HttpMethod.PATCH,
                                "/api/empleos/**")
                        .hasAnyAuthority("ROLE_EMPRESA", "ROLE_ADMIN")

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/empleos/**")
                        .hasAnyAuthority("ROLE_EMPRESA", "ROLE_ADMIN")

                        .requestMatchers(
                                "/api/estadisticas/**")
                        .hasAnyAuthority("ROLE_EMPRESA", "ROLE_ADMIN")

                        .requestMatchers(HttpMethod.GET,
                                "/api/postulaciones/por-empleo/**")
                        .hasAnyAuthority("ROLE_EMPRESA", "ROLE_ADMIN")

                        .requestMatchers(HttpMethod.PATCH,
                                "/api/postulaciones/**")
                        .hasAnyAuthority("ROLE_EMPRESA", "ROLE_ADMIN")

                        .requestMatchers(HttpMethod.GET,
                                "/api/postulaciones/cv/**")
                        .hasAnyAuthority(
                                "ROLE_EMPRESA",
                                "ROLE_ADMIN",
                                "ROLE_USER")

                        // =====================================================
                        // ADMIN
                        // =====================================================

                        .requestMatchers(
                                "/api/usuarios/solicitudes-empresa")
                        .hasAuthority("ROLE_ADMIN")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/usuarios/*/aprobar-empresa")
                        .hasAuthority("ROLE_ADMIN")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/usuarios/*/rechazar-empresa")
                        .hasAuthority("ROLE_ADMIN")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/usuarios/*/switch-rol")
                        .hasAuthority("ROLE_ADMIN")

                        .requestMatchers(HttpMethod.GET,
                                "/api/usuarios")
                        .hasAuthority("ROLE_ADMIN")

                        // =====================================================
                        // PERFIL
                        // =====================================================

                        .requestMatchers(HttpMethod.GET,
                                "/api/perfil/**")
                        .authenticated()

                        .requestMatchers(HttpMethod.POST,
                                "/api/perfil/**")
                        .authenticated()

                        .requestMatchers(HttpMethod.PATCH,
                                "/api/perfil/**")
                        .authenticated()

                        // =====================================================
                        // TODO LO DEMÁS
                        // =====================================================

                        .anyRequest().authenticated())

                // ─────────────────────────────────────────────────────────────
                // AUTH PROVIDER + JWT
                // ─────────────────────────────────────────────────────────────
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .toList();

        config.setAllowedOrigins(origins);

        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept"));

        config.setExposedHeaders(List.of(
                "Authorization"));

        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {

        return config.getAuthenticationManager();
    }
}