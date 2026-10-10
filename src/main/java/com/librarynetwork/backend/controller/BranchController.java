
package com.librarynetwork.backend.controller;

import com.librarynetwork.backend.entity.Branch;
import com.librarynetwork.backend.repository.BookCopyRepository;
import com.librarynetwork.backend.repository.BranchRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchRepository branchRepository;
    private final BookCopyRepository bookCopyRepository;

    public BranchController(
            BranchRepository branchRepository,
            BookCopyRepository bookCopyRepository
    ) {
        this.branchRepository = branchRepository;
        this.bookCopyRepository = bookCopyRepository;
    }

    @GetMapping
    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    @GetMapping("/{id}")
    public Branch getBranchById(@PathVariable Long id) {
        return findBranch(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Branch createBranch(@Valid @RequestBody Branch branch) {
        branch.setId(null);
        return branchRepository.save(branch);
    }

    @PutMapping("/{id}")
    public Branch updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody Branch request
    ) {
        Branch existing = findBranch(id);

        existing.setName(request.getName());
        existing.setAddress(request.getAddress());
        existing.setLatitude(request.getLatitude());
        existing.setLongitude(request.getLongitude());

        return branchRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBranch(@PathVariable Long id) {
        findBranch(id);

        if (bookCopyRepository.existsByBranchId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete a branch while inventory copies reference it"
            );
        }

        branchRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Branch findBranch(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Branch not found"
                ));
    }
}
