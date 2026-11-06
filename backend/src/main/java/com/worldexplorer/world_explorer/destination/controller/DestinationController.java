
package com.worldexplorer.world_explorer.destination.controller;


import com.worldexplorer.world_explorer.destination.dto.CreateDestinationRequest;
import com.worldexplorer.world_explorer.destination.dto.DestinationResponse;
import com.worldexplorer.world_explorer.destination.dto.UpdateDestinationRequest;
import com.worldexplorer.world_explorer.destination.service.DestinationService;
import com.worldexplorer.world_explorer.entity.DestinationType;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class DestinationController {

    private final DestinationService destinationService;

    public DestinationController(
            DestinationService destinationService
    ) {
        this.destinationService = destinationService;
    }


    // Get active destinations with search, filters and pagination
    @GetMapping("/destinations")
    public ResponseEntity<Page<DestinationResponse>> getDestinations(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID countryId,
            @RequestParam(required = false) UUID parentId,
            @RequestParam(required = false) DestinationType type,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {

        Page<DestinationResponse> response =
                destinationService.getDestinations(
                        keyword,
                        countryId,
                        parentId,
                        type,
                        true,
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    // Get destination details
    @GetMapping("/destinations/{destinationId}")
    public ResponseEntity<DestinationResponse> getDestinationById(
            @PathVariable UUID destinationId
    ) {

        DestinationResponse response =
                destinationService.getDestinationById(destinationId);

        if (!response.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Destination not found"
            );
        }

        return ResponseEntity.ok(response);
    }


    // Get all destinations for administration
    @GetMapping("/admin/destinations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<DestinationResponse>> getAllDestinations(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID countryId,
            @RequestParam(required = false) UUID parentId,
            @RequestParam(required = false) DestinationType type,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {

        Page<DestinationResponse> response =
                destinationService.getDestinations(
                        keyword,
                        countryId,
                        parentId,
                        type,
                        isActive,
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    // Create a destination
    @PostMapping("/admin/destinations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DestinationResponse> createDestination(
            @Valid @RequestBody CreateDestinationRequest request
    ) {

        DestinationResponse response =
                destinationService.createDestination(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    // Update a destination
    @PutMapping("/admin/destinations/{destinationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DestinationResponse> updateDestination(
            @PathVariable UUID destinationId,
            @Valid @RequestBody UpdateDestinationRequest request
    ) {

        DestinationResponse response =
                destinationService.updateDestination(
                        destinationId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    // Activate or deactivate a destination
    @PatchMapping("/admin/destinations/{destinationId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DestinationResponse> updateDestinationStatus(
            @PathVariable UUID destinationId,
            @RequestParam boolean isActive
    ) {

        DestinationResponse response =
                destinationService.updateDestinationStatus(
                        destinationId,
                        isActive
                );

        return ResponseEntity.ok(response);
    }

    // Soft delete a destination
    @DeleteMapping("/admin/destinations/{destinationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDestination(
            @PathVariable UUID destinationId
    ) {

        destinationService.deleteDestination(destinationId);

        return ResponseEntity.noContent().build();
    }

    // Upload or replace destination image
    @PostMapping(
            value = "/admin/destinations/{destinationId}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DestinationResponse> uploadDestinationImage(
            @PathVariable UUID destinationId,
            @RequestParam("file") MultipartFile file
    ) {

        DestinationResponse response =
                destinationService.uploadDestinationImage(
                        destinationId,
                        file
                );

        return ResponseEntity.ok(response);
    }

    // Delete destination image
    @DeleteMapping("/admin/destinations/{destinationId}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DestinationResponse> deleteDestinationImage(
            @PathVariable UUID destinationId
    ) {

        DestinationResponse response =
                destinationService.deleteDestinationImage(destinationId);

        return ResponseEntity.ok(response);
    }
}
