package com.banhangonline.store.service;

import com.banhangonline.auth.service.Rules;
import com.banhangonline.common.exception.ApiException;
import com.banhangonline.store.dto.CreateStoreRequest;
import com.banhangonline.store.dto.StoreResponse;
import com.banhangonline.store.entity.Store;
import com.banhangonline.store.repository.StoreRepository;
import com.banhangonline.user.entity.User;
import com.banhangonline.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StoreManagementService {
    private final StoreRepository stores;
    private final UserRepository users;

    public StoreManagementService(StoreRepository stores, UserRepository users) {
        this.stores = stores;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> listOwned(Long ownerId) {
        return stores.findByOwnerIdOrderById(ownerId).stream().map(StoreResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> listActive() {
        return stores.findByStatusOrderByName("ACTIVE").stream().map(StoreResponse::from).toList();
    }

    @Transactional
    public StoreResponse create(Long ownerId, CreateStoreRequest request) {
        User owner = users.findById(ownerId).orElseThrow(() ->
                new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Phiên đăng nhập không hợp lệ"));
        if (!owner.hasRole("OWNER")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "OWNER_REQUIRED", "Chỉ chủ cửa hàng mới được tạo cửa hàng");
        }
        if (stores.existsByOwnerId(ownerId)) {
            throw ApiException.conflict("STORE_ALREADY_EXISTS", "Cửa hàng đã được khai báo cho tài khoản này");
        }
        Store store = new Store();
        store.setOwner(owner);
        apply(store, request);
        store.setStatus("DRAFT");
        return StoreResponse.from(stores.save(store));
    }

    @Transactional
    public StoreResponse updateDraft(Long storeId, Long ownerId, CreateStoreRequest request) {
        Store store = stores.findByIdAndOwnerId(storeId, ownerId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "STORE_NOT_FOUND", "Không tìm thấy cửa hàng"));
        if (!"DRAFT".equals(store.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "STORE_NOT_EDITABLE",
                    "Chỉ có thể sửa thông tin khi cửa hàng đang ở trạng thái nháp");
        }
        apply(store, request);
        return StoreResponse.from(store);
    }

    @Transactional
    public StoreResponse submitForReview(Long storeId, Long ownerId) {
        Store store = stores.findByIdAndOwnerId(storeId, ownerId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "STORE_NOT_FOUND", "Không tìm thấy cửa hàng"));
        if (!"DRAFT".equals(store.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "STORE_NOT_DRAFT",
                    "Chỉ có thể gửi duyệt cửa hàng đang ở trạng thái nháp");
        }
        store.setStatus("PENDING_REVIEW");
        return StoreResponse.from(store);
    }

    @Transactional(readOnly = true)
    public Store requireOwnedActive(Long storeId, Long ownerId) {
        Store store = stores.findByIdAndOwnerId(storeId, ownerId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "STORE_NOT_FOUND", "Không tìm thấy cửa hàng"));
        if (!"ACTIVE".equals(store.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "STORE_NOT_ACTIVE",
                    "Cửa hàng chưa được duyệt hoạt động");
        }
        return store;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void apply(Store store, CreateStoreRequest request) {
        store.setName(request.name().trim());
        store.setPhone(Rules.phone(request.phone()));
        store.setEmail(request.email() == null || request.email().isBlank() ? null : Rules.email(request.email()));
        store.setProvince(request.province().trim());
        store.setDistrict(request.district().trim());
        store.setWard(request.ward().trim());
        store.setAddressDetail(request.addressDetail().trim());
        store.setPostalCode(clean(request.postalCode()));
        store.setDescription(clean(request.description()));
    }
}
