package com.banhangonline.order.dto;

import java.time.Instant;

public record OrderHistoryView(String status, String note, Instant createdAt) {}
