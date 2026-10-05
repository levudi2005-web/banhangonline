package com.banhangonline.store.repository;

import com.banhangonline.store.entity.StoreStaffPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreStaffPermissionRepository extends JpaRepository<StoreStaffPermission, Long> {
    List<StoreStaffPermission> findByMembershipIdOrderByPermissionName(Long membershipId);
    void deleteByMembershipId(Long membershipId);
    boolean existsByMembershipIdAndPermissionName(Long membershipId, String permissionName);
}
