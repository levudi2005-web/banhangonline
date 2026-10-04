package com.banhangonline.auth.dto;

import com.banhangonline.address.dto.AddressRequest;
import com.banhangonline.common.validation.Msg;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = Msg.REQ) @Size(max = 120, message = Msg.LONG) String fullName,
        @NotBlank(message = Msg.REQ) @Pattern(regexp = Msg.USERNAME_RE, message = Msg.USERNAME) String username,
        @NotBlank(message = Msg.REQ) @Email(message = Msg.EMAIL) @Size(max = 190, message = Msg.LONG) String email,
        @NotBlank(message = Msg.REQ) String phone,
        @NotBlank(message = Msg.REQ) @Size(min = 8, max = 72, message = Msg.PASSWORD) String password,
        @NotBlank(message = Msg.REQ) String confirmPassword,
        @Valid AddressRequest address) {}
