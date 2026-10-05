package com.banhangonline.store.repository;

import com.banhangonline.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByOwnerIdOrderById(Long ownerId);
    List<Store> findByStatusOrderByName(String status);
    Optional<Store> findByIdAndOwnerId(Long id, Long ownerId);
    boolean existsByOwnerId(Long ownerId);
}
