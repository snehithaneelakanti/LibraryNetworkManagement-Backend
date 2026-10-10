package com.librarynetwork.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.librarynetwork.backend.dto.BranchAvailabilityResponse;
import com.librarynetwork.backend.entity.BookCopy;
import com.librarynetwork.backend.repository.BookCopyRepository;
import com.librarynetwork.backend.repository.BookRepository;

@Service
public class BranchAvailabilityService {

    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;

    public BranchAvailabilityService(
            BookCopyRepository bookCopyRepository,
            BookRepository bookRepository
    ) {
        this.bookCopyRepository = bookCopyRepository;
        this.bookRepository = bookRepository;
    }

    public List<BranchAvailabilityResponse> getAvailability(Long bookId) {
        return getAvailability(bookId, null, null);
    }

    public List<BranchAvailabilityResponse> getAvailability(
            Long bookId,
            Double userLatitude,
            Double userLongitude
    ) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Book not found"
            );
        }

        if ((userLatitude == null) != (userLongitude == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide both latitude and longitude."
            );
        }

        validateCoordinates(userLatitude, userLongitude);

        List<BookCopy> copies = bookCopyRepository.findByBookId(bookId);

        Map<Long, List<BookCopy>> copiesByBranch = new LinkedHashMap<>();

        for (BookCopy copy : copies) {
            Long branchId = copy.getBranch().getId();

            copiesByBranch
                    .computeIfAbsent(branchId, ignored -> new ArrayList<>())
                    .add(copy);
        }

        List<BranchAvailabilityResponse> results = new ArrayList<>();

        for (List<BookCopy> branchCopies : copiesByBranch.values()) {
            BookCopy firstCopy = branchCopies.get(0);
            var branch = firstCopy.getBranch();

            int totalCopies = branchCopies.size();

            int availableCopies = (int) branchCopies.stream()
                    .filter(copy ->
                            copy.getStatus() == BookCopy.Status.AVAILABLE)
                    .count();

            Double distanceKm = null;

            if (userLatitude != null) {
                distanceKm = haversineDistance(
                        userLatitude,
                        userLongitude,
                        branch.getLatitude(),
                        branch.getLongitude()
                );
            }

            results.add(new BranchAvailabilityResponse(
                    branch.getId(),
                    branch.getName(),
                    branch.getAddress(),
                    totalCopies,
                    availableCopies,
                    distanceKm
            ));
        }

        if (userLatitude != null) {
            results.sort(
                    Comparator
                            .comparingInt(
                                    (BranchAvailabilityResponse branch) ->
                                            branch.availableCopies() > 0 ? 0 : 1
                            )
                            .thenComparing(
                                    BranchAvailabilityResponse::distanceKm,
                                    Comparator.nullsLast(
                                            Comparator.naturalOrder()
                                    )
                            )
            );
        }

        return results;
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null) {
            return;
        }

        if (!Double.isFinite(latitude)
                || latitude < -90
                || latitude > 90
                || !Double.isFinite(longitude)
                || longitude < -180
                || longitude > 180) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid geographic coordinates."
            );
        }
    }

    private double haversineDistance(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2
    ) {
        final double earthRadiusKm = 6371.0088;

        double lat1 = Math.toRadians(latitude1);
        double lat2 = Math.toRadians(latitude2);

        double deltaLat = Math.toRadians(latitude2 - latitude1);
        double deltaLon = Math.toRadians(longitude2 - longitude1);

        double a =
                Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1)
                * Math.cos(lat2)
                * Math.sin(deltaLon / 2)
                * Math.sin(deltaLon / 2);

        double centralAngle = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a)
        );

        return earthRadiusKm * centralAngle;
    }
}