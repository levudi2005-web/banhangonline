package com.banhangonline.address.dto;

import com.banhangonline.common.validation.Msg;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = Msg.REQ) @Size(max = 120, message = Msg.LONG) String recipientName,
        @NotBlank(message = Msg.REQ) String phone,
        @NotBlank(message = Msg.REQ) @Size(max = 100, message = Msg.LONG) String province,
        @NotBlank(message = Msg.REQ) @Size(max = 100, message = Msg.LONG) String district,
        @NotBlank(message = Msg.REQ) @Size(max = 100, message = Msg.LONG) String ward,
        @NotBlank(message = Msg.REQ) @Size(max = 255, message = Msg.LONG) String addressLine) {}
