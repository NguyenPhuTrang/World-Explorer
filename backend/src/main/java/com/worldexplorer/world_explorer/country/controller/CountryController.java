package com.worldexplorer.world_explorer.country.controller;

import com.worldexplorer.world_explorer.country.dto.CountryResponse;
import com.worldexplorer.world_explorer.country.dto.CreateCountryRequest;
import com.worldexplorer.world_explorer.country.dto.UpdateCountryRequest;
import com.worldexplorer.world_explorer.country.service.CountryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;


import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CountryController {
    private final CountryService countryService;

    public CountryController(CountryService countryService) {
        this.countryService = countryService;
    }

    @GetMapping("/countries")
    public ResponseEntity<Page<CountryResponse>> getCountries(
            @RequestParam(required = false) String keyword,
            @PageableDefault(
                    size = 20,
                    sort = "name",
                    direction = Sort.Direction.ASC
            )Pageable pageable
    ){
        Page<CountryResponse> responses = countryService.getCountries(
                keyword,
                true,
                pageable
        );
        return ResponseEntity.ok(responses);
    }

    @GetMapping(":/countries/{countryId}")
    public ResponseEntity<CountryResponse> getCountryById(
            @PathVariable UUID countryId
            ){
            CountryResponse response = countryService.getCountryById(countryId);

            if(!response.isActive()){
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Country not found");
            }
            return ResponseEntity.ok(response);
    }

    @GetMapping("admin/countries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CountryResponse>> getAllCountries(
            @RequestParam(required = false) String keyword, // @RequestParam: lấy tham số keyword từ URL
            @RequestParam(required = false) Boolean isActive, // lấy tham số isActive từ query string,
            @PageableDefault(
                    size = 20,
                    sort = "createAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
            Page<CountryResponse> responses =
                    countryService.getCountries(
                            keyword,
                            isActive,
                            pageable
                    );
            return ResponseEntity.ok(responses);
    }

    @PostMapping("/admin/countries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CountryResponse> createCountry(
            @Valid @RequestBody CreateCountryRequest request
            ) {
        CountryResponse response = countryService.createCountry(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/admin/countries/{countryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CountryResponse> updateCountry(
            @PathVariable UUID countryId, // lấy ID từ URL để biết cập nhật dữ liệu vào quốc gia nào.
            @Valid @RequestBody UpdateCountryRequest request // lấy nội dung mới từ body JSON, đồng thời kiểm tra tính hợp lệ.
            ){

        CountryResponse response = countryService.updateCountry(
                countryId,
                request
        );
        return ResponseEntity.ok(response);

    }

    // Active or deactive a country
    @PutMapping("/admin/countries/{countryId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CountryResponse> updateCountryStatus(
            @PathVariable UUID countryId,
            @RequestParam boolean isActive
    ) {
        CountryResponse response =
                countryService.updateCountryStatus(
                        countryId,
                        isActive
                );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/admin/countries/{countryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCountry(
            @PathVariable UUID countryId
    ) {
        countryService.deleteCountry(countryId);
        return ResponseEntity.noContent().build();
    }

    // Upload or replace country image
    @PostMapping(
            value = "/admin/countries/{countryId}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CountryResponse> uploadCountryImage(
            @PathVariable UUID countryId,
            @RequestParam("file")MultipartFile file
            ) {
        CountryResponse response =
                countryService.uploadCountryImage(countryId,file);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("admin/countries/{countryId}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CountryResponse> deleteCountryImage(
            @PathVariable UUID countryId
    ) {
        CountryResponse response = countryService.deleteCountryImage(countryId);

        return ResponseEntity.ok(response);
    }





}
