package com.iset.projet_integration.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    // ============================
    // CORS
    // ============================
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ============================
    // JWT Converter pour Keycloak
    // ============================
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            List<String> roles = (realmAccess != null && realmAccess.get("roles") != null)
                    ? (List<String>) realmAccess.get("roles")
                    : List.of();

            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        });

        return converter;
    }

    // ============================
    // SECURITY FILTER
    // ============================
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/keycloak/register",
                                "/api/keycloak/login",
                                "/api/keycloak/profile",  // ⚠️ AJOUT: autoriser profile
                                "/api/keycloak/**",  // ⚠️ AJOUT: autoriser profile
                                "/api/users/sync",
                                "/api/users/ajouter",
                                "/api/admin/summary-report",
                                "/api/notifications/user/**",
                                "/api/users/**",
                                "/api/notifications/**",
                                "/donations",
                                "/donations/**",
                                "/api/keycloak/profile/{username}",
                                "/api/keycloak/profile/{username}/password",
                                "/api/posts/**"


                ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(request -> {
                            String path = request.getRequestURI();

                            // 🚀 Autoriser aucun token pour les endpoints publics
                            if (path.startsWith("/api/keycloak/register")
                                    || path.startsWith("/api/keycloak/login")
                                    || path.startsWith("/api/keycloak/profile")
                                    || path.startsWith("/api/keycloak/**")  // ⚠️ AJOUT
                                    || path.startsWith("/api/users/sync")
                                    || path.startsWith("/api/users/ajouter")
                                   // || path.startsWith("/api/admin/summary-report")
                                    || path.startsWith("/api/notifications/user/**")
                                    || path.startsWith("/api/notifications/**")
                                    || path.startsWith("/donations")
                                    || path.startsWith("/api/posts/**")
                                    || path.startsWith("/api/users/**")

                            ) {
                                return null;
                            }

                            // Pour les endpoints protégés → chercher le token normalement
                            String authHeader = request.getHeader("Authorization");
                            return (authHeader != null && authHeader.startsWith("Bearer "))
                                    ? authHeader.substring(7)
                                    : null;
                        })
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                );

        return http.build();
    }
}