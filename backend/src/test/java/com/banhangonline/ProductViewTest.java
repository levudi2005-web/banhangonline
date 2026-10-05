package com.banhangonline;

import com.banhangonline.category.entity.Category;
import com.banhangonline.inventory.entity.Inventory;
import com.banhangonline.product.dto.ProductView;
import com.banhangonline.product.entity.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductViewTest {
    @Test
    void hidesInventoryValuesAndStatusWhenInventoryPermissionIsMissing() {
        Category category = new Category();
        category.setId(3L);
        category.setName("Đồ uống");
        Product product = new Product();
        product.setId(12L);
        product.setCategory(category);
        product.setName("Trà");
        product.setSku("TEA-12");
        product.setSlug("tra");
        product.setPrice(new BigDecimal("25000"));
        product.setStatus("ACTIVE");
        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setQuantity(24);
        inventory.setReservedQuantity(7);
        inventory.setReorderLevel(5);
        inventory.setStatus("SUSPENDED");

        ProductView view = ProductView.from(inventory, false);

        assertThat(view.quantity()).isNull();
        assertThat(view.reservedQuantity()).isNull();
        assertThat(view.reorderLevel()).isNull();
        assertThat(view.status()).isEqualTo("ACTIVE");
    }

    @Test
    void includesInventoryValuesWhenInventoryPermissionIsGranted() {
        Category category = new Category();
        category.setId(3L);
        category.setName("Đồ uống");
        Product product = new Product();
        product.setId(12L);
        product.setCategory(category);
        product.setName("Trà");
        product.setSku("TEA-12");
        product.setSlug("tra");
        product.setPrice(new BigDecimal("25000"));
        product.setStatus("ACTIVE");
        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setQuantity(24);
        inventory.setReservedQuantity(7);
        inventory.setReorderLevel(5);
        inventory.setStatus("SUSPENDED");

        ProductView view = ProductView.from(inventory, true);

        assertThat(view.quantity()).isEqualTo(24);
        assertThat(view.reservedQuantity()).isEqualTo(7);
        assertThat(view.reorderLevel()).isEqualTo(5);
        assertThat(view.status()).isEqualTo("SUSPENDED");
    }
}
