package com.banhangonline.common.validation;

public interface Msg {
    String REQ = "Vui lòng nhập đủ thông tin bắt buộc";
    String EMAIL = "Email không hợp lệ";
    String USERNAME = "Tên đăng nhập gồm 4–30 ký tự (chữ, số, _ hoặc .) và có ít nhất một chữ cái";
    String PASSWORD = "Mật khẩu phải có từ 8 đến 72 ký tự";
    String LONG = "Nội dung quá dài";
    String USERNAME_RE = "^(?=.*[A-Za-z])[A-Za-z0-9_.]{4,30}$";
}
