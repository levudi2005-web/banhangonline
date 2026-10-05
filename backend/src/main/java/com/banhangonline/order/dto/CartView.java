package com.banhangonline.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartView(Long storeId, Long cartId, List<CartItemView> items,
                       BigDecimal subtotal, String currency) {}
