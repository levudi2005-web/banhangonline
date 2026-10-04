package com.banhangonline.auth.dto;

import com.banhangonline.permission.entity.Permission;
import com.banhangonline.role.entity.Role;
import com.banhangonline.user.entity.User;
import java.util.List;

/** Chỉ chứa thông tin an toàn để trả về frontend (không có passwordHash). */
public record UserResponse(Long id, String fullName, String username, String email, String phone,
                           String status, List<String> roles, List<String> permissions) {
    public static UserResponse from(User u) {
        List<String> roles = u.getRoles().stream().map(Role::getName).sorted().toList();
        List<String> perms = u.getRoles().stream().flatMap(r -> r.getPermissions().stream())
                .map(Permission::getName).distinct().sorted().toList();
        return new UserResponse(u.getId(), u.getFullName(), u.getUsername(), u.getEmail(), maskPhone(u.getPhone()),
                u.getStatus().name(), roles, perms);
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return null;
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }
}
