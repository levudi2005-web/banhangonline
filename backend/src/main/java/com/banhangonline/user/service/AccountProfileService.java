package com.banhangonline.user.service;

import com.banhangonline.auth.dto.UserResponse;
import com.banhangonline.auth.service.Rules;
import com.banhangonline.common.exception.ApiException;
import com.banhangonline.user.dto.ProfileUpdateRequest;
import com.banhangonline.user.entity.User;
import com.banhangonline.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
public class AccountProfileService {
    private final UserRepository users;

    public AccountProfileService(UserRepository users) {
        this.users = users;
    }

    @Transactional
    public UserResponse update(Long userId, ProfileUpdateRequest request) {
        User user = users.findById(userId).orElseThrow(() ->
                new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Phiên đăng nhập không hợp lệ"));
        if (!Rules.phoneLastFourMatches(user.getPhone(), request.phoneLastFour())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PHONE_VERIFICATION",
                    "Bốn số cuối của số điện thoại hiện tại không chính xác.");
        }

        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String email = Rules.email(request.email());
        String phone = request.phone() == null || request.phone().isBlank()
                ? user.getPhone() : Rules.phone(request.phone());
        if (users.existsByUsernameAndIdNot(username, userId)) {
            throw ApiException.conflict("USERNAME_TAKEN", "Tên đăng nhập đã được sử dụng");
        }
        if (users.existsByEmailAndIdNot(email, userId)) {
            throw ApiException.conflict("EMAIL_TAKEN", "Email đã được sử dụng");
        }
        if (users.existsByPhoneAndIdNot(phone, userId)) {
            throw ApiException.conflict("PHONE_TAKEN", "Số điện thoại đã được sử dụng");
        }

        user.setFullName(request.fullName().trim());
        user.setUsername(username);
        if (!user.getEmail().equals(email)) user.setEmailVerified(false);
        user.setEmail(email);
        user.setPhone(phone);
        return UserResponse.from(user);
    }
}
