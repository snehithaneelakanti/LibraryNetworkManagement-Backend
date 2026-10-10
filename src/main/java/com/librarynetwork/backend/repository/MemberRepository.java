
package com.librarynetwork.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.librarynetwork.backend.entity.Member;

public interface MemberRepository
        extends JpaRepository<Member, Long> {

    Optional<Member> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
