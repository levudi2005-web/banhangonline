package com.banhangonline.auth.dto;

import com.banhangonline.common.validation.Msg;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(@NotBlank(message = Msg.REQ) @Size(max = 190, message = Msg.LONG) String account) {}
