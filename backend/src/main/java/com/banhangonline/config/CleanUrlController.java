package com.banhangonline.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CleanUrlController {

    @GetMapping("/")
    public String landing() {
        return "forward:/pages/auth/index.html";
    }

    @GetMapping("/dang-nhap")
    public String customerLogin() {
        return "forward:/pages/customer/auth/login.html";
    }

    @GetMapping("/dang-ky")
    public String customerRegistration() {
        return "forward:/pages/customer/auth/register.html";
    }

    @GetMapping("/quen-mat-khau")
    public String customerPasswordRecovery() {
        return "forward:/pages/customer/auth/forgot-password.html";
    }

    @GetMapping("/dat-lai-mat-khau")
    public String customerPasswordReset() {
        return "forward:/pages/customer/auth/reset-password.html";
    }

    @GetMapping("/dang-xuat")
    public String customerLogout() {
        return "forward:/pages/customer/auth/logout.html";
    }

    @GetMapping("/quan-ly/dang-xuat")
    public String managementLogout() {
        return "forward:/pages/staff/auth/logout.html";
    }

    @GetMapping({"/cua-hang", "/san-pham", "/tim-kiem", "/danh-muc/{slug}"})
    public String storefront() {
        return "forward:/pages/customer/index.html";
    }

    @GetMapping("/san-pham/{slug}")
    public String product() {
        return "forward:/pages/customer/product.html";
    }

    @GetMapping({"/gio-hang", "/thanh-toan"})
    public String cart() {
        return "forward:/pages/customer/cart.html";
    }

    @GetMapping({"/don-hang", "/don-hang/{orderId}"})
    public String customerOrders() {
        return "forward:/pages/customer/orders.html";
    }

    @GetMapping("/thong-bao")
    public String customerNotifications() {
        return "forward:/pages/customer/notifications.html";
    }

    @GetMapping("/tai-khoan")
    public String customerAccount() {
        return "forward:/pages/auth/session.html";
    }

    @GetMapping("/tai-khoan/dia-chi")
    public String customerAddresses() {
        return "forward:/pages/customer/addresses.html";
    }

    @GetMapping("/403")
    public String forbidden() {
        return "forward:/pages/auth/403.html";
    }

    @GetMapping("/404")
    public String notFound() {
        return "forward:/error/404.html";
    }

    @GetMapping({"/quan-ly/dang-nhap"})
    public String managementLogin() {
        return "forward:/pages/staff/auth/login.html";
    }

    @GetMapping("/quan-ly/quen-mat-khau")
    public String managementPasswordRecovery() {
        return "forward:/pages/staff/auth/forgot-password.html";
    }

    @GetMapping("/quan-ly/dat-lai-mat-khau")
    public String managementPasswordReset() {
        return "forward:/pages/staff/auth/reset-password.html";
    }

    @GetMapping({"/quan-ly/owner", "/quan-ly/owner/tong-quan"})
    public String ownerDashboard() {
        return "forward:/pages/owner/dashboard.html";
    }

    @GetMapping("/quan-ly/owner/cua-hang")
    public String ownerStore() {
        return "forward:/pages/owner/store.html";
    }

    @GetMapping("/quan-ly/owner/nhan-vien")
    public String ownerStaff() {
        return "forward:/pages/owner/staff.html";
    }

    @GetMapping("/quan-ly/owner/phan-quyen")
    public String ownerPermissions() {
        return "forward:/pages/owner/permissions.html";
    }

    @GetMapping("/quan-ly/owner/danh-muc")
    public String ownerCategories() {
        return "forward:/pages/owner/categories.html";
    }

    @GetMapping("/quan-ly/owner/san-pham")
    public String ownerProducts() {
        return "forward:/pages/owner/products.html";
    }

    @GetMapping("/quan-ly/owner/ton-kho")
    public String ownerInventory() {
        return "forward:/pages/owner/inventory.html";
    }

    @GetMapping("/quan-ly/owner/don-hang")
    public String ownerOrders() {
        return "forward:/pages/owner/orders.html";
    }

    @GetMapping("/quan-ly/owner/thong-bao")
    public String ownerNotifications() {
        return "forward:/pages/owner/notifications.html";
    }

    @GetMapping({"/quan-ly/staff", "/quan-ly/staff/tong-quan"})
    public String staffDashboard() {
        return "forward:/pages/staff/dashboard.html";
    }

    @GetMapping("/quan-ly/staff/san-pham")
    public String staffProducts() {
        return "forward:/pages/staff/products.html";
    }

    @GetMapping("/quan-ly/staff/ton-kho")
    public String staffInventory() {
        return "forward:/pages/staff/inventory.html";
    }

    @GetMapping("/quan-ly/staff/don-hang")
    public String staffOrders() {
        return "forward:/pages/staff/orders.html";
    }

    @GetMapping("/quan-ly/staff/thong-bao")
    public String staffNotifications() {
        return "forward:/pages/staff/notifications.html";
    }
}
