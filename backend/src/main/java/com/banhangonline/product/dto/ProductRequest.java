package com.banhangonline.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 80) String sku,
        @NotBlank @Size(max = 180) String name,
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9-]{1,200}") String slug,
        @Size(max = 10000) String description,
        @NotNull @DecimalMin(value = "0.0001") @Digits(integer = 15, fraction = 4) BigDecimal price,
        @Pattern(regexp = "^$|[A-Z]{3}") String currency,
        @Pattern(regexp = "^$|https?://.{1,490}$") String imageUrl,
        @NotNull @Min(0) Integer quantity,
        @Min(0) Integer reorderLevel) {}
