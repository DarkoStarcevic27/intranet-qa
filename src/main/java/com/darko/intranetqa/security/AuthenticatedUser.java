package com.darko.intranetqa.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads the access-scoping facts out of the current request's JWT: who the
 * caller is, and which document groups they're allowed to see.
 *
 * <p>This is the piece that makes retrieval permission-aware: every vector
 * search is filtered down to documents whose {@code allowedGroups} metadata
 * intersects with the caller's own groups, so the model is never handed
 * context the user isn't allowed to read.
 */
@Component
public class AuthenticatedUser {

    /** Claim on the Keycloak token holding the document-access groups for this user. */
    private static final String GROUPS_CLAIM = "document_groups";

    public String username(Authentication authentication) {
        return jwt(authentication).getClaimAsString("preferred_username");
    }

    public boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @SuppressWarnings("unchecked")
    public Set<String> documentGroups(Authentication authentication) {
        Jwt jwt = jwt(authentication);
        List<String> groups = jwt.getClaim(GROUPS_CLAIM);
        return groups == null ? Set.of() : Set.copyOf(groups);
    }

    /**
     * Builds a Spring AI vector-store filter expression (e.g.
     * {@code allowedGroup in ["engineering","finance"]}) that restricts
     * retrieval to documents the caller is entitled to see. Each indexed
     * chunk carries a single owning {@code allowedGroup}; a document that
     * should be visible to several groups is ingested once per group (see
     * IngestionService), which keeps the filter a simple scalar match
     * instead of requiring list-intersection support the filter DSL
     * doesn't have. Returns a filter that matches nothing for users with
     * no groups, rather than silently returning unfiltered (i.e.
     * everything).
     */
    public String filterExpression(Authentication authentication) {
        Set<String> groups = documentGroups(authentication);
        if (groups.isEmpty()) {
            return "allowedGroup == 'none'";
        }
        String quoted = groups.stream()
                .sorted()
                .map(g -> "'" + g.replace("'", "") + "'")
                .collect(Collectors.joining(","));
        return "allowedGroup in [" + quoted + "]";
    }

    private Jwt jwt(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }
        throw new IllegalStateException(
                "Expected a JWT-authenticated request; got " + authentication.getClass());
    }
}
