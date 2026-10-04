package com.banhangonline.address.repository;

import com.banhangonline.address.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {}
