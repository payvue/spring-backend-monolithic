package com.payvue.supporting.user.command;

public record OAuthPrincipal(
    String provider,
    String providerId,
    String email
) implements AuthenticatedPrincipal {}
