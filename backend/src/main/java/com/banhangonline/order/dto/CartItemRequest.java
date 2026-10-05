package com.banhangonline.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemRequest(@NotNull Long storeId, @NotNull Long productId, @Min(1) int quantity) {}
