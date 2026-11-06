
package com.worldexplorer.world_explorer.destination.dto;

import com.worldexplorer.world_explorer.entity.DestinationType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DestinationResponse(

        UUID id,

        UUID countryId,

        UUID parentId,

        String name,

        String slug,

        DestinationType type,

        String description,

        String imageUrl,

        BigDecimal latitude,

        BigDecimal longitude,

        boolean isActive,

        OffsetDateTime createdAt,

        OffsetDateTime updatedAt

) {
}
