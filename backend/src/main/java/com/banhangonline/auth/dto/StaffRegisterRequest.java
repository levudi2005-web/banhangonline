package com.banhangonline.auth.dto;

import com.banhangonline.common.validation.Msg;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StaffRegisterRequest(
        @NotNull(message = Msg.REQ) @Valid Owner owner,
        @NotNull(message = Msg.REQ) @Valid Store store,
        @AssertTrue(message = "Bạn cần đồng ý điều khoản sử dụng") boolean termsAccepted) {

    public record Owner(
            @NotBlank(message = Msg.REQ) @Size(max = 120, message = Msg.LONG) String fullName,
            @NotBlank(message = Msg.REQ) @Pattern(regexp = Msg.USERNAME_RE, message = Msg.USERNAME) String username,
            @NotBlank(message = Msg.REQ) @Email(message = Msg.EMAIL) @Size(max = 190, message = Msg.LONG) String email,
            @NotBlank(message = Msg.REQ) String phone,
            @NotBlank(message = Msg.REQ) @Size(min = 8, max = 72, message = Msg.PASSWORD) String password,
            @NotBlank(message = Msg.REQ) String confirmPassword) {}

    public record Store(
            @NotBlank(message = Msg.REQ) @Size(max = 160, message = Msg.LONG) String name,
            @NotBlank(message = Msg.REQ) String phone,
            @Email(message = Msg.EMAIL) @Size(max = 190, message = Msg.LONG) String email,
            @NotBlank(message = Msg.REQ) @Size(max = 100, message = Msg.LONG) String province,
            @NotBlank(message = Msg.REQ) @Size(max = 100, message = Msg.LONG) String district,
            @NotBlank(message = Msg.REQ) @Size(max = 100, message = Msg.LONG) String ward,
            @NotBlank(message = Msg.REQ) @Size(max = 255, message = Msg.LONG) String detailedAddress,
            @Size(max = 20, message = Msg.LONG) String postalCode,
            @Size(max = 1000, message = Msg.LONG) String description) {}
}
