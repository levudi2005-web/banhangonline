package com.banhangonline.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PickupConfirmationRequest(@NotBlank @Pattern(regexp = "\\d{10}") String pickupCode) {}
