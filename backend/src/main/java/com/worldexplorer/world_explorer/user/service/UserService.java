
package com.worldexplorer.world_explorer.user.service;

import com.worldexplorer.world_explorer.entity.Role;
import com.worldexplorer.world_explorer.entity.RoleName;
import com.worldexplorer.world_explorer.entity.User;
import com.worldexplorer.world_explorer.repository.RefreshTokenRepository;
import com.worldexplorer.world_explorer.repository.RoleRepository;
import com.worldexplorer.world_explorer.repository.UserRepository;
import com.worldexplorer.world_explorer.storage.CloudinaryService;
import com.worldexplorer.world_explorer.user.dto.AdminUserResponse;
import com.worldexplorer.world_explorer.user.dto.AvatarUploadResponse;
import com.worldexplorer.world_explorer.user.dto.UpdateProfileRequest;
import com.worldexplorer.world_explorer.user.dto.UserProfileResponse;
import com.worldexplorer.world_explorer.user.specification.UserSpecification;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService {

    private static final String AVATAR_FOLDER = "world-explorer/avatars";
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CloudinaryService cloudinaryService;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            CloudinaryService cloudinaryService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.cloudinaryService = cloudinaryService;
    }


    // Update current user profile
    @Transactional
    public User updateProfile(
            UUID userId,
            UpdateProfileRequest request
    ) {
        User user = getActiveUser(userId);

        user.setFullName(request.fullName().trim());

        return userRepository.save(user);
    }

    // Get current user profile
    @Transactional
    public UserProfileResponse getProfile(UUID userId) {

        User user = getActiveUser(userId);

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl()
        );
    }


    // Upload or replace user avatar
    @Transactional
    public AvatarUploadResponse uploadAvatar(
            UUID userId,
            MultipartFile file
    ) throws IOException {

        User user = getActiveUser(userId);

        validateAvatar(file);

        Map<String, Object> result = cloudinaryService.uploadImage(
                file,
                AVATAR_FOLDER
        );

        String newAvatarUrl = (String) result.get("secure_url");

        if (newAvatarUrl == null || newAvatarUrl.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Cloudinary did not return an image URL"
            );
        }

        String oldAvatarUrl = user.getAvatarUrl();

        user.setAvatarUrl(newAvatarUrl);
        userRepository.save(user);

        // Delete the old avatar after saving the new one
        if (oldAvatarUrl != null && !oldAvatarUrl.isBlank()) {
            try {
                String oldPublicId =
                        cloudinaryService.extractPublicId(oldAvatarUrl);

                cloudinaryService.deleteImage(oldPublicId);

            } catch (Exception ignored) {
                // Keep the new avatar if deleting the old image fails.
            }
        }

        return new AvatarUploadResponse(newAvatarUrl);
    }

    // Delete current user avatar
    @Transactional
    public void deleteAvatar(UUID userId) throws IOException {

        User user = getActiveUser(userId);

        String avatarUrl = user.getAvatarUrl();

        if (avatarUrl == null || avatarUrl.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User does not have an avatar"
            );
        }

        String publicId;

        try {
            publicId = cloudinaryService.extractPublicId(avatarUrl);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Stored avatar URL is invalid"
            );
        }

        Map<String, Object> result =
                cloudinaryService.deleteImage(publicId);

        String deleteStatus = (String) result.get("result");

        if (!"ok".equals(deleteStatus)
                && !"not found".equals(deleteStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to delete avatar from Cloudinary"
            );
        }

        user.setAvatarUrl(null);
        userRepository.save(user);
    }


    // Soft delete current user account
    @Transactional
    public void deleteMyAccount(UUID userId) {

        // Lock the ADMIN role during account operations
        Role adminRole = lockAdminRole();

        User user = getActiveUser(userId);

        // Protect the last active administrator
        if (user.isActive()
                && user.getRoleId().equals(adminRole.getId())) {
            ensureAnotherActiveAdminRemains(adminRole);
        }

        user.deactivateAndDelete();

        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(userId);
    }


    // Get paginated users with search and filters
    @Transactional
    public Page<AdminUserResponse> getAllUsers(
            String keyword,
            UUID roleId,
            Boolean isActive,
            Pageable pageable
    ) {

        return userRepository.findAll(
                UserSpecification.build(keyword, roleId, isActive),
                pageable
        ).map(this::toAdminUserResponse);
    }

    // Get user details by ID
    @Transactional
    public AdminUserResponse getUserById(UUID userId) {

        User user = findUserById(userId);

        return toAdminUserResponse(user);
    }

    // Activate or deactivate a user account
    @Transactional
    public AdminUserResponse updateUserStatus(
            UUID adminId,
            UUID targetUserId,
            boolean isActive
    ) {

        // Serialize account status changes
        Role adminRole = lockAdminRole();

        preventSelfManagement(adminId, targetUserId);

        User targetUser = findUserById(targetUserId);

        preventDeletedAccountModification(targetUser);

        if (targetUser.isActive() == isActive) {
            return toAdminUserResponse(targetUser);
        }

        // Prevent deactivating the last active administrator
        if (!isActive
                && targetUser.getRoleId().equals(adminRole.getId())) {
            ensureAnotherActiveAdminRemains(adminRole);
        }

        targetUser.setActive(isActive);

        User updatedUser = userRepository.save(targetUser);

        return toAdminUserResponse(updatedUser);
    }

    // Change a user's role
    @Transactional
    public AdminUserResponse updateUserRole(
            UUID adminId,
            UUID targetUserId,
            RoleName newRoleName
    ) {

        // Serialize role changes involving ADMIN
        Role adminRole = lockAdminRole();

        preventSelfManagement(adminId, targetUserId);

        User targetUser = findUserById(targetUserId);

        preventDeletedAccountModification(targetUser);

        if (newRoleName == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role name must not be null"
            );
        }

        Role currentRole = getRoleById(targetUser.getRoleId());

        Role newRole;

        if (newRoleName == RoleName.ADMIN) {
            newRole = adminRole;
        } else {
            newRole = roleRepository.findByName(newRoleName)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Role not found"
                    ));
        }

        if (currentRole.getId().equals(newRole.getId())) {
            return toAdminUserResponse(targetUser);
        }

        // Prevent demoting the last active administrator
        if (targetUser.isActive()
                && currentRole.getName() == RoleName.ADMIN
                && newRoleName != RoleName.ADMIN) {
            ensureAnotherActiveAdminRemains(adminRole);
        }

        targetUser.setRoleId(newRole.getId());

        User updatedUser = userRepository.save(targetUser);

        return toAdminUserResponse(updatedUser);
    }

    // Soft delete a user account by ADMIN
    @Transactional
    public void deleteUser(
            UUID adminId,
            UUID targetUserId
    ) {

        // Serialize account deletion operations
        Role adminRole = lockAdminRole();

        preventSelfManagement(adminId, targetUserId);

        User targetUser = findUserById(targetUserId);

        preventDeletedAccountModification(targetUser);

        // Prevent deleting the last active administrator
        if (targetUser.isActive()
                && targetUser.getRoleId().equals(adminRole.getId())) {
            ensureAnotherActiveAdminRemains(adminRole);
        }

        targetUser.deactivateAndDelete();

        userRepository.save(targetUser);

        refreshTokenRepository.revokeAllByUserId(targetUserId);
    }


    // Find an active user
    private User getActiveUser(UUID userId) {

        User user = findUserById(userId);

        if (!user.isActive() || user.getDeletedAt() != null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User account is inactive"
            );
        }

        return user;
    }

    // Find a user by ID
    private User findUserById(UUID userId) {

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    // Find a role by ID
    private Role getRoleById(UUID roleId) {

        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "User role configuration is invalid"
                ));
    }

    // Lock the ADMIN role record for transactional account operations
    private Role lockAdminRole() {

        return roleRepository.findByNameForUpdate(RoleName.ADMIN)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "ADMIN role configuration is missing"
                ));
    }

    // Convert User entity to AdminUserResponse
    private AdminUserResponse toAdminUserResponse(User user) {

        Role role = getRoleById(user.getRoleId());

        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getRoleId(),
                role.getName(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getDeletedAt()
        );
    }

    // Prevent an administrator from managing their own account
    private void preventSelfManagement(
            UUID adminId,
            UUID targetUserId
    ) {

        if (adminId.equals(targetUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Administrators cannot manage their own account through this endpoint"
            );
        }
    }

    // Prevent modifications to soft-deleted accounts
    private void preventDeletedAccountModification(User user) {

        if (user.getDeletedAt() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Deleted user accounts cannot be modified"
            );
        }
    }

    // Ensure that at least one other active administrator remains
    private void ensureAnotherActiveAdminRemains(Role adminRole) {

        long activeAdminCount = userRepository.count(
                UserSpecification.build(
                        null,
                        adminRole.getId(),
                        true
                )
        );

        if (activeAdminCount <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The last active administrator cannot be deactivated, deleted or demoted"
            );
        }
    }

    // Validate avatar file
    private void validateAvatar(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Avatar file must not be empty"
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

        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Avatar image must not exceed 5MB"
            );
        }
    }
}
