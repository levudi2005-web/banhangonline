package com.banhangonline;

import com.banhangonline.user.entity.User;
import com.banhangonline.user.entity.UserStatus;
import com.banhangonline.role.repository.RoleRepository;
import com.banhangonline.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
@EnabledIfSystemProperty(named = "runLiveDbTests", matches = "true")
class AuthIntegrationTest {
    private static final String PW = "Passw0rd!x";
    private static final AtomicInteger SEQ = new AtomicInteger(10_000_000);
    private static final AtomicInteger CLIENT_IP_SEQ = new AtomicInteger(1);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PasswordEncoder encoder;
    private String testRemoteAddress;

    @BeforeEach
    void isolateRateLimitClient() {
        testRemoteAddress = "198.51.100." + CLIENT_IP_SEQ.getAndIncrement();
    }

    // ---------- helpers ----------
    private MvcResult send(String method, String url, Object body, Cookie cookie, boolean withHeader) throws Exception {
        MockHttpServletRequestBuilder b = method.equals("GET") ? MockMvcRequestBuilders.get(url) : MockMvcRequestBuilders.post(url);
        b.with(request -> {
            request.setRemoteAddr(testRemoteAddress);
            return request;
        });
        if (withHeader) b.header("X-Requested-With", "test");
        if (body != null) b.contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body));
        if (cookie != null) b.cookie(cookie);
        return mvc.perform(b).andReturn();
    }
    private MvcResult post(String url, Object body) throws Exception { return send("POST", url, body, null, true); }
    private int status(MvcResult r) { return r.getResponse().getStatus(); }
    private JsonNode json(MvcResult r) throws Exception { return om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8)); }
    private String code(MvcResult r) throws Exception { return json(r).path("code").asText(); }
    private String phone() { return "09" + SEQ.incrementAndGet(); }

    private Map<String, Object> customer() {
        int n = SEQ.incrementAndGet();
        Map<String, Object> m = new HashMap<>();
        m.put("fullName", "Nguyen Van A");
        m.put("username", "user" + n);
        m.put("email", "u" + n + "@example.com");
        m.put("phone", phone());
        m.put("password", PW);
        m.put("confirmPassword", PW);
        m.put("address", Map.of(
                "recipientName", "Nguyen Van A",
                "phone", phone(),
                "province", "Ha Noi",
                "district", "Ba Dinh",
                "ward", "Phuc Xa",
                "addressLine", "1 Test Street"));
        return m;
    }
    private Map<String, Object> registered() throws Exception {
        Map<String, Object> c = customer();
        assertThat(status(post("/api/auth/register", c))).isEqualTo(201);
        return c;
    }
    private MvcResult login(String url, String account, String password) throws Exception {
        return post(url, Map.of("account", account, "password", password));
    }

    // ---------- tests ----------
    @Test void registerCustomer_hashesPasswordAndHidesHash() throws Exception {
        Map<String, Object> c = customer();
        MvcResult r = post("/api/auth/register", c);
        assertThat(status(r)).isEqualTo(201);
        String body = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).doesNotContain("passwordHash").doesNotContain(PW);
        User u = users.findByEmail((String) c.get("email")).orElseThrow();
        assertThat(u.getPasswordHash()).startsWith("$2");
        assertThat(encoder.matches(PW, u.getPasswordHash())).isTrue();
        assertThat(u.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(u.isEmailVerified()).isFalse();
        assertThat(u.hasRole("CUSTOMER")).isTrue();
    }

    @Test void duplicateEmail_username_phone_areRejected() throws Exception {
        Map<String, Object> c = registered();
        Map<String, Object> a = customer(); a.put("email", c.get("email"));
        assertThat(code(post("/api/auth/register", a))).isEqualTo("EMAIL_TAKEN");
        Map<String, Object> b = customer(); b.put("username", c.get("username"));
        assertThat(code(post("/api/auth/register", b))).isEqualTo("USERNAME_TAKEN");
        Map<String, Object> d = customer(); d.put("phone", c.get("phone"));
        MvcResult r = post("/api/auth/register", d);
        assertThat(status(r)).isEqualTo(409);
        assertThat(code(r)).isEqualTo("PHONE_TAKEN");
    }

    @Test void login_byEmail_phone_username_setsHttpOnlyCookie() throws Exception {
        Map<String, Object> c = registered();
        for (String account : new String[]{(String) c.get("email"), (String) c.get("phone"), (String) c.get("username")}) {
            MvcResult r = login("/api/auth/login", account, PW);
            assertThat(status(r)).isEqualTo(200);
            assertThat(r.getResponse().getHeader("Set-Cookie")).contains("SID=").contains("HttpOnly");
        }
    }

    @Test void login_wrongPassword_isRejected() throws Exception {
        Map<String, Object> c = registered();
        MvcResult r = login("/api/auth/login", (String) c.get("email"), "wrong-password");
        assertThat(status(r)).isEqualTo(401);
        assertThat(code(r)).isEqualTo("INVALID_CREDENTIALS");
        assertThat(code(login("/api/auth/login", "khong-ton-tai", PW))).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test void me_and_logout() throws Exception {
        Map<String, Object> c = registered();
        Cookie sid = login("/api/auth/login", (String) c.get("email"), PW).getResponse().getCookie("SID");
        assertThat(sid).isNotNull();
        MvcResult me = send("GET", "/api/auth/me", null, sid, true);
        assertThat(status(me)).isEqualTo(200);
        assertThat(json(me).path("data").path("email").asText()).isEqualTo(c.get("email"));
        assertThat(status(send("GET", "/api/auth/me", null, null, true))).isEqualTo(401);

        assertThat(status(send("POST", "/api/auth/logout", null, sid, true))).isEqualTo(200);
        assertThat(status(send("GET", "/api/auth/me", null, sid, true))).isEqualTo(401);
    }

    @Test void staffLogin_allowsStaff_rejectsCustomer() throws Exception {
        int n = SEQ.incrementAndGet();
        User s = new User();
        s.setFullName("Nhan Vien"); s.setUsername("staff" + n); s.setEmail("s" + n + "@example.com");
        s.setPhone(phone()); s.setPasswordHash(encoder.encode(PW)); s.setStatus(UserStatus.ACTIVE);
        s.getRoles().add(roles.findByName("STAFF").orElseThrow());
        users.save(s);

        MvcResult ok = login("/api/auth/staff/login", "staff" + n, PW);
        assertThat(status(ok)).isEqualTo(200);
        assertThat(json(ok).path("data").path("permissions").toString()).contains("CONFIRM_PICKUP");

        Map<String, Object> c = registered();
        MvcResult denied = login("/api/auth/staff/login", (String) c.get("email"), PW);
        assertThat(status(denied)).isEqualTo(403);
        assertThat(code(denied)).isEqualTo("STAFF_ACCESS_DENIED");
    }

    @Test void publicStaffRegistration_isUnavailable() throws Exception {
        MvcResult response = post("/api/auth/staff/register", Map.of());
        assertThat(status(response)).isEqualTo(404);
    }

    @Test void validation_rejectsBadInput() throws Exception {
        Map<String, Object> shortPw = customer(); shortPw.put("password", "123"); shortPw.put("confirmPassword", "123");
        MvcResult r1 = post("/api/auth/register", shortPw);
        assertThat(status(r1)).isEqualTo(400);
        assertThat(code(r1)).isEqualTo("VALIDATION_ERROR");

        Map<String, Object> mismatch = customer(); mismatch.put("confirmPassword", "Different1!");
        assertThat(status(post("/api/auth/register", mismatch))).isEqualTo(400);

        Map<String, Object> badEmail = customer(); badEmail.put("email", "khong-phai-email");
        assertThat(status(post("/api/auth/register", badEmail))).isEqualTo(400);

        Map<String, Object> badPhone = customer(); badPhone.put("phone", "12345");
        assertThat(status(post("/api/auth/register", badPhone))).isEqualTo(400);
    }

    @Test void mutatingRequestWithoutHeader_isBlocked() throws Exception {
        MvcResult r = send("POST", "/api/auth/login", Map.of("account", "a", "password", "b"), null, false);
        assertThat(status(r)).isEqualTo(403);
        assertThat(code(r)).isEqualTo("CSRF_BLOCKED");
    }

    @Test void frontendPagesAndAssets_areServedFromFrontendModule() throws Exception {
        MvcResult customerLogin = send("GET", "/pages/customer/auth/login.html", null, null, true);
        assertThat(status(customerLogin)).isEqualTo(200);
        assertThat(customerLogin.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("/api/auth/login", "../../../assets/css/style.css");

        assertThat(status(send("GET", "/pages/staff/auth/register.html", null, null, true))).isEqualTo(404);

        assertThat(status(send("GET", "/assets/css/style.css", null, null, true))).isEqualTo(200);
        MvcResult appScript = send("GET", "/assets/js/core/app.js", null, null, true);
        assertThat(status(appScript)).isEqualTo(200);
        assertThat(appScript.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .contains("X-Requested-With");
    }
}
