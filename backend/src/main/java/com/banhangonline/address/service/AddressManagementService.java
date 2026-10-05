package com.banhangonline.address.service;

import com.banhangonline.address.dto.AddressRequest;
import com.banhangonline.address.dto.AddressResponse;
import com.banhangonline.address.entity.UserAddress;
import com.banhangonline.address.repository.UserAddressRepository;
import com.banhangonline.auth.service.Rules;
import com.banhangonline.common.exception.ApiException;
import com.banhangonline.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressManagementService {
    private final UserAddressRepository addresses;
    private final UserRepository users;

    public AddressManagementService(UserAddressRepository addresses, UserRepository users) {
        this.addresses = addresses;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long userId) {
        return addresses.findByUserIdOrderByDefaultAddressDescIdDesc(userId).stream()
                .map(AddressResponse::from).toList();
    }

    @Transactional
    public AddressResponse create(Long userId, AddressRequest request) {
        UserAddress address = new UserAddress();
        address.setUser(users.getReferenceById(userId));
        apply(address, request);
        address.setDefaultAddress(addresses.findByUserIdOrderByDefaultAddressDescIdDesc(userId).isEmpty());
        return AddressResponse.from(addresses.save(address));
    }

    @Transactional
    public AddressResponse update(Long userId, Long addressId, AddressRequest request) {
        UserAddress address = findOwned(userId, addressId);
        apply(address, request);
        return AddressResponse.from(address);
    }

    @Transactional
    public AddressResponse setDefault(Long userId, Long addressId) {
        UserAddress selected = findOwned(userId, addressId);
        for (UserAddress address : addresses.findByUserIdOrderByDefaultAddressDescIdDesc(userId)) {
            address.setDefaultAddress(address.getId().equals(selected.getId()));
        }
        return AddressResponse.from(selected);
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        UserAddress selected = findOwned(userId, addressId);
        boolean wasDefault = selected.isDefaultAddress();
        addresses.delete(selected);
        if (wasDefault) {
            List<UserAddress> remaining = addresses.findByUserIdOrderByDefaultAddressDescIdDesc(userId);
            if (!remaining.isEmpty()) {
                remaining.getFirst().setDefaultAddress(true);
            }
        }
    }

    private UserAddress findOwned(Long userId, Long addressId) {
        return addresses.findByIdAndUserId(addressId, userId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "ADDRESS_NOT_FOUND", "Không tìm thấy địa chỉ"));
    }

    private void apply(UserAddress address, AddressRequest request) {
        address.setRecipientName(request.recipientName().trim());
        address.setPhone(Rules.phone(request.phone()));
        address.setProvince(request.province().trim());
        address.setDistrict(request.district().trim());
        address.setWard(request.ward().trim());
        address.setAddressLine(request.addressLine().trim());
    }
}
