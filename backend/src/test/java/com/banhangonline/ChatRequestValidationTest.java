package com.banhangonline;

import com.banhangonline.chat.dto.ChatMessageRequest;
import com.banhangonline.chat.dto.ChatStartRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRequestValidationTest {
    private static Validator validator;
    private static AutoCloseable validatorFactory;

    @BeforeAll
    static void createValidator() {
        var factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        validatorFactory = factory;
    }

    @AfterAll
    static void closeValidator() throws Exception {
        validatorFactory.close();
    }

    @Test
    void startRequiresPositiveStoreIdAndOptionalPositiveOrderId() {
        assertThat(validator.validate(new ChatStartRequest(null, null))).hasSize(1);
        assertThat(validator.validate(new ChatStartRequest(0L, -2L))).hasSize(2);
        assertThat(validator.validate(new ChatStartRequest(7L, null))).isEmpty();
    }

    @Test
    void messageMustContainTextAndStayWithinFourThousandCharacters() {
        assertThat(validator.validate(new ChatMessageRequest("   "))).hasSize(1);
        assertThat(validator.validate(new ChatMessageRequest("x".repeat(4001)))).hasSize(1);
        assertThat(validator.validate(new ChatMessageRequest("Xin chào"))).isEmpty();
    }
}
