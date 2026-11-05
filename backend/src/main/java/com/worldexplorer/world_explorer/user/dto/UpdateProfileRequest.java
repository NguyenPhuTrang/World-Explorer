package com.worldexplorer.world_explorer.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(

        @NotBlank(message = "Full name must not be blank")
        String fullName

) {
}