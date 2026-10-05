package com.banhangonline;

import com.banhangonline.category.entity.Category;
import com.banhangonline.category.repository.CategoryRepository;
import com.banhangonline.inventory.entity.Inventory;
import com.banhangonline.inventory.repository.InventoryRepository;
import com.banhangonline.product.dto.ProductRequest;
import com.banhangonline.product.entity.Product;
import com.banhangonline.product.repository.ProductRepository;
import com.banhangonline.product.service.CatalogService;
import com.banhangonline.store.entity.Store;
import com.banhangonline.store.repository.StoreRepository;
import com.banhangonline.store.service.StoreManagementService;
import com.banhangonline.store.service.StorePermissionService;
import com.banhangonline.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CatalogServiceTest {
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final InventoryRepository inventory = mock(InventoryRepository.class);
    private final StoreRepository stores = mock(StoreRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final StorePermissionService storePermissions = mock(StorePermissionService.class);
    private final StoreManagementService storeManagement = mock(StoreManagementService.class);
    private final CatalogService service = new CatalogService(categories, products, inventory, stores,
            users, storePermissions, storeManagement);

    @Test
    void productManagerCanCreateListingWithoutInventoryPermission() {
        Category category = category();
        when(categories.findByIdAndStatus(5L, "ACTIVE")).thenReturn(Optional.of(category));
        when(stores.getReferenceById(7L)).thenReturn(new Store());

        service.create(7L, 12L, Set.of("STAFF"), request(null, null));

        ArgumentCaptor<Inventory> createdInventory = ArgumentCaptor.forClass(Inventory.class);
        verify(inventory).save(createdInventory.capture());
        assertThat(createdInventory.getValue().getQuantity()).isZero();
        assertThat(createdInventory.getValue().getReorderLevel()).isZero();
        verify(storePermissions).require(7L, 12L, Set.of("STAFF"), "MANAGE_PRODUCTS");
        verify(storePermissions, never()).require(7L, 12L, Set.of("STAFF"), "MANAGE_INVENTORY");
    }

    @Test
    void productManagerCanChangePriceWithoutChangingInventory() {
        Inventory stock = stock();
        when(inventory.findByStoreIdAndProductId(7L, 22L)).thenReturn(Optional.of(stock));
        when(inventory.existsByProductIdAndStoreIdNot(22L, 7L)).thenReturn(false);
        when(categories.findByIdAndStatus(5L, "ACTIVE")).thenReturn(Optional.of(category()));

        service.update(7L, 22L, 12L, Set.of("STAFF"), request(null, null, "32000"));

        assertThat(stock.getProduct().getPrice()).isEqualByComparingTo("32000");
        assertThat(stock.getProduct().getName()).isEqualTo("Trà xanh");
        assertThat(stock.getQuantity()).isEqualTo(24);
        assertThat(stock.getReorderLevel()).isEqualTo(5);
        verify(storePermissions).require(7L, 12L, Set.of("STAFF"), "MANAGE_PRODUCTS");
        verify(storePermissions, never()).require(7L, 12L, Set.of("STAFF"), "MANAGE_INVENTORY");
    }

    @Test
    void archivingProductHidesStoreStockAndPreservesSharedProduct() {
        Inventory stock = stock();
        when(inventory.findByStoreIdAndProductId(7L, 22L)).thenReturn(Optional.of(stock));
        when(inventory.existsByProductIdAndStoreIdNot(22L, 7L)).thenReturn(true);

        service.archive(7L, 22L, 12L, Set.of("STAFF"));

        assertThat(stock.getStatus()).isEqualTo("INACTIVE");
        assertThat(stock.getProduct().getStatus()).isEqualTo("ACTIVE");
        verify(storePermissions).require(7L, 12L, Set.of("STAFF"), "MANAGE_PRODUCTS");
    }

    @Test
    void archivingOnlyStoreListingAlsoDeactivatesUnsharedProduct() {
        Inventory stock = stock();
        when(inventory.findByStoreIdAndProductId(7L, 22L)).thenReturn(Optional.of(stock));
        when(inventory.existsByProductIdAndStoreIdNot(22L, 7L)).thenReturn(false);

        service.archive(7L, 22L, 12L, Set.of("STAFF"));

        assertThat(stock.getStatus()).isEqualTo("INACTIVE");
        assertThat(stock.getProduct().getStatus()).isEqualTo("INACTIVE");
    }

    private static ProductRequest request(Integer quantity, Integer reorderLevel) {
        return request(quantity, reorderLevel, "25000");
    }

    private static ProductRequest request(Integer quantity, Integer reorderLevel, String price) {
        return new ProductRequest(5L, "TEA-22", "Trà xanh", "tra-xanh", "Trà xanh tự nhiên",
                new BigDecimal(price), "VND", null, quantity, reorderLevel);
    }

    private static Category category() {
        Category category = new Category();
        category.setId(5L);
        category.setName("Đồ uống");
        return category;
    }

    private static Inventory stock() {
        Product product = new Product();
        product.setId(22L);
        product.setCategory(category());
        product.setSku("TEA-22");
        product.setName("Trà");
        product.setSlug("tra-xanh");
        product.setPrice(new BigDecimal("25000"));
        product.setCurrency("VND");
        product.setStatus("ACTIVE");
        Inventory stock = new Inventory();
        stock.setProduct(product);
        stock.setStore(new Store());
        stock.setQuantity(24);
        stock.setReservedQuantity(7);
        stock.setReorderLevel(5);
        stock.setStatus("ACTIVE");
        return stock;
    }
}
