package com.banhangonline.store.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record ReplaceStaffPermissionsRequest(@NotNull Set<@Size(max = 60) String> permissions) {}
