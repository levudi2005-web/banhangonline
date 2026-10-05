package com.banhangonline.order.controller;

import com.banhangonline.auth.security.AuthPrincipal;
import com.banhangonline.common.response.ApiResponse;
import com.banhangonline.order.dto.*;
import com.banhangonline.order.service.ClickCollectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ClickCollectController {
    private final ClickCollectService commerce;

    public ClickCollectController(ClickCollectService commerce) {
        this.commerce = commerce;
    }

    @GetMapping("/api/customer/cart/{storeId}")
    public ApiResponse<CartView> cart(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long storeId) {
        requireRole(principal, "CUSTOMER");
        return ApiResponse.ok("Giỏ hàng", commerce.cart(principal.userId(), storeId));
    }

    @PostMapping("/api/customer/cart/items")
    public ApiResponse<CartView> addItem(@AuthenticationPrincipal AuthPrincipal principal,
                                         @Valid @RequestBody CartItemRequest request) {
        requireRole(principal, "CUSTOMER");
        return ApiResponse.ok("Đã thêm vào giỏ hàng", commerce.addItem(principal.userId(), request));
    }

    @PatchMapping("/api/customer/cart/items/{cartItemId}")
    public ApiResponse<CartView> setQuantity(@AuthenticationPrincipal AuthPrincipal principal,
                                              @PathVariable Long cartItemId,
                                              @Valid @RequestBody CartQuantityRequest request) {
        requireRole(principal, "CUSTOMER");
        return ApiResponse.ok("Đã cập nhật giỏ hàng",
                commerce.setQuantity(principal.userId(), cartItemId, request.quantity()));
    }

    @DeleteMapping("/api/customer/cart/items/{cartItemId}")
    public ApiResponse<Void> removeItem(@AuthenticationPrincipal AuthPrincipal principal,
                                        @PathVariable Long cartItemId) {
        requireRole(principal, "CUSTOMER");
        commerce.removeItem(principal.userId(), cartItemId);
        return ApiResponse.ok("Đã xóa sản phẩm khỏi giỏ hàng", null);
    }

    @PostMapping("/api/customer/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CheckoutResponse> checkout(@AuthenticationPrincipal AuthPrincipal principal,
                                                   @Valid @RequestBody CheckoutRequest request) {
        requireRole(principal, "CUSTOMER");
        return ApiResponse.ok("Đặt hàng thành công",
                commerce.checkout(principal.userId(), principal.roles(), request));
    }

    @GetMapping("/api/customer/orders")
    public ApiResponse<List<OrderView>> customerOrders(@AuthenticationPrincipal AuthPrincipal principal) {
        requireRole(principal, "CUSTOMER");
        return ApiResponse.ok("Đơn hàng của bạn", commerce.customerOrders(principal.userId()));
    }

    @GetMapping("/api/customer/orders/{orderId}")
    public ApiResponse<OrderView> customerOrder(@AuthenticationPrincipal AuthPrincipal principal,
                                                 @PathVariable Long orderId) {
        requireRole(principal, "CUSTOMER");
        return ApiResponse.ok("Chi tiết đơn hàng", commerce.customerOrder(principal.userId(), orderId));
    }

    @PostMapping("/api/customer/orders/{orderId}/cancel")
    public ApiResponse<OrderView> cancel(@AuthenticationPrincipal AuthPrincipal principal,
                                          @PathVariable Long orderId) {
        requireRole(principal, "CUSTOMER");
        return ApiResponse.ok("Đã hủy đơn hàng", commerce.cancelByCustomer(principal.userId(), orderId));
    }

    @GetMapping("/api/stores/{storeId}/orders")
    public ApiResponse<List<OrderView>> storeOrders(@AuthenticationPrincipal AuthPrincipal principal,
                                                     @PathVariable Long storeId) {
        requireManagementRole(principal);
        return ApiResponse.ok("Đơn hàng cửa hàng",
                commerce.storeOrders(storeId, principal.userId(), principal.roles()));
    }

    @PatchMapping("/api/stores/{storeId}/orders/{orderId}/status")
    public ApiResponse<OrderView> updateStatus(@AuthenticationPrincipal AuthPrincipal principal,
                                                 @PathVariable Long storeId, @PathVariable Long orderId,
                                                 @Valid @RequestBody OrderStatusRequest request) {
        requireManagementRole(principal);
        return ApiResponse.ok("Đã cập nhật trạng thái đơn hàng",
                commerce.updateStoreOrder(storeId, orderId, principal.userId(), principal.roles(), request));
    }

    @PostMapping("/api/stores/{storeId}/orders/{orderId}/pickup-confirmation")
    public ApiResponse<OrderView> confirmPickup(@AuthenticationPrincipal AuthPrincipal principal,
                                                 @PathVariable Long storeId, @PathVariable Long orderId,
                                                 @Valid @RequestBody PickupConfirmationRequest request) {
        requireManagementRole(principal);
        return ApiResponse.ok("Đã xác nhận nhận hàng và thanh toán tại cửa hàng",
                commerce.confirmPickup(storeId, orderId, principal.userId(), principal.roles(), request));
    }

    private void requireRole(AuthPrincipal principal, String role) {
        if (principal == null || !principal.roles().contains(role)) {
            throw new AccessDeniedException(role + "_REQUIRED");
        }
    }

    private void requireManagementRole(AuthPrincipal principal) {
        if (principal == null || (!principal.roles().contains("OWNER") && !principal.roles().contains("STAFF"))) {
            throw new AccessDeniedException("STORE_STAFF_REQUIRED");
        }
    }
}
