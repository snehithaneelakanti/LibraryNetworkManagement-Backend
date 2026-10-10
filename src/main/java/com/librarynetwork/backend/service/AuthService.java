
package com.librarynetwork.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.librarynetwork.backend.dto.AuthResponse;
import com.librarynetwork.backend.dto.LoginRequest;
import com.librarynetwork.backend.dto.RegisterRequest;
import com.librarynetwork.backend.entity.Member;
import com.librarynetwork.backend.repository.MemberRepository;

@Service
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (memberRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists"
            );
        }

        Member member = new Member();
        member.setName(request.name().trim());
        member.setEmail(email);
        member.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        member.setRole(Member.Role.READER);

        Member saved = memberRepository.save(member);

        return createResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        Member member = memberRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException(
                        "Invalid email or password"
                ));

        if (!passwordEncoder.matches(
                request.password(),
                member.getPasswordHash())) {
            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        return createResponse(member);
    }

    private AuthResponse createResponse(Member member) {
        String token = jwtService.generateToken(member);

        return new AuthResponse(
                token,
                "Bearer",
                member.getId(),
                member.getName(),
                member.getEmail(),
                member.getRole().name()
        );
    }
}
