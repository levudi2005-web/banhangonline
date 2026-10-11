package com.banhangonline.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChatStartRequest(
        @NotNull @Positive Long storeId,
        @Positive Long orderId
) {}
