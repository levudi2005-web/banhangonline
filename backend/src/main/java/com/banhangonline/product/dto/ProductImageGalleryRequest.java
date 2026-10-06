package com.banhangonline.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProductImageGalleryRequest(
        @NotNull @Size(max = 8)
        List<@NotBlank @Size(max = 500) @Pattern(regexp = "^https?://.{1,492}$") String> imageUrls) {}
