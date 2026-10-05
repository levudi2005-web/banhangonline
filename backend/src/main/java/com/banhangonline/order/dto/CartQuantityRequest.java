package com.banhangonline.order.dto;

import jakarta.validation.constraints.Min;

public record CartQuantityRequest(@Min(1) int quantity) {}
