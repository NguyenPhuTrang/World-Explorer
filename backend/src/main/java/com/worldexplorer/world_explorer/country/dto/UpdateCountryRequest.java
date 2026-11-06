
package com.worldexplorer.world_explorer.country.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCountryRequest(

        @NotBlank(message = "Country name must not be blank")
        String name,

        @NotBlank(message = "Country code must not be blank")
        @Size(min = 2, max = 2, message = "Country code must contain exactly 2 characters")
        @Pattern(
                regexp = "^[A-Za-z]{2}$",
                message = "Country code must contain exactly 2 letters"
        )
        String code,

        @NotBlank(message = "Country slug must not be blank")
        String slug,

        String description

) {
}
