package com.banhangonline.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InventoryRequest(@Min(0) int quantity,
                               @Min(0) int reorderLevel,
                               @NotBlank @Pattern(regexp = "ACTIVE|INACTIVE") String status) {}
