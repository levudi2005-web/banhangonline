package com.banhangonline.product.dto;

import com.banhangonline.category.entity.Category;

public record CategoryView(Long id, String name, String slug, String description) {
    public static CategoryView from(Category category) {
        return new CategoryView(category.getId(), category.getName(), category.getSlug(), category.getDescription());
    }
}
