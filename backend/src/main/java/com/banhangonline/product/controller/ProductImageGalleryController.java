package com.banhangonline.product.controller;

import com.banhangonline.auth.security.AuthPrincipal;
import com.banhangonline.common.response.ApiResponse;
import com.banhangonline.product.dto.ProductImageGalleryRequest;
import com.banhangonline.product.dto.ProductImageView;
import com.banhangonline.product.service.ProductImageGalleryService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProductImageGalleryController {
    private final ProductImageGalleryService gallery;

    public ProductImageGalleryController(ProductImageGalleryService gallery) {
        this.gallery = gallery;
    }

    @GetMapping("/api/catalog/stores/{storeId}/products/{productId}/images")
    public ApiResponse<List<ProductImageView>> customerImages(@PathVariable Long storeId,
                                                               @PathVariable Long productId) {
        return ApiResponse.ok("Ảnh sản phẩm", gallery.forCustomer(storeId, productId));
    }

    @GetMapping("/api/owner/stores/{storeId}/products/{productId}/images")
    public ApiResponse<List<ProductImageView>> storeImages(@AuthenticationPrincipal AuthPrincipal principal,
                                                            @PathVariable Long storeId,
                                                            @PathVariable Long productId) {
        return ApiResponse.ok("Ảnh sản phẩm",
                gallery.forStore(storeId, productId, principal.userId(), principal.roles()));
    }

    @PutMapping("/api/owner/stores/{storeId}/products/{productId}/images")
    public ApiResponse<List<ProductImageView>> replaceStoreImages(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long storeId,
            @PathVariable Long productId, @Valid @RequestBody ProductImageGalleryRequest request) {
        return ApiResponse.ok("Đã cập nhật thư viện ảnh",
                gallery.replace(storeId, productId, principal.userId(), principal.roles(), request.imageUrls()));
    }
}
