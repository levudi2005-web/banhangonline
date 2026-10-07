package com.banhangonline.product.service;

import com.banhangonline.common.exception.ApiException;
import com.banhangonline.inventory.entity.Inventory;
import com.banhangonline.inventory.repository.InventoryRepository;
import com.banhangonline.product.dto.ProductImageView;
import com.banhangonline.product.entity.Product;
import com.banhangonline.product.repository.ProductImageRepository;
import com.banhangonline.product.repository.ProductRepository;
import com.banhangonline.product.storage.ProductImageStorageService;
import com.banhangonline.store.service.StorePermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProductImageGalleryService {
    private static final int MAX_IMAGES = 8;

    private final ProductImageRepository images;
    private final InventoryRepository inventory;
    private final ProductRepository products;
    private final StorePermissionService storePermissions;
    private final ProductImageStorageService storage;

    public ProductImageGalleryService(ProductImageRepository images, InventoryRepository inventory,
                                      ProductRepository products, StorePermissionService storePermissions,
                                      ProductImageStorageService storage) {
        this.images = images;
        this.inventory = inventory;
        this.products = products;
        this.storePermissions = storePermissions;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<ProductImageView> forCustomer(Long storeId, Long productId) {
        Inventory stock = inventory.findAvailableByStoreIdAndProductId(storeId, productId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                        "Không tìm thấy sản phẩm đang được bán tại cửa hàng này"));
        return withLegacyPrimary(productId, stock.getProduct().getImageUrl());
    }

    @Transactional(readOnly = true)
    public List<ProductImageView> forStore(Long storeId, Long productId, Long userId, Set<String> roles) {
        storePermissions.requireAnyView(storeId, userId, roles, "VIEW_PRODUCTS", "MANAGE_PRODUCTS");
        if (!inventory.existsByProductIdAndStoreId(productId, storeId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                    "Sản phẩm không có trong cửa hàng này");
        }
        Product product = products.findById(productId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm"));
        return withLegacyPrimary(productId, product.getImageUrl());
    }

    @Transactional
    public List<ProductImageView> replace(Long storeId, Long productId, Long userId, Set<String> roles,
                                          List<String> imageUrls) {
        storePermissions.require(storeId, userId, roles, "MANAGE_PRODUCTS");
        if (imageUrls == null || imageUrls.size() > MAX_IMAGES) {
            throw ApiException.validation("Mỗi sản phẩm có thể có tối đa 8 ảnh.");
        }
        validateUrls(storeId, imageUrls);

        Inventory stock = inventory.findByStoreIdAndProductId(storeId, productId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                        "Sản phẩm không có trong cửa hàng này"));
        if (inventory.existsByProductIdAndStoreIdNot(productId, storeId)) {
            throw new ApiException(HttpStatus.CONFLICT, "SHARED_PRODUCT",
                    "Không thể thay đổi ảnh của sản phẩm đang được dùng ở cửa hàng khác");
        }

        Set<String> currentUrls = new HashSet<>(images.findByProductId(productId).stream()
                .map(ProductImageView::imageUrl).toList());
        if (stock.getProduct().getImageUrl() != null) {
            currentUrls.add(stock.getProduct().getImageUrl());
        }
        for (String imageUrl : imageUrls) {
            if (!currentUrls.contains(imageUrl) && !storage.isManagedImageUrl(storeId, imageUrl)) {
                throw ApiException.validation("Ảnh mới phải được tải lên kho ảnh của cửa hàng này.");
            }
        }

        images.replace(productId, imageUrls);
        stock.getProduct().setImageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0));
        return toViews(imageUrls);
    }

    private List<ProductImageView> withLegacyPrimary(Long productId, String primaryImageUrl) {
        List<ProductImageView> result = images.findByProductId(productId);
        if (!result.isEmpty() || primaryImageUrl == null || primaryImageUrl.isBlank()) {
            return result;
        }
        return List.of(new ProductImageView(primaryImageUrl, 0, true));
    }

    private List<ProductImageView> toViews(List<String> imageUrls) {
        return java.util.stream.IntStream.range(0, imageUrls.size())
                .mapToObj(index -> new ProductImageView(imageUrls.get(index), index, index == 0))
                .toList();
    }

    private void validateUrls(Long storeId, List<String> imageUrls) {
        Set<String> unique = new HashSet<>();
        for (String imageUrl : imageUrls) {
            if (imageUrl == null || imageUrl.isBlank() || imageUrl.length() > 500 || !unique.add(imageUrl)) {
                throw ApiException.validation("Danh sách ảnh không hợp lệ hoặc có ảnh bị trùng.");
            }
            if (imageUrl.matches("^/uploads/stores/" + storeId
                    + "/products/[0-9a-fA-F-]{36}\\.(png|jpg|webp)$")) {
                continue;
            }
            if (!imageUrl.startsWith("/") && !imageUrl.matches("^https?://[^/].*$")) {
                throw ApiException.validation("Đường dẫn ảnh phải là URL HTTP hoặc HTTPS hợp lệ.");
            }
        }
    }
}
