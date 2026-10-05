package com.banhangonline.product.controller;

import com.banhangonline.common.response.ApiResponse;
import com.banhangonline.product.dto.CategoryView;
import com.banhangonline.product.dto.ProductView;
import com.banhangonline.product.service.CatalogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryView>> categories() {
        return ApiResponse.ok("Danh mục sản phẩm", catalog.categories());
    }

    @GetMapping("/stores/{storeId}/products")
    public ApiResponse<List<ProductView>> products(@PathVariable Long storeId) {
        return ApiResponse.ok("Sản phẩm còn hàng", catalog.customerProducts(storeId));
    }
}
