package com.banhangonline.auth.security;

import com.banhangonline.config.AppProperties;
import com.banhangonline.permission.entity.Permission;
import com.banhangonline.role.entity.Role;
import com.banhangonline.user.entity.User;
import com.banhangonline.auth.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Đọc cookie phiên (HttpOnly), tra bảng sessions và nạp Authentication kèm role + permission. */
public class SessionAuthFilter extends OncePerRequestFilter {
    private final SessionService sessions;
    private final AppProperties props;

    public SessionAuthFilter(SessionService sessions, AppProperties props) {
        this.sessions = sessions;
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        Cookie c = WebUtils.getCookie(req, props.cookie().name());
        if (c != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<User> user = sessions.resolve(c.getValue());
            if (user.isPresent()) {
                User u = user.get();
                List<GrantedAuthority> auths = new ArrayList<>();
                for (Role r : u.getRoles()) {
                    auths.add(new SimpleGrantedAuthority("ROLE_" + r.getName()));
                    for (Permission p : r.getPermissions()) auths.add(new SimpleGrantedAuthority(p.getName()));
                }
                Set<String> roleNames = u.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(new AuthPrincipal(u.getId(), u.getUsername(), roleNames), null, auths));
            }
        }
        chain.doFilter(req, res);
    }
}
