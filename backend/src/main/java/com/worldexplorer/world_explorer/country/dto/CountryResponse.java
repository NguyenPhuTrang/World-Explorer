
package com.worldexplorer.world_explorer.country.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CountryResponse(

        UUID id,

        String name,

        String code,

        String slug,

        String description,

        String imageUrl,

        boolean isActive,

        OffsetDateTime createdAt,

        OffsetDateTime updatedAt

) {
}
