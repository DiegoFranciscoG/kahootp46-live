package com.kahootp46.live.auth.config;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.kahootp46.live.shared.web.CorsProperties;

/**
 * Deny-by-default HTTP security. Tokens travel in the Authorization header (no cookies), so CSRF does not apply.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    private static final String ADMIN = "SCOPE_ADMIN";
    private static final String VIEWER = "SCOPE_VIEWER";

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, @Value("${app.serve-spa:false}") boolean serveSpa)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth
                            .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/games/connect").permitAll()
                            // The STOMP CONNECT frame carries the token; it is verified by the WebSocket interceptor.
                            .requestMatchers("/ws").permitAll()
                            .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                            .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                            .requestMatchers("/error").permitAll()
                            .requestMatchers("/api/quizzes/**", "/api/host/**").hasAuthority(ADMIN)
                            .requestMatchers(HttpMethod.GET, "/api/games/**").hasAnyAuthority(VIEWER, ADMIN)
                            .requestMatchers("/api/**", "/actuator/**").authenticated();
                    if (serveSpa) {
                        // Single-container deployment: the Angular bundle is public, everything under /api stays protected.
                        auth.requestMatchers(HttpMethod.GET, "/**").permitAll();
                        auth.requestMatchers(HttpMethod.HEAD, "/**").permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; "
                                        + "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'"))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                        .frameOptions(frame -> frame.deny()));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);
        config.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // BCrypt by default, with an {id} prefix so the algorithm can be upgraded later.
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
