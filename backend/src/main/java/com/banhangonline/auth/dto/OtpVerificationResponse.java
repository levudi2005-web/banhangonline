package com.banhangonline.auth.dto;

public record OtpVerificationResponse(boolean verified, String resetToken) {}
