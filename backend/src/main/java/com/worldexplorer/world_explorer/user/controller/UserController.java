
package com.worldexplorer.world_explorer.user.controller;

import com.worldexplorer.world_explorer.entity.RoleName;
import com.worldexplorer.world_explorer.user.dto.AdminUserResponse;
import com.worldexplorer.world_explorer.user.dto.AvatarUploadResponse;
import com.worldexplorer.world_explorer.user.dto.UpdateProfileRequest;
import com.worldexplorer.world_explorer.user.dto.UserProfileResponse;
import com.worldexplorer.world_explorer.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Get current user profile
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        UserProfileResponse response =
                userService.getProfile(userId);

        return ResponseEntity.ok(response);
    }

    // Update current user profile
    @PutMapping("/me")
    public ResponseEntity<Void> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        userService.updateProfile(userId, request);

        return ResponseEntity.noContent().build();
    }

    // Upload or replace current user avatar
    @PostMapping(
            value = "/me/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<AvatarUploadResponse> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) throws IOException {

        UUID userId = (UUID) authentication.getPrincipal();

        AvatarUploadResponse response =
                userService.uploadAvatar(userId, file);

        return ResponseEntity.ok(response);
    }

    // Delete current user avatar
    @DeleteMapping("/me/avatar")
    public ResponseEntity<Void> deleteAvatar(
            Authentication authentication
    ) throws IOException {

        UUID userId = (UUID) authentication.getPrincipal();

        userService.deleteAvatar(userId);

        return ResponseEntity.noContent().build();
    }


    // Soft delete current user account
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        userService.deleteMyAccount(userId);

        return ResponseEntity.noContent().build();
    }


    // Get paginated users with search and filters
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AdminUserResponse>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID roleId,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {

        Page<AdminUserResponse> response =
                userService.getAllUsers(
                        keyword,
                        roleId,
                        isActive,
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    // Get user details by ID
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserResponse> getUserById(
            @PathVariable UUID userId
    ) {

        AdminUserResponse response =
                userService.getUserById(userId);

        return ResponseEntity.ok(response);
    }

    // Activate or deactivate a user account
    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserResponse> updateUserStatus(
            @PathVariable UUID userId,
            @RequestParam boolean isActive,
            Authentication authentication
    ) {

        UUID adminId = (UUID) authentication.getPrincipal();

        AdminUserResponse response =
                userService.updateUserStatus(
                        adminId,
                        userId,
                        isActive
                );

        return ResponseEntity.ok(response);
    }

    // Change a user's role
    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserResponse> updateUserRole(
            @PathVariable UUID userId,
            @RequestParam RoleName roleName,
            Authentication authentication
    ) {

        UUID adminId = (UUID) authentication.getPrincipal();

        AdminUserResponse response =
                userService.updateUserRole(
                        adminId,
                        userId,
                        roleName
                );

        return ResponseEntity.ok(response);
    }

    // Soft delete a user account by ADMIN
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID userId,
            Authentication authentication
    ) {

        UUID adminId = (UUID) authentication.getPrincipal();

        userService.deleteUser(adminId, userId);

        return ResponseEntity.noContent().build();
    }
}
