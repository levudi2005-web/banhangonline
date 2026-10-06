package com.banhangonline;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banhangonline.config.CleanUrlController;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CleanUrlControllerTest {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new CleanUrlController()).build();

    @Test
    void forwardsCustomerCleanUrlsToTheirExistingPages() throws Exception {
        Map<String, String> routes = Map.ofEntries(
                Map.entry("/", "/pages/auth/index.html"),
                Map.entry("/dang-nhap", "/pages/customer/auth/login.html"),
                Map.entry("/dang-ky", "/pages/customer/auth/register.html"),
                Map.entry("/quen-mat-khau", "/pages/customer/auth/forgot-password.html"),
                Map.entry("/dat-lai-mat-khau", "/pages/customer/auth/reset-password.html"),
                Map.entry("/dang-xuat", "/pages/customer/auth/logout.html"),
                Map.entry("/cua-hang", "/pages/customer/index.html"),
                Map.entry("/san-pham", "/pages/customer/index.html"),
                Map.entry("/san-pham/e2e-laptop-one", "/pages/customer/product.html"),
                Map.entry("/danh-muc/e2e-computers", "/pages/customer/index.html"),
                Map.entry("/tim-kiem", "/pages/customer/index.html"),
                Map.entry("/gio-hang", "/pages/customer/cart.html"),
                Map.entry("/thanh-toan", "/pages/customer/cart.html"),
                Map.entry("/don-hang", "/pages/customer/orders.html"),
                Map.entry("/don-hang/1001", "/pages/customer/orders.html"),
                Map.entry("/thong-bao", "/pages/customer/notifications.html"),
                Map.entry("/tai-khoan", "/pages/auth/session.html"),
                Map.entry("/tai-khoan/dia-chi", "/pages/customer/addresses.html"),
                Map.entry("/403", "/pages/auth/403.html"),
                Map.entry("/404", "/error/404.html"),
                Map.entry("/quan-ly/dang-nhap", "/pages/staff/auth/login.html"),
                Map.entry("/quan-ly/quen-mat-khau", "/pages/staff/auth/forgot-password.html"),
                Map.entry("/quan-ly/dat-lai-mat-khau", "/pages/staff/auth/reset-password.html"),
                Map.entry("/quan-ly/dang-xuat", "/pages/staff/auth/logout.html"),
                Map.entry("/quan-ly/owner", "/pages/owner/dashboard.html"),
                Map.entry("/quan-ly/owner/cua-hang", "/pages/owner/store.html"),
                Map.entry("/quan-ly/owner/nhan-vien", "/pages/owner/staff.html"),
                Map.entry("/quan-ly/owner/phan-quyen", "/pages/owner/permissions.html"),
                Map.entry("/quan-ly/owner/danh-muc", "/pages/owner/categories.html"),
                Map.entry("/quan-ly/owner/san-pham", "/pages/owner/products.html"),
                Map.entry("/quan-ly/owner/ton-kho", "/pages/owner/inventory.html"),
                Map.entry("/quan-ly/owner/don-hang", "/pages/owner/orders.html"),
                Map.entry("/quan-ly/owner/thong-bao", "/pages/owner/notifications.html"),
                Map.entry("/quan-ly/staff", "/pages/staff/dashboard.html"),
                Map.entry("/quan-ly/staff/san-pham", "/pages/staff/products.html"),
                Map.entry("/quan-ly/staff/ton-kho", "/pages/staff/inventory.html"),
                Map.entry("/quan-ly/staff/don-hang", "/pages/staff/orders.html"),
                Map.entry("/quan-ly/staff/thong-bao", "/pages/staff/notifications.html"));

        for (var route : routes.entrySet()) {
            mvc.perform(get(route.getKey()))
                    .andExpect(status().isOk())
                    .andExpect(forwardedUrl(route.getValue()));
        }
    }
}
