package com.banhangonline.product.dto;

import com.banhangonline.inventory.entity.Inventory;
import com.banhangonline.product.entity.Product;

import java.math.BigDecimal;

public record ProductView(Long id, Long categoryId, String categoryName, String sku, String name,
                          String slug, String description, BigDecimal price, String currency,
                          String imageUrl, Integer quantity, Integer reservedQuantity, Integer reorderLevel,
                          String status) {
    public static ProductView from(Inventory inventory) {
        return from(inventory, true);
    }

    public static ProductView from(Inventory inventory, boolean includeInventory) {
        Product product = inventory.getProduct();
        return new ProductView(product.getId(), product.getCategory().getId(), product.getCategory().getName(),
                product.getSku(), product.getName(), product.getSlug(), product.getDescription(), product.getPrice(),
                product.getCurrency(), product.getImageUrl(),
                includeInventory ? inventory.getQuantity() : null,
                includeInventory ? inventory.getReservedQuantity() : null,
                includeInventory ? inventory.getReorderLevel() : null,
                includeInventory ? inventory.getStatus() : product.getStatus());
    }

    public static ProductView forCustomer(Inventory inventory) {
        Product product = inventory.getProduct();
        return new ProductView(product.getId(), product.getCategory().getId(), product.getCategory().getName(),
                product.getSku(), product.getName(), product.getSlug(), product.getDescription(), product.getPrice(),
                product.getCurrency(), product.getImageUrl(),
                Math.max(0, inventory.getQuantity() - inventory.getReservedQuantity()), null, null,
                product.getStatus());
    }
}
