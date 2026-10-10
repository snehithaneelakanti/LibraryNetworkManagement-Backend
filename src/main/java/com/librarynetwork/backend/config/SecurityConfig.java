
package com.librarynetwork.backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.librarynetwork.backend.repository.MemberRepository;
import com.librarynetwork.backend.service.JwtService;

@Configuration
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService,
            MemberRepository memberRepository) {
        return new JwtAuthenticationFilter(
                jwtService,
                memberRepository
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session
                .sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/auth/**").permitAll()

                // Calico AI chat endpoint
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/ai/chat"
                ).permitAll()

                // Public read-only catalogue endpoints
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/books",
                    "/api/books/**",
                    "/api/branches",
                    "/api/branches/**",
                    "/api/book-copies",
                    "/api/book-copies/**"
                ).permitAll()

                // Only administrators can create catalogue records
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/books",
                    "/api/books/**",
                    "/api/branches",
                    "/api/branches/**",
                    "/api/book-copies",
                    "/api/book-copies/**"
                ).hasRole("ADMIN")

                // Only administrators can update catalogue records
                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/books/**",
                    "/api/branches/**",
                    "/api/book-copies/**"
                ).hasRole("ADMIN")

                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/books/**",
                    "/api/branches/**",
                    "/api/book-copies/**"
                ).hasRole("ADMIN")

                // Only administrators can delete catalogue records
                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/books/**",
                    "/api/branches/**",
                    "/api/book-copies/**"
                ).hasRole("ADMIN")

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
            "http://localhost:5173"
        ));

        config.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type"
        ));

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
