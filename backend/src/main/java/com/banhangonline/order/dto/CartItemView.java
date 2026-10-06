package com.banhangonline.order.dto;

import java.math.BigDecimal;

public record CartItemView(Long cartItemId, Long productId, String name, String sku, String imageUrl, int quantity,
                           BigDecimal unitPrice, String currency) {}
