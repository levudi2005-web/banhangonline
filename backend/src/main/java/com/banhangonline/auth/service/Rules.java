package com.banhangonline.auth.service;

import com.banhangonline.common.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/** Quy tắc chuẩn hoá và kiểm tra dữ liệu dùng chung. */
public final class Rules {
    /** Số điện thoại sau chuẩn hoá: 0 + 9 chữ số. */
    public static final Pattern PHONE = Pattern.compile("^0\\d{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@(?:[A-Za-z0-9-]+\\.)+[A-Za-z]{2,63}$");
    private Rules() {}

    public static String normalizePhone(String raw) {
        if (raw == null) return "";
        String p = raw.trim().replaceAll("[\\s.\\-()]", "");
        if (p.startsWith("+84")) return "0" + p.substring(3);
        if (p.startsWith("84")) return "0" + p.substring(2);
        return p;
    }

    public static String phone(String raw) {
        String p = normalizePhone(raw == null ? null : raw.trim());
        if (!PHONE.matcher(p).matches()) throw ApiException.validation("Số điện thoại không hợp lệ");
        return p;
    }

    public static String email(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(value).matches()) throw ApiException.validation("Email không hợp lệ");
        return value;
    }

    public static void password(String password, String confirm) {
        if (password == null || confirm == null) throw ApiException.validation("Mật khẩu không hợp lệ");
        if (password.getBytes(StandardCharsets.UTF_8).length < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw ApiException.validation("Mật khẩu phải có từ 8 đến 72 ký tự");
        if (!password.equals(confirm)) throw ApiException.validation("Mật khẩu xác nhận không khớp");
    }
}
