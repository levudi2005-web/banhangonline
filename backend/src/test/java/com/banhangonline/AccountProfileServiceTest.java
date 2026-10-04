package com.banhangonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.banhangonline.common.exception.ApiException;
import com.banhangonline.user.dto.ProfileUpdateRequest;
import com.banhangonline.user.entity.User;
import com.banhangonline.user.repository.UserRepository;
import com.banhangonline.user.service.AccountProfileService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountProfileServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final AccountProfileService profiles = new AccountProfileService(users);
    private final User user = new User();

    @BeforeEach
    void setUp() {
        user.setId(42L);
        user.setFullName("Old Name");
        user.setUsername("olduser");
        user.setEmail("old@example.com");
        user.setPhone("0912345678");
        when(users.findById(42L)).thenReturn(Optional.of(user));
        when(users.existsByUsernameAndIdNot(anyString(), eq(42L))).thenReturn(false);
        when(users.existsByEmailAndIdNot(anyString(), eq(42L))).thenReturn(false);
        when(users.existsByPhoneAndIdNot(anyString(), eq(42L))).thenReturn(false);
    }

    @Test
    void correctCurrentPhoneLastFourAllowsProfileChangesAndMasksPhone() {
        var response = profiles.update(42L,
                new ProfileUpdateRequest("New Name", "newuser", "new@example.com", "0987654321", "5678"));

        assertThat(user.getFullName()).isEqualTo("New Name");
        assertThat(user.getPhone()).isEqualTo("0987654321");
        assertThat(response.phone()).isEqualTo("******4321");
        assertThat(response.phone()).doesNotContain("0987654321");
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void newPhoneCannotBeUsedToVerifyItsOwnChange() {
        assertThatThrownBy(() -> profiles.update(42L,
                new ProfileUpdateRequest("New Name", "newuser", "new@example.com", "0987654321", "4321")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("INVALID_PHONE_VERIFICATION");
        assertThat(user.getPhone()).isEqualTo("0912345678");
        verify(users, never()).existsByPhoneAndIdNot(anyString(), anyLong());
    }

    @Test
    void wrongCurrentPhoneLastFourRejectsChanges() {
        assertThatThrownBy(() -> profiles.update(42L,
                new ProfileUpdateRequest("New Name", "newuser", "new@example.com", "", "0000")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("INVALID_PHONE_VERIFICATION");
        assertThat(user.getFullName()).isEqualTo("Old Name");
    }
}
