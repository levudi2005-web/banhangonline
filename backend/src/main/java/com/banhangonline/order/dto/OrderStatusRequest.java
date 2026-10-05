package com.banhangonline.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OrderStatusRequest(
        @NotBlank @Pattern(regexp = "CONFIRMED|PREPARING|READY_FOR_PICKUP|CANCELLED") String status,
        @Size(max = 500) String note) {}
