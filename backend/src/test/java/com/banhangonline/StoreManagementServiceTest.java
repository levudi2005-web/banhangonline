package com.banhangonline;

import com.banhangonline.common.exception.ApiException;
import com.banhangonline.store.dto.CreateStoreRequest;
import com.banhangonline.store.entity.Store;
import com.banhangonline.store.repository.StoreRepository;
import com.banhangonline.store.service.StoreManagementService;
import com.banhangonline.user.entity.User;
import com.banhangonline.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class StoreManagementServiceTest {
    private final StoreRepository stores = mock(StoreRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final StoreManagementService service = new StoreManagementService(stores, users);

    @Test
    void ownerStoreStartsPendingAndPhoneIsNormalized() {
        User owner = new User();
        owner.setId(9L);
        owner.getRoles().add(role("OWNER"));
        when(users.findById(9L)).thenReturn(Optional.of(owner));
        when(stores.save(any(Store.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.create(9L, new CreateStoreRequest(" Cửa hàng A ", "+84912345678",
                null, " Hà Nội ", " Ba Đình ", " Phúc Xá ", " 1 Phố Mẫu ", null, null));

        assertThat(response.phone()).isEqualTo("0912345678");
        assertThat(response.status()).isEqualTo("DRAFT");
        verify(stores).save(argThat(store -> store.getOwner() == owner
                && store.getName().equals("Cửa hàng A")
                && store.getAddressDetail().equals("1 Phố Mẫu")));
    }

    @Test
    void customerCannotCreateStore() {
        User customer = new User();
        customer.setId(3L);
        customer.getRoles().add(role("CUSTOMER"));
        when(users.findById(3L)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.create(3L, new CreateStoreRequest("A", "0912345678",
                null, "P", "D", "W", "Address", null, null)))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("OWNER_REQUIRED");
        verify(stores, never()).save(any());
    }

    @Test
    void draftCanBeEditedAndSubmittedForReview() {
        User owner = new User();
        owner.setId(9L);
        owner.getRoles().add(role("OWNER"));
        Store store = new Store();
        store.setId(17L);
        store.setOwner(owner);
        store.setStatus("DRAFT");
        when(stores.findByIdAndOwnerId(17L, 9L)).thenReturn(Optional.of(store));
        CreateStoreRequest request = new CreateStoreRequest("Cửa hàng mới", "0912345678",
                null, "Hà Nội", "Ba Đình", "Phúc Xá", "1 Phố Mẫu", null, null);

        var updated = service.updateDraft(17L, 9L, request);
        var submitted = service.submitForReview(17L, 9L);

        assertThat(updated.status()).isEqualTo("DRAFT");
        assertThat(updated.name()).isEqualTo("Cửa hàng mới");
        assertThat(submitted.status()).isEqualTo("PENDING_REVIEW");
    }

    @Test
    void submittedStoreCannotBeEditedOrSubmittedAgain() {
        User owner = new User();
        owner.setId(9L);
        Store store = new Store();
        store.setId(17L);
        store.setOwner(owner);
        store.setStatus("PENDING_REVIEW");
        when(stores.findByIdAndOwnerId(17L, 9L)).thenReturn(Optional.of(store));
        CreateStoreRequest request = new CreateStoreRequest("Cửa hàng mới", "0912345678",
                null, "Hà Nội", "Ba Đình", "Phúc Xá", "1 Phố Mẫu", null, null);

        assertThatThrownBy(() -> service.updateDraft(17L, 9L, request))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("STORE_NOT_EDITABLE");
        assertThatThrownBy(() -> service.submitForReview(17L, 9L))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("STORE_NOT_DRAFT");
    }

    private static com.banhangonline.role.entity.Role role(String name) {
        var role = new com.banhangonline.role.entity.Role();
        role.setName(name);
        return role;
    }
}
