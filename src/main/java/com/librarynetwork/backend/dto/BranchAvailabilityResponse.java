
package com.librarynetwork.backend.dto;

public record BranchAvailabilityResponse(
        Long branchId,
        String branchName,
        String address,
        int totalCopies,
        int availableCopies,
        Double distanceKm
) {
}