package com.banhangonline.auth.dto;

import com.banhangonline.common.validation.Msg;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = Msg.REQ) @Size(max = 200, message = Msg.LONG) String token,
        @NotBlank(message = Msg.REQ) @Size(min = 8, max = 72, message = Msg.PASSWORD) String newPassword,
        @NotBlank(message = Msg.REQ) String confirmPassword) {}
