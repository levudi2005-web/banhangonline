package com.banhangonline.store.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStoreRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 20) String phone,
        @Email @Size(max = 190) String email,
        @NotBlank @Size(max = 100) String province,
        @NotBlank @Size(max = 100) String district,
        @NotBlank @Size(max = 100) String ward,
        @NotBlank @Size(max = 255) String addressDetail,
        @Size(max = 20) String postalCode,
        @Size(max = 1000) String description) {}
