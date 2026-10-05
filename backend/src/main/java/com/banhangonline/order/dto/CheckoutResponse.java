package com.banhangonline.order.dto;

public record CheckoutResponse(OrderView order, String pickupCode) {}
