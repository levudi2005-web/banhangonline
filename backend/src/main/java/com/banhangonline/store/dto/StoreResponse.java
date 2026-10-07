package com.banhangonline.store.dto;

import com.banhangonline.store.entity.Store;

import java.math.BigDecimal;

public record StoreResponse(
        Long id,
        String name,
        String phone,
        String email,
        String province,
        String district,
        String ward,
        String addressDetail,
        String postalCode,
        String description,
        String status,
        BigDecimal latitude,
        BigDecimal longitude) {
    public static StoreResponse from(Store store) {
        return new StoreResponse(store.getId(), store.getName(), store.getPhone(), store.getEmail(),
                store.getProvince(), store.getDistrict(), store.getWard(), store.getAddressDetail(),
                store.getPostalCode(), store.getDescription(), store.getStatus(),
                store.getLatitude(), store.getLongitude());
    }
}
