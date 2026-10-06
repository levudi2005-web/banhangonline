package com.banhangonline.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductImageDeleteRequest(
        @NotBlank @Size(max = 2048) String imageUrl) {}
