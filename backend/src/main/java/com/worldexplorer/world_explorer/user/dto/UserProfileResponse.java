package com.worldexplorer.world_explorer.user.dto;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String fullName,
        String avatarUrl
) {
}