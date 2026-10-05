package com.banhangonline.store.controller;

import com.banhangonline.common.response.ApiResponse;
import com.banhangonline.store.dto.StoreResponse;
import com.banhangonline.store.service.StoreManagementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
public class StoreDirectoryController {
    private final StoreManagementService stores;

    public StoreDirectoryController(StoreManagementService stores) {
        this.stores = stores;
    }

    @GetMapping
    public ApiResponse<List<StoreResponse>> listActive() {
        return ApiResponse.ok("Danh sách cửa hàng đang hoạt động", stores.listActive());
    }
}
