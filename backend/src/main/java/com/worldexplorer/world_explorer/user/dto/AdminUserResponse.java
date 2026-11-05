
package com.worldexplorer.world_explorer.user.dto;

import com.worldexplorer.world_explorer.entity.RoleName;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String fullName,
        String avatarUrl,
        UUID roleId,
        RoleName role,
        boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt
) {
}
