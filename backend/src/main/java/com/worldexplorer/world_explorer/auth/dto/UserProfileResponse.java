
package com.worldexplorer.world_explorer.auth.dto;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String fullName,
        String avatarUrl
) {
}
