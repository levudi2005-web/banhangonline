package com.banhangonline.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9-]{1,160}") String slug,
        @Size(max = 500) String description) {}
