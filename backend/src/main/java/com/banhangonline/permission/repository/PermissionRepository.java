package com.banhangonline.permission.repository;

import com.banhangonline.permission.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    List<Permission> findByNameIn(Collection<String> names);
}
