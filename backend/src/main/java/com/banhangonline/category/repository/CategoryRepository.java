package com.banhangonline.category.repository;

import com.banhangonline.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByStatusOrderByName(String status);
    Optional<Category> findByIdAndStatus(Long id, String status);
    boolean existsBySlug(String slug);
}
