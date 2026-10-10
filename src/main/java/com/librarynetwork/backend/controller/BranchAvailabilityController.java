package com.librarynetwork.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.librarynetwork.backend.dto.BranchAvailabilityResponse;
import com.librarynetwork.backend.service.BranchAvailabilityService;

@RestController
@RequestMapping("/api/books")
public class BranchAvailabilityController {

    private final BranchAvailabilityService availabilityService;

    public BranchAvailabilityController(
            BranchAvailabilityService availabilityService
    ) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/{bookId}/availability")
    public List<BranchAvailabilityResponse> getAvailability(
            @PathVariable Long bookId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude
    ) {
        return availabilityService.getAvailability(
                bookId,
                latitude,
                longitude
        );
    }
}