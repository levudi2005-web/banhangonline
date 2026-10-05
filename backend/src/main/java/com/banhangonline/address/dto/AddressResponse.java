package com.banhangonline.address.dto;

import com.banhangonline.address.entity.UserAddress;

public record AddressResponse(Long id, String recipientName, String phone, String province, String district,
                              String ward, String addressLine, boolean isDefault) {
    public static AddressResponse from(UserAddress address) {
        return new AddressResponse(address.getId(), address.getRecipientName(), address.getPhone(),
                address.getProvince(), address.getDistrict(), address.getWard(), address.getAddressLine(),
                address.isDefaultAddress());
    }
}
