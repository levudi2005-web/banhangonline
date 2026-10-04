package com.banhangonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.banhangonline.common.exception.ApiException;
import com.banhangonline.auth.service.Rules;
import org.junit.jupiter.api.Test;

class RulesTest {
    @Test
    void phoneNormalization_acceptsVietnameseFormats() {
        assertThat(Rules.normalizePhone("+84 901234567")).isEqualTo("0901234567");
        assertThat(Rules.normalizePhone("84 901234567")).isEqualTo("0901234567");
        assertThat(Rules.phone("090-123-4567")).isEqualTo("0901234567");
    }

    @Test
    void passwordValidation_rejectsWeakOrMismatchedPasswords() {
        assertThatThrownBy(() -> Rules.password("short", "short"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Mật khẩu phải có từ 8 đến 72 ký tự");
        assertThatThrownBy(() -> Rules.password("Abc12345!", "Different!123"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Mật khẩu xác nhận không khớp");
    }
}
