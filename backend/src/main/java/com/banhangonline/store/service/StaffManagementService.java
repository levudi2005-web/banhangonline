package com.banhangonline.store.service;

import com.banhangonline.auth.service.Rules;
import com.banhangonline.common.exception.ApiException;
import com.banhangonline.permission.entity.Permission;
import com.banhangonline.permission.repository.PermissionRepository;
import com.banhangonline.role.entity.Role;
import com.banhangonline.role.repository.RoleRepository;
import com.banhangonline.store.dto.CreateStaffRequest;
import com.banhangonline.store.dto.ReplaceStaffPermissionsRequest;
import com.banhangonline.store.dto.StaffResponse;
import com.banhangonline.store.dto.StaffStoreResponse;
import com.banhangonline.store.dto.StaffStatusRequest;
import com.banhangonline.store.entity.StoreStaff;
import com.banhangonline.store.entity.StoreStaffPermission;
import com.banhangonline.store.repository.StoreStaffPermissionRepository;
import com.banhangonline.store.repository.StoreStaffRepository;
import com.banhangonline.user.entity.User;
import com.banhangonline.user.entity.UserStatus;
import com.banhangonline.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StaffManagementService {
    private final StoreManagementService storeService;
    private final StoreStaffRepository memberships;
    private final StoreStaffPermissionRepository grants;
    private final UserRepository users;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final PasswordEncoder encoder;

    public StaffManagementService(StoreManagementService storeService, StoreStaffRepository memberships,
                                  StoreStaffPermissionRepository grants, UserRepository users,
                                  RoleRepository roles, PermissionRepository permissions, PasswordEncoder encoder) {
        this.storeService = storeService;
        this.memberships = memberships;
        this.grants = grants;
        this.users = users;
        this.roles = roles;
        this.permissions = permissions;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> list(Long storeId, Long ownerId) {
        storeService.requireOwnedActive(storeId, ownerId);
        return memberships.findByStoreIdOrderById(storeId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<StaffStoreResponse> storesForUser(Long userId) {
        return memberships.findByUserIdAndStatusOrderByStoreId(userId, "ACTIVE").stream()
                .map(membership -> {
                    Set<String> names = grants.findByMembershipIdOrderByPermissionName(membership.getId()).stream()
                            .map(grant -> grant.getPermission().getName())
                            .collect(Collectors.toUnmodifiableSet());
                    return StaffStoreResponse.from(membership, names);
                }).toList();
    }

    @Transactional
    public StaffResponse create(Long storeId, Long ownerId, CreateStaffRequest request) {
        var store = storeService.requireOwnedActive(storeId, ownerId);
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String email = Rules.email(request.email());
        String phone = Rules.phone(request.phone());
        Rules.password(request.initialPassword(), request.initialPassword());
        if (users.existsByUsername(username) || users.existsByEmail(email) || users.existsByPhone(phone)) {
            throw ApiException.conflict("STAFF_ACCOUNT_TAKEN", "Tên đăng nhập, email hoặc số điện thoại đã được sử dụng");
        }

        Role staffRole = roles.findByName("STAFF").orElseThrow(() ->
                new IllegalStateException("Role STAFF chưa được tạo trong database"));
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(encoder.encode(request.initialPassword()));
        user.setEmailVerified(false);
        user.setStatus(UserStatus.ACTIVE);
        user.getRoles().add(staffRole);
        users.save(user);

        StoreStaff membership = new StoreStaff();
        membership.setStore(store);
        membership.setUser(user);
        membership.setStatus("ACTIVE");
        memberships.save(membership);
        replacePermissions(membership, ownerId, request.permissions() == null ? Set.of() : request.permissions());
        return toResponse(membership);
    }

    @Transactional
    public StaffResponse setStatus(Long storeId, Long membershipId, Long ownerId, StaffStatusRequest request) {
        storeService.requireOwnedActive(storeId, ownerId);
        StoreStaff membership = findMembership(storeId, membershipId);
        membership.setStatus(request.status());
        memberships.save(membership);
        return toResponse(membership);
    }

    @Transactional
    public StaffResponse replacePermissions(Long storeId, Long membershipId, Long ownerId,
                                            ReplaceStaffPermissionsRequest request) {
        storeService.requireOwnedActive(storeId, ownerId);
        StoreStaff membership = findMembership(storeId, membershipId);
        replacePermissions(membership, ownerId, request.permissions());
        return toResponse(membership);
    }

    private void replacePermissions(StoreStaff membership, Long ownerId, Set<String> requestedNames) {
        Set<String> names = requestedNames.stream().map(String::trim).collect(Collectors.toSet());
        List<Permission> resolved = names.isEmpty() ? List.of() : permissions.findByNameIn(names);
        Set<String> resolvedNames = resolved.stream().map(Permission::getName).collect(Collectors.toSet());
        if (!resolvedNames.equals(names)) {
            Set<String> unknown = names.stream().filter(name -> !resolvedNames.contains(name)).collect(Collectors.toSet());
            throw ApiException.validation("Quyền không hợp lệ: " + String.join(", ", unknown));
        }
        grants.deleteByMembershipId(membership.getId());
        User grantor = users.getReferenceById(ownerId);
        for (Permission permission : resolved) {
            StoreStaffPermission grant = new StoreStaffPermission();
            grant.setMembership(membership);
            grant.setPermission(permission);
            grant.setGrantedBy(grantor);
            grants.save(grant);
        }
    }

    private StoreStaff findMembership(Long storeId, Long membershipId) {
        return memberships.findByIdAndStoreId(membershipId, storeId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "STAFF_NOT_FOUND", "Không tìm thấy nhân viên"));
    }

    private StaffResponse toResponse(StoreStaff membership) {
        Set<String> names = grants.findByMembershipIdOrderByPermissionName(membership.getId()).stream()
                .map(grant -> grant.getPermission().getName())
                .collect(Collectors.toUnmodifiableSet());
        return StaffResponse.from(membership, names);
    }
}
