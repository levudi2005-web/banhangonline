package com.banhangonline.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record StaffStatusRequest(@NotBlank @Pattern(regexp = "ACTIVE|DISABLED") String status) {}
