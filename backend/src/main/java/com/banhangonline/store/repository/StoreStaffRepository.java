package com.banhangonline.store.repository;

import com.banhangonline.store.entity.StoreStaff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreStaffRepository extends JpaRepository<StoreStaff, Long> {
    List<StoreStaff> findByStoreIdOrderById(Long storeId);
    List<StoreStaff> findByUserIdAndStatusOrderByStoreId(Long userId, String status);
    Optional<StoreStaff> findByIdAndStoreId(Long id, Long storeId);
    Optional<StoreStaff> findByStoreIdAndUserId(Long storeId, Long userId);
}
