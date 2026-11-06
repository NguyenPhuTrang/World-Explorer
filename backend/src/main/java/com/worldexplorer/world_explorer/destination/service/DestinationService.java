
package com.worldexplorer.world_explorer.destination.service;

import com.worldexplorer.world_explorer.entity.Country;
import com.worldexplorer.world_explorer.entity.Destination;
import com.worldexplorer.world_explorer.entity.DestinationType;
import com.worldexplorer.world_explorer.repository.CountryRepository;
import com.worldexplorer.world_explorer.repository.DestinationRepository;
import com.worldexplorer.world_explorer.storage.CloudinaryService;
import com.worldexplorer.world_explorer.destination.dto.CreateDestinationRequest;
import com.worldexplorer.world_explorer.destination.dto.DestinationResponse;
import com.worldexplorer.world_explorer.destination.dto.UpdateDestinationRequest;
import com.worldexplorer.world_explorer.destination.specification.DestinationSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DestinationService {

    private static final String DESTINATION_FOLDER =
            "world-explorer/destinations";

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;

    private final DestinationRepository destinationRepository;
    private final CountryRepository countryRepository;
    private final CloudinaryService cloudinaryService;

    public DestinationService(
            DestinationRepository destinationRepository,
            CountryRepository countryRepository,
            CloudinaryService cloudinaryService
    ) {
        this.destinationRepository = destinationRepository;
        this.countryRepository = countryRepository;
        this.cloudinaryService = cloudinaryService;
    }


    @Transactional
    public DestinationResponse createDestination(
            CreateDestinationRequest request
    ) {

        Country country = getActiveCountry(request.countryId());

        validateParent(
                request.parentId(),
                request.countryId(),
                null
        );

        Destination destination = new Destination();

        destination.setCountryId(country.getId());
        destination.setParentId(request.parentId());
        destination.setName(request.name().trim());
        destination.setSlug(request.slug().trim().toLowerCase());
        destination.setType(request.type());
        destination.setDescription(request.description());
        destination.setLatitude(request.latitude());
        destination.setLongitude(request.longitude());

        destination.setActive(true);

        Destination savedDestination = destinationRepository.save(destination);

        return toDestinationResponse(savedDestination);
    }


    @Transactional
    public DestinationResponse updateDestination(
            UUID destinationId,
            UpdateDestinationRequest request
    ) {

        Destination destination = findDestinationById(destinationId);

        Country country = getActiveCountry(request.countryId());

        validateParent(
                request.parentId(),
                request.countryId(),
                destinationId
        );

        // Prevent changing country while child destinations exist
        if (!destination.getCountryId().equals(country.getId())) {

            long childCount = destinationRepository.count(
                    DestinationSpecification.build(
                            null,
                            null,
                            destinationId,
                            null,
                            null
                    )
            );

            if (childCount > 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cannot change country while child destinations exist"
                );
            }
        }

        destination.setCountryId(country.getId());
        destination.setParentId(request.parentId());
        destination.setName(request.name().trim());
        destination.setSlug(request.slug().trim().toLowerCase());
        destination.setType(request.type());
        destination.setDescription(request.description());
        destination.setLatitude(request.latitude());
        destination.setLongitude(request.longitude());

        Destination updatedDestination = destinationRepository.save(destination);

        return toDestinationResponse(updatedDestination);
    }


    @Transactional(readOnly = true)
    public DestinationResponse getDestinationById(UUID destinationId) {

        Destination destination = findDestinationById(destinationId);

        return toDestinationResponse(destination);
    }


    @Transactional(readOnly = true)
    public Page<DestinationResponse> getDestinations(
            String keyword,
            UUID countryId,
            UUID parentId,
            DestinationType type,
            Boolean isActive,
            Pageable pageable
    ) {

        return destinationRepository.findAll(
                DestinationSpecification.build(
                        keyword,
                        countryId,
                        parentId,
                        type,
                        isActive
                ),
                pageable
        ).map(this::toDestinationResponse);
    }


    @Transactional
    public DestinationResponse updateDestinationStatus(
            UUID destinationId,
            boolean isActive
    ) {

        Destination destination = findDestinationById(destinationId);

        if (destination.isActive() == isActive) {
            return toDestinationResponse(destination);
        }

        if (isActive) {

            getActiveCountry(destination.getCountryId());

            validateParent(
                    destination.getParentId(),
                    destination.getCountryId(),
                    destinationId
            );

        } else {

            ensureNoActiveChildren(destinationId);
        }

        destination.setActive(isActive);

        Destination updatedDestination =
                destinationRepository.save(destination);

        return toDestinationResponse(updatedDestination);
    }


    @Transactional
    public void deleteDestination(UUID destinationId) {

        Destination destination = findDestinationById(destinationId);

        if (!destination.isActive()) {
            return;
        }

        ensureNoActiveChildren(destinationId);

        // Soft delete by deactivating the destination
        destination.setActive(false);

        destinationRepository.save(destination);
    }


    // Upload or replace destination image
    @Transactional
    public DestinationResponse uploadDestinationImage(
            UUID destinationId,
            MultipartFile file
    ) {

        Destination destination = findDestinationById(destinationId);

        validateImage(file);

        String newImageUrl;

        try {
            Map<String, Object> result = cloudinaryService.uploadImage(
                            file,
                            DESTINATION_FOLDER
                    );

            newImageUrl = (String) result.get("secure_url");

        } catch (IOException exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to upload destination image",
                    exception
            );
        }

        if (newImageUrl == null || newImageUrl.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Cloudinary did not return an image URL"
            );
        }

        String oldImageUrl = destination.getImageUrl();

        destination.setImageUrl(newImageUrl);

        Destination updatedDestination =
                destinationRepository.save(destination);

        // Delete the old image after saving the new image
        if (oldImageUrl != null && !oldImageUrl.isBlank()) {

            try {
                String oldPublicId =
                        cloudinaryService.extractPublicId(oldImageUrl);

                cloudinaryService.deleteImage(oldPublicId);

            } catch (Exception ignored) {
                // Keep the new image if deleting the old image fails.
            }
        }

        return toDestinationResponse(updatedDestination);
    }

    // Delete destination image
    @Transactional
    public DestinationResponse deleteDestinationImage(
            UUID destinationId
    ) {

        Destination destination = findDestinationById(destinationId);

        String imageUrl = destination.getImageUrl();

        if (imageUrl == null || imageUrl.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Destination does not have an image"
            );
        }

        String publicId;

        try {
            publicId = cloudinaryService.extractPublicId(imageUrl);

        } catch (IllegalArgumentException exception) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Stored destination image URL is invalid"
            );
        }

        try {
            Map<String, Object> result = cloudinaryService.deleteImage(publicId);

            String deleteStatus = (String) result.get("result");

            if (!"ok".equals(deleteStatus)
                    && !"not found".equals(deleteStatus)) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Failed to delete destination image"
                );
            }

        } catch (IOException exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to delete destination image",
                    exception
            );
        }

        destination.setImageUrl(null);

        Destination updatedDestination = destinationRepository.save(destination);

        return toDestinationResponse(updatedDestination);
    }


    // Find destination by ID
    private Destination findDestinationById(UUID destinationId) {

        return destinationRepository.findById(destinationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Destination not found"
                ));
    }

    // Find an active country
    private Country getActiveCountry(UUID countryId) {

        Country country = countryRepository.findById(countryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Country not found"
                ));

        if (!country.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Country is inactive"
            );
        }

        return country;
    }

    // Validate parent destination
    private void validateParent(
            UUID parentId,
            UUID countryId,
            UUID destinationId
    ) {

        if (parentId == null) {
            return;
        }

        if (parentId.equals(destinationId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A destination cannot be its own parent"
            );
        }

        Destination parent = findDestinationById(parentId);

        if (!parent.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Parent destination is inactive"
            );
        }

        if (!parent.getCountryId().equals(countryId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Parent destination must belong to the same country"
            );
        }

        preventCircularHierarchy(
                parent,
                destinationId
        );
    }

    // Prevent circular destination hierarchy
    private void preventCircularHierarchy(
            Destination parent,
            UUID destinationId
    ) {

        if (destinationId == null) {
            return;
        }

        Set<UUID> visited = new HashSet<>();

        Destination current = parent;

        while (current != null) {

            UUID currentId = current.getId();

            if (currentId.equals(destinationId)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Destination hierarchy cannot contain circular references"
                );
            }

            if (!visited.add(currentId)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Existing destination hierarchy contains a circular reference"
                );
            }

            UUID nextParentId = current.getParentId();

            if (nextParentId == null) {
                break;
            }

            current = destinationRepository.findById(nextParentId)
                    .orElse(null);
        }
    }

    // Prevent deactivating a destination with active children
    private void ensureNoActiveChildren(UUID destinationId) {

        long activeChildrenCount = destinationRepository.count(
                DestinationSpecification.build(
                        null,
                        null,
                        destinationId,
                        null,
                        true
                )
        );

        if (activeChildrenCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Active child destinations must be deactivated first"
            );
        }
    }

    // Convert Destination Entity to Response DTO
    private DestinationResponse toDestinationResponse(
            Destination destination
    ) {

        return new DestinationResponse(
                destination.getId(),
                destination.getCountryId(),
                destination.getParentId(),
                destination.getName(),
                destination.getSlug(),
                destination.getType(),
                destination.getDescription(),
                destination.getImageUrl(),
                destination.getLatitude(),
                destination.getLongitude(),
                destination.isActive(),
                destination.getCreatedAt(),
                destination.getUpdatedAt()
        );
    }

    // Validate destination image
    private void validateImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Destination image must not be empty"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null
                || !contentType.matches("image/(jpeg|png|webp)")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only JPEG, PNG and WebP images are allowed"
            );
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Destination image must not exceed 10MB"
            );
        }
    }
}
