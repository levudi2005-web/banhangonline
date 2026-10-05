package com.banhangonline.store.dto;

import com.banhangonline.store.entity.StoreStaff;

import java.util.Set;

public record StaffStoreResponse(Long membershipId, Long storeId, String storeName,
                                 String membershipStatus, Set<String> permissions) {
    public static StaffStoreResponse from(StoreStaff membership, Set<String> permissions) {
        return new StaffStoreResponse(membership.getId(), membership.getStore().getId(),
                membership.getStore().getName(), membership.getStatus(), permissions);
    }
}
