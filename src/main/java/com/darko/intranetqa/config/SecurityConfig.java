package com.darko.intranetqa.config;

import com.darko.intranetqa.security.KeycloakRealmRoleConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, KeycloakRealmRoleConverter converter) throws Exception {
        http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable()) // stateless, token-based API; no browser session to forge
            .authorizeHttpRequests(auth -> auth
                // A streamed response finishes with an ASYNC re-dispatch that carries no token;
                // the original REQUEST dispatch was already authenticated and authorized.
                .requestMatchers(request -> request.getDispatcherType() == jakarta.servlet.DispatcherType.ASYNC).permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/", "/index.html", "/favicon.ico").permitAll() // the UI shell; every API call still needs a token
                .requestMatchers("/api/documents/**").hasAnyRole("UPLOADER", "ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));

        return http.build();
    }
}
