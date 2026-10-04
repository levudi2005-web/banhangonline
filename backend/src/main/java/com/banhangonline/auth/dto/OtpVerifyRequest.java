package com.banhangonline.auth.dto;

import com.banhangonline.auth.entity.OtpChannel;
import com.banhangonline.auth.entity.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OtpVerifyRequest(
        @NotNull OtpChannel channel,
        @NotNull OtpPurpose purpose,
        @NotBlank @Size(max = 190) String destination,
        @NotBlank @Pattern(regexp = "\\d{6}") String code) {}
