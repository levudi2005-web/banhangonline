package com.banhangonline.address.entity;

import com.banhangonline.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "user_addresses") @Getter @Setter @NoArgsConstructor
public class UserAddress {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(name = "recipient_name", nullable = false, length = 120) private String recipientName;
    @Column(nullable = false, length = 20) private String phone;
    @Column(nullable = false, length = 100) private String province;
    @Column(nullable = false, length = 100) private String district;
    @Column(nullable = false, length = 100) private String ward;
    @Column(name = "address_line", nullable = false, length = 255) private String addressLine;
    @Column(precision = 10, scale = 7) private BigDecimal latitude;
    @Column(precision = 10, scale = 7) private BigDecimal longitude;
    @Column(name = "is_default", nullable = false) private boolean defaultAddress;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
