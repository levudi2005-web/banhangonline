package com.banhangonline.store.dto;

import com.banhangonline.store.entity.StoreStaff;

import java.util.Set;

public record StaffResponse(Long membershipId, Long userId, String fullName, String username,
                            String email, String phone, String status, Set<String> permissions) {
    public static StaffResponse from(StoreStaff membership, Set<String> permissions) {
        var user = membership.getUser();
        return new StaffResponse(membership.getId(), user.getId(), user.getFullName(), user.getUsername(),
                user.getEmail(), user.getPhone(), membership.getStatus(), permissions);
    }
}
