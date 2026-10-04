package com.banhangonline.auth.security;

import java.util.Set;

public record AuthPrincipal(Long userId, String username, Set<String> roles) {}
