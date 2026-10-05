package com.banhangonline.store.controller;

import com.banhangonline.auth.security.AuthPrincipal;
import com.banhangonline.common.response.ApiResponse;
import com.banhangonline.store.dto.CreateStoreRequest;
import com.banhangonline.store.dto.StoreResponse;
import com.banhangonline.store.service.StoreManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/owner/stores")
public class OwnerStoreController {
    private final StoreManagementService stores;

    public OwnerStoreController(StoreManagementService stores) {
        this.stores = stores;
    }

    @GetMapping
    public ApiResponse<List<StoreResponse>> list(@AuthenticationPrincipal AuthPrincipal principal) {
        requireOwner(principal);
        return ApiResponse.ok("Danh sách cửa hàng", stores.listOwned(principal.userId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StoreResponse> create(@AuthenticationPrincipal AuthPrincipal principal,
                                             @Valid @RequestBody CreateStoreRequest request) {
        requireOwner(principal);
        return ApiResponse.ok("Đã lưu thông tin cửa hàng dưới dạng bản nháp",
                stores.create(principal.userId(), request));
    }

    @PutMapping("/{storeId}")
    public ApiResponse<StoreResponse> updateDraft(@AuthenticationPrincipal AuthPrincipal principal,
                                                  @PathVariable Long storeId,
                                                  @Valid @RequestBody CreateStoreRequest request) {
        requireOwner(principal);
        return ApiResponse.ok("Đã cập nhật bản nháp",
                stores.updateDraft(storeId, principal.userId(), request));
    }

    @PostMapping("/{storeId}/submit-review")
    public ApiResponse<StoreResponse> submitForReview(@AuthenticationPrincipal AuthPrincipal principal,
                                                       @PathVariable Long storeId) {
        requireOwner(principal);
        return ApiResponse.ok("Đã gửi cửa hàng để quản trị viên kiểm duyệt",
                stores.submitForReview(storeId, principal.userId()));
    }

    private void requireOwner(AuthPrincipal principal) {
        if (principal == null || !principal.roles().contains("OWNER")) {
            throw new org.springframework.security.access.AccessDeniedException("OWNER_REQUIRED");
        }
    }
}
