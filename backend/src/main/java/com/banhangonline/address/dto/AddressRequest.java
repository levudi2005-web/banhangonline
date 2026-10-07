package com.banhangonline.address.dto;

import com.banhangonline.common.validation.Msg;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.AssertTrue;

import java.math.BigDecimal;

public record AddressRequest(
        @NotBlank(message = Msg.REQ) @Size(max = 120, message = Msg.LONG) String recipientName,
        @NotBlank(message = Msg.REQ) String phone,
        @NotBlank(message = Msg.REQ) @Size(max = 100, message = Msg.LONG) String province,
        @NotBlank(message = Msg.REQ) @Size(max = 100, message = Msg.LONG) String district,
        @NotBlank(message = Msg.REQ) @Size(max = 100, message = Msg.LONG) String ward,
        @NotBlank(message = Msg.REQ) @Size(max = 255, message = Msg.LONG) String addressLine,
        @DecimalMin("-90.0") @DecimalMax("90.0") @Digits(integer = 2, fraction = 7) BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") @Digits(integer = 3, fraction = 7) BigDecimal longitude) {
    @AssertTrue(message = "Vui lòng chọn cả vĩ độ và kinh độ của địa chỉ")
    public boolean isCoordinatePairValid() {
        return (latitude == null) == (longitude == null);
    }
}
