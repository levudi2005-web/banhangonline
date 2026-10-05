package com.banhangonline.address.controller;

import com.banhangonline.address.dto.AddressRequest;
import com.banhangonline.address.dto.AddressResponse;
import com.banhangonline.address.service.AddressManagementService;
import com.banhangonline.auth.security.AuthPrincipal;
import com.banhangonline.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/addresses")
public class UserAddressController {
    private final AddressManagementService addresses;

    public UserAddressController(AddressManagementService addresses) {
        this.addresses = addresses;
    }

    @GetMapping
    public ApiResponse<List<AddressResponse>> list(@AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.ok("Địa chỉ nhận hàng", addresses.list(principal.userId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AddressResponse> create(@AuthenticationPrincipal AuthPrincipal principal,
                                                @Valid @RequestBody AddressRequest request) {
        return ApiResponse.ok("Đã thêm địa chỉ", addresses.create(principal.userId(), request));
    }

    @PutMapping("/{addressId}")
    public ApiResponse<AddressResponse> update(@AuthenticationPrincipal AuthPrincipal principal,
                                                @PathVariable Long addressId,
                                                @Valid @RequestBody AddressRequest request) {
        return ApiResponse.ok("Đã cập nhật địa chỉ",
                addresses.update(principal.userId(), addressId, request));
    }

    @PatchMapping("/{addressId}/default")
    public ApiResponse<AddressResponse> setDefault(@AuthenticationPrincipal AuthPrincipal principal,
                                                    @PathVariable Long addressId) {
        return ApiResponse.ok("Đã đặt địa chỉ mặc định",
                addresses.setDefault(principal.userId(), addressId));
    }

    @DeleteMapping("/{addressId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthPrincipal principal,
                                    @PathVariable Long addressId) {
        addresses.delete(principal.userId(), addressId);
        return ApiResponse.ok("Đã xóa địa chỉ", null);
    }
}
