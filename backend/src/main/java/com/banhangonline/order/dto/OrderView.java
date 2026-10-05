package com.banhangonline.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderView(Long id, String orderCode, Long storeId, String storeName, String customerName, String status,
                        BigDecimal subtotal, BigDecimal totalAmount, String currency, String note,
                        Instant createdAt, List<OrderItemView> items, List<OrderHistoryView> history) {}
