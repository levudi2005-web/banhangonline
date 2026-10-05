package com.banhangonline.order.dto;

import java.math.BigDecimal;

public record OrderItemView(Long productId, String productName, String sku, int quantity,
                            BigDecimal unitPrice, BigDecimal lineTotal, String currency) {}
