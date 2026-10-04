package com.banhangonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.banhangonline.auth.service.AuthService;
import com.banhangonline.auth.service.PasswordRecoveryService;
import com.banhangonline.auth.service.PasswordResetService;
import com.banhangonline.common.exception.ApiException;
import com.banhangonline.user.entity.User;
import com.banhangonline.user.entity.UserStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PasswordRecoveryServiceTest {
    private final AuthService auth = mock(AuthService.class);
    private final PasswordResetService resets = mock(PasswordResetService.class);
    private final PasswordRecoveryService recovery = new PasswordRecoveryService(auth, resets);

    @Test
    void correctPhoneLastFourIssuesResetToken() {
        User user = user();
        when(auth.findByAccount("buyer@example.com")).thenReturn(Optional.of(user));
        when(resets.issueAfterPhoneVerification(user)).thenReturn("opaque-reset-token");

        assertThat(recovery.verifyPhoneAndIssueToken("buyer@example.com", "5678"))
                .isEqualTo("opaque-reset-token");
        verify(resets).issueAfterPhoneVerification(user);
    }

    @Test
    void wrongPhoneLastFourDoesNotIssueResetToken() {
        when(auth.findByAccount("buyer@example.com")).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> recovery.verifyPhoneAndIssueToken("buyer@example.com", "0000"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("INVALID_RECOVERY");
        verifyNoInteractions(resets);
    }

    @Test
    void unknownAccountUsesTheSameRecoveryFailure() {
        when(auth.findByAccount("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recovery.verifyPhoneAndIssueToken("missing@example.com", "5678"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("INVALID_RECOVERY");
        verifyNoInteractions(resets);
    }

    @Test
    void nonActiveAccountCannotReceiveResetToken() {
        User user = user();
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        when(auth.findByAccount("buyer@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> recovery.verifyPhoneAndIssueToken("buyer@example.com", "5678"))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("INVALID_RECOVERY");
        verifyNoInteractions(resets);
    }

    private User user() {
        User user = new User();
        user.setPhone("0912345678");
        return user;
    }
}
