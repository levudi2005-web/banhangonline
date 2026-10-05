package com.banhangonline.store.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateStaffRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Pattern(regexp = "^(?=.*[A-Za-z])[A-Za-z0-9_.]{4,30}$") String username,
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Size(max = 20) String phone,
        @NotBlank String initialPassword,
        Set<@NotBlank @Size(max = 60) String> permissions) {}
