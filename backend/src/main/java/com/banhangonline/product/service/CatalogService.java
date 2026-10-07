package com.banhangonline.product.service;

import com.banhangonline.category.entity.Category;
import com.banhangonline.category.repository.CategoryRepository;
import com.banhangonline.common.exception.ApiException;
import com.banhangonline.inventory.entity.Inventory;
import com.banhangonline.inventory.repository.InventoryRepository;
import com.banhangonline.product.dto.CategoryRequest;
import com.banhangonline.product.dto.CategoryView;
import com.banhangonline.product.dto.InventoryRequest;
import com.banhangonline.product.dto.ProductRequest;
import com.banhangonline.product.dto.ProductView;
import com.banhangonline.product.entity.Product;
import com.banhangonline.product.repository.ProductRepository;
import com.banhangonline.store.entity.Store;
import com.banhangonline.store.repository.StoreRepository;
import com.banhangonline.store.service.StorePermissionService;
import com.banhangonline.store.service.StoreManagementService;
import com.banhangonline.user.entity.User;
import com.banhangonline.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CatalogService {
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final InventoryRepository inventory;
    private final StoreRepository stores;
    private final UserRepository users;
    private final StorePermissionService storePermissions;
    private final StoreManagementService storeManagement;

    public CatalogService(CategoryRepository categories, ProductRepository products,
                          InventoryRepository inventory, StoreRepository stores, UserRepository users,
                          StorePermissionService storePermissions, StoreManagementService storeManagement) {
        this.categories = categories;
        this.products = products;
        this.inventory = inventory;
        this.stores = stores;
        this.users = users;
        this.storePermissions = storePermissions;
        this.storeManagement = storeManagement;
    }

    @Transactional(readOnly = true)
    public List<CategoryView> categories() {
        return categories.findByStatusOrderByName("ACTIVE").stream().map(CategoryView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductView> customerProducts(Long storeId) {
        requireActiveStore(storeId);
        return inventory.findAvailableByStoreId(storeId).stream().map(ProductView::forCustomer).toList();
    }

    @Transactional(readOnly = true)
    public ProductView customerProduct(Long storeId, Long productId) {
        requireActiveStore(storeId);
        Inventory stock = inventory.findAvailableByStoreIdAndProductId(storeId, productId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                        "Không tìm thấy sản phẩm đang được bán tại cửa hàng này"));
        return ProductView.forCustomer(stock);
    }

    @Transactional(readOnly = true)
    public List<ProductView> storeProducts(Long storeId, Long userId, Set<String> roles) {
        storePermissions.requireAnyView(storeId, userId, roles,
                "VIEW_PRODUCTS", "MANAGE_PRODUCTS", "VIEW_INVENTORY", "MANAGE_INVENTORY");
        boolean canViewInventory = storePermissions.hasAnyPermission(storeId, userId, roles,
                "VIEW_INVENTORY", "MANAGE_INVENTORY");
        return inventory.findAllByStoreId(storeId).stream()
                .map(item -> ProductView.from(item, canViewInventory)).toList();
    }

    @Transactional
    public ProductView create(Long storeId, Long userId, Set<String> roles, ProductRequest request) {
        storePermissions.require(storeId, userId, roles, "MANAGE_PRODUCTS");
        if (request.quantity() != null || request.reorderLevel() != null) {
            storePermissions.require(storeId, userId, roles, "MANAGE_INVENTORY");
        }
        Store store = stores.getReferenceById(storeId);
        Category category = categories.findByIdAndStatus(request.categoryId(), "ACTIVE").orElseThrow(() ->
                new ApiException(HttpStatus.BAD_REQUEST, "CATEGORY_NOT_FOUND", "Danh mục không tồn tại hoặc đã ẩn"));
        String sku = request.sku().trim();
        String slug = request.slug().trim().toLowerCase(Locale.ROOT);
        if (products.existsBySku(sku) || products.existsBySlug(slug)) {
            throw ApiException.conflict("PRODUCT_IDENTIFIER_TAKEN", "SKU hoặc đường dẫn sản phẩm đã tồn tại");
        }

        Product product = new Product();
        updateProduct(product, request, category, sku, slug);
        products.save(product);
        Inventory stock = new Inventory();
        stock.setStore(store);
        stock.setProduct(product);
        stock.setQuantity(request.quantity() == null ? 0 : request.quantity());
        stock.setReservedQuantity(0);
        stock.setReorderLevel(request.reorderLevel() == null ? 0 : request.reorderLevel());
        stock.setStatus("ACTIVE");
        inventory.save(stock);
        return ProductView.from(stock);
    }

    @Transactional
    public ProductView update(Long storeId, Long productId, Long userId, Set<String> roles, ProductRequest request) {
        storePermissions.require(storeId, userId, roles, "MANAGE_PRODUCTS");
        if (request.quantity() != null || request.reorderLevel() != null) {
            storePermissions.require(storeId, userId, roles, "MANAGE_INVENTORY");
        }
        Inventory stock = inventory.findByStoreIdAndProductId(storeId, productId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Sản phẩm không có trong cửa hàng này"));
        if (inventory.existsByProductIdAndStoreIdNot(productId, storeId)) {
            throw new ApiException(HttpStatus.CONFLICT, "SHARED_PRODUCT",
                    "Sản phẩm đang được dùng ở cửa hàng khác; chỉ có thể cập nhật tồn kho tại cửa hàng này");
        }
        Product product = stock.getProduct();
        if (!product.getSku().equals(request.sku().trim())
                || !product.getSlug().equalsIgnoreCase(request.slug().trim())) {
            throw ApiException.validation("Không thể đổi SKU hoặc đường dẫn sau khi tạo sản phẩm");
        }
        if (request.quantity() != null && request.quantity() < stock.getReservedQuantity()) {
            throw ApiException.validation("Tồn kho không được thấp hơn số lượng đã giữ cho đơn hàng");
        }
        Category category = categories.findByIdAndStatus(request.categoryId(), "ACTIVE").orElseThrow(() ->
                new ApiException(HttpStatus.BAD_REQUEST, "CATEGORY_NOT_FOUND", "Danh mục không tồn tại hoặc đã ẩn"));
        updateProduct(product, request, category, product.getSku(), product.getSlug());
        if (request.quantity() != null) {
            stock.setQuantity(request.quantity());
        }
        if (request.reorderLevel() != null) {
            stock.setReorderLevel(request.reorderLevel());
        }
        return ProductView.from(stock);
    }

    @Transactional
    public void archive(Long storeId, Long productId, Long userId, Set<String> roles) {
        storePermissions.require(storeId, userId, roles, "MANAGE_PRODUCTS");
        Inventory stock = inventory.findByStoreIdAndProductId(storeId, productId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Sản phẩm không có trong cửa hàng này"));
        stock.setStatus("INACTIVE");
        if (!inventory.existsByProductIdAndStoreIdNot(productId, storeId)) {
            stock.getProduct().setStatus("INACTIVE");
        }
    }

    @Transactional
    public ProductView updateInventory(Long storeId, Long productId, Long userId, Set<String> roles,
                                       InventoryRequest request) {
        storePermissions.require(storeId, userId, roles, "MANAGE_INVENTORY");
        Inventory stock = inventory.findByStoreIdAndProductId(storeId, productId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Sản phẩm không có trong cửa hàng này"));
        if (request.quantity() < stock.getReservedQuantity()) {
            throw ApiException.validation("Tồn kho không được thấp hơn số lượng đã giữ cho đơn hàng");
        }
        stock.setQuantity(request.quantity());
        stock.setReorderLevel(request.reorderLevel());
        stock.setStatus(request.status());
        return ProductView.from(stock);
    }

    @Transactional
    public CategoryView createCategory(Long userId, Set<String> roles, CategoryRequest request) {
        if (!roles.contains("OWNER")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "OWNER_REQUIRED", "Chỉ chủ cửa hàng mới được tạo danh mục");
        }
        User actor = users.findById(userId).orElseThrow(() ->
                new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Phiên đăng nhập không hợp lệ"));
        if (!actor.hasRole("OWNER")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "OWNER_REQUIRED", "Chỉ chủ cửa hàng mới được tạo danh mục");
        }
        String slug = request.slug().trim().toLowerCase(Locale.ROOT);
        if (categories.existsBySlug(slug)) {
            throw ApiException.conflict("CATEGORY_SLUG_TAKEN", "Đường dẫn danh mục đã được sử dụng");
        }
        Category category = new Category();
        category.setName(request.name().trim());
        category.setSlug(slug);
        category.setDescription(request.description() == null || request.description().isBlank()
                ? null : request.description().trim());
        category.setStatus("ACTIVE");
        return CategoryView.from(categories.save(category));
    }

    private void updateProduct(Product product, ProductRequest request, Category category, String sku, String slug) {
        product.setCategory(category);
        product.setSku(sku);
        product.setName(request.name().trim());
        product.setSlug(slug);
        product.setDescription(request.description() == null || request.description().isBlank()
                ? null : request.description().trim());
        product.setPrice(request.price());
        product.setCurrency(request.currency() == null || request.currency().isBlank()
                ? "VND" : request.currency().trim().toUpperCase(Locale.ROOT));
        product.setStatus("ACTIVE");
    }

    private void requireActiveStore(Long storeId) {
        var store = stores.findById(storeId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "STORE_NOT_FOUND", "Không tìm thấy cửa hàng"));
        if (!"ACTIVE".equals(store.getStatus())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "STORE_NOT_FOUND", "Không tìm thấy cửa hàng đang hoạt động");
        }
    }
}
