package com.banhangonline.auth.service;

import com.banhangonline.address.dto.AddressRequest;
import com.banhangonline.address.entity.UserAddress;
import com.banhangonline.address.repository.UserAddressRepository;
import com.banhangonline.auth.dto.RegisterRequest;
import com.banhangonline.auth.security.TokenUtil;
import com.banhangonline.common.exception.ApiException;
import com.banhangonline.role.entity.Role;
import com.banhangonline.role.repository.RoleRepository;
import com.banhangonline.user.entity.User;
import com.banhangonline.user.entity.UserStatus;
import com.banhangonline.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository users;
    private final RoleRepository roles;
    private final UserAddressRepository addresses;
    private final PasswordEncoder encoder;
    /** Hash giả để thời gian xử lý như nhau khi tài khoản không tồn tại (chống dò tài khoản). */
    private final String dummyHash;

    public AuthService(UserRepository users, RoleRepository roles, UserAddressRepository addresses,
                       PasswordEncoder encoder) {
        this.users = users;
        this.roles = roles;
        this.addresses = addresses;
        this.encoder = encoder;
        this.dummyHash = encoder.encode(TokenUtil.newToken());
    }

    /** Backend tự nhận dạng: có '@' là email, dạng số điện thoại là phone, còn lại là username. */
    public Optional<User> findByAccount(String account) {
        String a = account.trim();
        if (a.contains("@")) return users.findByEmail(Rules.email(a));
        String p = Rules.normalizePhone(a);
        if (Rules.PHONE.matcher(p).matches()) return users.findByPhone(p);
        return users.findByUsername(a.toLowerCase(Locale.ROOT));
    }

    public User findUser(Long id) {
        return users.findById(id).orElseThrow(() ->
                new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Phiên đăng nhập không hợp lệ"));
    }

    public User authenticate(String account, String password) {
        Optional<User> found = findByAccount(account);
        boolean ok = encoder.matches(password, found.map(User::getPasswordHash).orElse(dummyHash));
        if (found.isEmpty() || !ok) {
            log.warn("Authentication failed: {}", found.isEmpty() ? "user_not_found" : "invalid_password");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Tài khoản hoặc mật khẩu không đúng");
        }
        User u = found.get();
        if (u.getStatus() == UserStatus.PENDING_VERIFICATION) {
            log.warn("Authentication blocked for pending account userId={}", u.getId());
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_PENDING", "Tài khoản đang chờ xác minh");
        }
        if (u.getStatus() == UserStatus.DISABLED) {
            log.warn("Authentication blocked for disabled account userId={}", u.getId());
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "Tài khoản đã bị khoá");
        }
        return u;
    }

    public User authenticateStaff(String account, String password) {
        User u = authenticate(account, password);
        if (!u.hasRole("STAFF") && !u.hasRole("OWNER"))
            throw new ApiException(HttpStatus.FORBIDDEN, "STAFF_ACCESS_DENIED", "Tài khoản này không có quyền truy cập khu vực quản trị");
        return u;
    }

    @Transactional
    public User registerCustomer(RegisterRequest r) {
        Rules.password(r.password(), r.confirmPassword());
        String email = Rules.email(r.email());
        String phone = Rules.phone(r.phone());
        String username = r.username().trim().toLowerCase(Locale.ROOT);
        assertAvailable(username, email, phone);

        User u = newUser(r.fullName(), username, email, phone, r.password(), UserStatus.ACTIVE);
        u.setEmailVerified(false);
        u.getRoles().add(role("CUSTOMER"));
        users.save(u);

        AddressRequest ad = r.address();
        UserAddress a = new UserAddress();
        a.setUser(u);
        a.setRecipientName(ad.recipientName().trim());
        a.setPhone(Rules.phone(ad.phone()));
        a.setProvince(ad.province().trim());
        a.setDistrict(ad.district().trim());
        a.setWard(ad.ward().trim());
        a.setAddressLine(ad.addressLine().trim());
        a.setDefaultAddress(true);
        addresses.save(a);
        return u;
    }

    private void assertAvailable(String username, String email, String phone) {
        if (users.existsByEmail(email)) throw ApiException.conflict("EMAIL_TAKEN", "Email đã được sử dụng");
        if (users.existsByUsername(username)) throw ApiException.conflict("USERNAME_TAKEN", "Tên đăng nhập đã được sử dụng");
        if (users.existsByPhone(phone)) throw ApiException.conflict("PHONE_TAKEN", "Số điện thoại đã được sử dụng");
    }

    private User newUser(String fullName, String username, String email, String phone, String rawPassword, UserStatus status) {
        User u = new User();
        u.setFullName(fullName.trim());
        u.setUsername(username);
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setStatus(status);
        return u;
    }

    private Role role(String name) {
        return roles.findByName(name).orElseThrow(() -> new IllegalStateException("Role chưa được tạo: " + name));
    }
}
