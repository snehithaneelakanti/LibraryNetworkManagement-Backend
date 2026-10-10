
package com.librarynetwork.backend.dto;

public record AuthResponse(
        String token,
        String tokenType,
        Long memberId,
        String name,
        String email,
        String role
) {}
