package com.banhangonline;

import com.banhangonline.common.exception.ApiException;
import com.banhangonline.store.entity.Store;
import com.banhangonline.store.entity.StoreStaff;
import com.banhangonline.store.repository.StoreRepository;
import com.banhangonline.store.repository.StoreStaffPermissionRepository;
import com.banhangonline.store.repository.StoreStaffRepository;
import com.banhangonline.store.service.StorePermissionService;
import com.banhangonline.user.entity.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class StorePermissionServiceTest {
    private final StoreRepository stores = mock(StoreRepository.class);
    private final StoreStaffRepository memberships = mock(StoreStaffRepository.class);
    private final StoreStaffPermissionRepository grants = mock(StoreStaffPermissionRepository.class);
    private final StorePermissionService service = new StorePermissionService(stores, memberships, grants);

    @Test
    void ownerCanManageOnlyTheirActiveStore() {
        User owner = new User();
        owner.setId(10L);
        Store store = store(4L, owner, "ACTIVE");
        when(stores.findById(4L)).thenReturn(Optional.of(store));

        assertThatCode(() -> service.require(4L, 10L, Set.of("OWNER"), "MANAGE_ORDERS"))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> service.require(4L, 11L, Set.of("OWNER"), "MANAGE_ORDERS"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("STORE_ACCESS_DENIED");
    }

    @Test
    void staffNeedsActiveMembershipAndExplicitStorePermission() {
        User owner = new User();
        owner.setId(10L);
        Store store = store(4L, owner, "ACTIVE");
        StoreStaff membership = new StoreStaff();
        membership.setId(23L);
        membership.setStatus("ACTIVE");
        when(stores.findById(4L)).thenReturn(Optional.of(store));
        when(memberships.findByStoreIdAndUserId(4L, 12L)).thenReturn(Optional.of(membership));
        when(grants.existsByMembershipIdAndPermissionName(23L, "MANAGE_ORDERS")).thenReturn(false, true);

        assertThatThrownBy(() -> service.require(4L, 12L, Set.of("STAFF"), "MANAGE_ORDERS"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("STORE_PERMISSION_REQUIRED");
        assertThatCode(() -> service.require(4L, 12L, Set.of("STAFF"), "MANAGE_ORDERS"))
                .doesNotThrowAnyException();
    }

    @Test
    void productViewPermissionDoesNotGrantInventoryAccess() {
        User owner = new User();
        owner.setId(10L);
        Store store = store(4L, owner, "ACTIVE");
        StoreStaff membership = new StoreStaff();
        membership.setId(23L);
        membership.setStatus("ACTIVE");
        when(stores.findById(4L)).thenReturn(Optional.of(store));
        when(memberships.findByStoreIdAndUserId(4L, 12L)).thenReturn(Optional.of(membership));
        when(grants.existsByMembershipIdAndPermissionName(23L, "VIEW_PRODUCTS")).thenReturn(true);
        when(grants.existsByMembershipIdAndPermissionName(23L, "VIEW_INVENTORY")).thenReturn(false);
        when(grants.existsByMembershipIdAndPermissionName(23L, "MANAGE_INVENTORY")).thenReturn(false);

        assertThatCode(() -> service.requireAnyView(4L, 12L, Set.of("STAFF"),
                "VIEW_PRODUCTS", "MANAGE_PRODUCTS", "VIEW_INVENTORY", "MANAGE_INVENTORY"))
                .doesNotThrowAnyException();
        assertThat(service.hasAnyPermission(
                4L, 12L, Set.of("STAFF"), "VIEW_INVENTORY", "MANAGE_INVENTORY")).isFalse();
    }

    @Test
    void inactiveStoreCannotBeManagedByOwner() {
        User owner = new User();
        owner.setId(10L);
        when(stores.findById(4L)).thenReturn(Optional.of(store(4L, owner, "PENDING")));

        assertThatThrownBy(() -> service.require(4L, 10L, Set.of("OWNER"), "MANAGE_STAFF"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("STORE_NOT_ACTIVE");
    }

    private static Store store(Long id, User owner, String status) {
        Store store = new Store();
        store.setId(id);
        store.setOwner(owner);
        store.setStatus(status);
        return store;
    }
}
