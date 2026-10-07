package com.banhangonline.address.dto;

import com.banhangonline.address.entity.UserAddress;

import java.math.BigDecimal;

public record AddressResponse(Long id, String recipientName, String phone, String province, String district,
                              String ward, String addressLine, boolean isDefault,
                              BigDecimal latitude, BigDecimal longitude) {
    public static AddressResponse from(UserAddress address) {
        return new AddressResponse(address.getId(), address.getRecipientName(), address.getPhone(),
                address.getProvince(), address.getDistrict(), address.getWard(), address.getAddressLine(),
                address.isDefaultAddress(), address.getLatitude(), address.getLongitude());
    }
}
