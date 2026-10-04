package com.banhangonline.auth.dto;

import com.banhangonline.common.validation.Msg;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank(message = Msg.REQ) @Size(max = 190, message = Msg.LONG) String account,
        @NotBlank(message = Msg.REQ) @Pattern(regexp = "\\d{4}", message = "Nhập bốn chữ số cuối của số điện thoại")
        String phoneLastFour) {}
