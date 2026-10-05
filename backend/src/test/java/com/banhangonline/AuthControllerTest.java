package com.banhangonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.banhangonline.address.dto.AddressRequest;
import com.banhangonline.auth.controller.AuthController;
import com.banhangonline.auth.controller.StaffAuthController;
import com.banhangonline.auth.dto.ForgotPasswordRequest;
import com.banhangonline.auth.dto.LoginRequest;
import com.banhangonline.auth.dto.RegisterRequest;
import com.banhangonline.auth.security.SessionCookies;
import com.banhangonline.auth.service.AuthService;
import com.banhangonline.auth.service.PasswordRecoveryService;
import com.banhangonline.auth.service.PasswordResetService;
import com.banhangonline.role.entity.Role;
import com.banhangonline.user.entity.User;
import com.banhangonline.user.entity.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AuthControllerTest {
    private final AuthService auth = mock(AuthService.class);
    private final PasswordResetService resets = mock(PasswordResetService.class);
    private final PasswordRecoveryService recovery = mock(PasswordRecoveryService.class);
    private final SessionCookies cookies = mock(SessionCookies.class);
    private final AuthController controller = new AuthController(auth, resets, recovery, cookies);
    private final StaffAuthController staffController = new StaffAuthController(auth, cookies);

    @Test
    void customerRegistrationDoesNotInvokeOtpServiceOrEmailSender() {
        RegisterRequest request = new RegisterRequest("Buyer Name", "buyer25", "buyer@example.com",
                "0912345678", "Test-password-25", "Test-password-25",
                new AddressRequest("Buyer Name", "0912345678", "Ha Noi", "Ba Dinh", "Phuc Xa", "1 Test Street"));
        User user = new User();
        user.setId(1L);
        user.setFullName("Buyer Name");
        user.setUsername("buyer25");
        user.setEmail("buyer@example.com");
        user.setPhone("0912345678");
        user.setStatus(UserStatus.ACTIVE);
        Role role = new Role();
        role.setName("CUSTOMER");
        user.getRoles().add(role);
        when(auth.registerCustomer(request)).thenReturn(user);

        var response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data().status()).isEqualTo("ACTIVE");
        assertThat(response.getBody().data().phone()).isEqualTo("******5678");
        verifyNoInteractions(cookies);
    }

    @Test
    void forgotPasswordUsesPhoneVerificationInsteadOfOtp() {
        when(recovery.verifyPhoneAndIssueToken("buyer@example.com", "5678")).thenReturn("reset-token");

        var response = controller.forgot(new ForgotPasswordRequest("buyer@example.com", "5678"));

        assertThat(response.data().resetToken()).isEqualTo("reset-token");
        verify(recovery).verifyPhoneAndIssueToken("buyer@example.com", "5678");
    }

    @Test
    void managementLoginAcceptsOnlyStaffOrOwnerAuthenticationPath() {
        LoginRequest request = new LoginRequest("owner1", "Test-password-25", true);
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        User owner = new User();
        owner.setId(7L);
        Role role = new Role();
        role.setName("OWNER");
        owner.getRoles().add(role);
        when(auth.authenticateStaff(request.account(), request.password())).thenReturn(owner);

        staffController.login(request, servletRequest);

        verify(auth).authenticateStaff(request.account(), request.password());
        verify(cookies).start(owner, true, "Đăng nhập quản trị thành công", servletRequest);
    }
}
