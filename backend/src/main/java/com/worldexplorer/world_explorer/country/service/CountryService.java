
package com.worldexplorer.world_explorer.country.service;

import com.worldexplorer.world_explorer.destination.specification.DestinationSpecification;
import com.worldexplorer.world_explorer.entity.Country;
import com.worldexplorer.world_explorer.repository.CountryRepository;
import com.worldexplorer.world_explorer.repository.DestinationRepository;
import com.worldexplorer.world_explorer.storage.CloudinaryService;
import com.worldexplorer.world_explorer.country.dto.CountryResponse;
import com.worldexplorer.world_explorer.country.dto.CreateCountryRequest;
import com.worldexplorer.world_explorer.country.dto.UpdateCountryRequest;
import com.worldexplorer.world_explorer.country.specification.CountrySpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CountryService {

    private static final String COUNTRY_FOLDER =
            "world-explorer/countries";

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;

    private final CountryRepository countryRepository;
    private final DestinationRepository destinationRepository;
    private final CloudinaryService cloudinaryService;

    public CountryService(
            CountryRepository countryRepository,
            DestinationRepository destinationRepository,
            CloudinaryService cloudinaryService
    ) {
        this.countryRepository = countryRepository;
        this.destinationRepository = destinationRepository;
        this.cloudinaryService = cloudinaryService;
    }

    // ==============================
    // Create Country
    // ==============================

    @Transactional
    public CountryResponse createCountry(
            CreateCountryRequest request
    ) {

        String code = normalizeCode(request.code());
        String slug = normalizeSlug(request.slug());

        validateUniqueCode(code, null);
        validateUniqueSlug(slug, null);

        Country country = new Country();

        country.setName(request.name().trim());
        country.setCode(code);
        country.setSlug(slug);
        country.setDescription(normalizeDescription(request.description()));
        country.setActive(true);

        Country savedCountry = countryRepository.save(country);

        return toCountryResponse(savedCountry);
    }

    // ==============================
    // Update Country
    // ==============================

    @Transactional
    public CountryResponse updateCountry(
            UUID countryId,
            UpdateCountryRequest request
    ) {

        Country country = findCountryById(countryId);

        String code = normalizeCode(request.code());
        String slug = normalizeSlug(request.slug());

        validateUniqueCode(code, countryId);
        validateUniqueSlug(slug, countryId);

        country.setName(request.name().trim());
        country.setCode(code);
        country.setSlug(slug);
        country.setDescription(normalizeDescription(request.description()));

        Country updatedCountry = countryRepository.save(country);

        return toCountryResponse(updatedCountry);
    }

    // ==============================
    // Get Country
    // ==============================

    @Transactional(readOnly = true)
    public CountryResponse getCountryById(UUID countryId) {

        Country country = findCountryById(countryId);

        return toCountryResponse(country);
    }

    // ==============================
    // List, Search, Filter and Pagination
    // ==============================

    @Transactional(readOnly = true)
    public Page<CountryResponse> getCountries(
            String keyword,
            Boolean isActive,
            Pageable pageable
    ) {

        return countryRepository.findAll(
                CountrySpecification.build(keyword, isActive),
                pageable
        ).map(this::toCountryResponse);
    }

    // ==============================
    // Update Country Status
    // ==============================

    @Transactional
    public CountryResponse updateCountryStatus(
            UUID countryId,
            boolean isActive
    ) {

        Country country = findCountryById(countryId);

        if (country.isActive() == isActive) {
            return toCountryResponse(country);
        }

        // Prevent deactivating a country with active destinations
        if (!isActive) {
            ensureNoActiveDestinations(countryId);
        }

        country.setActive(isActive);

        Country updatedCountry = countryRepository.save(country);

        return toCountryResponse(updatedCountry);
    }

    // ==============================
    // Delete Country
    // ==============================

    @Transactional
    public void deleteCountry(UUID countryId) {

        Country country = findCountryById(countryId);

        if (!country.isActive()) {
            return;
        }

        ensureNoActiveDestinations(countryId);

        // Soft delete by deactivating the country
        country.setActive(false);

        countryRepository.save(country);
    }

    // ==============================
    // Country Image Management
    // ==============================

    // Upload or replace country image
    @Transactional
    public CountryResponse uploadCountryImage(
            UUID countryId,
            MultipartFile file
    ) {

        Country country = findCountryById(countryId);

        validateImage(file);

        String newImageUrl;

        try {
            Map<String, Object> result =
                    cloudinaryService.uploadImage(
                            file,
                            COUNTRY_FOLDER
                    );

            newImageUrl = (String) result.get("secure_url");

        } catch (IOException exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to upload country image",
                    exception
            );
        }

        if (newImageUrl == null || newImageUrl.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Cloudinary did not return an image URL"
            );
        }

        String oldImageUrl = country.getImageUrl();

        country.setImageUrl(newImageUrl);

        Country updatedCountry = countryRepository.save(country);

        // Delete old image after successfully saving the new image
        if (oldImageUrl != null && !oldImageUrl.isBlank()) {

            try {
                String oldPublicId =
                        cloudinaryService.extractPublicId(oldImageUrl);

                cloudinaryService.deleteImage(oldPublicId);

            } catch (Exception ignored) {
                // Keep the new image if deleting the old image fails.
            }
        }

        return toCountryResponse(updatedCountry);
    }

    // Delete country image
    @Transactional
    public CountryResponse deleteCountryImage(UUID countryId) {

        Country country = findCountryById(countryId);

        String imageUrl = country.getImageUrl();

        if (imageUrl == null || imageUrl.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Country does not have an image"
            );
        }

        String publicId;

        try {
            publicId = cloudinaryService.extractPublicId(imageUrl);

        } catch (IllegalArgumentException exception) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Stored country image URL is invalid"
            );
        }

        try {
            Map<String, Object> result =
                    cloudinaryService.deleteImage(publicId);

            String deleteStatus = (String) result.get("result");

            if (!"ok".equals(deleteStatus)
                    && !"not found".equals(deleteStatus)) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Failed to delete country image"
                );
            }

        } catch (IOException exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to delete country image",
                    exception
            );
        }

        country.setImageUrl(null);

        Country updatedCountry = countryRepository.save(country);

        return toCountryResponse(updatedCountry);
    }

    // ==============================
    // Shared Helper Methods
    // ==============================

    // Find country by ID
    private Country findCountryById(UUID countryId) {

        return countryRepository.findById(countryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Country not found"
                ));
    }

    // Validate country code uniqueness
    private void validateUniqueCode(
            String code,
            UUID excludedCountryId
    ) {

        boolean exists = countryRepository.exists(
                (root, query, criteriaBuilder) -> {

                    var codePredicate = criteriaBuilder.equal(
                            root.<String>get("code"),
                            code
                    );

                    if (excludedCountryId == null) {
                        return codePredicate;
                    }

                    return criteriaBuilder.and(
                            codePredicate,
                            criteriaBuilder.notEqual(
                                    root.get("id"),
                                    excludedCountryId
                            )
                    );
                }
        );

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Country code already exists"
            );
        }
    }

    // Validate country slug uniqueness
    private void validateUniqueSlug(
            String slug,
            UUID excludedCountryId
    ) {

        boolean exists = countryRepository.exists(
                (root, query, criteriaBuilder) -> {

                    var slugPredicate = criteriaBuilder.equal(
                            root.<String>get("slug"),
                            slug
                    );

                    if (excludedCountryId == null) {
                        return slugPredicate;
                    }

                    return criteriaBuilder.and(
                            slugPredicate,
                            criteriaBuilder.notEqual(
                                    root.get("id"),
                                    excludedCountryId
                            )
                    );
                }
        );

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Country slug already exists"
            );
        }
    }

    // Prevent deactivating countries with active destinations
    private void ensureNoActiveDestinations(UUID countryId) {

        long activeDestinationCount = destinationRepository.count(
                DestinationSpecification.build(
                        null,
                        countryId,
                        null,
                        null,
                        true
                )
        );

        if (activeDestinationCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Active destinations must be deactivated before deactivating this country"
            );
        }
    }

    // Normalize country code
    private String normalizeCode(String code) {

        return code.trim().toUpperCase(Locale.ROOT);
    }

    // Normalize country slug
    private String normalizeSlug(String slug) {

        return slug.trim().toLowerCase(Locale.ROOT);
    }

    // Normalize optional description
    private String normalizeDescription(String description) {

        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }

    // Convert Country Entity to Response DTO
    private CountryResponse toCountryResponse(Country country) {

        return new CountryResponse(
                country.getId(),
                country.getName(),
                country.getCode(),
                country.getSlug(),
                country.getDescription(),
                country.getImageUrl(),
                country.isActive(),
                country.getCreatedAt(),
                country.getUpdatedAt()
        );
    }

    // Validate country image
    private void validateImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Country image must not be empty"
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
                    "Country image must not exceed 10MB"
            );
        }
    }
}
