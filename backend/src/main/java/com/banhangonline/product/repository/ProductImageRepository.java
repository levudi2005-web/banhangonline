package com.banhangonline.product.repository;

import com.banhangonline.product.dto.ProductImageView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProductImageRepository {
    private final JdbcTemplate jdbc;

    public ProductImageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ProductImageView> findByProductId(Long productId) {
        return jdbc.query("""
                SELECT image_url, sort_order
                FROM product_images
                WHERE product_id = ?
                ORDER BY sort_order
                """, (rs, row) -> new ProductImageView(
                rs.getString("image_url"), rs.getInt("sort_order"), rs.getInt("sort_order") == 0), productId);
    }

    public void replace(Long productId, List<String> imageUrls) {
        jdbc.update("DELETE FROM product_images WHERE product_id = ?", productId);
        for (int index = 0; index < imageUrls.size(); index++) {
            jdbc.update("""
                    INSERT INTO product_images (product_id, image_url, sort_order)
                    VALUES (?, ?, ?)
                    """, productId, imageUrls.get(index), index);
        }
    }
}
