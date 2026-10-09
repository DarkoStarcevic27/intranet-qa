package com.darko.intranetqa.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticatedUserTest {

    private final AuthenticatedUser authenticatedUser = new AuthenticatedUser();

    @Test
    void buildsFilterExpressionFromDocumentGroupsClaim() {
        var token = tokenWithGroups(List.of("engineering", "finance"));

        String expression = authenticatedUser.filterExpression(token);

        assertThat(expression).isEqualTo("allowedGroup in ['engineering','finance']");
    }

    @Test
    void matchesNothingWhenUserHasNoGroups() {
        var token = tokenWithGroups(List.of());

        String expression = authenticatedUser.filterExpression(token);

        assertThat(expression).isEqualTo("allowedGroup == 'none'");
    }

    private JwtAuthenticationToken tokenWithGroups(List<String> groups) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("preferred_username", "darko")
                .claim("document_groups", groups)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        return new JwtAuthenticationToken(jwt, List.of());
    }
}
