
package com.librarynetwork.backend.config;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.librarynetwork.backend.entity.Member;
import com.librarynetwork.backend.repository.MemberRepository;
import com.librarynetwork.backend.service.JwtService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final MemberRepository memberRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            MemberRepository memberRepository) {
        this.jwtService = jwtService;
        this.memberRepository = memberRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                String email = jwtService.extractEmail(token);

                if (SecurityContextHolder.getContext()
                        .getAuthentication() == null) {

                    Member member = memberRepository
                            .findByEmailIgnoreCase(email)
                            .orElse(null);

                    if (member != null) {
                        var authentication =
                                new UsernamePasswordAuthenticationToken(
                                        member.getEmail(),
                                        null,
                                        List.of(new SimpleGrantedAuthority(
                                                "ROLE_" +
                                                member.getRole().name()
                                        ))
                                );

                        SecurityContextHolder.getContext()
                                .setAuthentication(authentication);
                    }
                }
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
