package com.example.movietickets.security;

import org.springframework.security.core.context.SecurityContextHolder;

/** Pulls the authenticated user's id off the security context — never trust a client-supplied userId. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
            .getAuthentication()
            .getPrincipal();
        return principal.getUserId();
    }
}
