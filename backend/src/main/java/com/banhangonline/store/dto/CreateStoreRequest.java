package com.banhangonline.store.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.AssertTrue;

import java.math.BigDecimal;

public record CreateStoreRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 20) String phone,
        @Email @Size(max = 190) String email,
        @NotBlank @Size(max = 100) String province,
        @NotBlank @Size(max = 100) String district,
        @NotBlank @Size(max = 100) String ward,
        @NotBlank @Size(max = 255) String addressDetail,
        @Size(max = 20) String postalCode,
        @Size(max = 1000) String description,
        @DecimalMin("-90.0") @DecimalMax("90.0") @Digits(integer = 2, fraction = 7) BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") @Digits(integer = 3, fraction = 7) BigDecimal longitude) {
    @AssertTrue(message = "Vui lòng chọn cả vĩ độ và kinh độ của cửa hàng")
    public boolean isCoordinatePairValid() {
        return (latitude == null) == (longitude == null);
    }
}
