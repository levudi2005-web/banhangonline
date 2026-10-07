package com.banhangonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.banhangonline.address.repository.UserAddressRepository;
import com.banhangonline.auth.dto.RegisterRequest;
import com.banhangonline.auth.service.AuthService;
import com.banhangonline.common.exception.ApiException;
import com.banhangonline.role.entity.Role;
import com.banhangonline.role.repository.RoleRepository;
import com.banhangonline.user.entity.User;
import com.banhangonline.user.entity.UserStatus;
import com.banhangonline.user.repository.UserRepository;
import com.banhangonline.address.dto.AddressRequest;
import java.util.Optional;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthServiceTest {
    private static final String PASSWORD = "Test-password-25";
    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final UserAddressRepository addresses = mock(UserAddressRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final AuthService auth = new AuthService(users, roles, addresses, encoder);

    @BeforeEach
    void setUp() {
        when(users.existsByEmail(anyString())).thenReturn(false);
        when(users.existsByUsername(anyString())).thenReturn(false);
        when(users.existsByPhone(anyString())).thenReturn(false);
        Role customer = new Role();
        customer.setName("CUSTOMER");
        when(roles.findByName("CUSTOMER")).thenReturn(Optional.of(customer));
    }

    @Test
    void customerRegistrationCreatesActiveBcryptAccountAndAddressWithoutOtp() {
        RegisterRequest request = registration("buyer@example.com", "buyer25", "0912345678");

        User user = auth.registerCustomer(request);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.hasRole("CUSTOMER")).isTrue();
        assertThat(user.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, user.getPasswordHash())).isTrue();
        verify(users).save(user);
        verify(addresses).save(any());
    }

    @Test
    void duplicateRegistrationIdentifiersRemainRejected() {
        when(users.existsByEmail("taken@example.com")).thenReturn(true);
        assertThatThrownBy(() -> auth.registerCustomer(
                registration("taken@example.com", "buyer25", "0912345678")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("EMAIL_TAKEN");

        when(users.existsByEmail("taken@example.com")).thenReturn(false);
        when(users.existsByUsername("buyer25")).thenReturn(true);
        assertThatThrownBy(() -> auth.registerCustomer(
                registration("taken@example.com", "buyer25", "0912345678")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("USERNAME_TAKEN");

        when(users.existsByUsername("buyer25")).thenReturn(false);
        when(users.existsByPhone("0912345678")).thenReturn(true);
        assertThatThrownBy(() -> auth.registerCustomer(
                registration("taken@example.com", "buyer25", "0912345678")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("PHONE_TAKEN");
    }

    @Test
    void customerRegistrationPersistsAddressCoordinates() {
        RegisterRequest request = new RegisterRequest(
                "Buyer Name", "buyer25", "buyer@example.com", "0912345678", PASSWORD, PASSWORD,
                new AddressRequest("Buyer Name", "0912345678", "Ha Noi", "Ba Dinh", "Phuc Xa",
                        "1 Test Street", new BigDecimal("21.0345000"), new BigDecimal("105.8123000")));

        auth.registerCustomer(request);

        verify(addresses).save(argThat(address ->
                address.getLatitude().compareTo(new BigDecimal("21.0345000")) == 0
                        && address.getLongitude().compareTo(new BigDecimal("105.8123000")) == 0));
    }

    @Test
    void loginAuthenticatesPasswordWithoutOtpDependency() {
        User user = new User();
        user.setEmail("buyer@example.com");
        user.setUsername("buyer25");
        user.setPhone("0912345678");
        user.setPasswordHash(encoder.encode(PASSWORD));
        user.setStatus(UserStatus.ACTIVE);
        when(users.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));

        assertThat(auth.authenticate("buyer@example.com", PASSWORD)).isSameAs(user);
    }

    private RegisterRequest registration(String email, String username, String phone) {
        return new RegisterRequest(
                "Buyer Name", username, email, phone, PASSWORD, PASSWORD,
                new AddressRequest("Buyer Name", phone, "Ha Noi", "Ba Dinh", "Phuc Xa", "1 Test Street",
                        null, null));
    }
}
