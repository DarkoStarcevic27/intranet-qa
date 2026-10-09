package com.darko.intranetqa.web;

import com.darko.intranetqa.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Tells the UI who is logged in, so it can show or hide upload controls. */
@RestController
public class MeController {

    private final AuthenticatedUser authenticatedUser;

    public MeController(AuthenticatedUser authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    @GetMapping("/api/me")
    public Me me(Authentication authentication) {
        return new Me(
                authenticatedUser.username(authentication),
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(a -> a.startsWith("ROLE_"))
                        .map(a -> a.substring(5).toLowerCase())
                        .sorted().toList(),
                authenticatedUser.documentGroups(authentication).stream().sorted().toList());
    }

    public record Me(String username, List<String> roles, List<String> groups) {
    }
}
