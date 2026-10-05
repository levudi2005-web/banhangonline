package com.banhangonline.store.controller;

import com.banhangonline.auth.security.AuthPrincipal;
import com.banhangonline.common.response.ApiResponse;
import com.banhangonline.store.dto.StaffStoreResponse;
import com.banhangonline.store.service.StaffManagementService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/staff/stores")
public class StaffStoreController {
    private final StaffManagementService staff;

    public StaffStoreController(StaffManagementService staff) {
        this.staff = staff;
    }

    @GetMapping
    public ApiResponse<List<StaffStoreResponse>> list(@AuthenticationPrincipal AuthPrincipal principal) {
        if (principal == null || !principal.roles().contains("STAFF")) {
            throw new AccessDeniedException("STAFF_REQUIRED");
        }
        return ApiResponse.ok("Cửa hàng được phân công", staff.storesForUser(principal.userId()));
    }
}
