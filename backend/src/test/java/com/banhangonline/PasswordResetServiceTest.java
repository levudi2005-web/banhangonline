package com.banhangonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.banhangonline.auth.entity.PasswordResetToken;
import com.banhangonline.auth.repository.PasswordResetTokenRepository;
import com.banhangonline.auth.repository.SessionRepository;
import com.banhangonline.auth.service.PasswordResetService;
import com.banhangonline.auth.security.TokenUtil;
import com.banhangonline.config.AppProperties;
import com.banhangonline.user.entity.User;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordResetServiceTest {
    private final PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
    private final SessionRepository sessions = mock(SessionRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final PasswordResetService resets =
            new PasswordResetService(tokens, sessions, encoder, mock(AppProperties.class));

    @Test
    void successfulResetHashesPasswordAndRevokesEverySession() {
        String rawToken = "valid-reset-token";
        User user = new User();
        user.setPasswordHash(encoder.encode("Old-password-25"));
        PasswordResetToken token = new PasswordResetToken();
        token.setId(15L);
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusSeconds(300));
        when(tokens.findByTokenHash(TokenUtil.sha256(rawToken))).thenReturn(Optional.of(token));
        when(tokens.markUsed(eq(15L), any(Instant.class))).thenReturn(1);

        resets.reset(rawToken, "New-password-25", "New-password-25");

        assertThat(user.getPasswordHash()).isNotEqualTo("New-password-25");
        assertThat(encoder.matches("New-password-25", user.getPasswordHash())).isTrue();
        verify(sessions).deleteByUser(user);
    }
}
