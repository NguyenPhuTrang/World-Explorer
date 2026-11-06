
package com.worldexplorer.world_explorer.destination.dto;

import com.worldexplorer.world_explorer.entity.DestinationType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateDestinationRequest(

        @NotNull(message = "Country ID must not be null") // bắt buộc phải truyền giá trị, không được để null.
        UUID countryId,

        UUID parentId, // Nó tạo ra một trường để lưu ID của địa điểm cha. cho hệ thống biết một địa điểm du lịch nằm bên trong địa điểm nào khác.

        @NotBlank(message = "Destination name must not be blank")
        String name,

        @NotBlank(message = "Destination slug must not be blank")
        String slug,

        @NotNull(message = "Destination type must not be null")
        DestinationType type,

        String description,

        @DecimalMin(value = "-90.0", message = "Latitude must be at least -90") // @DecimalMin – Giá trị nhỏ nhất
        @DecimalMax(value = "90.0", message = "Latitude must not exceed 90") // @DecimalMax – Giá trị lớn nhất
        @Digits(integer = 3, fraction = 6)
        BigDecimal latitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be at least -180")
        @DecimalMax(value = "180.0", message = "Longitude must not exceed 180")
        @Digits(integer = 3, fraction = 6)
        BigDecimal longitude

) {
}
