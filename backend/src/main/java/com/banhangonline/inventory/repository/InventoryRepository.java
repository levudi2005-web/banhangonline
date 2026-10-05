package com.banhangonline.inventory.repository;

import com.banhangonline.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    @Query("""
            select i from Inventory i join fetch i.product p join fetch p.category
            where i.store.id = :storeId and i.status = 'ACTIVE'
              and p.status = 'ACTIVE' and i.quantity > i.reservedQuantity
              and i.store.status = 'ACTIVE'
            order by p.name
            """)
    List<Inventory> findAvailableByStoreId(Long storeId);

    @Query("""
            select i from Inventory i join fetch i.product p join fetch p.category
            where i.store.id = :storeId and p.id = :productId
              and i.status = 'ACTIVE' and p.status = 'ACTIVE'
              and i.quantity > i.reservedQuantity and i.store.status = 'ACTIVE'
            """)
    Optional<Inventory> findAvailableByStoreIdAndProductId(Long storeId, Long productId);

    @Query("""
            select i from Inventory i join fetch i.product p join fetch p.category
            where i.store.id = :storeId
            order by p.name
            """)
    List<Inventory> findAllByStoreId(Long storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findByStoreIdAndProductId(Long storeId, Long productId);
    boolean existsByProductIdAndStoreIdNot(Long productId, Long storeId);
}
