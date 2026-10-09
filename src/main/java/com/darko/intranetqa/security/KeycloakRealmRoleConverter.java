package com.darko.intranetqa.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Maps Keycloak's {@code realm_access.roles} claim onto Spring Security
 * authorities (prefixed with {@code ROLE_}), so {@code @PreAuthorize} and
 * {@code hasRole(...)} work the same way they would with any other
 * Spring Security setup.
 */
@Component
public class KeycloakRealmRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    @SuppressWarnings("unchecked")
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        List<String> roles = realmAccess == null
                ? List.of()
                : (List<String>) realmAccess.getOrDefault("roles", List.of());

        Collection<GrantedAuthority> authorities = roles.stream()
                .filter(Objects::nonNull)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());

        return new JwtAuthenticationToken(jwt, authorities);
    }
}
