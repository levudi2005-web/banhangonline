package com.banhangonline.store.service;

import com.banhangonline.common.exception.ApiException;
import com.banhangonline.store.repository.StoreRepository;
import com.banhangonline.store.repository.StoreStaffPermissionRepository;
import com.banhangonline.store.repository.StoreStaffRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class StorePermissionService {
    private final StoreRepository stores;
    private final StoreStaffRepository memberships;
    private final StoreStaffPermissionRepository grants;

    public StorePermissionService(StoreRepository stores, StoreStaffRepository memberships,
                                  StoreStaffPermissionRepository grants) {
        this.stores = stores;
        this.memberships = memberships;
        this.grants = grants;
    }

    public void require(Long storeId, Long userId, Set<String> roles, String permission) {
        var store = stores.findById(storeId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "STORE_NOT_FOUND", "Không tìm thấy cửa hàng"));
        if (!"ACTIVE".equals(store.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "STORE_NOT_ACTIVE", "Cửa hàng chưa được duyệt hoạt động");
        }
        if (roles.contains("OWNER") && store.getOwner().getId().equals(userId)) {
            return;
        }
        var membership = memberships.findByStoreIdAndUserId(storeId, userId)
                .filter(item -> "ACTIVE".equals(item.getStatus()))
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "STORE_ACCESS_DENIED",
                        "Bạn chưa được phân công vào cửa hàng này"));
        if (!grants.existsByMembershipIdAndPermissionName(membership.getId(), permission)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "STORE_PERMISSION_REQUIRED",
                    "Bạn chưa được cấp quyền cần thiết trong cửa hàng này");
        }
    }

    public void requireAnyView(Long storeId, Long userId, Set<String> roles, String... permissions) {
        if (roles.contains("OWNER")) {
            require(storeId, userId, roles, permissions[0]);
            return;
        }
        for (String permission : permissions) {
            try {
                require(storeId, userId, roles, permission);
                return;
            } catch (ApiException error) {
                if (!"STORE_PERMISSION_REQUIRED".equals(error.getCode())) {
                    throw error;
                }
            }
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "STORE_PERMISSION_REQUIRED",
                "Bạn chưa được cấp quyền cần thiết trong cửa hàng này");
    }

    public boolean hasAnyPermission(Long storeId, Long userId, Set<String> roles, String... permissions) {
        for (String permission : permissions) {
            try {
                require(storeId, userId, roles, permission);
                return true;
            } catch (ApiException error) {
                if (!"STORE_PERMISSION_REQUIRED".equals(error.getCode())) {
                    throw error;
                }
            }
        }
        return false;
    }
}
