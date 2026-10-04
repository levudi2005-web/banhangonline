package com.banhangonline.user.dto;

import com.banhangonline.common.validation.Msg;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank(message = Msg.REQ) @Size(max = 120, message = Msg.LONG) String fullName,
        @NotBlank(message = Msg.REQ) @Pattern(regexp = Msg.USERNAME_RE, message = Msg.USERNAME) String username,
        @NotBlank(message = Msg.REQ) @Email(message = Msg.EMAIL) @Size(max = 190, message = Msg.LONG) String email,
        @Size(max = 20, message = Msg.LONG) String phone,
        @NotBlank(message = Msg.REQ) @Pattern(regexp = "\\d{4}", message = "Nhập bốn chữ số cuối của số điện thoại")
        String phoneLastFour) {}
