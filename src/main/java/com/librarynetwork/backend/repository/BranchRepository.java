
package com.librarynetwork.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.librarynetwork.backend.entity.Branch;

public interface BranchRepository extends JpaRepository<Branch, Long> {
}
