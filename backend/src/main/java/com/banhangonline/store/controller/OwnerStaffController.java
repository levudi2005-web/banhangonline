package com.banhangonline.store.controller;

import com.banhangonline.auth.security.AuthPrincipal;
import com.banhangonline.common.response.ApiResponse;
import com.banhangonline.store.dto.CreateStaffRequest;
import com.banhangonline.store.dto.ReplaceStaffPermissionsRequest;
import com.banhangonline.store.dto.StaffResponse;
import com.banhangonline.store.dto.StaffStatusRequest;
import com.banhangonline.store.service.StaffManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/owner/stores/{storeId}/staff")
public class OwnerStaffController {
    private final StaffManagementService staff;

    public OwnerStaffController(StaffManagementService staff) {
        this.staff = staff;
    }

    @GetMapping
    public ApiResponse<List<StaffResponse>> list(@AuthenticationPrincipal AuthPrincipal principal,
                                                 @PathVariable Long storeId) {
        requireOwner(principal);
        return ApiResponse.ok("Danh sách nhân viên", staff.list(storeId, principal.userId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StaffResponse> create(@AuthenticationPrincipal AuthPrincipal principal,
                                              @PathVariable Long storeId,
                                              @Valid @RequestBody CreateStaffRequest request) {
        requireOwner(principal);
        return ApiResponse.ok("Đã tạo tài khoản nhân viên",
                staff.create(storeId, principal.userId(), request));
    }

    @PatchMapping("/{membershipId}/status")
    public ApiResponse<StaffResponse> setStatus(@AuthenticationPrincipal AuthPrincipal principal,
                                                 @PathVariable Long storeId,
                                                 @PathVariable Long membershipId,
                                                 @Valid @RequestBody StaffStatusRequest request) {
        requireOwner(principal);
        return ApiResponse.ok("Đã cập nhật trạng thái nhân viên",
                staff.setStatus(storeId, membershipId, principal.userId(), request));
    }

    @PutMapping("/{membershipId}/permissions")
    public ApiResponse<StaffResponse> replacePermissions(@AuthenticationPrincipal AuthPrincipal principal,
                                                           @PathVariable Long storeId,
                                                           @PathVariable Long membershipId,
                                                           @Valid @RequestBody ReplaceStaffPermissionsRequest request) {
        requireOwner(principal);
        return ApiResponse.ok("Đã cập nhật quyền nhân viên",
                staff.replacePermissions(storeId, membershipId, principal.userId(), request));
    }

    private void requireOwner(AuthPrincipal principal) {
        if (principal == null || !principal.roles().contains("OWNER")) {
            throw new AccessDeniedException("OWNER_REQUIRED");
        }
    }
}
