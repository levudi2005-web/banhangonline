package com.banhangonline.product.controller;

import com.banhangonline.auth.security.AuthPrincipal;
import com.banhangonline.common.response.ApiResponse;
import com.banhangonline.product.dto.CategoryRequest;
import com.banhangonline.product.dto.CategoryView;
import com.banhangonline.product.dto.InventoryRequest;
import com.banhangonline.product.dto.ProductRequest;
import com.banhangonline.product.dto.ProductView;
import com.banhangonline.product.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/owner")
public class StoreCatalogController {
    private final CatalogService catalog;

    public StoreCatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/stores/{storeId}/products")
    public ApiResponse<List<ProductView>> list(@AuthenticationPrincipal AuthPrincipal principal,
                                               @PathVariable Long storeId) {
        return ApiResponse.ok("Danh mục cửa hàng", catalog.storeProducts(storeId,
                principal.userId(), principal.roles()));
    }

    @PostMapping("/stores/{storeId}/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductView> create(@AuthenticationPrincipal AuthPrincipal principal,
                                           @PathVariable Long storeId,
                                           @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok("Đã thêm sản phẩm",
                catalog.create(storeId, principal.userId(), principal.roles(), request));
    }

    @PutMapping("/stores/{storeId}/products/{productId}")
    public ApiResponse<ProductView> update(@AuthenticationPrincipal AuthPrincipal principal,
                                           @PathVariable Long storeId, @PathVariable Long productId,
                                           @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok("Đã cập nhật sản phẩm",
                catalog.update(storeId, productId, principal.userId(), principal.roles(), request));
    }

    @DeleteMapping("/stores/{storeId}/products/{productId}")
    public ApiResponse<Void> archive(@AuthenticationPrincipal AuthPrincipal principal,
                                     @PathVariable Long storeId, @PathVariable Long productId) {
        catalog.archive(storeId, productId, principal.userId(), principal.roles());
        return ApiResponse.ok("Đã ẩn sản phẩm khỏi cửa hàng", null);
    }

    @PatchMapping("/stores/{storeId}/inventory/{productId}")
    public ApiResponse<ProductView> updateInventory(@AuthenticationPrincipal AuthPrincipal principal,
                                                    @PathVariable Long storeId, @PathVariable Long productId,
                                                    @Valid @RequestBody InventoryRequest request) {
        return ApiResponse.ok("Đã cập nhật tồn kho",
                catalog.updateInventory(storeId, productId, principal.userId(), principal.roles(), request));
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryView> createCategory(@AuthenticationPrincipal AuthPrincipal principal,
                                                    @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok("Đã tạo danh mục",
                catalog.createCategory(principal.userId(), principal.roles(), request));
    }
}
