package com.banhangonline;

import com.banhangonline.permission.entity.Permission;
import com.banhangonline.permission.repository.PermissionRepository;
import com.banhangonline.role.entity.Role;
import com.banhangonline.role.repository.RoleRepository;
import com.banhangonline.store.dto.CreateStaffRequest;
import com.banhangonline.store.entity.Store;
import com.banhangonline.store.entity.StoreStaff;
import com.banhangonline.store.repository.StoreStaffPermissionRepository;
import com.banhangonline.store.repository.StoreStaffRepository;
import com.banhangonline.store.service.StaffManagementService;
import com.banhangonline.store.service.StoreManagementService;
import com.banhangonline.user.entity.User;
import com.banhangonline.user.entity.UserStatus;
import com.banhangonline.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StaffManagementServiceTest {
    private final StoreManagementService storeService = mock(StoreManagementService.class);
    private final StoreStaffRepository memberships = mock(StoreStaffRepository.class);
    private final StoreStaffPermissionRepository grants = mock(StoreStaffPermissionRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final PermissionRepository permissions = mock(PermissionRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final StaffManagementService service = new StaffManagementService(storeService, memberships, grants,
            users, roles, permissions, encoder);

    @Test
    void createsBcryptStaffAccountWithStoreScopedMembershipAndGrantedPermission() {
        Store store = new Store();
        store.setId(6L);
        when(storeService.requireOwnedActive(6L, 10L)).thenReturn(store);
        Role staffRole = new Role();
        staffRole.setName("STAFF");
        when(roles.findByName("STAFF")).thenReturn(java.util.Optional.of(staffRole));
        when(permissions.findByNameIn(Set.of("MANAGE_ORDERS")))
                .thenReturn(List.of(permission("MANAGE_ORDERS")));
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(21L);
            return user;
        });
        when(memberships.save(any(StoreStaff.class))).thenAnswer(invocation -> {
            StoreStaff membership = invocation.getArgument(0);
            membership.setId(31L);
            return membership;
        });
        when(grants.findByMembershipIdOrderByPermissionName(31L)).thenReturn(List.of());

        var response = service.create(6L, 10L,
                new CreateStaffRequest("Staff Name", "staffname", "staff@example.com",
                        "0912345678", "Correct-Horse1", Set.of("MANAGE_ORDERS")));

        assertThat(response.membershipId()).isEqualTo(31L);
        assertThat(response.userId()).isEqualTo(21L);
        assertThat(response.status()).isEqualTo("ACTIVE");
        verify(users).save(argThat(user -> user.getStatus() == UserStatus.ACTIVE
                && user.hasRole("STAFF")
                && encoder.matches("Correct-Horse1", user.getPasswordHash())));
        verify(memberships).save(argThat(membership -> membership.getStore() == store
                && membership.getUser().getId().equals(21L)));
        verify(grants).save(any());
    }

    private static Permission permission(String name) {
        Permission permission = new Permission();
        permission.setName(name);
        return permission;
    }
}
