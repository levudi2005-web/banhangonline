package com.banhangonline.store.entity;

import com.banhangonline.permission.entity.Permission;
import com.banhangonline.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "store_staff_permissions",
        uniqueConstraints = @UniqueConstraint(name = "uq_store_staff_permission",
                columnNames = {"store_staff_id", "permission_id"}))
@Getter
@Setter
@NoArgsConstructor
public class StoreStaffPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_staff_id", nullable = false)
    private StoreStaff membership;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "granted_by_user_id", nullable = false)
    private User grantedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
