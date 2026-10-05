package com.banhangonline.order.dto;

import java.math.BigDecimal;

public record CartItemView(Long cartItemId, Long productId, String name, String sku, int quantity,
                           BigDecimal unitPrice, String currency) {}
